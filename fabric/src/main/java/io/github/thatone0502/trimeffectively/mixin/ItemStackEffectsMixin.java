package io.github.thatone0502.trimeffectively.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.thatone0502.trimeffectively.effect.ItemPropertyEffect;
import net.minecraft.world.item.ItemStack;

/**
 * {@code item_property: durability_multiplier} — the trimmed item's own max damage is scaled on the way out of
 * {@code getMaxDamage}, so every consumer (durability bar, damage calculation, tooltips) sees the same number
 * and nothing has to be copied onto the stack.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackEffectsMixin {
	@Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
	private void trimeffectively$durabilityMultiplier(CallbackInfoReturnable<Integer> cir) {
		int base = cir.getReturnValueI();
		if (base <= 0) {
			return;
		}
		double multiplier = ItemPropertyEffect.durabilityMultiplier((ItemStack) (Object) this);
		if (multiplier == 1.0D) {
			return;
		}
		cir.setReturnValue(Math.max(1, (int) Math.floor(base * multiplier)));
	}
}
