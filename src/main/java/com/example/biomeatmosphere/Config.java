package com.example.biomeatmosphere;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    private static final String KEY = BiomeAtmosphere.MODID + ".configuration.";

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue FOG_ENABLED;
    public static final ModConfigSpec.DoubleValue FOG_STRENGTH;
    public static final ModConfigSpec.BooleanValue SKY_ENABLED;
    public static final ModConfigSpec.DoubleValue SKY_STRENGTH;
    public static final ModConfigSpec.BooleanValue WATER_ENABLED;
    public static final ModConfigSpec.DoubleValue WATER_STRENGTH;
    public static final ModConfigSpec.BooleanValue PARTICLES_ENABLED;
    public static final ModConfigSpec.DoubleValue PARTICLE_DENSITY;
    public static final ModConfigSpec.BooleanValue GRADING_ENABLED;
    public static final ModConfigSpec.DoubleValue GRADING_STRENGTH;
    public static final ModConfigSpec.BooleanValue DYNAMIC_LIGHTS_ENABLED;
    public static final ModConfigSpec.IntValue DYNAMIC_LIGHTS_MAX;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        FOG_ENABLED = b.translation(KEY + "fogEnabled")
                .comment("Туман по биому, времени суток и погоде (только в Верхнем мире).")
                .define("fogEnabled", true);
        FOG_STRENGTH = b.translation(KEY + "fogStrength")
                .comment("1.0 = как задумано, 0 = ванильный туман, 1.5 = гуще.")
                .defineInRange("fogStrength", 1.0, 0.0, 1.5);

        SKY_ENABLED = b.translation(KEY + "skyEnabled")
                .comment("Оттенок неба и горизонта по биому. Небо тонируется миксином: если сломается, выключи.")
                .define("skyEnabled", true);
        SKY_STRENGTH = b.translation(KEY + "skyStrength")
                .defineInRange("skyStrength", 1.0, 0.0, 1.5);

        WATER_ENABLED = b.translation(KEY + "waterEnabled")
                .comment("Оттенок воды по биому. Чанки перерисуются после F3+A.")
                .define("waterEnabled", true);
        WATER_STRENGTH = b.translation(KEY + "waterStrength")
                .defineInRange("waterStrength", 1.0, 0.0, 1.5);

        PARTICLES_ENABLED = b.translation(KEY + "particlesEnabled")
                .comment("Пыльца, пыль, снег, светлячки, лепестки в воздухе.")
                .define("particlesEnabled", true);
        PARTICLE_DENSITY = b.translation(KEY + "particleDensity")
                .comment("Множитель количества частиц. Настройка частиц в игре тоже учитывается.")
                .defineInRange("particleDensity", 1.0, 0.0, 3.0);

        GRADING_ENABLED = b.translation(KEY + "gradingEnabled")
                .comment("Мягкий цветовой оттенок экрана по времени суток, погоде и биому.")
                .define("gradingEnabled", true);
        GRADING_STRENGTH = b.translation(KEY + "gradingStrength")
                .defineInRange("gradingStrength", 1.0, 0.0, 1.5);

        DYNAMIC_LIGHTS_ENABLED = b.translation(KEY + "dynamicLightsEnabled")
                .comment("Предметы в руках (факел, фонарь, глоустоун, лава и т.д.) светят вокруг игрока.")
                .define("dynamicLightsEnabled", true);
        DYNAMIC_LIGHTS_MAX = b.translation(KEY + "dynamicLightsMaxLevel")
                .comment("Максимальная яркость света от предмета в руке (1..15).")
                .defineInRange("dynamicLightsMaxLevel", 15, 1, 15);

        SPEC = b.build();
    }

    private Config() {}
}
