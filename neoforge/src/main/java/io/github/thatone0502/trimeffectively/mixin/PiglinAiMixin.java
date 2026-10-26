package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.GoldRegard;
import io.github.thatone0502.trimeffectively.effect.PiglinNeutralEffect;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code piglin_neutral}, both halves of "a gold trim makes the item gold".
 *
 * <p>{@code isLovedItem} is the item-level question - it drives the piglin's attraction to a dropped
 * stack, its pickup goal and its admiration of a held one - so gold-trimmed items become
 * {@code #piglin_loved} for every piglin regardless of who carries them.
 *
 * <p>{@code isWearingSafeArmor} is the entity-level question, asked by the piglin sensor that chooses
 * the nearest enemy; granting it keeps the wearer out of every piglin's target search rather than only
 * calming piglins that are already angry.
 */
@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin {
	@Inject(method = "isLovedItem", at = @At("HEAD"), cancellable = true)
	private static void trimeffectively$lovedItem(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (GoldRegard.hasGoldTrim(stack)) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}

	@Inject(method = "isWearingSafeArmor", at = @At("HEAD"), cancellable = true)
	private static void trimeffectively$piglinNeutral(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (PiglinNeutralEffect.neutral(entity)) {
			cir.setReturnValue(Boolean.TRUE);
		}
	}
}
