package com.example.biomeatmosphere;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/** Клиентский мод: атмосфера по биомам. Сервер ему не нужен. */
@Mod(value = BiomeAtmosphere.MODID, dist = Dist.CLIENT)
public class BiomeAtmosphere {
    public static final String MODID = "biomeatmosphere";

    public BiomeAtmosphere(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modBus.addListener(ClientEvents::onRegisterBlockColors);
        modBus.addListener(ClientEvents::onRegisterGuiLayers);

        NeoForge.EVENT_BUS.addListener(ClientEvents::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onRenderFog);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onFogColor);
        NeoForge.EVENT_BUS.addListener(ClientEvents::onLoggingOut);
    }
}
