package com.example.biomeatmosphere;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec3;

/** Текущее (плавно меняющееся) состояние атмосферы вокруг игрока. Обновляется раз в клиентский тик. */
public final class AtmosphereState {
    /** true, если мы в Верхнем мире (или похожем измерении с небом). */
    public static boolean valid;
    /** Профиль биома, в котором стоит игрок. */
    public static Profile profile = Profiles.DEFAULT;
    /** 0..1: насколько игрок под открытым небом (плавно). */
    public static float exposure = 1F;

    private static float fogDensity = 1F;
    private static float mist = 0.25F;
    private static final float[] SKY = {1F, 1F, 1F};
    private static final float[] WATER = {1F, 1F, 1F};
    private static final float[] GRADE = {1F, 1F, 1F};
    private static float gradeAlpha;
    private static boolean initialized;

    // Ключевые точки цветокоррекции по времени суток (тики 0..24000, 0 = рассвет).
    private static final int[] KEY_T = {0, 1500, 11000, 12500, 14000, 22000, 23500, 24000};
    private static final int[] KEY_RGB = {0xFFC890, 0xFFF2D8, 0xFFF0D0, 0xFF9A5C, 0x5A78C8, 0x5A78C8, 0xFFB070, 0xFFC890};
    private static final float[] KEY_A = {0.06F, 0.00F, 0.02F, 0.09F, 0.09F, 0.08F, 0.08F, 0.06F};

    private AtmosphereState() {}

    public static void reset() {
        valid = false;
        initialized = false;
        profile = Profiles.DEFAULT;
        exposure = 1F;
    }

    public static boolean isOverworldLike(ClientLevel level) {
        DimensionType d = level.dimensionType();
        return d.hasSkyLight() && !d.hasCeiling() && !d.hasFixedTime();
    }

    public static boolean isNight(long dayTime) {
        long t = dayTime % 24000L;
        return t >= 12800L && t <= 23200L;
    }

    public static void tick(ClientLevel level, LocalPlayer player) {
        valid = isOverworldLike(level);
        if (!valid) return;

        Profile target = Profiles.of(level.getBiome(player.blockPosition()));
        profile = target;

        float k = initialized ? 0.02F : 1F; // ~2.5 секунды на плавный переход между биомами
        fogDensity += (target.fogDensity() - fogDensity) * k;
        mist += (target.mist() - mist) * k;
        approach(SKY, target.skyTint(), k);
        approach(WATER, target.waterTint(), k);
        approach(GRADE, target.gradeColor(), k);
        gradeAlpha += (target.gradeAlpha() - gradeAlpha) * k;

        float sky = level.getBrightness(LightLayer.SKY, BlockPos.containing(player.getEyePosition())) / 15F;
        exposure += (sky - exposure) * (initialized ? 0.05F : 1F);
        initialized = true;
    }

    // ---------------------------------------------------------------- туман

    /** Множитель дальности тумана: 1 = ваниль, меньше = гуще. */
    public static float fogFactor(ClientLevel level, float partialTick) {
        float time = timeFog(level.getDayTime());
        float rain = level.getRainLevel(partialTick);
        float thunder = level.getThunderLevel(partialTick);
        float weather = 1F - 0.30F * rain - 0.20F * thunder;

        float f = fogDensity * time * weather;
        f = Mth.lerp(exposure, 1F, f); // под землёй туман не трогаем
        float strength = (float) Config.FOG_STRENGTH.get().doubleValue();
        return Mth.clamp(Mth.lerp(strength, 1F, f), 0.25F, 1F);
    }

    private static float timeFog(long dayTime) {
        float t = (dayTime % 24000L) / 24000F;
        float dawn = bump(t, 0.00F, 0.10F);
        float dusk = bump(t, 0.52F, 0.06F);
        float night = bump(t, 0.75F, 0.18F);
        return 1F - mist * (0.34F * dawn + 0.12F * dusk) - 0.08F * night;
    }

    private static float bump(float t, float center, float halfWidth) {
        float d = Math.abs(t - center);
        d = Math.min(d, 1F - d);
        if (d >= halfWidth) return 0F;
        return 0.5F * (1F + Mth.cos(d / halfWidth * Mth.PI));
    }

    // ---------------------------------------------------------------- цвета

    public static Vec3 tintSky(Vec3 c) {
        if (!valid || !Config.SKY_ENABLED.get()) return c;
        float s = (float) Config.SKY_STRENGTH.get().doubleValue();
        return new Vec3(
                Mth.clamp(c.x * Mth.lerp(s, 1F, SKY[0]), 0.0, 1.0),
                Mth.clamp(c.y * Mth.lerp(s, 1F, SKY[1]), 0.0, 1.0),
                Mth.clamp(c.z * Mth.lerp(s, 1F, SKY[2]), 0.0, 1.0));
    }

    public static void tintFogColor(float[] rgb) {
        float s = (float) Config.SKY_STRENGTH.get().doubleValue() * exposure;
        for (int i = 0; i < 3; i++) {
            rgb[i] = Mth.clamp(rgb[i] * Mth.lerp(s, 1F, SKY[i]), 0F, 1F);
        }
    }

    public static void tintWaterFog(float[] rgb) {
        float s = (float) Config.WATER_STRENGTH.get().doubleValue();
        for (int i = 0; i < 3; i++) {
            rgb[i] = Mth.clamp(rgb[i] * Mth.lerp(s, 1F, WATER[i]), 0F, 1F);
        }
    }

    /** ARGB-оттенок для всего экрана (0 = ничего не рисовать). */
    public static int gradeArgb(ClientLevel level, float partialTick) {
        float strength = (float) Config.GRADING_STRENGTH.get().doubleValue();
        float indoors = Mth.lerp(exposure, 0.35F, 1F);

        float[] d = new float[4];
        over(d, GRADE[0], GRADE[1], GRADE[2], gradeAlpha);

        // время суток
        long t = level.getDayTime() % 24000L;
        int i = 0;
        while (i < KEY_T.length - 2 && t >= KEY_T[i + 1]) i++;
        float f = (t - KEY_T[i]) / (float) (KEY_T[i + 1] - KEY_T[i]);
        int c0 = KEY_RGB[i];
        int c1 = KEY_RGB[i + 1];
        over(d,
                Mth.lerp(f, ((c0 >> 16) & 255) / 255F, ((c1 >> 16) & 255) / 255F),
                Mth.lerp(f, ((c0 >> 8) & 255) / 255F, ((c1 >> 8) & 255) / 255F),
                Mth.lerp(f, (c0 & 255) / 255F, (c1 & 255) / 255F),
                Mth.lerp(f, KEY_A[i], KEY_A[i + 1]));

        // погода: серая дымка
        float rain = level.getRainLevel(partialTick);
        float thunder = level.getThunderLevel(partialTick);
        over(d, 0.54F, 0.57F, 0.60F, 0.10F * rain + 0.07F * thunder);

        float a = Mth.clamp(d[3] * strength * indoors, 0F, 0.22F);
        int ia = Math.round(a * 255F);
        if (ia <= 0) return 0;
        int r = Mth.clamp(Math.round(d[0] * 255F), 0, 255);
        int g = Mth.clamp(Math.round(d[1] * 255F), 0, 255);
        int b = Mth.clamp(Math.round(d[2] * 255F), 0, 255);
        return (ia << 24) | (r << 16) | (g << 8) | b;
    }

    // ---------------------------------------------------------------- ветер

    public static double windX(ClientLevel level) {
        double t = level.getGameTime() * 0.0006;
        return Math.cos(t) * 0.012 + Math.sin(t * 2.3) * 0.004;
    }

    public static double windZ(ClientLevel level) {
        double t = level.getGameTime() * 0.0006;
        return Math.sin(t * 0.8) * 0.012 + Math.cos(t * 1.7) * 0.004;
    }

    // ---------------------------------------------------------------- утилиты

    private static void approach(float[] cur, int rgb, float k) {
        cur[0] += (((rgb >> 16) & 255) / 255F - cur[0]) * k;
        cur[1] += (((rgb >> 8) & 255) / 255F - cur[1]) * k;
        cur[2] += ((rgb & 255) / 255F - cur[2]) * k;
    }

    /** Накладывает цвет с прозрачностью поверх накопленного (обычное альфа-смешивание). */
    private static void over(float[] d, float r, float g, float b, float a) {
        if (a <= 0F) return;
        float outA = a + d[3] * (1F - a);
        d[0] = (r * a + d[0] * d[3] * (1F - a)) / outA;
        d[1] = (g * a + d[1] * d[3] * (1F - a)) / outA;
        d[2] = (b * a + d[2] * d[3] * (1F - a)) / outA;
        d[3] = outA;
    }
}
