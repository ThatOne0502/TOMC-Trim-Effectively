package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.EndphaseEffect;
import io.github.thatone0502.trimeffectively.effect.ThornsEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Living entity hooks: the endphase effect, and the {@code thorns} retaliation.
 *
 * <p>{@code actuallyHurt} is entered after armour, enchantments and absorption have already been
 * applied, which is exactly the moment the effect wants: the damage type is rewritten there so the
 * killing blow carries {@code trimeffectively:endphase} and the custom death message fires, while
 * protection maths still sees the original elemental type.
 *
 * <p>For {@code thorns} the same point is used from both ends: health is sampled on entry and compared
 * on exit, so the reflected fraction applies to the health the victim actually lost instead of to the
 * nominal amount vanilla was handed.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Unique
	private float trimeffectively$healthBefore;

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

	@Inject(method = "heal", at = @At("HEAD"))
	private void trimeffectively$endphaseOverflowHeal(float amount, CallbackInfo ci) {
		EndphaseEffect.onHealed((LivingEntity) (Object) this, amount);
	}

	@Inject(method = "actuallyHurt", at = @At("HEAD"))
	private void trimeffectively$healthBefore(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
		trimeffectively$healthBefore = ((LivingEntity) (Object) this).getHealth();
	}

	@Inject(method = "actuallyHurt", at = @At("TAIL"))
	private void trimeffectively$thorns(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
		LivingEntity self = (LivingEntity) (Object) this;
		float taken = trimeffectively$healthBefore - self.getHealth();
		ThornsEffect.afterDamage(self, level, source, taken);
	}
}
