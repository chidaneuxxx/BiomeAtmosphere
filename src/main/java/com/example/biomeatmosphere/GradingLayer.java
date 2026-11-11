package com.example.biomeatmosphere;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.multiplayer.ClientLevel;

/** Полупрозрачный цветовой оттенок на весь экран. Рисуется под HUD. */
public final class GradingLayer implements LayeredDraw.Layer {
    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!Config.GRADING_ENABLED.get() || !AtmosphereState.valid) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        int argb = AtmosphereState.gradeArgb(level, deltaTracker.getGameTimeDeltaPartialTick(false));
        if ((argb >>> 24) == 0) return;
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), argb);
    }
}
