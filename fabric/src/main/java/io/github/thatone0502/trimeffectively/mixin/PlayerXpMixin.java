package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.XpGainEffect;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * {@code xp_gain}: scales the amount {@code Player#giveExperiencePoints} awards.
 *
 * <p>Every experience source ends in that method, and it also feeds the level-up check, so scaling the
 * argument keeps the levels consistent with the points. Non-positive amounts are passed through: a
 * "gain" that removes experience is not what the schema describes.
 */
@Mixin(Player.class)
public abstract class PlayerXpMixin {
	@ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
	private int trimeffectively$xpGain(int amount) {
		if (amount <= 0) {
			return amount;
		}
		return XpGainEffect.scale((Player) (Object) this, amount);
	}
}
