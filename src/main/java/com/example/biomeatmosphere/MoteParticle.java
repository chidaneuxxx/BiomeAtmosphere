package com.example.biomeatmosphere;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/** Маленькая плавно появляющаяся и затухающая частица. Не зарегистрирована в реестре, поэтому мод остаётся клиентским. */
public class MoteParticle extends TextureSheetParticle {
    private final float baseAlpha;
    private final boolean glow;
    private final float wander;
    private final float phase;

    public MoteParticle(ClientLevel level, double x, double y, double z,
                        TextureAtlasSprite sprite, MoteKind kind, double windX, double windZ) {
        super(level, x, y, z);
        RandomSource r = level.random;

        this.setSprite(sprite);
        this.quadSize = Mth.lerp(r.nextFloat(), kind.sizeMin, kind.sizeMax);
        this.lifetime = Mth.randomBetweenInclusive(r, kind.lifeMin, kind.lifeMax);
        this.gravity = kind.gravity;
        this.friction = 0.96F;
        this.hasPhysics = false;

        float shade = 0.9F + r.nextFloat() * 0.1F;
        this.setColor(kind.r * shade, kind.g * shade, kind.b * shade);
        this.baseAlpha = kind.alpha * (0.7F + r.nextFloat() * 0.3F);
        this.alpha = 0F;
        this.glow = kind.glow;
        this.wander = kind.wander;
        this.phase = r.nextFloat() * 100F;

        this.xd = windX * kind.wind + (r.nextFloat() - 0.5F) * 0.004;
        this.yd = (r.nextFloat() - 0.5F) * 0.002 - (kind.falling ? 0.02 : 0.0);
        this.zd = windZ * kind.wind + (r.nextFloat() - 0.5F) * 0.004;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;

        float t = (float) this.age / (float) this.lifetime;
        float fade = Math.min(1F, Math.min(t / 0.2F, (1F - t) / 0.3F));
        float flicker = glow ? 0.7F + 0.3F * Mth.sin((this.age + phase) * 0.3F) : 1F;
        this.alpha = Mth.clamp(baseAlpha * fade * flicker, 0F, 1F);

        this.xd += (this.random.nextFloat() - 0.5F) * 2F * wander;
        this.yd += (this.random.nextFloat() - 0.5F) * 2F * wander * 0.5F;
        this.zd += (this.random.nextFloat() - 0.5F) * 2F * wander;
    }

    @Override
    public int getLightColor(float partialTick) {
        return glow ? LightTexture.FULL_BRIGHT : super.getLightColor(partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
