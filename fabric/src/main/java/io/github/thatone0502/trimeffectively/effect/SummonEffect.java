package io.github.thatone0502.trimeffectively.effect;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.thatone0502.trimeffectively.Trimeffectively;
import io.github.thatone0502.trimeffectively.data.EffectEntry;
import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * {@code trimeffectively:summon} — spawn {@code count} copies of {@code entity} for {@code lifetime} seconds.
 *
 * <p>Fixed semantics (not configurable, per the design): a summon never drops loot or experience and
 * vanishes the moment its lifetime runs out.
 *
 * <p>Lifetime is expressed in seconds in data and converted to game ticks here. The expiry table is
 * checked from {@code Entity#tick} instead of a server-tick scan so nothing has to iterate every entity
 * on a timer; a map lookup per entity tick is cheaper than a full scan every tick.
 */
public final class SummonEffect {
	public static final String TYPE = "summon";
	/** Marker tag so the loot/experience suppression applies to exactly our summons. */
	public static final String TAG = "trimeffectively:summon";
	private static final List<EquipmentSlot> EQUIPMENT_SLOTS = List.of(
			EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
			EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
	private static final Map<UUID, Long> EXPIRY = new ConcurrentHashMap<>();

	public static void afterDamage(LivingEntity victim, ServerLevel level, DamageSource source, float amount) {
		List<TrimEffects.Active> actives = DamageEffects.effectives(victim, DamageEffects.TRIGGER_AFTER, TYPE);
		if (actives.isEmpty()) {
			return;
		}
		LivingEntity attacker = DamageEffects.attackerOf(victim, source);
		Map<String, Double> variables = DamageVars.forVictim(victim, attacker, amount);
		for (TrimEffects.Active active : actives) {
			EffectEntry entry = active.entry();
			if (entry.entity() == null) {
				continue;
			}
			double count = entry.count() == null ? 1.0D : entry.count().eval(active.pieces(), variables);
			if (count <= 0.0D) {
				continue;
			}
			Optional<Holder.Reference<EntityType<?>>> type = BuiltInRegistries.ENTITY_TYPE.get(entry.entity());
			if (type.isEmpty()) {
				Trimeffectively.LOGGER.warn("summon effect references unknown entity type {}", entry.entity());
				continue;
			}
			int summons = (int) Math.floor(count);
			double seconds = entry.lifetime() == null ? 0.0D : entry.lifetime().eval(active.pieces(), variables);
			for (int i = 0; i < summons; i++) {
				spawn(type.get().value(), level, victim, attacker, entry, seconds);
			}
		}
	}

	private static void spawn(EntityType<?> type, ServerLevel level, LivingEntity victim, LivingEntity attacker,
			EffectEntry entry, double seconds) {
		Entity entity = type.create(level, EntitySpawnReason.MOB_SUMMONED);
		if (entity == null) {
			return;
		}
		entity.snapTo(victim.getX(), victim.getY(), victim.getZ(), victim.getYRot(), 0.0F);
		entity.addTag(TAG);
		if (entity instanceof LivingEntity summon) {
			if (entry.inheritEquipment()) {
				for (EquipmentSlot slot : EQUIPMENT_SLOTS) {
					if (victim.getItemBySlot(slot).isEmpty()) {
						continue;
					}
					summon.setItemSlot(slot, victim.getItemBySlot(slot).copy());
				}
			}
		}
		if (entity instanceof Mob mob) {
			mob.setPersistenceRequired();
			LivingEntity target = attackTarget(entry.attackTarget(), victim, attacker, mob);
			if (target != null && target != mob) {
				mob.setTarget(target);
			}
		}
		if (seconds > 0.0D) {
			EXPIRY.put(entity.getUUID(), level.getGameTime() + Math.round(seconds * 20.0D));
		}
		level.addFreshEntity(entity);
	}

	private static LivingEntity attackTarget(String attackTarget, LivingEntity victim, LivingEntity attacker,
			Mob summon) {
		if (attackTarget == null) {
			return null;
		}
		return switch (attackTarget) {
			case "attacker" -> attacker;
			case "victim" -> victim;
			case "self" -> summon;
			default -> null;
		};
	}

	/** Called from {@code Entity#tick} for every entity; only tagged summons with an expiry are touched. */
	public static void tickEntity(Entity entity) {
		if (EXPIRY.isEmpty() || entity.level().isClientSide()) {
			return;
		}
		if (!entity.getTags().contains(TAG)) {
			return;
		}
		Long expiry = EXPIRY.get(entity.getUUID());
		if (expiry != null && entity.level().getGameTime() >= expiry) {
			EXPIRY.remove(entity.getUUID());
			entity.discard();
		}
	}

	private SummonEffect() {
	}
}
