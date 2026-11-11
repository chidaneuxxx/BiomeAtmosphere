package com.example.biomeatmosphere;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

/** Таблица профилей и определение профиля по биому (по имени и тегам, работает и с модовыми биомами). */
public final class Profiles {
    //                                      id             fog    mist   sky       water     gradeRGB  gradeA  day                     night                    rate
    public static final Profile DEFAULT   = new Profile("default",     1.00F, 0.25F, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0.000F, MoteKind.DUST,         null,                    0.5F);
    public static final Profile PLAINS    = new Profile("plains",      1.00F, 0.40F, 0xFFFFFF, 0xFFFFFF, 0xFFE9B8, 0.020F, MoteKind.POLLEN,       MoteKind.FIREFLY,        0.5F);
    public static final Profile FOREST    = new Profile("forest",      0.88F, 0.80F, 0xE9F7E4, 0xE2F6EE, 0xCFE8A8, 0.030F, MoteKind.POLLEN,       MoteKind.FIREFLY,        0.8F);
    public static final Profile DARK_FOREST = new Profile("dark_forest", 0.68F, 0.90F, 0xCFDCCB, 0xC9DFD6, 0x6F8A5A, 0.050F, MoteKind.SPORE,        MoteKind.FIREFLY,        0.7F);
    public static final Profile TAIGA     = new Profile("taiga",       0.84F, 0.70F, 0xE4EDF7, 0xD9ECF6, 0xBBD3E8, 0.030F, MoteKind.DUST,         null,                    0.4F);
    public static final Profile SNOWY     = new Profile("snowy",       0.76F, 0.60F, 0xE1EDFF, 0xCDE6FF, 0xB8D4FF, 0.040F, MoteKind.SNOW,         MoteKind.SNOW,           1.0F);
    public static final Profile SWAMP     = new Profile("swamp",       0.58F, 1.00F, 0xD8E2B8, 0xA7C79A, 0x8FA050, 0.060F, MoteKind.SPORE,        MoteKind.FIREFLY,        0.9F);
    public static final Profile JUNGLE    = new Profile("jungle",      0.80F, 0.85F, 0xE2F6E0, 0xD3F4E6, 0xB5E08A, 0.040F, MoteKind.POLLEN,       MoteKind.FIREFLY,        0.8F);
    public static final Profile SAVANNA   = new Profile("savanna",     0.97F, 0.20F, 0xFFF1D6, 0xFFF4E2, 0xFFCE8A, 0.040F, MoteKind.DUST,         null,                    0.5F);
    public static final Profile DESERT    = new Profile("desert",      0.94F, 0.05F, 0xFFEFCF, 0xFFF4E0, 0xFFD08A, 0.050F, MoteKind.SAND,         null,                    0.9F);
    public static final Profile MOUNTAIN  = new Profile("mountain",    0.92F, 0.55F, 0xE8F1FF, 0xE4F0FF, 0xC9DAF2, 0.025F, MoteKind.DUST,         null,                    0.4F);
    public static final Profile OCEAN     = new Profile("ocean",       0.93F, 0.45F, 0xE9F4FF, 0xD6EAFF, 0xBFE0FF, 0.025F, null,                  null,                    0.0F);
    public static final Profile WATERSIDE = new Profile("waterside",   0.95F, 0.65F, 0xFFFFFF, 0xEDF8FF, 0xFFFFFF, 0.000F, MoteKind.DUST,         null,                    0.3F);
    public static final Profile MUSHROOM  = new Profile("mushroom",    0.72F, 0.80F, 0xF0DDFF, 0xE8D6FF, 0xC59CFF, 0.050F, MoteKind.SPORE_PURPLE, MoteKind.SPORE_PURPLE,   0.8F);
    public static final Profile CHERRY    = new Profile("cherry",      0.90F, 0.60F, 0xFFEAF2, 0xFFEAF6, 0xFFB8D0, 0.040F, MoteKind.PETAL,        MoteKind.PETAL,          0.8F);

    private static final Map<Biome, Profile> CACHE = new ConcurrentHashMap<>();

    private Profiles() {}

    public static void clearCache() {
        CACHE.clear();
    }

    /** Для мест, где есть только объект Biome (например, ColorResolver воды). */
    public static Profile of(Biome biome) {
        Profile cached = CACHE.get(biome);
        if (cached != null) return cached;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return DEFAULT;
        Holder<Biome> holder = level.registryAccess().registryOrThrow(Registries.BIOME).wrapAsHolder(biome);
        Profile p = classify(holder);
        CACHE.put(biome, p);
        return p;
    }

    public static Profile of(Holder<Biome> biome) {
        Profile cached = CACHE.get(biome.value());
        if (cached != null) return cached;
        Profile p = classify(biome);
        CACHE.put(biome.value(), p);
        return p;
    }

    private static Profile classify(Holder<Biome> biome) {
        String path = biome.unwrapKey().map(k -> k.location().getPath()).orElse("");
        Set<String> t = new HashSet<>(Arrays.asList(path.split("[_/\\-.]")));

        // 1) по названию: ванильные и большинство модовых биомов
        if (any(t, "mushroom")) return MUSHROOM;
        if (any(t, "cherry")) return CHERRY;
        if (any(t, "swamp", "mangrove", "bog", "marsh")) return SWAMP;
        if (t.contains("dark") && t.contains("forest")) return DARK_FOREST;
        if (any(t, "snow", "snowy", "frozen", "ice", "icy", "glacier", "glacial", "tundra", "grove")) return SNOWY;
        if (any(t, "desert", "badlands", "mesa", "dunes")) return DESERT;
        if (any(t, "jungle", "bamboo", "rainforest")) return JUNGLE;
        if (any(t, "plains", "meadow", "sunflower", "prairie")) return PLAINS;

        // 2) по тегам
        if (biome.is(BiomeTags.IS_NETHER) || biome.is(BiomeTags.IS_END)) return DEFAULT;
        if (biome.is(BiomeTags.IS_DEEP_OCEAN) || biome.is(BiomeTags.IS_OCEAN)) return OCEAN;
        if (biome.is(BiomeTags.IS_RIVER) || biome.is(BiomeTags.IS_BEACH)) return WATERSIDE;
        if (biome.is(BiomeTags.IS_TAIGA)) return TAIGA;
        if (biome.is(BiomeTags.IS_JUNGLE)) return JUNGLE;
        if (biome.is(BiomeTags.IS_SAVANNA)) return SAVANNA;
        if (biome.is(BiomeTags.IS_BADLANDS)) return DESERT;
        if (biome.is(BiomeTags.IS_MOUNTAIN) || biome.is(BiomeTags.IS_HILL)) return MOUNTAIN;
        if (biome.is(BiomeTags.IS_FOREST)) return FOREST;

        // 3) по температуре
        float temp = biome.value().getBaseTemperature();
        if (temp <= 0.05F) return SNOWY;
        if (temp >= 1.5F) return DESERT;
        return DEFAULT;
    }

    private static boolean any(Set<String> tokens, String... words) {
        for (String w : words) {
            if (tokens.contains(w)) return true;
        }
        return false;
    }
}
