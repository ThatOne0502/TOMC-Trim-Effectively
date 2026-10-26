package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.Map;

/**
 * {@code trimeffectively:trade_price} - fractional change to the prices a trader asks the wearer.
 *
 * <p>There is no event for "a trade menu opened", so the offers are rewritten in
 * {@code Villager#updateSpecialPrices}, the private method vanilla runs immediately before it sends the
 * offer list to the player (and the same place it applies reputation and hero-of-the-village discounts).
 * Rewriting there means the discount is already inside the packet the client receives, so the prices on
 * screen are the prices being charged.
 *
 * <p>The lever is {@code MerchantOffer#addToSpecialPriceDiff}: {@code MerchantOffer#getCostA} is
 * {@code clamp(base + demandBonus + specialPriceDiff, 1, maxStackSize)}, so a negative diff makes the
 * trade cheaper and the diff travels to the client with the offer. The diff is applied to the cost as
 * it stands when the player opens the menu, which is exactly what vanilla's own reputation discount
 * does; {@code Villager#stopTrading} clears every diff again, so nothing accumulates across sessions.
 *
 * <p>Only the primary cost is adjusted: {@code costB} has no diff of its own in the vanilla offer
 * format. A value of -0.10 asks for ten percent less; the result is never allowed below one item,
 * because the vanilla clamp would show one item while the diff would keep growing.
 */
public final class TradePriceEffect {
	public static final String TYPE = "trade_price";
	public static final String TRIGGER_TRADE = "trade";

	/** Applies the summed fraction of every matching entry to the primary cost of each offer. */
	public static void applyTo(MerchantOffers offers, LivingEntity wearer) {
		if (wearer == null || offers == null || offers.isEmpty()) {
			return;
		}
		List<TrimEffects.Active> actives = DamageEffects.effectives(wearer, TRIGGER_TRADE, TYPE);
		if (actives.isEmpty()) {
			return;
		}
		Map<String, Double> variables = TrimEffects.variables(wearer);
		double fraction = 0.0D;
		for (TrimEffects.Active active : actives) {
			fraction += DamageEffects.amountOf(active, variables);
		}
		if (fraction == 0.0D) {
			return;
		}
		double multiplier = 1.0D + fraction;
		for (MerchantOffer offer : offers) {
			if (offer == null) {
				continue;
			}
			int current = offer.getCostA().getCount();
			if (current <= 0) {
				continue;
			}
			double scaled = current * multiplier;
			int target = scaled >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.round(scaled);
			if (target < 1) {
				target = 1;
			}
			int delta = target - current;
			if (delta != 0) {
				offer.addToSpecialPriceDiff(delta);
			}
		}
	}

	private TradePriceEffect() {
	}
}
