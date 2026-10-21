package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.EndphaseEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Endphase is exempt from the vanilla immunities that would otherwise make it unreachable on the two bosses.
 *
 * <p>{@code LivingEntity#canBeAffected} refuses any harmful effect on an undead mob, which covers the wither, and
 * mobs can override it further (the ender dragon refuses everything). The design says the effect works on both,
 * so the whole answer is short-circuited for this one effect and every other effect keeps vanilla behaviour.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityCanBeAffectedMixin {

	@Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
	private void trimeffectively$endphaseIsNotImmune(MobEffectInstance instance, CallbackInfoReturnable<Boolean> cir) {
		if (instance != null && EndphaseEffect.holder() != null
				&& instance.getEffect().value() == EndphaseEffect.INSTANCE) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}
}
