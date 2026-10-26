package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.TradePriceEffect;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trade_price}: rewrites the offer prices at the end of {@code Villager#updateSpecialPrices}.
 *
 * <p>That method is the one vanilla runs from {@code startTrading}, after the reputation and
 * hero-of-the-village discounts and before the offers are sent to the player, so the tail of it is the
 * last moment the diff can be set and still reach the client. Hooking the send instead would leave the
 * already-serialised packet untouched.
 */
@Mixin(Villager.class)
public abstract class VillagerTradeMixin {
	@Inject(method = "updateSpecialPrices", at = @At("TAIL"))
	private void trimeffectively$tradePrice(Player player, CallbackInfo ci) {
		if (player == null) {
			return;
		}
		MerchantOffers offers = ((Villager) (Object) this).getOffers();
		TradePriceEffect.applyTo(offers, player);
	}
}
