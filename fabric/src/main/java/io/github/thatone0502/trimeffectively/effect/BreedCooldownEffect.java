package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;

/**
 * {@code trimeffectively:breed_cooldown} - fractional change to the breeding cooldown the wearer causes.
 *
 * <p>Applied in {@code Animal#finalizeSpawnChildFromBreeding}, where vanilla sets both parents' age to
 * the breeding cooldown ({@code PARENT_AGE_AFTER_BREEDING}) right after the child is created. Scaling
 * the value vanilla just wrote, instead of guessing the constant, keeps the effect correct for animals
 * that override the cooldown and for the second parent, which is scaled with the same factor.
 *
 * <p>The breeder is {@code Animal#getLoveCause}, the player that fed the animals. It has to be read
 * before vanilla calls {@code resetLove} (which clears it), so the value is sampled on entry and used
 * after the ages have been written. A value of -0.20 makes the parents ready 20% sooner.
 *
 * <p>The scaled cooldown is floored at zero: a negative value would turn the parent into a baby.
 */
public final class BreedCooldownEffect {
	public static final String TYPE = "breed_cooldown";
	public static final String TRIGGER_BREED = "breed";

	public static int scale(LivingEntity breeder, int ticks) {
		if (breeder == null || ticks <= 0) {
			return ticks;
		}
		List<TrimEffects.Active> actives = DamageEffects.effectives(breeder, TRIGGER_BREED, TYPE);
		if (actives.isEmpty()) {
			return ticks;
		}
		Map<String, Double> variables = TrimEffects.variables(breeder);
		double multiplier = 1.0D;
		for (TrimEffects.Active active : actives) {
			multiplier += DamageEffects.amountOf(active, variables);
		}
		if (multiplier == 1.0D) {
			return ticks;
		}
		double scaled = ticks * multiplier;
		if (!(scaled > 0.0D)) {
			return 0;
		}
		if (scaled >= Integer.MAX_VALUE) {
			return Integer.MAX_VALUE;
		}
		return (int) Math.round(scaled);
	}

	private BreedCooldownEffect() {
	}
}
