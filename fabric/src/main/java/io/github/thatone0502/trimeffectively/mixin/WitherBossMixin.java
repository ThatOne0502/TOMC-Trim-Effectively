package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.EndphaseEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Endphase is not immune to the wither.
 *
 * <p>{@code WitherBoss} overrides {@code canBeAffected} (undead entities reject harmful effects), so the
 * check on {@code LivingEntity} never runs for it. This override has to be answered on its own class.
 */
@Mixin(WitherBoss.class)
public abstract class WitherBossMixin {
	@Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
	private void trimeffectively$endphaseIgnoresUndead(MobEffectInstance instance, CallbackInfoReturnable<Boolean> cir) {
		if (instance != null && instance.getEffect().value() == EndphaseEffect.INSTANCE) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}
}
