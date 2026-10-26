package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.Trimeffectively;
import io.github.thatone0502.trimeffectively.data.Value;
import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The six damage-related effect types, wired through one entry point.
 *
 * <p>Every one of them needs the damage source, the victim and the raw amount at the same moment, so
 * they cannot be attribute modifiers and Fabric has no damage-modification event: they are intercepted
 * in {@code EntityMixin}/{@code LivingEntityMixin} and orchestrated here. {@link #init()} exists so the
 * slice is armed from {@code Trimeffectively#onInitialize} in the same shape as the other handlers;
 * the mixins themselves are registered in {@code trimeffectively.mixins.json}, not at runtime.
 *
 * <p>Order of application for one hit: {@code damage_type_override} first, because the type decides
 * which {@code damage_source_properties} conditions the other entries would see; then
 * {@code damage_in} on the victim, then {@code damage_out} and {@code crit_damage} on the attacker. The
 * fractions multiply independently, so a 20% reduction and a 20% bonus leave the hit at 96%.
 */
public final class DamageEffects {
	public static final String TYPE_DAMAGE_OUT = "damage_out";
	public static final String TYPE_DAMAGE_IN = "damage_in";
	public static final String TYPE_DAMAGE_TYPE_OVERRIDE = "damage_type_override";
	public static final String TYPE_CRIT_DAMAGE = "crit_damage";
	public static final String TYPE_THORNS = "thorns";

	public static final String TRIGGER_ATTACK_OUT = "attack_out";
	public static final String TRIGGER_BEFORE = "take_damage_before";
	public static final String TRIGGER_AFTER = "take_damage_after";
	public static final String TRIGGER_EQUIPPED = "equipped";

	/** Guards the re-dispatch in {@code LivingEntityDamageMixin}: the nested call must reach the vanilla body. */
	private static int depth;

	public static boolean busy() {
		return depth > 0;
	}

	public static void push() {
		depth++;
	}

	public static void pop() {
		if (depth > 0) {
			depth--;
		}
	}

	/** The adjusted hit: {@code changed} is false when nothing applied and vanilla should run as is. */
	public record Result(DamageSource source, float amount, boolean changed) {
	}

	public static Result compute(LivingEntity victim, ServerLevel level, DamageSource source, float amount) {
		Result unchanged = new Result(source, amount, false);
		if (TrimEffects.index().isEmpty() || !Float.isFinite(amount) || amount < 0.0F) {
			return unchanged;
		}
		LivingEntity attacker = attackerOf(victim, source);
		DamageSource rewritten = DamageTypeOverrideEffect.rewrite(level, victim, attacker, source);
		double incoming = DamageInEffect.fraction(victim, attacker, rewritten, amount);
		double outgoing = attacker == null ? 0.0D : DamageOutEffect.fraction(attacker, victim, amount);
		double critical = attacker == null ? 0.0D : CritDamageEffect.fraction(attacker, victim, amount);
		// Every reduction is a fraction of the same hit, so the three factors commute with armour:
		// multiplying before armour gives the same product as vanilla's after-armour pass.
		double scaled = amount * (1.0D + incoming) * (1.0D + outgoing + critical);
		// A negative result would heal; the schema leaves the fractions unclamped, so only the final
		// amount is floored at zero. That floor is also what lets a reduction reach a full 100%: like
		// vanilla's resistance, a large enough fraction lands on 0 instead of going negative.
		float result = scaled <= 0.0D ? 0.0F : (float) Math.min(scaled, Float.MAX_VALUE);
		boolean changed = rewritten != source || Math.abs(result - amount) > 1.0E-4F;
		return new Result(rewritten, result, changed);
	}

	public static LivingEntity attackerOf(LivingEntity victim, DamageSource source) {
		Entity causing = source.getEntity();
		return causing instanceof LivingEntity living && living != victim ? living : null;
	}

	/**
	 * Entries of one type for this entity, from the type's own trigger and from {@code equipped}.
	 *
	 * <p>An entry has exactly one trigger, so the two lists never overlap; the fallback keeps data that
	 * leaves {@code trigger} out (the codec defaults it to {@code equipped}) working.
	 */
	public static List<TrimEffects.Active> effectives(LivingEntity entity, String trigger, String type) {
		return effectives(entity, trigger, type, null);
	}

	/**
	 * Same, with a damage source so that entries carrying {@code conditions} can actually be judged.
	 *
	 * <p>The {@code equipped} fallback deliberately keeps passing no source: an entry written for the passive
	 * trigger is not supposed to be re-judged on every hit, and a condition that cannot be tested is skipped
	 * rather than assumed true.
	 */
	public static List<TrimEffects.Active> effectives(LivingEntity entity, String trigger, String type,
			DamageSource source) {
		List<TrimEffects.Active> direct = TrimEffects.activeOfType(entity, trigger, type, source);
		if (TRIGGER_EQUIPPED.equals(trigger)) {
			return direct;
		}
		List<TrimEffects.Active> equipped = TrimEffects.activeOfType(entity, TRIGGER_EQUIPPED, type);
		if (equipped.isEmpty()) {
			return direct;
		}
		List<TrimEffects.Active> all = new ArrayList<>(direct.size() + equipped.size());
		all.addAll(direct);
		all.addAll(equipped);
		return all;
	}

	/** {@code Value#eval} with the piece count the resolver already reduced; absent amounts are zero. */
	public static double amountOf(TrimEffects.Active active, Map<String, Double> variables) {
		Value amount = active.entry().amount();
		return amount == null ? 0.0D : amount.eval(active.pieces(), variables);
	}

	private DamageEffects() {
	}
}
