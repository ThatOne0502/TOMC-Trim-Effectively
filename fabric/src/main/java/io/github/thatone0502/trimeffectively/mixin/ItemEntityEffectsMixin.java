package io.github.thatone0502.trimeffectively.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.thatone0502.trimeffectively.effect.ItemPropertyEffect;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * {@code item_property: fire_resistant} — the drop itself is immune to fire and lava instead of the wearer,
 * which is what the data (netherite) describes: the item survives, not the player. {@code ItemEntity} is the
 * right target because it overrides {@code fireImmune} and it is the only place where a dropped stack has an
 * identity of its own.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityEffectsMixin {
	@Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
	private void trimeffectively$fireResistant(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ()) {
			return;
		}
		if (ItemPropertyEffect.fireResistant(((ItemEntity) (Object) this).getItem())) {
			cir.setReturnValue(true);
		}
	}
}
