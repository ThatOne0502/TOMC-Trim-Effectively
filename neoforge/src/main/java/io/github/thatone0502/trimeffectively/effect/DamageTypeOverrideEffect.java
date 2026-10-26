package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * {@code trimeffectively:damage_type_override} - replaces the type of a hit with {@code damage_type}.
 *
 * <p>Read on {@code attack_out} for the wearer as the attacker and on {@code take_damage_before} for the
 * wearer as the victim, so a pattern can rewrite either side of the exchange. The victim's own rule wins
 * when both sides carry one, since the type is what the victim is hurt by.
 *
 * <p>The first entry that names a type the registry knows is used; an unknown or absent
 * {@code damage_type} leaves the hit untouched. {@code keep_attacker} (default false) decides whether
 * the original attacker survives the rewrite, which matters for aggro, death messages and damage credit.
 */
public final class DamageTypeOverrideEffect {
	public static final String TYPE = DamageEffects.TYPE_DAMAGE_TYPE_OVERRIDE;

	/** The type the hit really carries, or {@code source} when nothing applies. */
	public static DamageSource rewrite(ServerLevel level, LivingEntity victim, LivingEntity attacker,
			DamageSource source) {
		DamageSource current = source;
		if (attacker != null) {
			current = from(level, attacker, DamageEffects.TRIGGER_ATTACK_OUT, current);
		}
		return from(level, victim, DamageEffects.TRIGGER_BEFORE, current);
	}

	private static DamageSource from(ServerLevel level, LivingEntity wearer, String trigger, DamageSource source) {
		List<TrimEffects.Active> actives = DamageEffects.effectives(wearer, trigger, TYPE, source);
		if (actives.isEmpty()) {
			return source;
		}
		Registry<DamageType> registry = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
		for (TrimEffects.Active active : actives) {
			Identifier id = active.entry().damageType();
			if (id == null) {
				continue;
			}
			ResourceKey<DamageType> key = ResourceKey.create(Registries.DAMAGE_TYPE, id);
			if (source.is(key)) {
				continue;
			}
			Holder<DamageType> type = registry.get(key).orElse(null);
			if (type == null) {
				continue;
			}
			return active.entry().keepAttacker()
					? new DamageSource(type, source.getDirectEntity(), source.getEntity())
					: new DamageSource(type);
		}
		return source;
	}

	private DamageTypeOverrideEffect() {
	}
}
