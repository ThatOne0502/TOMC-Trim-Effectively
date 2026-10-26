package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.EndphaseEffect;
import io.github.thatone0502.trimeffectively.effect.MobEffectApplyEffect;
import io.github.thatone0502.trimeffectively.effect.SummonEffect;
import io.github.thatone0502.trimeffectively.effect.ThornsEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The same {@code actuallyHurt} hooks as {@link LivingEntityMixin}, for players.
 *
 * <p>{@code Player#actuallyHurt} is a full reimplementation - it redoes the armour and absorption maths
 * and never calls {@code super} - so an injection on {@code LivingEntity#actuallyHurt} simply does not
 * run for a player whose health is being reduced. Everything that reacts to damage on the player
 * (endphase accumulation, the thorns reflection, summons, on-hit mob effects) has to be hooked on the
 * player's own copy of the method as well. The game dispatches {@code actuallyHurt} virtually, so
 * exactly one of the two copies runs per hit and there is no double application.
 */
@Mixin(Player.class)
public abstract class PlayerActuallyHurtMixin {
	@Unique
	private float trimeffectively$playerHealthBefore;

	@ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
	private DamageSource trimeffectively$endphaseDamageSource(DamageSource source) {
		LivingEntity self = (LivingEntity) (Object) this;
		if (!(self.level() instanceof ServerLevel level) || !EndphaseEffect.has(self)) {
			return source;
		}
		if (source.is(EndphaseEffect.DAMAGE_TYPE_KEY)) {
			return source;
		}
		return EndphaseEffect.source(level, source.getDirectEntity(), source.getEntity());
	}

	@ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
	private float trimeffectively$endphaseAccumulate(float amount) {
		EndphaseEffect.onDamageTaken((LivingEntity) (Object) this, amount);
		return amount;
	}

	@Inject(method = "actuallyHurt", at = @At("HEAD"))
	private void trimeffectively$healthBefore(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
		trimeffectively$playerHealthBefore = ((LivingEntity) (Object) this).getHealth();
	}

	@Inject(method = "actuallyHurt", at = @At("TAIL"))
	private void trimeffectively$afterHurt(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		float taken = trimeffectively$playerHealthBefore - self.getHealth();
		ThornsEffect.afterDamage(self, level, source, taken);
		SummonEffect.afterDamage(self, level, source, amount);
		MobEffectApplyEffect.afterDamage(level, self, source, amount);
	}
}
