package io.github.thatone0502.trimeffectively.data;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

/**
 * One file inside {@code data/<pack>/trimeffectively/tre_pattern/} or {@code .../tre_material/}.
 *
 * <p>The file name is only a name - which trim ids this entry actually applies to is decided by the
 * {@code pattern}/{@code material} field(s) inside the JSON.
 *
 * @param appliesTo trim pattern (or material) ids, e.g. {@code minecraft:sentry}
 * @param slots     equipment slots that carry the effect; default is armour plus both hands
 * @param effects   the effects themselves
 */
public record TrimEffect(List<Identifier> appliesTo, List<String> slots, List<EffectEntry> effects) {
	public static final List<String> DEFAULT_SLOTS = List.of("armor", "hand");

	/**
	 * Accepts either a single id ({@code "pattern": "minecraft:sentry"}) or a list of them. A lone id
	 * is by far the common case, so it must not be forced into an array.
	 */
	public static final Codec<List<Identifier>> ID_LIST = Codec
			.either(Identifier.CODEC, Identifier.CODEC.listOf())
			.xmap(
					either -> either.map(List::of, list -> list),
					list -> list.size() == 1 ? Either.left(list.getFirst()) : Either.right(list));

	public static final Codec<TrimEffect> CODEC = RecordCodecBuilder.create(i -> i.group(
			ID_LIST.optionalFieldOf("pattern", List.of()).forGetter(TrimEffect::appliesTo),
			ID_LIST.optionalFieldOf("material", List.of()).forGetter(TrimEffect::appliesTo),
			Codec.STRING.listOf().optionalFieldOf("slots", DEFAULT_SLOTS).forGetter(TrimEffect::slots),
			EffectEntry.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(TrimEffect::effects)
	).apply(i, TrimEffect::of));

	private static TrimEffect of(List<Identifier> pattern, List<Identifier> material, List<String> slots,
								 List<EffectEntry> effects) {
		LinkedHashSet<Identifier> merged = new LinkedHashSet<>(pattern);
		merged.addAll(material);
		return new TrimEffect(List.copyOf(new ArrayList<>(merged)), slots, effects);
	}

	public boolean isClientOnly() {
		return !effects.isEmpty() && effects.stream().allMatch(EffectEntry::client);
	}

	public Optional<Identifier> primaryId() {
		return appliesTo.isEmpty() ? Optional.empty() : Optional.of(appliesTo.getFirst());
	}
}
