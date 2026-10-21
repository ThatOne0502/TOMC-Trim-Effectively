package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;

/**
 * {@code trimeffectively:xp_gain} - fractional change to the experience a living entity is awarded.
 *
 * <p>Applied in {@code Player#giveExperiencePoints}, the single method every source of experience funnels
 * through (mining, smelting, mob kills, trading, mending, bottles o' enchanting); scaling there means the
 * change also feeds level ups, since they are computed from the points added by that method.
 *
 * <p>Only players have experience, so this is the one type in the slice that is inherently player-only.
 * A value of 1.0 doubles the experience; the result is floored at zero because a negative award would
 * take experience away through a method that is only meant to add it.
 */
public final class XpGainEffect {
	public static final String TYPE = "xp_gain";
	public static final String TRIGGER_XP_GAIN = "xp_gain";

	public static int scale(LivingEntity wearer, int xp) {
		if (wearer == null || xp <= 0) {
			return xp;
		}
		List<TrimEffects.Active> actives = DamageEffects.effectives(wearer, TRIGGER_XP_GAIN, TYPE);
		if (actives.isEmpty()) {
			return xp;
		}
		Map<String, Double> variables = TrimEffects.variables(wearer);
		double multiplier = 1.0D;
		for (TrimEffects.Active active : actives) {
			multiplier += DamageEffects.amountOf(active, variables);
		}
		if (multiplier == 1.0D) {
			return xp;
		}
		double scaled = xp * multiplier;
		if (!(scaled > 0.0D)) {
			return 0;
		}
		if (scaled >= Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) Math.round(scaled);
	}

	private XpGainEffect() {
	}
}
