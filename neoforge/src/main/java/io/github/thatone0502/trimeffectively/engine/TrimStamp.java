package io.github.thatone0502.trimeffectively.engine;

import io.github.thatone0502.trimeffectively.Tre;
import io.github.thatone0502.trimeffectively.data.EffectEntry;
import io.github.thatone0502.trimeffectively.data.TrimComponents;
import io.github.thatone0502.trimeffectively.data.TrimProperties;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

import java.util.List;
import java.util.Map;

/**
 * Keeps the {@link TrimComponents#TRIM_PROPERTIES} component of a trimmed item in sync with its trim.
 *
 * <p>The answer is carried by the item, so it has to be kept honest in three ways:
 * <ul>
 *   <li>trim present, no stamp - the item predates the component, or another mod applied the trim: stamp it;</li>
 *   <li>no trim, stamp present - something removed the trim (another mod, a command): drop the stamp, or the
 *       item would keep its powers forever;</li>
 *   <li>trim present, stamp of a <b>different</b> trim - a smithing table replaced the pattern or the material
 *       and vanilla overwrote {@code minecraft:trim} in place. The old values are stale by definition, so they
 *       are thrown away and re-derived rather than merged over.</li>
 * </ul>
 *
 * <p>The third case is why the stamp records which trim produced it: presence alone cannot tell "same trim,
 * still fine" from "different trim, the item no longer qualifies". Comparing the stored pattern and material
 * against the item's current ones is two identifier comparisons, so it costs nothing on the paths that run
 * constantly (equipment reads, one inventory slot per tick).
 *
 * <p>Deliberately <em>not</em> handled: a stamp whose contents no longer match the datapack. The author's call
 * is that a changed datapack should not silently rewrite items people already own; the value is refreshed only
 * when the item is re-stamped, or forced by {@code /tre reload} for what online players are carrying.
 */
public final class TrimStamp {
	/**
	 * Brings a stack in line with its trim, by the presence and identity rules.
	 *
	 * <p>There is exactly one invariant: <b>the trim component and our property component are either both
	 * present or both absent, and a present property component belongs to the trim that is actually on the
	 * item</b>. Anything else is drift and gets fixed on the spot.
	 *
	 * <p>The check is two component lookups plus two identifier comparisons and no registry access on the
	 * common path, which is why it is safe to call on every equipment read and on a one-slot-per-tick
	 * inventory sweep. The datapack is only consulted in the rare transition case.
	 *
	 * <p>Consequence worth naming: a trim whose data grants no item properties still gets a component, an empty
	 * one. Otherwise "trim present, component absent" would be both a legal state and a drift to repair, and the
	 * sweep would never stop repairing it.
	 */
	public static void reconcile(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		ArmorTrim trim = stack.get(DataComponents.TRIM);
		TrimProperties stamp = stack.get(TrimComponents.TRIM_PROPERTIES);
		if (trim == null) {
			// No trim means nothing to describe, and the kill switch has nothing left to switch off either.
			stack.remove(TrimComponents.TRIM_PROPERTIES);
			stack.remove(TrimComponents.TRE_ENABLED);
			return;
		}
		boolean enabled = enabled(stack);
		if (stamp == null) {
			stack.set(TrimComponents.TRIM_PROPERTIES, expected(trim, enabled));
			return;
		}
		Identifier pattern = patternId(trim);
		Identifier material = materialId(trim);
		// A trim whose holders are not registry-backed has no id to compare, so identity drift cannot be
		// detected for it; leaving the existing stamp alone is the only choice that does not re-derive on
		// every single read of the item.
		if (pattern != null && material != null && !stamp.matches(pattern, material)) {
			stack.set(TrimComponents.TRIM_PROPERTIES, expected(trim, enabled));
			return;
		}
		// A disabled trim keeps its identity but loses its values. The identity is what lets the item be
		// switched back on without a datapack lookup at that moment, and what keeps "stamp present, trim
		// present" true while the values are empty.
		if (!enabled && !stamp.isEmpty()) {
			stack.set(TrimComponents.TRIM_PROPERTIES, expected(trim, false));
		}
	}

	/** Recomputes the stamp from the datapack, replacing whatever the item carries. */
	public static void force(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		ArmorTrim trim = stack.get(DataComponents.TRIM);
		if (trim == null) {
			stack.remove(TrimComponents.TRIM_PROPERTIES);
			stack.remove(TrimComponents.TRE_ENABLED);
			return;
		}
		// Set even when the property map comes out empty: presence is the invariant, emptiness is not.
		stack.set(TrimComponents.TRIM_PROPERTIES, expected(trim, enabled(stack)));
	}

	/**
	 * Whether the trim on this item is currently switched on.
	 *
	 * <p>Absent means on: the kill switch is only ever written by the glass-pane recipe, so every item that
	 * never went through it answers {@code true} without carrying a marker.
	 */
	public static boolean enabled(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return true;
		}
		return stack.getOrDefault(TrimComponents.TRE_ENABLED, Boolean.TRUE);
	}

	/** Switches the trim on or off; switching on removes the component instead of storing {@code true}. */
	public static void setEnabled(ItemStack stack, boolean enabled) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		if (enabled) {
			stack.remove(TrimComponents.TRE_ENABLED);
		} else {
			stack.set(TrimComponents.TRE_ENABLED, Boolean.FALSE);
		}
	}


	/**
	 * Reads one property off an item, tolerating an item that has not been stamped yet.
	 *
	 * <p>The stamp is the normal source, but an item that has only just been smithed - or one handed to us by a
	 * command or another mod - may carry a trim without having passed a stamping point. Falling back to the
	 * datapack answer keeps the property correct in exactly the window where the component is still missing.
	 */
	public static boolean flag(ItemStack stack, String key) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		TrimProperties properties = stack.get(TrimComponents.TRIM_PROPERTIES);
		if (properties == null) {
			ArmorTrim trim = stack.get(DataComponents.TRIM);
			if (trim == null) {
				return false;
			}
			properties = expected(trim, enabled(stack));
		}
		return properties.flag(key);
	}

	/** The property set the datapack says this trim grants; material first, pattern laid over it. */
	private static TrimProperties expected(ArmorTrim trim, boolean enabled) {
		Identifier pattern = patternId(trim);
		Identifier material = materialId(trim);
		if (!enabled) {
			// Identity, no values: the item stays recognisable as "this trim, switched off".
			return TrimProperties.EMPTY.withIdentity(pattern, material);
		}
		TrimEffects.Index index = TrimEffects.index();
		TrimProperties properties = TrimProperties.EMPTY;
		properties = properties.merge(collect(index.materials(), trim.material()));
		properties = properties.merge(collect(index.patterns(), trim.pattern()));
		return properties.withIdentity(pattern, material);
	}

	/** Registry id of a trim holder, or null when it is not registry-backed (then nothing can match it). */
	private static Identifier patternId(ArmorTrim trim) {
		return idOf(trim.pattern());
	}

	private static Identifier materialId(ArmorTrim trim) {
		return idOf(trim.material());
	}

	private static Identifier idOf(Holder<?> holder) {
		return holder == null ? null : holder.unwrapKey().map(key -> key.identifier()).orElse(null);
	}

	private static TrimProperties collect(Map<Identifier, List<TrimEffects.Named>> entries, Holder<?> trimId) {
		Identifier id = idOf(trimId);
		if (id == null) {
			return TrimProperties.EMPTY;
		}
		TrimProperties properties = TrimProperties.EMPTY;
		for (TrimEffects.Named named : entries.getOrDefault(id, List.of())) {
			EffectEntry entry = named.entry();
			if (!Tre.id("item_property").equals(entry.type())) {
				continue;
			}
			if (!entry.properties().isEmpty()) {
				properties = properties.merge(new TrimProperties(entry.properties()));
			} else if (entry.property() != null && entry.literal() != null) {
				// Legacy single-property form; still accepted so older datapacks keep working.
				properties = properties.with(entry.property(), entry.literal());
			}
		}
		return properties;
	}

	private TrimStamp() {
	}
}
