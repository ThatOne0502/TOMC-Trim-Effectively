package io.github.thatone0502.trimeffectively.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.thatone0502.trimeffectively.effect.ExtraJumps;
import io.github.thatone0502.trimeffectively.effect.HarmfulEffectRate;
import io.github.thatone0502.trimeffectively.effect.InvulnerabilityWindow;
import io.github.thatone0502.trimeffectively.effect.MobEffectApplyEffect;
import io.github.thatone0502.trimeffectively.effect.SummonEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Damage-timing, effect-ticking and jump hooks for the trim effect types that act on a living entity.
 *
 * <p>Every entry here is placed at the point where the vanilla behaviour has already happened, so the
 * effect reacts to real outcomes (damage that actually landed, effects that actually ticked, a hurt that
 * actually registered) instead of predicting them.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectsMixin {
	@Inject(method = "actuallyHurt", at = @At("TAIL"))
	private void trimeffectively$afterHurt(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		SummonEffect.afterDamage(self, level, source, amount);
		MobEffectApplyEffect.afterDamage(level, self, source, amount);
	}

	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void trimeffectively$invulnerabilityWindow(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			InvulnerabilityWindow.afterHurt((LivingEntity) (Object) this, source);
		}
	}

	@Inject(method = "tickEffects", at = @At("TAIL"))
	private void trimeffectively$harmfulEffectRate(CallbackInfo ci) {
		HarmfulEffectRate.tickEffects((LivingEntity) (Object) this);
	}

	@Inject(method = "aiStep", at = @At("TAIL"))
	private void trimeffectively$extraJumps(CallbackInfo ci) {
		ExtraJumps.aiStep((LivingEntity) (Object) this);
	}

	@Inject(method = "dropAllDeathLoot", at = @At("HEAD"), cancellable = true)
	private void trimeffectively$summonLoot(ServerLevel level, DamageSource source, CallbackInfo ci) {
		if (((LivingEntity) (Object) this).entityTags().contains(SummonEffect.TAG)) {
			ci.cancel();
		}
	}

	@Inject(method = "dropExperience", at = @At("HEAD"), cancellable = true)
	private void trimeffectively$summonExperience(ServerLevel level, Entity killer, CallbackInfo ci) {
		if (((LivingEntity) (Object) this).entityTags().contains(SummonEffect.TAG)) {
			ci.cancel();
		}
	}
}
