package io.github.thatone0502.trimeffectively.engine;

import io.github.thatone0502.trimeffectively.Trimeffectively;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.context.ContextKeySet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.List;
import java.util.Optional;

/**
 * Evaluates vanilla loot conditions against an entity, outside of a loot roll.
 *
 * <p>The conditions in our JSON are plain {@code LootItemCondition}s, so the only thing needed is a
 * {@link LootContext}. Vanilla's predefined parameter sets either require a block or demand keys we
 * cannot supply, so this builds its own set: {@code this_entity} is mandatory, everything else a
 * condition might want is merely allowed and filled when we have it.
 */
public final class Conditions {
	private static final ContextKeySet KEYS = new ContextKeySet.Builder()
			.required(LootContextParams.THIS_ENTITY)
			.optional(LootContextParams.ORIGIN)
			.optional(LootContextParams.DAMAGE_SOURCE)
			.optional(LootContextParams.ATTACKING_ENTITY)
			.optional(LootContextParams.DIRECT_ATTACKING_ENTITY)
			.optional(LootContextParams.TARGET_ENTITY)
			.optional(LootContextParams.INTERACTING_ENTITY)
			.optional(LootContextParams.LAST_DAMAGE_PLAYER)
			.optional(LootContextParams.TOOL)
			.optional(LootContextParams.BLOCK_STATE)
			.optional(LootContextParams.BLOCK_ENTITY)
			.optional(LootContextParams.EXPLOSION_RADIUS)
			.optional(LootContextParams.ENCHANTMENT_LEVEL)
			.optional(LootContextParams.ENCHANTMENT_ACTIVE)
			.build();

	private static boolean warned;

	/** True when every condition passes; an empty list always passes. */
	public static boolean test(List<LootItemCondition> conditions, LivingEntity entity, DamageSource source) {
		if (conditions.isEmpty()) {
			return true;
		}
		if (!(entity.level() instanceof ServerLevel level)) {
			return false;
		}
		try {
			LootParams.Builder params = new LootParams.Builder(level)
					.withParameter(LootContextParams.THIS_ENTITY, entity)
					.withOptionalParameter(LootContextParams.ORIGIN, entity.position());
			if (source != null) {
				params.withOptionalParameter(LootContextParams.DAMAGE_SOURCE, source)
						.withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
						.withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity());
			}
			LootContext context = new LootContext.Builder(params.create(KEYS)).create(Optional.empty());
			for (LootItemCondition condition : conditions) {
				if (!condition.test(context)) {
					return false;
				}
			}
			return true;
		} catch (RuntimeException exception) {
			if (!warned) {
				warned = true;
				Trimeffectively.LOGGER.warn("[tre] condition evaluation failed: {}", exception.toString());
			}
			return false;
		}
	}
}
