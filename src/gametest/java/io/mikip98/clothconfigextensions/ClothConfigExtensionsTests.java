package io.mikip98.clothconfigextensions;

import io.mikip98.clothconfigextensions.api.CommentedTomlSerializer;
import io.mikip98.clothconfigextensions.client.api.ExtendedGUIRegistrar;
import io.mikip98.clothconfigextensions.test_config.ModConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClothConfigExtensionsTests implements ModInitializer, ClientModInitializer {
    public static final String MOD_ID = "cloth-config-extensions-tests";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Cloth Config Extensions Tests are being initialized!");
        AutoConfig.register(ModConfig.class, CommentedTomlSerializer::new);
        ModConfig.INSTANCE = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void onInitializeClient() {
        ExtendedGUIRegistrar.register(ModConfig.class);
    }
}
