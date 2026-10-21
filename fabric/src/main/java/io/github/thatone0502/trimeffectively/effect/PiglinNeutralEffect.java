package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * {@code trimeffectively:piglin_neutral} - piglins treat the wearer as if it were wearing gold.
 *
 * <p>A gold-material trim makes the trimmed item count as gold (see {@link GoldRegard}), which the
 * engine exposes as {@link #neutral(LivingEntity)}: the wearer is neutral to piglins whenever an armour
 * slot holds something {@link GoldRegard#isGolden} accepts. Vanilla decides whether a piglin considers a
 * target at all in {@code PiglinAi#isWearingSafeArmor}, called from the piglin sensor that picks its
 * nearest enemy, so answering that check removes the wearer from every piglin's target search instead of
 * only pacifying already-angry ones.
 *
 * <p>The entry carries no value: being treated as safe is a flag, so the piece count does not scale it.
 * {@code also_brutes} adds the stricter {@link GoldRegard#isGoldenTrimmedGold(ItemStack)} tier, which
 * only an item that was already {@code #piglin_loved} in vanilla and then trimmed with gold satisfies -
 * brutes normally ignore gold armour entirely, which is why the field defaults to false.
 */
public final class PiglinNeutralEffect {
	public static final String TYPE = "piglin_neutral";
	public static final String TRIGGER_EQUIPPED = "equipped";

	/** True for any entity wearing a golden piece while carrying a matching entry. */
	public static boolean neutral(LivingEntity entity) {
		if (entity == null || !GoldRegard.wearsGolden(entity)) {
			return false;
		}
		return !DamageEffects.effectives(entity, TRIGGER_EQUIPPED, TYPE).isEmpty();
	}

	/** True when a matching entry asks for piglin brutes too and the stricter gold tier is worn. */
	public static boolean coversBrutes(LivingEntity entity) {
		if (entity == null || !GoldRegard.wearsGoldenTrimmedGold(entity)) {
			return false;
		}
		List<TrimEffects.Active> actives = DamageEffects.effectives(entity, TRIGGER_EQUIPPED, TYPE);
		for (TrimEffects.Active active : actives) {
			if (active.entry().alsoBrutes()) {
				return true;
			}
		}
		return false;
	}

	private PiglinNeutralEffect() {
	}
}
