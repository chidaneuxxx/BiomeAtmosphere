package com.example.biomeatmosphere;

/** Виды частиц в воздухе. Размер quadSize: реальная ширина квадрата = 2 * quadSize блоков. */
public enum MoteKind {
    //            r      g      b     size0   size1  life0 life1  gravity  wind  wander   alpha  glow   ground falling
    POLLEN      (1.00F, 0.93F, 0.55F, 0.025F, 0.040F, 100, 200,  0.000F, 0.5F, 0.0025F, 0.80F, false, true,  false),
    DUST        (1.00F, 0.98F, 0.90F, 0.020F, 0.032F, 100, 220,  0.000F, 0.4F, 0.0020F, 0.60F, false, false, false),
    SAND        (0.90F, 0.78F, 0.55F, 0.030F, 0.048F,  70, 140,  0.000F, 1.6F, 0.0030F, 0.65F, false, true,  false),
    SNOW        (0.96F, 0.98F, 1.00F, 0.030F, 0.050F, 140, 260,  0.050F, 0.8F, 0.0020F, 0.90F, false, false, true),
    SPORE       (0.62F, 0.80F, 0.38F, 0.024F, 0.040F, 120, 240, -0.004F, 0.3F, 0.0030F, 0.60F, false, true,  false),
    FIREFLY     (0.78F, 1.00F, 0.35F, 0.035F, 0.055F, 120, 260,  0.000F, 0.2F, 0.0040F, 0.95F, true,  true,  false),
    SPORE_PURPLE(0.78F, 0.55F, 0.95F, 0.025F, 0.045F, 120, 240, -0.004F, 0.3F, 0.0030F, 0.80F, true,  true,  false),
    PETAL       (1.00F, 0.72F, 0.82F, 0.045F, 0.065F, 140, 260,  0.035F, 1.0F, 0.0030F, 0.90F, false, false, true);

    public final float r, g, b;
    public final float sizeMin, sizeMax;
    public final int lifeMin, lifeMax;
    public final float gravity;
    public final float wind;
    public final float wander;
    public final float alpha;
    public final boolean glow;
    /** Появляется у земли (светлячки, пыльца). */
    public final boolean groundBound;
    /** Появляется сверху и падает (снег, лепестки). */
    public final boolean falling;

    MoteKind(float r, float g, float b, float sizeMin, float sizeMax, int lifeMin, int lifeMax,
             float gravity, float wind, float wander, float alpha, boolean glow,
             boolean groundBound, boolean falling) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.sizeMin = sizeMin;
        this.sizeMax = sizeMax;
        this.lifeMin = lifeMin;
        this.lifeMax = lifeMax;
        this.gravity = gravity;
        this.wind = wind;
        this.wander = wander;
        this.alpha = alpha;
        this.glow = glow;
        this.groundBound = groundBound;
        this.falling = falling;
    }
}
