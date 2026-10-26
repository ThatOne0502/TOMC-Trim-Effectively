package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;

/**
 * {@code trimeffectively:damage_out} - extra damage the wearer deals, as a fraction of the hit.
 *
 * <p>Read at {@code attack_out}, i.e. while the wearer's own strike is being resolved, so the effect
 * scales the damage the wearer is about to inflict rather than the damage taken.
 */
public final class DamageOutEffect {
	public static final String TYPE = DamageEffects.TYPE_DAMAGE_OUT;

	/** Fractional bonus for this hit; zero when no trim contributes. */
	public static double fraction(LivingEntity attacker, LivingEntity victim, float amount) {
		List<TrimEffects.Active> actives = DamageEffects.effectives(attacker, DamageEffects.TRIGGER_ATTACK_OUT, TYPE);
		if (actives.isEmpty()) {
			return 0.0D;
		}
		Map<String, Double> variables = DamageVars.forAttacker(attacker, victim, amount);
		double total = 0.0D;
		for (TrimEffects.Active active : actives) {
			total += DamageEffects.amountOf(active, variables);
		}
		return total;
	}

	private DamageOutEffect() {
	}
}
