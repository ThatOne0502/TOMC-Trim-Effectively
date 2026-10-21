package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.EndphaseEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Endphase is not immune to the ender dragon.
 *
 * <p>The dragon rejects every effect by overriding {@code addEffect}, before {@code canBeAffected} is
 * ever consulted, so the only place to answer is that override. The instance is installed with
 * {@code forceAddEffect}, the same call the game uses when an effect must land regardless of immunity.
 */
@Mixin(EnderDragon.class)
public abstract class EnderDragonMixin {
	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
			at = @At("HEAD"), cancellable = true)
	private void trimeffectively$endphaseIgnoresDragon(MobEffectInstance instance, Entity source,
			CallbackInfoReturnable<Boolean> cir) {
		if (instance == null || instance.getEffect().value() != EndphaseEffect.INSTANCE) {
			return;
		}
		((LivingEntity) (Object) this).forceAddEffect(instance, source);
		cir.setReturnValue(Boolean.TRUE);
	}
}
