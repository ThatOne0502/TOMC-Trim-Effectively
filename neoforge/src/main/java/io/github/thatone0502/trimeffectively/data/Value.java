package io.github.thatone0502.trimeffectively.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A single numeric field of an effect.
 *
 * <p>{@code final = value * product(per)^power * stackFactor}, then clamped by {@code min}/{@code max}.
 * {@code power} (default 1) bends the curve of the variables without adding an expression language: 0.5 is
 * a square root, which is how "grows with damage, but not linearly" is written.
 * {@code stack} decides how the value grows with the number of worn pieces:
 * <ul>
 *   <li>{@code per_piece} (default) - multiply by the piece count</li>
 *   <li>{@code once} - ignore the piece count</li>
 *   <li>{@code full_set} - piece count if all four armour pieces carry it, otherwise 0</li>
 * </ul>
 */
public record Value(double value, List<String> per, double power, String stack, Double min, Double max) {
	public static final String PER_PIECE = "per_piece";
	public static final String ONCE = "once";
	public static final String FULL_SET = "full_set";

	public static final Codec<Value> CODEC = RecordCodecBuilder.create(i -> i.group(
			Codec.DOUBLE.fieldOf("value").forGetter(Value::value),
			Codec.STRING.listOf().optionalFieldOf("per", List.of()).forGetter(Value::per),
			Codec.DOUBLE.optionalFieldOf("power", 1.0D).forGetter(Value::power),
			Codec.STRING.optionalFieldOf("stack", PER_PIECE).forGetter(Value::stack),
			Codec.DOUBLE.optionalFieldOf("min").forGetter(v -> Optional.ofNullable(v.min())),
			Codec.DOUBLE.optionalFieldOf("max").forGetter(v -> Optional.ofNullable(v.max()))
	).apply(i, (value, per, power, stack, min, max) ->
			new Value(value, per, power, stack, min.orElse(null), max.orElse(null))));

	public static Value of(double value) {
		return new Value(value, List.of(), 1.0D, PER_PIECE, null, null);
	}

	/**
	 * @param stackCount number of pieces currently contributing
	 * @param variables  context variables referenced by {@link #per}
	 */
	public double eval(double stackCount, Map<String, Double> variables) {
		double product = 1.0D;
		for (String name : per) {
			product *= variables.getOrDefault(name, 1.0D);
		}
		if (power != 1.0D && product >= 0.0D) {
			product = Math.pow(product, power);
		}
		double result = value * product;
		result *= switch (stack == null ? PER_PIECE : stack) {
			case ONCE -> 1.0D;
			case FULL_SET -> stackCount >= 4.0D ? stackCount : 0.0D;
			default -> stackCount;
		};
		if (min != null) {
			result = Math.max(min, result);
		}
		if (max != null) {
			result = Math.min(max, result);
		}
		return result;
	}
}
