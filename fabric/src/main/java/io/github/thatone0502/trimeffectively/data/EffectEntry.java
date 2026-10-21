package io.github.thatone0502.trimeffectively.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Map;

/**
 * One effect attached to a trim pattern or material.
 *
 * <p>Deliberately one flat record with a generous set of optional fields instead of one record per
 * effect type: the engine dispatches on {@link #type}, and an unimplemented type simply does nothing
 * rather than failing the whole data file.
 *
 * <p>The codec is written by hand instead of with {@code RecordCodecBuilder}: that builder tops out at
 * sixteen fields and this record has twenty-eight.
 */
public record EffectEntry(
		Identifier type,
		Identifier trigger,
		Value amount,
		Value chance,
		Value count,
		Value duration,
		Value amplifier,
		Value lifetime,
		Value radius,
		Identifier attribute,
		String operation,
		Identifier entity,
		Identifier effect,
		Identifier damageType,
		Identifier particle,
		Identifier blockedBy,
		String target,
		String attackTarget,
		boolean inheritEquipment,
		boolean keepAttacker,
		boolean excludeOwn,
		boolean client,
		Label label,
		List<LootItemCondition> conditions,
		Value rolls,
		String property,
		Literal literal,
		Map<String, Literal> properties,
		String scope,
		boolean alsoBrutes
) {
	public static final String DEFAULT_TRIGGER = "equipped";
	public static final String DEFAULT_OPERATION = "add_value";

	public static final Codec<EffectEntry> CODEC = new Codec<>() {
		@Override
		public <T> DataResult<Pair<EffectEntry, T>> decode(DynamicOps<T> ops, T input) {
			DataResult<MapLike<T>> mapped = ops.getMap(input);
			if (mapped.result().isEmpty()) {
				return DataResult.error(() -> "expected an object for a trimeffectively effect");
			}
			MapLike<T> map = mapped.result().get();
			try {
				EffectEntry entry = new EffectEntry(
						required(ops, map, "type", Identifier.CODEC),
						optional(ops, map, "trigger", Identifier.CODEC),
						optional(ops, map, "amount", Value.CODEC),
						optional(ops, map, "chance", Value.CODEC),
						optional(ops, map, "count", Value.CODEC),
						optional(ops, map, "duration", Value.CODEC),
						optional(ops, map, "amplifier", Value.CODEC),
						optional(ops, map, "lifetime", Value.CODEC),
						optional(ops, map, "radius", Value.CODEC),
						optional(ops, map, "attribute", Identifier.CODEC),
						optional(ops, map, "operation", Codec.STRING),
						optional(ops, map, "entity", Identifier.CODEC),
						optional(ops, map, "effect", Identifier.CODEC),
						optional(ops, map, "damage_type", Identifier.CODEC),
						optional(ops, map, "particle", Identifier.CODEC),
						optional(ops, map, "blocked_by", Identifier.CODEC),
						optional(ops, map, "target", Codec.STRING),
						optional(ops, map, "attack_target", Codec.STRING),
						flag(ops, map, "inherit_equipment", false),
						flag(ops, map, "keep_attacker", false),
						flag(ops, map, "exclude_own", true),
						flag(ops, map, "client", false),
						optional(ops, map, "label", Label.CODEC),
						conditions(ops, map),
						optional(ops, map, "rolls", Value.CODEC),
						optional(ops, map, "property", Codec.STRING),
						optional(ops, map, "value", Literal.CODEC),
						properties(ops, map),
						optional(ops, map, "scope", Codec.STRING),
						flag(ops, map, "also_brutes", false)
				);
				return DataResult.success(Pair.of(entry, input));
			} catch (RuntimeException exception) {
				return DataResult.error(() -> "invalid trimeffectively effect: " + exception.getMessage());
			}
		}

		@Override
		public <T> DataResult<T> encode(EffectEntry entry, DynamicOps<T> ops, T prefix) {
			RecordBuilder<T> builder = ops.mapBuilder();
			write(ops, builder, "type", Identifier.CODEC, entry.type());
			write(ops, builder, "trigger", Identifier.CODEC, entry.trigger());
			write(ops, builder, "amount", Value.CODEC, entry.amount());
			write(ops, builder, "chance", Value.CODEC, entry.chance());
			write(ops, builder, "count", Value.CODEC, entry.count());
			write(ops, builder, "duration", Value.CODEC, entry.duration());
			write(ops, builder, "amplifier", Value.CODEC, entry.amplifier());
			write(ops, builder, "lifetime", Value.CODEC, entry.lifetime());
			write(ops, builder, "radius", Value.CODEC, entry.radius());
			write(ops, builder, "attribute", Identifier.CODEC, entry.attribute());
			write(ops, builder, "operation", Codec.STRING, entry.operation());
			write(ops, builder, "entity", Identifier.CODEC, entry.entity());
			write(ops, builder, "effect", Identifier.CODEC, entry.effect());
			write(ops, builder, "damage_type", Identifier.CODEC, entry.damageType());
			write(ops, builder, "particle", Identifier.CODEC, entry.particle());
			write(ops, builder, "blocked_by", Identifier.CODEC, entry.blockedBy());
			write(ops, builder, "target", Codec.STRING, entry.target());
			write(ops, builder, "attack_target", Codec.STRING, entry.attackTarget());
			if (entry.inheritEquipment()) {
				write(ops, builder, "inherit_equipment", Codec.BOOL, Boolean.TRUE);
			}
			if (entry.keepAttacker()) {
				write(ops, builder, "keep_attacker", Codec.BOOL, Boolean.TRUE);
			}
			if (!entry.excludeOwn()) {
				write(ops, builder, "exclude_own", Codec.BOOL, Boolean.FALSE);
			}
			if (entry.client()) {
				write(ops, builder, "client", Codec.BOOL, Boolean.TRUE);
			}
			write(ops, builder, "label", Label.CODEC, entry.label());
			if (!entry.conditions().isEmpty()) {
				write(ops, builder, "conditions", LootItemCondition.DIRECT_CODEC.listOf(), entry.conditions());
			}
			write(ops, builder, "rolls", Value.CODEC, entry.rolls());
			write(ops, builder, "property", Codec.STRING, entry.property());
			write(ops, builder, "value", Literal.CODEC, entry.literal());
			if (!entry.properties().isEmpty()) {
				write(ops, builder, "properties", Codec.unboundedMap(Codec.STRING, Literal.CODEC), entry.properties());
			}
			write(ops, builder, "scope", Codec.STRING, entry.scope());
			if (entry.alsoBrutes()) {
				write(ops, builder, "also_brutes", Codec.BOOL, Boolean.TRUE);
			}
			return builder.build(prefix);
		}
	};

	private static <T, A> A optional(DynamicOps<T> ops, MapLike<T> map, String key, Codec<A> codec) {
		T raw = map.get(key);
		if (raw == null) {
			return null;
		}
		return codec.parse(ops, raw).getOrThrow(message -> new IllegalArgumentException(key + ": " + message));
	}

	private static <T, A> A required(DynamicOps<T> ops, MapLike<T> map, String key, Codec<A> codec) {
		A value = optional(ops, map, key, codec);
		if (value == null) {
			throw new IllegalArgumentException("missing required field '" + key + "'");
		}
		return value;
	}

	private static <T> boolean flag(DynamicOps<T> ops, MapLike<T> map, String key, boolean fallback) {
		Boolean value = optional(ops, map, key, Codec.BOOL);
		return value == null ? fallback : value;
	}

	private static <T> List<LootItemCondition> conditions(DynamicOps<T> ops, MapLike<T> map) {
		List<LootItemCondition> value = optional(ops, map, "conditions", LootItemCondition.DIRECT_CODEC.listOf());
		return value == null ? List.of() : value;
	}

	private static <T> Map<String, Literal> properties(DynamicOps<T> ops, MapLike<T> map) {
		Map<String, Literal> value =
				optional(ops, map, "properties", Codec.unboundedMap(Codec.STRING, Literal.CODEC));
		return value == null ? Map.of() : value;
	}

	private static <T, A> void write(DynamicOps<T> ops, RecordBuilder<T> builder, String key, Codec<A> codec, A value) {
		if (value == null) {
			return;
		}
		builder.add(key, codec.encodeStart(ops, value)
				.getOrThrow(message -> new IllegalArgumentException(key + ": " + message)));
	}

	/** Trigger name with the namespace stripped; defaults to {@code equipped} when absent. */
	public String triggerName() {
		return trigger == null ? DEFAULT_TRIGGER : trigger.getPath();
	}

	/** The floating subtitle shown for an annotated sound (client side only). */
	public record Label(boolean subtitle, int visibleTicks, int fadeTicks, boolean seeThrough, String billboard) {
		public static final Codec<Label> CODEC = RecordCodecBuilder.create(i -> i.group(
				Codec.BOOL.optionalFieldOf("subtitle", true).forGetter(Label::subtitle),
				Codec.INT.optionalFieldOf("visible_ticks", 40).forGetter(Label::visibleTicks),
				Codec.INT.optionalFieldOf("fade_ticks", 60).forGetter(Label::fadeTicks),
				Codec.BOOL.optionalFieldOf("see_through", true).forGetter(Label::seeThrough),
				Codec.STRING.optionalFieldOf("billboard", "center").forGetter(Label::billboard)
		).apply(i, Label::new));
	}

	/**
	 * A bare JSON literal that may be written as either a boolean or a number.
	 *
	 * <p>Some effect properties are naturally boolean ({@code unbreakable}) and some numeric
	 * ({@code mining_speed}), and the data format lets both be written under the same {@code value} key
	 * without a discriminating wrapper object. The Java side keeps the distinction so a property lookup
	 * can ask for the shape it expects.
	 */
	public record Literal(boolean isBool, boolean flag, double number, String text) {
		/**
		 * Number first, then boolean, then string - and the order is load-bearing.
		 *
		 * <p>{@code DynamicOps#getBooleanValue} is implemented as "a non-zero number is true", so asking
		 * {@code Codec.BOOL} first turned every numeric property into {@code true}: a datapack writing
		 * {@code "durability_multiplier": 2} stamped {@code true} (1 byte) onto the item and every reader
		 * got 1.0 out of it. Numbers are unambiguous, so they must be tried before booleans; a real boolean
		 * is rejected by {@code getNumberValue}, which keeps {@code true} a boolean.
		 */
		public static final Codec<Literal> CODEC = new Codec<>() {
			@Override
			public <T> DataResult<Pair<Literal, T>> decode(DynamicOps<T> ops, T input) {
				DataResult<Double> asNumber = Codec.DOUBLE.parse(ops, input);
				if (asNumber.result().isPresent()) {
					return DataResult.success(Pair.of(Literal.ofNumber(asNumber.result().get()), input));
				}
				DataResult<Boolean> asBool = Codec.BOOL.parse(ops, input);
				if (asBool.result().isPresent()) {
					return DataResult.success(Pair.of(Literal.ofBoolean(asBool.result().get()), input));
				}
				DataResult<String> asText = Codec.STRING.parse(ops, input);
				if (asText.result().isPresent()) {
					return DataResult.success(Pair.of(Literal.ofText(asText.result().get()), input));
				}
				return DataResult.error(() -> "expected a boolean, a number or a string");
			}

			@Override
			public <T> DataResult<T> encode(Literal literal, DynamicOps<T> ops, T prefix) {
				if (literal.isText()) {
					return Codec.STRING.encodeStart(ops, literal.text());
				}
				return literal.isBool()
						? Codec.BOOL.encodeStart(ops, literal.flag())
						: Codec.DOUBLE.encodeStart(ops, literal.number());
			}
		};

		public static Literal ofBoolean(boolean value) {
			return new Literal(true, value, value ? 1.0D : 0.0D, null);
		}

		public static Literal ofNumber(double value) {
			return new Literal(false, value != 0.0D, value, null);
		}

		public static Literal ofText(String value) {
			return new Literal(false, false, 0.0D, value);
		}

		/** True when the literal was written as a JSON string. */
		public boolean isText() {
			return text != null;
		}

		/** The numeric reading, with booleans mapped to 1/0 so callers can be uniform. */
		public double asNumber() {
			return isBool ? (flag ? 1.0D : 0.0D) : number;
		}

		/** The boolean reading, with numbers mapped to "not zero". */
		public boolean asBoolean() {
			return isBool ? flag : (isText() ? !text.isEmpty() : number != 0.0D);
		}
	}
}
