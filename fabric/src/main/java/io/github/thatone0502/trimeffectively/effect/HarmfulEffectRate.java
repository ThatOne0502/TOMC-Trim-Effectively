package io.github.thatone0502.trimeffectively.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import io.github.thatone0502.trimeffectively.mixin.MobEffectInstanceAccessor;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code trimeffectively:harmful_effect_rate} — scale how fast harmful effects count down on the wearer.
 *
 * <p>An authoring value of {@code 2.0} means "+200%": the effect ticks down {@code 1 + amount} times per
 * game tick, i.e. three times as fast. Vanilla has no per-effect countdown modifier, so the extra ticks
 * are removed from the remaining duration right after {@code LivingEntity#tickEffects} has done its own
 * countdown; fractional remainders accumulate per entity so a non-integer rate still averages out.
 */
public final class HarmfulEffectRate {
	public static final String TYPE = "harmful_effect_rate";
	private static final Map<UUID, Double> REMAINDER = new ConcurrentHashMap<>();

	public static void tickEffects(LivingEntity entity) {
		List<TrimEffects.Active> actives = DamageEffects.effectives(entity, DamageEffects.TRIGGER_EQUIPPED, TYPE);
		if (actives.isEmpty()) {
			REMAINDER.remove(entity.getUUID());
			return;
		}
		Map<String, Double> variables = TrimEffects.variables(entity);
		double extra = 0.0D;
		for (TrimEffects.Active active : actives) {
			extra += DamageEffects.amountOf(active, variables);
		}
		if (extra <= 0.0D) {
			REMAINDER.remove(entity.getUUID());
			return;
		}
		double accumulator = REMAINDER.getOrDefault(entity.getUUID(), 0.0D) + extra;
		int whole = (int) Math.floor(accumulator);
		REMAINDER.put(entity.getUUID(), accumulator - whole);
		if (whole <= 0) {
			return;
		}
		for (MobEffectInstance instance : new ArrayList<>(entity.getActiveEffects())) {
			if (instance.isInfiniteDuration()) {
				continue;
			}
			// "Non-positive" rather than "harmful": neutral effects such as Bad Omen are not beneficial
			// either, and the table's wording ("每种非正面效果") includes them.
			if (instance.getEffect().value().getCategory() == MobEffectCategory.BENEFICIAL) {
				continue;
			}
			// Endphase's remaining time *is* its damage, and a stare is supposed to hold it still. Speeding up that
			// countdown would silently undo the hold, so it stands down for the ticks where the hold applies.
			if (instance.getEffect().value() == EndphaseEffect.INSTANCE && EndphaseEffect.isHeld(entity)) {
				continue;
			}
			int remaining = instance.getDuration() - whole;
			if (remaining <= 0) {
				entity.removeEffect(instance.getEffect());
			} else {
				((MobEffectInstanceAccessor) instance).trimeffectively$setDuration(remaining);
			}
		}
	}

	private HarmfulEffectRate() {
	}
}
