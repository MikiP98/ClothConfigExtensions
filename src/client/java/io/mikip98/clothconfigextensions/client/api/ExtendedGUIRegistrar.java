package io.mikip98.clothconfigextensions.client.api;

import io.mikip98.clothconfigextensions.client.registries.ProviderRegistry;
import io.mikip98.clothconfigextensions.client.registries.TransformerRegistry;
#if MC_VERSION < 260000 import me.shedaniel.autoconfig.AutoConfig; #endif
#if MC_VERSION >= 260000 import me.shedaniel.autoconfig.AutoConfigClient; #endif
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.gui.registry.GuiRegistry;

public class ExtendedGUIRegistrar {
    public static <T extends ConfigData> void register(Class<T> configClass) {
        GuiRegistry registry = #if MC_VERSION < 260000 AutoConfig #else AutoConfigClient #endif
                .getGuiRegistry(configClass);

        ProviderRegistry.registerCustomProviders(registry);
        TransformerRegistry.registerCustomTransformers(registry);
    }
}
