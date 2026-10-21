package io.mikip98.clothconfigextensions.client.registries;

import io.mikip98.clothconfigextensions.api.annotaions.ConfigExtension;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.BooleanToggleBuilder;
import me.shedaniel.clothconfig2.impl.builders.EnumSelectorBuilder;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.mikip98.clothconfigextensions.ClothConfigExtensions.LOGGER;

public class ProviderRegistry {
    @ApiStatus.Internal
    public static void registerCustomProviders(GuiRegistry registry) {
        registerEnumBooleanMapProvider(registry);
        registerEnumEnumMapProvider(registry);
        registerFloatRangeStringFieldProvider(registry);
        registerIntRangeStringFieldProvider(registry);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static void registerEnumBooleanMapProvider(GuiRegistry registry) {
        registry.registerPredicateProvider(
                (i13n, field, config, defaults, guiProvider) -> {
                    ConfigEntryBuilder builder = ConfigEntryBuilder.create();
                    List<AbstractConfigListEntry<?>> entries = new ArrayList<>();

                    try {
                        // Extract the specific Enum class using reflection
                        ParameterizedType pType = (ParameterizedType) field.getGenericType();

                        Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) pType.getActualTypeArguments()[0];

                        // Grab the actual map and default state map
                        Map<Enum<?>, Boolean> map = (Map<Enum<?>, Boolean>) field.get(config);
                        Map<Enum<?>, Boolean> defaultMap = (Map<Enum<?>, Boolean>) field.get(defaults);

                        for (Enum<?> enumValue : enumClass.getEnumConstants()) {
                            String translationKey = i13n + "." + enumValue.name().toLowerCase();

                            boolean defaultValue = defaultMap == null || !defaultMap.containsKey(enumValue) || defaultMap.get(enumValue);

                            BooleanToggleBuilder toggleBuilder = builder.startBooleanToggle(
                                            Component.translatable(translationKey),
                                            map.getOrDefault(enumValue, true)
                                    )
                                    .setDefaultValue(defaultValue)
                                    .setSaveConsumer(newValue -> map.put(enumValue, newValue));

                            boolean hasTooltip = field.isAnnotationPresent(ConfigEntry.Gui.Tooltip.class) || Util.getGlobalTooltip(field) != null;
                            if (hasTooltip) {
                                toggleBuilder.setTooltip(Component.translatable(translationKey + ".@Tooltip"));
                            }

                            entries.add(toggleBuilder.build());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error: {};\nStacktrace: {}", e, e.getStackTrace());
                    }

                    return (List<AbstractConfigListEntry>) (Object) entries;
                },
                field -> {
                    if (Map.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType pType) {
                        Type[] typeArgs = pType.getActualTypeArguments();
                        if (typeArgs.length == 2) {
                            boolean isEnumKey = typeArgs[0] instanceof Class<?> clazz && clazz.isEnum();
                            boolean isBoolValue = typeArgs[1] == Boolean.class;
                            return isEnumKey && isBoolValue;
                        }
                    }
                    return false;
                }
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static void registerEnumEnumMapProvider(GuiRegistry registry) {
        registry.registerPredicateProvider(
                (i13n, field, config, defaults, guiProvider) -> {
                    ConfigEntryBuilder builder = ConfigEntryBuilder.create();
                    List<AbstractConfigListEntry<?>> entries = new ArrayList<>();
                    try {
                        ParameterizedType pType = (ParameterizedType) field.getGenericType();
                        Class keyEnumClass = (Class) pType.getActualTypeArguments()[0];
                        Class valueEnumClass = (Class) pType.getActualTypeArguments()[1];

                        Map map = (Map) field.get(config);
                        Map defaultMap = (Map) field.get(defaults);

                        for (Object keyObj : keyEnumClass.getEnumConstants()) {
                            Enum keyEnum = (Enum) keyObj;
                            String translationKey = i13n + "." + keyEnum.name().toLowerCase();
                            Enum defaultValue = (defaultMap != null && defaultMap.containsKey(keyEnum))
                                    ? (Enum) defaultMap.get(keyEnum) : (Enum) valueEnumClass.getEnumConstants()[0];
                            Enum currentValue = (Enum) map.getOrDefault(keyEnum, defaultValue);

                            EnumSelectorBuilder enumBuilder = builder.startEnumSelector(
                                            Component.translatable(translationKey),
                                            valueEnumClass,
                                            currentValue
                                    )
                                    .setDefaultValue(defaultValue)
                                    .setSaveConsumer(newValue -> map.put(keyEnum, newValue));

                            if (field.isAnnotationPresent(ConfigEntry.Gui.Tooltip.class) || Util.getGlobalTooltip(field) != null) {
                                enumBuilder.setTooltip(Component.translatable(translationKey + ".@Tooltip"));
                            }
                            entries.add(enumBuilder.build());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error: {};\nStacktrace: {}", e, e.getStackTrace());
                    }
                    return (List<AbstractConfigListEntry>) (Object) entries;
                },
                field -> {
                    if (Map.class.isAssignableFrom(field.getType()) && field.getGenericType() instanceof ParameterizedType pType) {
                        Type[] typeArgs = pType.getActualTypeArguments();
                        if (typeArgs.length == 2) {
                            return typeArgs[0] instanceof Class<?> c1 && c1.isEnum() &&
                                    typeArgs[1] instanceof Class<?> c2 && c2.isEnum();
                        }
                    }
                    return false;
                }
        );
    }

    // TODO: Consider replacing the two below with one that checks for field type?

    protected static void registerFloatRangeStringFieldProvider(GuiRegistry registry) {
        registry.registerAnnotationProvider(
                (i13n, field, config, defaults, guiProvider) -> {
                    ConfigExtension.Gui.FloatLimit bounds = field.getAnnotation(ConfigExtension.Gui.FloatLimit.class);
                    float min = bounds.min();
                    float max = bounds.max();

                    try {
                        float currentValue = field.getFloat(config);
                        float defaultValue = field.getFloat(defaults);

                        var floatBuilder = ConfigEntryBuilder.create()
                                .startFloatField(Component.translatable(i13n), currentValue)
                                .setDefaultValue(defaultValue)
                                .setMin(min)
                                .setMax(max)
                                .setSaveConsumer(newValue -> {
                                    try {
                                        field.setFloat(config, newValue);
                                    } catch (IllegalAccessException e) {
                                        LOGGER.error("Error, could not set the value of the FloatRange field: {};\nStacktrace: {}", e, e.getStackTrace());
                                    }
                                });

                        return Collections.singletonList(floatBuilder.build());

                    } catch (Exception e) {
                        LOGGER.error("Error (returning empty FloatRange): {};\nStacktrace: {}", e, e.getStackTrace());
                        return Collections.emptyList();
                    }
                },
                field -> field.getType() == float.class,
                ConfigExtension.Gui.FloatLimit.class
        );
    }

    protected static void registerIntRangeStringFieldProvider(GuiRegistry registry) {
        registry.registerAnnotationProvider(
                (i13n, field, config, defaults, guiProvider) -> {
                    ConfigExtension.Gui.IntLimit bounds = field.getAnnotation(ConfigExtension.Gui.IntLimit.class);
                    int min = bounds.min();
                    int max = bounds.max();

                    try {
                        int currentValue = field.getInt(config);
                        int defaultValue = field.getInt(defaults);

                        var intBuilder = ConfigEntryBuilder.create()
                                .startIntField(Component.translatable(i13n), currentValue)
                                .setDefaultValue(defaultValue)
                                .setMin(min)
                                .setMax(max)
                                .setSaveConsumer(newValue -> {
                                    try {
                                        field.setInt(config, newValue);
                                    } catch (IllegalAccessException e) {
                                        LOGGER.error("Error, could not set the value of the IntRange field: {};\nStacktrace: {}", e, e.getStackTrace());
                                    }
                                });

                        return Collections.singletonList(intBuilder.build());

                    } catch (Exception e) {
                        LOGGER.error("Error (returning empty IntRange): {};\nStacktrace: {}", e, e.getStackTrace());
                        return Collections.emptyList();
                    }
                },
                field -> field.getType() == int.class,
                ConfigExtension.Gui.IntLimit.class
        );
    }
}
