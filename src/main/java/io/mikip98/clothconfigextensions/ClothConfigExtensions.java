package io.mikip98.clothconfigextensions;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClothConfigExtensions implements ModInitializer {
	public static final String MOD_ID = "cloth-config-extensions";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Cloth Config Extensions is initializing!");
	}
}
