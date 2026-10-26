package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Context variables for the damage handlers.
 *
 * <p>{@link TrimEffects#variables(LivingEntity)} already supplies the generic set; a damage event has
 * three extra facts the engine cannot know while it recomputes attributes, so they are added here on
 * top of a fresh copy instead of being faked inside the base map.
 */
public final class DamageVars {
	/** Variables for the entity that is being hit: {@code damage_taken}, plus the hit distance. */
	public static Map<String, Double> forVictim(LivingEntity victim, LivingEntity attacker, float amount) {
		Map<String, Double> variables = new LinkedHashMap<>(TrimEffects.variables(victim));
		variables.put("damage_taken", (double) amount);
		if (attacker != null && attacker != victim) {
			variables.put("attack_distance", distance(attacker, victim));
		}
		return variables;
	}

	/**
	 * Variables for the entity that is hitting: {@code damage_dealt}, the hit distance, the gold stack
	 * count and a harmful-effect count that includes being on fire.
	 *
	 * <p>{@code docs/DATA.md} §4.2 defines {@code harmful_effect_count} as "harmful effects, and
	 * being on fire counts as one"; {@code AttributeEngine} keeps {@code on_fire} as its own variable,
	 * so it is folded in here rather than changing the shared engine.
	 */
	public static Map<String, Double> forAttacker(LivingEntity attacker, LivingEntity victim, float amount) {
		Map<String, Double> variables = new LinkedHashMap<>(TrimEffects.variables(attacker));
		variables.put("damage_dealt", (double) amount);
		if (victim != null && victim != attacker) {
			variables.put("attack_distance", distance(attacker, victim));
		}
		variables.put("harmful_effect_count",
				variables.getOrDefault("harmful_effect_count", 0.0D) + variables.getOrDefault("on_fire", 0.0D));
		variables.put("gold_group_count", goldGroupCount(attacker));
		return variables;
	}

	public static double distance(Entity from, Entity to) {
		return from.position().distanceTo(to.position());
	}

	/** Number of inventory stacks of gold items; piglin-loved is the vanilla definition of "gold". */
	public static double goldGroupCount(LivingEntity entity) {
		if (!(entity instanceof Player player)) {
			return 0.0D;
		}
		Inventory inventory = player.getInventory();
		int count = 0;
		for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
			ItemStack stack = inventory.getItem(slot);
			if (!stack.isEmpty() && stack.is(ItemTags.PIGLIN_LOVED)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * Vanilla's critical-hit condition, which lives inline in {@code Player#attack} and has no
	 * accessor: a falling attacker that is in the air, not climbing, not wet, not riding and not blind.
	 */
	public static boolean isCrit(LivingEntity attacker) {
		return attacker.fallDistance > 0.0D
				&& !attacker.onGround()
				&& !attacker.onClimbable()
				&& !attacker.isInWater()
				&& !attacker.isPassenger()
				&& !attacker.hasEffect(MobEffects.BLINDNESS);
	}

	private DamageVars() {
	}
}
