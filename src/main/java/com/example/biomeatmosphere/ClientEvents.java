package com.example.biomeatmosphere;

import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FogType;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

final class ClientEvents {
    private ClientEvents() {}

    // ---------------------------------------------------------------- вода

    /** Как ванильный Biome::getWaterColor, но с оттенком профиля биома (учитывает смешивание биомов). */
    private static final ColorResolver WATER_RESOLVER = (biome, x, z) -> {
        int base = biome.getWaterColor();
        if (!Config.WATER_ENABLED.get()) return base;
        return multiplyRgb(base, Profiles.of(biome).waterTint(), (float) Config.WATER_STRENGTH.get().doubleValue());
    };

    static void onRegisterBlockColors(RegisterColorHandlersEvent.Block event) {
        BlockColor water = (state, level, pos, tintIndex) ->
                level != null && pos != null ? level.getBlockTint(pos, WATER_RESOLVER) : -1;
        event.register(water, Blocks.WATER, Blocks.BUBBLE_COLUMN);
    }

    private static int multiplyRgb(int rgb, int mul, float strength) {
        float mr = Mth.lerp(strength, 1F, ((mul >> 16) & 255) / 255F);
        float mg = Mth.lerp(strength, 1F, ((mul >> 8) & 255) / 255F);
        float mb = Mth.lerp(strength, 1F, (mul & 255) / 255F);
        int r = Mth.clamp(Math.round(((rgb >> 16) & 255) * mr), 0, 255);
        int g = Mth.clamp(Math.round(((rgb >> 8) & 255) * mg), 0, 255);
        int b = Mth.clamp(Math.round((rgb & 255) * mb), 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    // ---------------------------------------------------------------- цветокоррекция

    static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelowAll(
                ResourceLocation.fromNamespaceAndPath(BiomeAtmosphere.MODID, "grading"),
                new GradingLayer());
    }

    // ---------------------------------------------------------------- тик

    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            AtmosphereState.reset();
            HeldLight.reset();
            return;
        }
        if (mc.isPaused()) return;

        AtmosphereState.tick(level, player);
        if (AtmosphereState.valid && Config.PARTICLES_ENABLED.get()) {
            AmbientParticles.tick(level, player);
        }
        HeldLight.tick(level, player);
    }

    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        AtmosphereState.reset();
        HeldLight.reset();
        Profiles.clearCache();
    }

    // ---------------------------------------------------------------- туман

    static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!Config.FOG_ENABLED.get() || !AtmosphereState.valid) return;
        if (event.getType() != FogType.NONE) return;
        if (event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) return;

        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        if (event.getCamera().getEntity() instanceof LivingEntity living
                && (living.hasEffect(MobEffects.BLINDNESS) || living.hasEffect(MobEffects.DARKNESS))) {
            return; // не мешаем ванильным эффектам
        }

        float far = event.getFarPlaneDistance();
        if (far < 32F) return;

        float factor = AtmosphereState.fogFactor(level, (float) event.getPartialTick());
        if (factor > 0.985F) return; // почти ваниль: ничего не меняем

        float newFar = Math.max(24F, far * factor);
        float nearRatio = Mth.clamp(0.9F - (1F - factor) * 1.2F, 0.3F, 0.9F);
        event.setFarPlaneDistance(newFar);
        event.setNearPlaneDistance(newFar * nearRatio);
        event.setCanceled(true); // без отмены NeoForge игнорирует наши значения
    }

    static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!AtmosphereState.valid) return;

        float[] rgb = {event.getRed(), event.getGreen(), event.getBlue()};
        FogType type = event.getCamera().getFluidInCamera();
        if (type == FogType.NONE && Config.SKY_ENABLED.get()) {
            AtmosphereState.tintFogColor(rgb);
        } else if (type == FogType.WATER && Config.WATER_ENABLED.get()) {
            AtmosphereState.tintWaterFog(rgb);
        } else {
            return;
        }
        event.setRed(rgb[0]);
        event.setGreen(rgb[1]);
        event.setBlue(rgb[2]);
    }
}
