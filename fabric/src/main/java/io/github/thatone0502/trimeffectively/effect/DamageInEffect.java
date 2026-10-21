package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.Map;

/**
 * {@code trimeffectively:damage_in} - damage the wearer takes, as a fraction; negative reduces.
 *
 * <p>Read at {@code take_damage_before}, while the incoming hit is still being resolved and before
 * armour, so a reduction and a protection enchantment stack multiplicatively on top of each other.
 *
 * <p>This is the mod's answer to vanilla's resistance effect, which lives in
 * {@code LivingEntity#getDamageAfterMagicAbsorb} as {@code amount * (25 - 5 * level) / 25} floored at zero.
 * Two of that method's guards are reproduced here, because we run a layer earlier: damage tagged
 * {@link DamageTypeTags#BYPASSES_EFFECTS} and {@link DamageTypeTags#BYPASSES_RESISTANCE} is left alone
 * entirely. Without them a 100% reduction would also swallow void and {@code /kill} damage, which vanilla
 * never lets any amount of resistance touch.
 */
public final class DamageInEffect {
	public static final String TYPE = DamageEffects.TYPE_DAMAGE_IN;

	/** Fractional modifier for this hit; zero when no trim contributes. */
	public static double fraction(LivingEntity victim, LivingEntity attacker, DamageSource source, float amount) {
		if (source.is(DamageTypeTags.BYPASSES_EFFECTS) || source.is(DamageTypeTags.BYPASSES_RESISTANCE)) {
			return 0.0D;
		}
		List<TrimEffects.Active> actives = DamageEffects.effectives(victim, DamageEffects.TRIGGER_BEFORE, TYPE, source);
		if (actives.isEmpty()) {
			return 0.0D;
		}
		Map<String, Double> variables = DamageVars.forVictim(victim, attacker, amount);
		double total = 0.0D;
		for (TrimEffects.Active active : actives) {
			total += DamageEffects.amountOf(active, variables);
		}
		return total;
	}

	private DamageInEffect() {
	}
}
