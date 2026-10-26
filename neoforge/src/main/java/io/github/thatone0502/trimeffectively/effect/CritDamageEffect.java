package io.github.thatone0502.trimeffectively.effect;

import net.minecraft.world.entity.LivingEntity;

/**
 * Critical-hit damage, expressed as the {@code trimeffectively:crit_multiplier} attribute.
 *
 * <p>The vanilla critical multiplier is 1.5 and is applied inside {@code Player#attack} before the hit
 * reaches any damage hook, so it cannot be replaced - but it can be scaled. The attribute therefore
 * carries the <em>total</em> multiplier (1.5 by default, exactly vanilla), and this class only reports
 * the difference relative to 1.5: an attribute of 1.59 turns every critical hit into 1.59x instead of
 * 1.5x, which is what the {@code snout} entry asks for when it adds 1% per stack of gold carried.
 *
 * <p>Being an attribute, the value is visible through {@code /attribute}, stacks with other modifiers
 * and can be raised beyond trim data by any other mod.
 */
public final class CritDamageEffect {
	/** Kept because the data type still exists; the numbers now live in the attribute. */
	public static final String TYPE = DamageEffects.TYPE_CRIT_DAMAGE;

	/** Extra fraction relative to what vanilla already multiplied in; zero on a non-critical hit. */
	public static double fraction(LivingEntity attacker, LivingEntity victim, float amount) {
		if (!DamageVars.isCrit(attacker)) {
			return 0.0D;
		}
		return TreAttributes.critMultiplier(attacker) / TreAttributes.CRIT_MULTIPLIER_DEFAULT - 1.0D;
	}

	private CritDamageEffect() {
	}
}
