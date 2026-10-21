package io.github.thatone0502.trimeffectively.engine;

import io.github.thatone0502.trimeffectively.Tre;
import io.github.thatone0502.trimeffectively.Trimeffectively;
import io.github.thatone0502.trimeffectively.data.EffectEntry;
import io.github.thatone0502.trimeffectively.data.TrimEffect;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The shared resolver every effect handler goes through.
 *
 * <p>The registry contents are loaded once per engine pass and kept in a flat index keyed by trim id,
 * so an event handler can ask "which of this entity's trims have effects for trigger X" without
 * touching the registries. Handlers never see a {@link TrimEffect}: they get an {@link Active}, which
 * already knows how many pieces carry the entry and therefore what the stacking factor is.
 */
public final class TrimEffects {
	/**
	 * One effect entry as it sits in a registry, with the id its data file gave it.
	 *
	 * <p>{@code index} is the position of the entry inside its file: a file usually holds several
	 * entries, and they all share one entry id, so the entry id alone cannot identify them.
	 */
	public record Named(Identifier entryId, int index, EffectEntry entry, List<String> slots) {
		/** Identity of one entry: the file id plus its position in that file. */
		public String key() {
			return entryId + "#" + index;
		}
	}

	/** One entry that applies to an entity right now, plus the piece count it stacks with. */
	public record Active(Identifier entryId, EffectEntry entry, int pieces) {
	}

	/** Immutable snapshot of both registries, indexed by the trim id each entry applies to. */
	public record Index(Map<Identifier, List<Named>> patterns, Map<Identifier, List<Named>> materials) {
		public static final Index EMPTY = new Index(Map.of(), Map.of());

		public boolean isEmpty() {
			return patterns.isEmpty() && materials.isEmpty();
		}

		public int size() {
			int n = 0;
			for (List<Named> list : patterns.values()) {
				n += list.size();
			}
			for (List<Named> list : materials.values()) {
				n += list.size();
			}
			return n;
		}
	}

	private static volatile Index index = Index.EMPTY;
	private static boolean warnedConditions;

	public static Index load(RegistryAccess access) {
		Map<Identifier, List<Named>> patterns = new HashMap<>();
		Map<Identifier, List<Named>> materials = new HashMap<>();
		try {
			fill(access, Tre.TRE_PATTERN, patterns);
			fill(access, Tre.TRE_MATERIAL, materials);
		} catch (RuntimeException exception) {
			Trimeffectively.LOGGER.warn("[tre] effect index load failed: {}", exception.toString());
			return Index.EMPTY;
		}
		return new Index(Map.copyOf(patterns), Map.copyOf(materials));
	}

	private static void fill(RegistryAccess access, ResourceKey<Registry<TrimEffect>> key,
			Map<Identifier, List<Named>> target) {
		Registry<TrimEffect> registry = access.lookupOrThrow(key);
		for (Map.Entry<ResourceKey<TrimEffect>, TrimEffect> entry : registry.entrySet()) {
			TrimEffect effect = entry.getValue();
			List<String> slots = effect.slots();
			int index = 0;
			for (EffectEntry inner : effect.effects()) {
				Named named = new Named(entry.getKey().identifier(), index++, inner, slots);
				for (Identifier appliesTo : effect.appliesTo()) {
					target.computeIfAbsent(appliesTo, ignored -> new ArrayList<>(2)).add(named);
				}
			}
		}
		target.replaceAll((ignored, list) -> List.copyOf(list));
	}

	public static void update(Index loaded) {
		index = loaded;
	}

	public static Index index() {
		return index;
	}

	/** Every entry of the given trigger that applies to this entity right now. */
	public static List<Active> active(LivingEntity entity, String trigger) {
		return active(entity, trigger, null);
	}

	/** Same, restricted to one effect type. {@code type == null} means every type. */
	public static List<Active> active(LivingEntity entity, String trigger, Identifier type) {
		return active(entity, trigger, type, null);
	}

	/**
	 * Same, with a damage source available for {@code conditions} evaluation.
	 *
	 * <p>Conditions are vanilla loot predicates, so they are evaluated against a real {@code LootContext}. When no
	 * source is supplied, an entry that carries conditions cannot be judged and is skipped - a condition that
	 * cannot be tested must never be treated as satisfied.
	 */
	public static List<Active> active(LivingEntity entity, String trigger, Identifier type, DamageSource source) {
		Index current = index;
		if (current.isEmpty()) {
			return List.of();
		}
		TrimSnapshot snapshot = TrimSnapshot.of(entity);
		if (snapshot.isEmpty()) {
			return List.of();
		}
		Set<Identifier> trimmed = new HashSet<>(6);
		for (TrimSnapshot.Piece piece : snapshot.pieces) {
			if (piece.pattern() != null) {
				trimmed.add(piece.pattern());
			}
			if (piece.material() != null) {
				trimmed.add(piece.material());
			}
		}
		Map<String, Active> found = new LinkedHashMap<>(4);
		for (Identifier trimId : trimmed) {
			collect(current.patterns().get(trimId), trimId, snapshot, trigger, type, found, entity, source);
			collect(current.materials().get(trimId), trimId, snapshot, trigger, type, found, entity, source);
		}
		return List.copyOf(found.values());
	}

	/** Convenience for handlers that only care about one type. */
	public static List<Active> activeOfType(LivingEntity entity, String trigger, String type) {
		return active(entity, trigger, Tre.id(type));
	}

	/** Convenience for handlers that only care about one type, with a damage source for conditions. */
	public static List<Active> activeOfType(LivingEntity entity, String trigger, String type, DamageSource source) {
		return active(entity, trigger, Tre.id(type), source);
	}

	private static void collect(List<Named> candidates, Identifier trimId, TrimSnapshot snapshot,
			String trigger, Identifier type, Map<String, Active> found,
			LivingEntity entity, DamageSource source) {
		if (candidates == null) {
			return;
		}
		for (Named named : candidates) {
			// Identity is the file id plus the entry's position: several entries of one file share the file
			// id, and keying on that alone silently dropped every entry after the first.
			if (found.containsKey(named.key())) {
				continue;
			}
			EffectEntry entry = named.entry();
			if (!entry.triggerName().equals(trigger)) {
				continue;
			}
			if (type != null && !type.equals(entry.type())) {
				continue;
			}
			if (!entry.conditions().isEmpty()) {
				if (source == null) {
					// A condition that cannot be tested must never be treated as satisfied.
					if (!warnedConditions) {
						warnedConditions = true;
						Trimeffectively.LOGGER.warn(
								"[tre] entry {} uses conditions but no context was supplied; skipped",
								named.entryId());
					}
					continue;
				}
				if (!Conditions.test(entry.conditions(), entity, source)) {
					continue;
				}
			}
			int pieces = snapshot.countByIdAndSlots(trimId, named.slots());
			if (pieces <= 0) {
				continue;
			}
			found.put(named.key(), new Active(named.entryId(), entry, pieces));
		}
	}

	/** Context variables for value evaluation, built from a fresh snapshot. */
	public static Map<String, Double> variables(LivingEntity entity) {
		return AttributeEngine.variables(entity, TrimSnapshot.of(entity));
	}
}
