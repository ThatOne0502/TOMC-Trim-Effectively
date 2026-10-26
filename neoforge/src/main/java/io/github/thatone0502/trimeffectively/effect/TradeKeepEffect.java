package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;

/**
 * {@code trimeffectively:trade_keep} - chance that a trade does not consume one of the offer's uses.
 *
 * <p>The roll happens in {@code MerchantResultSlot#onTake}, the only place where the trading player and
 * the offer are both in scope and where a trade is known to have gone through: the result slot is the
 * single funnel every completed trade passes, both for villagers and for wandering traders. One use is
 * put back afterwards, which is what "the trade did not count" means for the offer.
 *
 * <p>{@code chance} is summed over the matching entries (so the per-piece stacking of the schema applies
 * the same way it does for amounts) and then clamped to [0,1], as the schema specifies for this effect.
 */
public final class TradeKeepEffect {
	public static final String TYPE = "trade_keep";
	public static final String TRIGGER_TRADE = "trade";

	/** Probability in [0,1] that a completed trade leaves the offer's use count untouched. */
	public static double probability(LivingEntity wearer) {
		if (wearer == null) {
			return 0.0D;
		}
		List<TrimEffects.Active> actives = DamageEffects.effectives(wearer, TRIGGER_TRADE, TYPE);
		if (actives.isEmpty()) {
			return 0.0D;
		}
		Map<String, Double> variables = TrimEffects.variables(wearer);
		double total = 0.0D;
		for (TrimEffects.Active active : actives) {
			if (active.entry().chance() == null) {
				continue;
			}
			total += active.entry().chance().eval(active.pieces(), variables);
		}
		if (!(total > 0.0D)) {
			return 0.0D;
		}
		return Math.min(total, 1.0D);
	}

	/** One roll against {@link #probability}; false whenever nothing is worn for this type. */
	public static boolean rolls(LivingEntity wearer) {
		double probability = probability(wearer);
		if (probability <= 0.0D) {
			return false;
		}
		if (probability >= 1.0D) {
			return true;
		}
		return wearer.getRandom().nextDouble() < probability;
	}

	private TradeKeepEffect() {
	}
}
