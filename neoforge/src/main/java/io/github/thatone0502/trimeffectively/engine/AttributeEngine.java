package io.github.thatone0502.trimeffectively.engine;

import io.github.thatone0502.trimeffectively.Tre;
import io.github.thatone0502.trimeffectively.data.EffectEntry;
import io.github.thatone0502.trimeffectively.data.TrimEffect;
import io.github.thatone0502.trimeffectively.data.Value;
import io.github.thatone0502.trimeffectively.effect.TreAttributes;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies {@code trimeffectively:attribute} effects.
 *
 * <p>One modifier per (attribute, entry, operation), named {@code trimeffectively:<entry path>}.
 * Recomputing always replaces that exact id, so the operation is idempotent and there is no bookkeeping
 * to leak; the only state kept is "which attributes did we ever touch on this entity" so stale modifiers
 * can be removed when the gear comes off.
 */
public final class AttributeEngine {
	private static final String TRIGGER = "equipped";
	private static final Map<UUID, Map<Holder<Attribute>, Set<Identifier>>> APPLIED = new ConcurrentHashMap<>();

	public static List<TrimEntry> load(RegistryAccess access) {
		List<TrimEntry> entries = new ArrayList<>();
		for (var key : List.of(Tre.TRE_PATTERN, Tre.TRE_MATERIAL)) {
			try {
				for (Map.Entry<net.minecraft.resources.ResourceKey<TrimEffect>, TrimEffect> e
						: access.lookupOrThrow(key).entrySet()) {
					entries.add(new TrimEntry(e.getKey().identifier(), e.getValue()));
				}
			} catch (RuntimeException ex) {
				// Registry absent (for example when effects never reached the client) - nothing to do.
			}
		}
		return entries;
	}

	public static void update(LivingEntity entity, List<TrimEntry> entries) {
		TrimSnapshot snapshot = TrimSnapshot.of(entity);
		Map<Holder<Attribute>, Map<Identifier, AttributeModifier>> wanted = new LinkedHashMap<>();

		if (!snapshot.isEmpty()) {
			Map<String, Double> variables = variables(entity, snapshot);
			for (TrimEntry entry : entries) {
				List<String> slots = entry.effect().slots();
				int pieces = 0;
				for (Identifier target : entry.effect().appliesTo()) {
					pieces = Math.max(pieces, snapshot.countByIdAndSlots(target, slots));
				}
				if (pieces <= 0) {
					continue;
				}
				for (EffectEntry effect : entry.effect().effects()) {
					Identifier attributeId;
					Value amountValue;
					AttributeModifier.Operation operation;
					if (TreAttributes.EXTRA_JUMPS_ID.equals(effect.type())) {
						// extra_jumps is authoring sugar: the grant itself is a real attribute modifier, so
						// commands, other mods and the client all see it like any other attribute.
						if (!TRIGGER.equals(effect.triggerName()) || effect.count() == null) {
							continue;
						}
						attributeId = TreAttributes.EXTRA_JUMPS_ID;
						amountValue = effect.count();
						operation = AttributeModifier.Operation.ADD_VALUE;
					} else if (Tre.id("attribute").equals(effect.type()) && TRIGGER.equals(effect.triggerName())
							&& effect.attribute() != null && effect.amount() != null) {
						attributeId = effect.attribute();
						amountValue = effect.amount();
						operation = operation(effect.operation());
						if (operation == null) {
							continue;
						}
					} else {
						continue;
					}
					// Conditions are evaluated on every recompute, not once at login: wayfinder's movement
					// bonus depends on which way the wearer is facing, so it has to switch on and off as that
					// changes. There is no damage source at this point, so conditions that need one fail closed.
					if (!effect.conditions().isEmpty()
							&& !Conditions.test(effect.conditions(), entity, null)) {
						continue;
					}
					Optional<Holder.Reference<Attribute>> attribute = BuiltInRegistries.ATTRIBUTE.get(attributeId);
					if (attribute.isEmpty()) {
						continue;
					}
					double amount = amountValue.eval(pieces, variables);
					if (amount == 0.0D) {
						continue;
					}
					Identifier modifierId = modifierId(entry.entryId(), operation);
					wanted.computeIfAbsent(attribute.get(), k -> new LinkedHashMap<>())
							.merge(modifierId,
									new AttributeModifier(modifierId, amount, operation),
									(a, b) -> a.amount() == b.amount() ? a
											: new AttributeModifier(modifierId, a.amount() + b.amount(), operation));
				}
			}
		}

		apply(entity, wanted);
	}

	/** Drops every modifier this engine ever added, for example when the entity dies or unloads. */
	public static void clear(LivingEntity entity) {
		Map<Holder<Attribute>, Set<Identifier>> previous = APPLIED.remove(entity.getUUID());
		if (previous == null) {
			return;
		}
		for (Map.Entry<Holder<Attribute>, Set<Identifier>> e : previous.entrySet()) {
			AttributeInstance instance = entity.getAttributes().getInstance(e.getKey());
			if (instance == null) {
				continue;
			}
			for (Identifier id : e.getValue()) {
				instance.removeModifier(id);
			}
		}
	}

	/** Forgets bookkeeping for entities that are no longer loaded anywhere. */
	public static void prune(Set<UUID> alive) {
		APPLIED.keySet().retainAll(alive);
	}

	private static void apply(LivingEntity entity,
							  Map<Holder<Attribute>, Map<Identifier, AttributeModifier>> wanted) {
		Map<Holder<Attribute>, Set<Identifier>> previous =
				APPLIED.getOrDefault(entity.getUUID(), Map.of());
		Map<Holder<Attribute>, Set<Identifier>> next = new LinkedHashMap<>();

		List<Holder<Attribute>> touched = new ArrayList<>(wanted.keySet());
		for (Holder<Attribute> attribute : previous.keySet()) {
			if (!touched.contains(attribute)) {
				touched.add(attribute);
			}
		}

		for (Holder<Attribute> attribute : touched) {
			AttributeInstance instance = entity.getAttributes().getInstance(attribute);
			if (instance == null) {
				continue;
			}
			Map<Identifier, AttributeModifier> target = wanted.getOrDefault(attribute, Map.of());
			Set<Identifier> oldIds = previous.getOrDefault(attribute, Set.of());
			for (Identifier id : oldIds) {
				if (!target.containsKey(id)) {
					instance.removeModifier(id);
				}
			}
			for (AttributeModifier modifier : target.values()) {
				AttributeModifier existing = instance.getModifier(modifier.id());
				if (existing != null
						&& existing.amount() == modifier.amount()
						&& existing.operation() == modifier.operation()) {
					continue;
				}
				instance.addOrReplacePermanentModifier(modifier);
			}
			if (!target.isEmpty()) {
				next.put(attribute, Set.copyOf(target.keySet()));
			}
		}

		if (next.isEmpty()) {
			APPLIED.remove(entity.getUUID());
		} else {
			APPLIED.put(entity.getUUID(), next);
		}
	}

	public static Map<String, Double> variables(LivingEntity entity, TrimSnapshot snapshot) {
		Map<String, Double> variables = new LinkedHashMap<>();
		double maxHealth = entity.getMaxHealth();
		double ratio = maxHealth > 0.0D ? Mth.clamp(entity.getHealth() / maxHealth, 0.0D, 1.0D) : 0.0D;
		variables.put("health_ratio", ratio);
		variables.put("missing_health_ratio", 1.0D - ratio);
		variables.put("gear.armor", snapshot.trimmedArmor(List.of("armor")));
		// Actual velocity, not the movement speed attribute - see SpeedTracker for why a short window is used
		// instead of the instantaneous value.
		variables.put("speed_mps", SpeedTracker.peak(entity));
		// Non-positive, not merely harmful: neutral effects (Bad Omen and friends) count for both
		// the countdown accelerator and the per-effect damage bonus. Being on fire counts as one more,
		// which is what "每一种非正面效果，包含着火" means.
		long nonPositive = entity.getActiveEffects().stream()
				.filter(e -> e.getEffect().value().getCategory() != MobEffectCategory.BENEFICIAL)
				.count();
		double harmful = nonPositive + (entity.isOnFire() ? 1 : 0);
		variables.put("harmful_effect_count", harmful);
		variables.put("on_fire", entity.isOnFire() ? 1.0D : 0.0D);
		return variables;
	}

	private static Identifier modifierId(Identifier entryId, AttributeModifier.Operation operation) {
		String path = entryId.getPath().replace(':', '_');
		if (operation != AttributeModifier.Operation.ADD_VALUE) {
			path = path + "_" + operation.getSerializedName();
		}
		return Tre.id(path);
	}

	private static AttributeModifier.Operation operation(String name) {
		if (name == null) {
			return AttributeModifier.Operation.ADD_VALUE;
		}
		return switch (name) {
			case "add_value" -> AttributeModifier.Operation.ADD_VALUE;
			case "add_multiplied_base" -> AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
			case "add_multiplied_total" -> AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
			default -> null;
		};
	}

	/** A registry entry together with the id it was registered under. */
	public record TrimEntry(Identifier entryId, TrimEffect effect) {
	}

	private AttributeEngine() {
	}
}
