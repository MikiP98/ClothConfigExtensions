package io.mikip98.clothconfigextensions.api;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mikip98.clothconfigextensions.api.annotaions.ConfigExtension;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.Toml4jConfigSerializer;
import me.shedaniel.autoconfig.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;

import static io.mikip98.clothconfigextensions.ClothConfigExtensions.LOGGER;

public class CommentedTomlSerializer<T extends ConfigData> extends Toml4jConfigSerializer<T> {
    protected final Config definition;
    protected final Class<T> configClass;
    protected final String modId;

    public CommentedTomlSerializer(Config definition, Class<T> configClass) {
        this(definition, configClass, definition.name().toLowerCase());
    }
    public CommentedTomlSerializer(Config definition, Class<T> configClass, String modId) {
        super(definition, configClass);
        this.definition = definition;
        this.configClass = configClass;
        this.modId = modId;
    }

    // Copy of private getConfigPath() in Toml4jConfigSerializer
    protected Path getConfigPath() {
        return Utils.getConfigFolder().resolve(this.definition.name() + ".toml");
    }

    @Override
    public void serialize(T config) throws SerializationException {
        super.serialize(config);
        langDataCache = new HashMap<>(1);
        injectComments();
        langDataCache = null;
    }

    protected Map<String, JsonObject> langDataCache;
    protected @Nullable JsonObject getLangData(String langFile) {
        if (langDataCache.containsKey(langFile)) return langDataCache.get(langFile);

        final String langPath = "/assets/" + this.modId + "/lang/" + langFile + ".json";
        final InputStream stream = this.configClass.getResourceAsStream(langPath);
        if (stream != null) {
            final JsonObject langData = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)
            ).getAsJsonObject();
            langDataCache.put(langFile, langData);
            return langData;
        } else LOGGER.error("Failed to load lang data from '{}'", langPath);

        return null;
    }

    public void injectComments() throws SerializationException {
        final Path configPath = this.getConfigPath();
        try {
            CommentedFileConfig tomlConfig = CommentedFileConfig.builder(configPath)
                    .sync()
                    .preserveInsertionOrder()
                    .build();

            tomlConfig.load();
            applyComments(this.configClass, tomlConfig, new ArrayList<>());
            tomlConfig.save();
            tomlConfig.close();

        } catch (Exception e) {
            throw new SerializationException(e);
        }
    }

    protected void applyComments(Class<?> clazz, CommentedConfig toml, List<String> path) {
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) continue;

            List<String> currentPath = new ArrayList<>(path);
            currentPath.add(field.getName());

            Class<?> fieldType = field.getType();
            boolean isConfigObject = !fieldType.isEnum() &&
                    (fieldType.getDeclaringClass() != null || fieldType.getName().startsWith(this.configClass.getPackage().getName()));  // TODO: Remove the .startsWith() check? maybe replace with something else
            // TODO: Why check 'fieldType.getDeclaringClass() != null'?

            if (isConfigObject) {
                applyComments(field.getType(), toml, currentPath);
            } else if (Map.class.isAssignableFrom(fieldType)) {
                // TODO: Separate to new method
                final ConfigExtension.File.AutoComment autoComment = tryGetAnnotation(field, ConfigExtension.File.AutoComment.class);
                if (autoComment != null) {
                    final JsonObject langData = getLangData(autoComment.langFile());
                    final Type genericType = field.getGenericType();

                    if (langData != null) {
                        if (
                                genericType instanceof ParameterizedType pType &&
                                        pType.getActualTypeArguments()[0] instanceof Class<?> keyClass &&
                                        keyClass.isEnum()
                        ) {

                            for (Object enumObj : keyClass.getEnumConstants()) {
                                Enum<?> enumVal = (Enum<?>) enumObj;  // TODO: Switch from enum to generic, or add boolean support
                                List<String> keyPath = new ArrayList<>(currentPath);
                                keyPath.add(enumVal.name()); // TOML uses the raw enum name
                                // TODO: How will it add the tooltips to the keys instead of the main map?

                                // Replicate the exact baseKey from ProviderRegistry
                                String customBaseKey = "text.autoconfig." + this.definition.name() + ".option." +
                                        String.join(".", currentPath) + "." + enumVal.name().toLowerCase();

                                String resolved = resolveCommentFromLangFile(field, keyPath, langData, customBaseKey);
                                if (!resolved.isEmpty()) {
                                    toml.setComment(keyPath, resolved);
                                }
                            }
                        }
                    } else {
                        LOGGER.error("Field '{}' won't have any comments cause 'langData' is NULL", field.getName());
                    }
                }
            } else {
                final String resolvedComment = tryResolveComment(field, currentPath);

                if (resolvedComment != null) {
                    if (resolvedComment.isEmpty()) {
                        LOGGER.warn("FAILED to resolve the comment for '{}' field that should have one", field.getName());
                    } else {
                        toml.setComment(currentPath, resolvedComment);
                    }
                }
            }
        }
    }

    protected @Nullable String tryResolveComment(Field field, List<String> currentPath) {
        if (field.isAnnotationPresent(ConfigExtension.File.Comment.class)) {
            return field.getAnnotation(ConfigExtension.File.Comment.class).comment();
        }

        if (field.isAnnotationPresent(ConfigExtension.File.SkipComment.class)) return null;

        final ConfigExtension.File.AutoComment autoComment = tryGetAnnotation(field, ConfigExtension.File.AutoComment.class);
        if (autoComment != null) {
            final JsonObject langData = getLangData(autoComment.langFile());
            if (langData != null) {
                return resolveCommentFromLangFile(field, currentPath, langData, autoComment.translationsString());
            } else {
                LOGGER.error("Field '{}' won't have any comments cause 'langData' is NULL", field.getName());
                return "";
            }
        }

        return null;
    }

    protected static <T extends Annotation> T tryGetAnnotation(Field field, Class<T> clazz) {
        if (field.isAnnotationPresent(clazz)) return field.getAnnotation(clazz);

        Class<?> currentClass = field.getDeclaringClass();
        while (currentClass != null) {
            if (currentClass.isAnnotationPresent(clazz)) return currentClass.getAnnotation(clazz);
            currentClass = currentClass.getEnclosingClass();
        }

        return null;
    }

    protected @NotNull String resolveCommentFromLangFile(Field field, List<String> currentPath, JsonObject lang, String customKey) {
        // TODO: Fix the Maps looking for the tooltip in the wrong place

        String baseKey;
        if (customKey.isEmpty()) {
            // AutoConfig format: text.autoconfig.configName.option.path.to.field
            final String configName = this.definition.name();
            final String fieldPath = String.join(".", currentPath);
            baseKey = "text.autoconfig." + configName + ".option." + fieldPath;
        } else {
            baseKey = customKey;
        }

        int tooltipCount = 1;

        final ConfigEntry.Gui.Tooltip tooltipAnnotation = tryGetAnnotation(field, ConfigEntry.Gui.Tooltip.class);
        if (tooltipAnnotation != null) {
            tooltipCount = tooltipAnnotation.count();
        } else {
            final ConfigExtension.Gui.GlobalTooltip globalTooltipAnnotation = tryGetAnnotation(
                    field, ConfigExtension.Gui.GlobalTooltip.class
            );
            if (globalTooltipAnnotation != null) tooltipCount = globalTooltipAnnotation.count();
        }

        if (tooltipCount < 0) throw new IllegalArgumentException("Tooltip count can't be negative");

        if (tooltipCount == 1) {
            // Single line tooltip (@Tooltip)
            final String singleTooltipKey = baseKey + ".@Tooltip";
            if (lang.has(singleTooltipKey)) {
                return " " + lang.get(singleTooltipKey).getAsString().replace("\n", "\n ");
            }
        } else {
            // Multi-line tooltip array (@Tooltip[0], @Tooltip[1], etc.)
            final StringJoiner tooltip = new StringJoiner("\n");
            int i = 0;
            while (lang.has(baseKey + ".@Tooltip[" + i + "]")) {
                tooltip.add(lang.get(baseKey + ".@Tooltip[" + i + "]").getAsString());
                i++;
            }
            if (tooltip.length() != 0) return tooltip.toString();
        }

        return "";
    }
}
