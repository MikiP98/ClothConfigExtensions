package io.mikip98.clothconfigextensions.test_config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
#if MC_VERSION < 260000
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
#else
import me.shedaniel.autoconfig.AutoConfigClient;
#endif

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        #if MC_VERSION < 260000
        return parent -> AutoConfig.getConfigScreen(ModConfig.class, parent).get();
        #else
        return parent -> AutoConfigClient.getConfigScreen(ModConfig.class, parent).get();
        #endif
    }
}
