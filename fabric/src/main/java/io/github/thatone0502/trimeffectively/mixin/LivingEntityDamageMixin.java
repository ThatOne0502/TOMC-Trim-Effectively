package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.DamageEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The single damage funnel the six damage effect types share.
 *
 * <p>{@code LivingEntity#hurtServer} is the one concrete method every hit goes through in 1.21.11:
 * {@code Entity#hurt} delegates to it, {@code Entity#hurtOrSimulate} (which {@code Player#attack} calls for
 * melee) delegates to it, and a good deal of vanilla simply calls it directly. Hooking {@code Entity#hurt}
 * covered only the first of those, which is why melee and ranged hits appeared to ignore the trim
 * effects entirely.
 *
 * <p>Cancelling here and re-dispatching with the adjusted {@code (source, amount)} pair keeps every vanilla
 * consequence of the hit - knockback, animations, statistics, the returned boolean - attached to the
 * <i>adjusted</i> hit. The {@link DamageEffects#busy()} guard lets the nested call fall through into the
 * vanilla body exactly once.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamageMixin {
	@Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
	private void trimeffectively$damage(ServerLevel level, DamageSource source, float amount,
			CallbackInfoReturnable<Boolean> cir) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (DamageEffects.busy()) {
			return;
		}
		DamageEffects.Result result = DamageEffects.compute(self, level, source, amount);
		if (!result.changed()) {
			return;
		}
		DamageEffects.push();
		boolean hurt;
		try {
			hurt = self.hurtServer(level, result.source(), result.amount());
		} finally {
			DamageEffects.pop();
		}
		cir.setReturnValue(hurt);
	}
}
