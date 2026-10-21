package io.github.thatone0502.trimeffectively.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.effect.MobEffectInstance;

/** Lets the harmful-effect-rate logic shorten an effect in place instead of rebuilding the instance. */
@Mixin(MobEffectInstance.class)
public interface MobEffectInstanceAccessor {
	@Accessor("duration")
	void trimeffectively$setDuration(int duration);
}