package io.github.thatone0502.trimeffectively.effect;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.thatone0502.trimeffectively.Trimeffectively;
import io.github.thatone0502.trimeffectively.data.EffectEntry;
import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code trimeffectively:apply_mob_effect} — apply a status effect to a target chosen by the data.
 *
 * <p>Evaluated when the wearer's hit lands ({@code attack_out}). {@code duration} is in ticks,
 * {@code chance} is a 0..1 probability, both are {@code Value}s so they scale per piece. Chance is the
 * only field the engine clamps (to 0..1, per the schema contract) — everything else is used as authored.
 */
public final class MobEffectApplyEffect {
	public static final String TYPE = "apply_mob_effect";

	public static void afterDamage(ServerLevel level, LivingEntity victim, DamageSource source, float amount) {
		LivingEntity wearer = DamageEffects.attackerOf(victim, source);
		if (wearer == null) {
			return;
		}
		List<TrimEffects.Active> actives = DamageEffects.effectives(wearer, DamageEffects.TRIGGER_ATTACK_OUT, TYPE, source);
		if (actives.isEmpty()) {
			return;
		}
		Map<String, Double> variables = DamageVars.forAttacker(wearer, victim, amount);
		for (TrimEffects.Active active : actives) {
			EffectEntry entry = active.entry();
			if (entry.effect() == null) {
				continue;
			}
			double chance = entry.chance() == null ? 1.0D : entry.chance().eval(active.pieces(), variables);
			chance = Math.max(0.0D, Math.min(1.0D, chance));
			if (chance <= 0.0D || level.getRandom().nextDouble() >= chance) {
				continue;
			}
			Optional<Holder.Reference<MobEffect>> effect = BuiltInRegistries.MOB_EFFECT.get(entry.effect());
			if (effect.isEmpty()) {
				Trimeffectively.LOGGER.warn("apply_mob_effect references unknown effect {}", entry.effect());
				continue;
			}
			LivingEntity target = target(entry.target(), wearer, victim);
			if (target == null) {
				continue;
			}
			int duration = entry.duration() == null ? 0
					: (int) Math.max(0.0D, entry.duration().eval(active.pieces(), variables));
			int amplifier = entry.amplifier() == null ? 0
					: (int) Math.max(0.0D, entry.amplifier().eval(active.pieces(), variables));
			target.forceAddEffect(new MobEffectInstance(effect.get(), Math.max(duration, remaining(target, effect.get(), duration)),
					Math.max(amplifier, existingAmplifier(target, effect.get()))), wearer);
		}
	}

	private static LivingEntity target(String target, LivingEntity wearer, LivingEntity victim) {		if (target == null) {
			return victim;
		}
		return switch (target) {
			case "victim" -> victim;
			case "attacker", "self" -> wearer;
			default -> victim;
		};
	}

	/** Re-application must never shorten a longer dose the target already carries. */
	private static int remaining(LivingEntity target, Holder<MobEffect> effect, int duration) {
		MobEffectInstance existing = target.getEffect(effect);
		return existing == null ? duration : Math.max(duration, existing.getDuration());
	}

	private static int existingAmplifier(LivingEntity target, Holder<MobEffect> effect) {
		MobEffectInstance existing = target.getEffect(effect);
		return existing == null ? 0 : existing.getAmplifier();
	}

	private MobEffectApplyEffect() {
	}
}
