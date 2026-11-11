package com.example.biomeatmosphere;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LightLayer;

/** Спавн частиц в воздухе вокруг игрока в зависимости от биома, времени суток и погоды. */
final class AmbientParticles {
    private static final ResourceLocation SPRITE =
            ResourceLocation.fromNamespaceAndPath(BiomeAtmosphere.MODID, "mote");

    private AmbientParticles() {}

    static void tick(ClientLevel level, LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        ParticleStatus status = mc.options.particles().get();
        if (status == ParticleStatus.MINIMAL) return;
        if (level.getRainLevel(1F) > 0.3F) return; // в дождь воздух "чистый"

        Profile p = AtmosphereState.profile;
        MoteKind kind = AtmosphereState.isNight(level.getDayTime()) ? p.nightParticles() : p.dayParticles();
        if (kind == null || p.particleRate() <= 0F) return;

        float exposure = AtmosphereState.exposure;
        if (exposure < 0.35F) return; // в пещерах и помещениях не спавним

        float density = (float) Config.PARTICLE_DENSITY.get().doubleValue()
                * (status == ParticleStatus.DECREASED ? 0.5F : 1F);
        float expected = 1.6F * p.particleRate() * density * exposure;

        RandomSource r = level.random;
        int count = (int) expected + (r.nextFloat() < expected - (int) expected ? 1 : 0);
        if (count <= 0) return;

        TextureAtlasSprite sprite = sprite(mc);
        if (sprite == null) return;

        double wx = AtmosphereState.windX(level);
        double wz = AtmosphereState.windZ(level);
        for (int i = 0; i < count; i++) {
            trySpawn(mc, level, player, kind, sprite, wx, wz);
        }
    }

    /** Спрайт берём каждый раз заново: после F3+T атлас пересобирается. */
    private static TextureAtlasSprite sprite(Minecraft mc) {
        AbstractTexture tex = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES);
        if (!(tex instanceof TextureAtlas atlas)) return null;
        TextureAtlasSprite s = atlas.getSprite(SPRITE);
        if (s.contents().name().equals(MissingTextureAtlasSprite.getLocation())) return null;
        return s;
    }

    private static void trySpawn(Minecraft mc, ClientLevel level, LocalPlayer player, MoteKind kind,
                                 TextureAtlasSprite sprite, double wx, double wz) {
        RandomSource r = level.random;
        for (int attempt = 0; attempt < 3; attempt++) {
            double ang = r.nextDouble() * (Math.PI * 2.0);
            double dist = 3.0 + Math.sqrt(r.nextDouble()) * 21.0;
            double x = player.getX() + Math.cos(ang) * dist;
            double z = player.getZ() + Math.sin(ang) * dist;
            double y;

            if (kind.groundBound) {
                int by = Mth.floor(player.getY() + r.nextDouble() * 10.0 - 4.0);
                BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(Mth.floor(x), by, Mth.floor(z));
                if (!level.hasChunkAt(m)) continue;
                int steps = 0;
                while (steps < 6 && level.getBlockState(m).isAir()) {
                    m.move(0, -1, 0);
                    steps++;
                }
                if (steps == 0 || steps >= 6) continue; // внутри блока или земли рядом нет
                y = m.getY() + 1.3 + r.nextDouble() * 1.8;
            } else if (kind.falling) {
                y = player.getY() + 4.0 + r.nextDouble() * 9.0;
            } else {
                y = player.getY() - 2.0 + r.nextDouble() * 9.0;
            }

            BlockPos bp = BlockPos.containing(x, y, z);
            if (!level.hasChunkAt(bp)) continue;
            if (!level.getBlockState(bp).isAir()) continue;
            if (level.getBrightness(LightLayer.SKY, bp) < (kind.groundBound ? 7 : 10)) continue;

            mc.particleEngine.add(new MoteParticle(level, x, y, z, sprite, kind, wx, wz));
            return;
        }
    }
}
