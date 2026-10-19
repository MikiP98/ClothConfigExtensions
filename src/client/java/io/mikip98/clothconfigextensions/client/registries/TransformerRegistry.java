package io.mikip98.clothconfigextensions.client.registries;

import io.mikip98.clothconfigextensions.annotaions.ConfigExtension;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TransformerRegistry {
    @ApiStatus.Internal
    public static void registerCustomTransformers(GuiRegistry registry) {
        registerGlobalTooltipTransformer(registry);
    }

    protected static void registerGlobalTooltipTransformer(GuiRegistry registry) {
        registry.registerPredicateTransformer(
                (guis, i18n, field, config, defaults, guiProvider) -> {
                    if (guis.size() > 1) return guis;  // TODO: Why is this here?

                    return guis.stream()
                            .peek(gui -> {
                                if (!(gui instanceof TextListEntry)) {
                                    ConfigExtension.Gui.GlobalTooltip globalTooltip = Util.getGlobalTooltip(field);

                                    assert globalTooltip != null;
                                    final int count = globalTooltip.count();

                                    if (count == 0) {
                                        tryRemoveTooltip(gui);
                                    } else if (count == 1) {
                                        tryApplyTooltip(
                                                gui,
                                                new Component[]{
                                                        Component.translatable(String.format("%s.%s", i18n, "@Tooltip"))
                                                }
                                        );
                                    } else {
                                        tryApplyTooltip(
                                                gui, IntStream.range(0, count).boxed()
                                                        .map(i -> String.format("%s.%s[%d]", i18n, "@Tooltip", i))
                                                        .map(Component::translatable)
                                                        .toArray(Component[]::new)
                                        );
                                    }
                                }
                            })
                            .collect(Collectors.toList());
                },
                field -> Util.getGlobalTooltip(field) != null && !field.isAnnotationPresent(ConfigExtension.Gui.SkipGlobalTooltip.class) && !field.isAnnotationPresent(ConfigEntry.Gui.Tooltip.class)
        );
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static void tryApplyTooltip(AbstractConfigListEntry gui, Component[] text) {
        if (gui instanceof TooltipListEntry tooltipGui) {
            tooltipGui.setTooltipSupplier(() -> Optional.of(text));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    protected static void tryRemoveTooltip(AbstractConfigListEntry gui) {
        if (gui instanceof TooltipListEntry tooltipGui) {
            tooltipGui.setTooltipSupplier(Optional::empty);
        }
    }
}
