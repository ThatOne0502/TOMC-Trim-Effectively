package io.github.thatone0502.trimeffectively.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The properties a trimmed item carries, stamped onto the item itself.
 *
 * <p>Why a component instead of reading the trim's data every time something asks "is this item gold?":
 * the question gets asked from places that have no registry access and no wearer - dropped items, item
 * frames, the durability bar, another mod checking a stack in a chest. Stamping the answer onto the stack
 * turns a registry lookup into a component read, and makes the answer travel with the item: a trimmed sword
 * handed to another player stays gold.
 *
 * <p>The map is deliberately open. A property is just a name plus a boolean/number/string, so a datapack can
 * add keys without the storage layer changing. Each key still needs Java somewhere to act on it - that is
 * unavoidable - but the item does not have to be re-derived to answer a question about it.
 *
 * <p><b>Which trim produced this.</b> The pattern and material ids are stored alongside the values, not
 * because any property needs them, but so that a stamp can be told apart from a stamp that belongs to a
 * <em>different</em> trim. A smithing table overwrites the {@code minecraft:trim} component with the new
 * pattern and material; without this, the old property set would survive the overwrite and the item would
 * keep powers it no longer qualifies for (a diamond-then-quartz chestplate keeping doubled durability).
 * Comparing two {@link Identifier}s is cheap enough to run on every equipment read and on the inventory
 * sweep, which is what makes the drift correct itself instead of waiting for the next re-trim.
 */
public record TrimProperties(Identifier pattern, Identifier material, Map<String, EffectEntry.Literal> values) {
	public static final TrimProperties EMPTY = new TrimProperties(null, null, Map.of());

	private static final Codec<Map<String, EffectEntry.Literal>> VALUES =
			Codec.unboundedMap(Codec.STRING, EffectEntry.Literal.CODEC);

	/**
	 * Codec shape: {@code {"pattern": ..., "material": ..., "values": {...}}}, every field optional.
	 *
	 * <p>Both identity fields are optional so that a component written by an older build - which stored the
	 * bare property map - still decodes instead of being dropped: it comes back with no identity, and
	 * {@code TrimStamp} treats "no identity" as "not provably in sync" and re-stamps it from the datapack.
	 */
	public static final Codec<TrimProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
					Identifier.CODEC.optionalFieldOf("pattern").forGetter(p -> Optional.ofNullable(p.pattern())),
					Identifier.CODEC.optionalFieldOf("material").forGetter(p -> Optional.ofNullable(p.material())),
					VALUES.optionalFieldOf("values", Map.of()).forGetter(TrimProperties::values))
			.apply(instance, (pattern, material, values) ->
					new TrimProperties(pattern.orElse(null), material.orElse(null), values)));

	public static final StreamCodec<RegistryFriendlyByteBuf, TrimProperties> STREAM_CODEC =
			ByteBufCodecs.fromCodecWithRegistries(CODEC);

	public TrimProperties {
		values = values == null ? Map.of() : Map.copyOf(values);
	}

	/** A property set with no identity attached; only meaningful as an intermediate during assembly. */
	public TrimProperties(Map<String, EffectEntry.Literal> values) {
		this(null, null, values);
	}

	/** This property set, tagged with the trim it was computed from. */
	public TrimProperties withIdentity(Identifier pattern, Identifier material) {
		return new TrimProperties(pattern, material, values);
	}

	/**
	 * Whether this stamp was produced by the given trim.
	 *
	 * <p>An unknown identity (null on either side, or a stamp written before identities existed) answers
	 * {@code false}: an unknown stamp is a stamp we cannot vouch for, and re-deriving it costs one datapack
	 * lookup that only happens once per item.
	 */
	public boolean matches(Identifier pattern, Identifier material) {
		return this.pattern != null && this.material != null
				&& this.pattern.equals(pattern) && this.material.equals(material);
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	public boolean has(String key) {
		return values.containsKey(key);
	}

	public boolean flag(String key, boolean fallback) {
		EffectEntry.Literal literal = values.get(key);
		return literal == null ? fallback : literal.asBoolean();
	}

	public boolean flag(String key) {
		return flag(key, false);
	}

	public double number(String key, double fallback) {
		EffectEntry.Literal literal = values.get(key);
		return literal == null ? fallback : literal.asNumber();
	}

	public String text(String key, String fallback) {
		EffectEntry.Literal literal = values.get(key);
		return literal == null || !literal.isText() ? fallback : literal.text();
	}

	/** A copy of this property set with {@code key} set to {@code value}. */
	public TrimProperties with(String key, EffectEntry.Literal value) {
		Map<String, EffectEntry.Literal> merged = new LinkedHashMap<>(values);
		merged.put(key, value);
		return new TrimProperties(pattern, material, merged);
	}

	/** A copy of this property set with every entry of {@code other} laid over this one. */
	public TrimProperties merge(TrimProperties other) {
		if (other == null || other.isEmpty()) {
			return this;
		}
		Map<String, EffectEntry.Literal> merged = new LinkedHashMap<>(values);
		merged.putAll(other.values);
		return new TrimProperties(pattern, material, merged);
	}
}
