package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;

/**
 * {@code trimeffectively:thorns} - melee retaliation, a fraction of the damage taken back to the attacker.
 *
 * <p>Runs at the tail of {@code actuallyHurt}, where the damage has already been dealt, so the fraction
 * applies to the health actually lost rather than to the nominal hit. Projectile damage is not
 * retaliated against; that is the "melee only" restriction the quartz data expresses through its
 * condition, and enforcing it here keeps the type meaningful even though conditions are not evaluated
 * by the resolver yet.
 *
 * <p>Reflected damage is ordinary damage of type {@code minecraft:thorns}: the attacker's own trims see
 * it, but the reflection itself is not computed again (depth guard), so two thorns wearers cannot bounce
 * damage between them forever.
 */
public final class ThornsEffect {
	public static final String TYPE = DamageEffects.TYPE_THORNS;

	private static int depth;

	/** @param taken health actually removed by the hit, never negative */
	public static void afterDamage(LivingEntity victim, ServerLevel level, DamageSource source, float taken) {
		if (depth > 0 || taken <= 0.0F) {
			return;
		}
		if (source.is(DamageTypeTags.IS_PROJECTILE)) {
			return;
		}
		Entity causing = source.getEntity();
		if (!(causing instanceof LivingEntity attacker) || attacker == victim) {
			return;
		}
		List<TrimEffects.Active> actives = DamageEffects.effectives(victim, DamageEffects.TRIGGER_AFTER, TYPE, source);
		if (actives.isEmpty()) {
			return;
		}
		Map<String, Double> variables = DamageVars.forVictim(victim, attacker, taken);
		double fraction = 0.0D;
		for (TrimEffects.Active active : actives) {
			fraction += DamageEffects.amountOf(active, variables);
		}
		float reflected = (float) (taken * fraction);
		if (!(reflected > 0.0F) || !Float.isFinite(reflected)) {
			return;
		}
		depth++;
		try {
			attacker.hurtServer(level, level.damageSources().thorns(victim), reflected);
		} finally {
			depth--;
		}
	}

	private ThornsEffect() {
	}
}
