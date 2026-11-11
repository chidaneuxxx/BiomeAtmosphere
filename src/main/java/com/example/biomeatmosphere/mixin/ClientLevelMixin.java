package com.example.biomeatmosphere.mixin;

import com.example.biomeatmosphere.AtmosphereState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Единственный миксин: оттенок цвета неба. Если инъекция не найдёт метод, мод просто останется без тонировки неба. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 0)
    private void biomeatmosphere$tintSky(Vec3 pos, float partialTick, CallbackInfoReturnable<Vec3> cir) {
        cir.setReturnValue(AtmosphereState.tintSky(cir.getReturnValue()));
    }
}
