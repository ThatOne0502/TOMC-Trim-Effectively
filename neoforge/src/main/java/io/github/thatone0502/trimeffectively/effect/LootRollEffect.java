package io.github.thatone0502.trimeffectively.effect;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * {@code trimeffectively:extra_loot_roll} - run a loot table {@code rolls} extra times.
 *
 * <p>Instead of re-implementing loot generation, this hooks the single place where a table is actually rolled
 * ({@code LootTable#getRandomItemsRaw(LootContext, Consumer)}) and asks the table for more rolls. The entitlement
 * comes from the loot parameters of that roll: for a killed entity {@code THIS_ENTITY} is the dying entity and the
 * killer is {@code ATTACKING_ENTITY} (or the {@code kill} trigger's {@code LAST_DAMAGE_PLAYER}), while chests,
 * fishing and archaeology pass the player as {@code THIS_ENTITY} and use the {@code loot_roll} trigger. Reading the
 * parameters instead of guessing the caller keeps this correct for every loot source that reports them; unrelated
 * rolls (mob equipment, trade tables) simply carry no wearer and are untouched.
 */
public final class LootRollEffect {
	public static final String TYPE = "extra_loot_roll";
	public static final String TRIGGER_KILL = "kill";
	public static final String TRIGGER_LOOT_ROLL = "loot_roll";

	private static final ThreadLocal<Boolean> REROLLING = ThreadLocal.withInitial(() -> Boolean.FALSE);
	/** Set while a container fills itself, because a container roll carries no player of its own. */
	private static final ThreadLocal<LivingEntity> CONTAINER_PLAYER = new ThreadLocal<>();
	/** How deep in nested table rolls this thread is; only depth zero is a loot generation. */
	private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

	/** Marks the player who opened a container, so the roll it triggers is attributed to them. */
	public static void beginContainer(LivingEntity player) {
		CONTAINER_PLAYER.set(player);
	}

	public static void endContainer() {
		CONTAINER_PLAYER.remove();
	}

	/**
	 * Marks the start of one table roll; {@code true} only for the outermost table of a generation.
	 *
	 * <p>Vanilla tables nest: {@code NestedLootTable#createItemStack} re-enters
	 * {@code LootTable#getRandomItemsRaw} with the <b>same</b> context for every {@code minecraft:nested} pool
	 * entry, so the hook fires once per layer. One catch of the fishing table rolled both
	 * {@code gameplay/fishing} and its nested {@code gameplay/fishing/fish}, which handed a four-piece set eight
	 * extra rolls instead of four. Nesting is how tables are composed, not an error, so the entitlement is
	 * measured per generation: the counter has to stay up for the whole outer body, which is why the mixin
	 * wraps the call instead of hooking its head.
	 */
	public static boolean enter() {
		int depth = DEPTH.get();
		DEPTH.set(depth + 1);
		return depth == 0;
	}

	public static void exit() {
		DEPTH.set(Math.max(0, DEPTH.get() - 1));
	}

	public static void onRoll(LootTable table, LootContext context, Consumer<ItemStack> output) {
		if (Boolean.TRUE.equals(REROLLING.get())) {
			return;
		}
		LivingEntity source = living(context, LootContextParams.THIS_ENTITY);
		if (source == null) {
			// Fishing puts the hook itself in THIS_ENTITY, not the angler, so the wearer has to be read
			// off the hook. Every other owner-based roll (archaeology, shearing) passes the player.
			Object raw = raw(context, LootContextParams.THIS_ENTITY);
			if (raw instanceof net.minecraft.world.entity.projectile.FishingHook hook) {
				source = hook.getPlayerOwner();
			}
		}
		LivingEntity attacker = living(context, LootContextParams.ATTACKING_ENTITY);
		if (attacker == null) {
			attacker = living(context, LootContextParams.DIRECT_ATTACKING_ENTITY);
		}
		LivingEntity lastDamagePlayer = living(context, LootContextParams.LAST_DAMAGE_PLAYER);
		LivingEntity wearer;
		String trigger;
		if (source != null && source.isDeadOrDying()) {
			// A dying entity means this table was rolled for a kill; the wearer is whoever killed it.
			wearer = attacker != null ? attacker : lastDamagePlayer;
			trigger = TRIGGER_KILL;
			if (wearer == source) {
				wearer = null;
			}
		} else if (source != null) {
			// Fishing, archaeology and any other roll the player themselves performs: the wearer is the
			// rolling entity, which is the same object vanilla put in THIS_ENTITY.
			wearer = source;
			trigger = TRIGGER_LOOT_ROLL;
		} else {
			// A container filling itself carries no entity at all; the opening player was parked on the
			// thread by ContainerLootMixin before vanilla started rolling.
			wearer = CONTAINER_PLAYER.get();
			trigger = TRIGGER_LOOT_ROLL;
		}
		if (wearer == null) {
			return;
		}
		List<TrimEffects.Active> actives = DamageEffects.effectives(wearer, trigger, TYPE);
		if (actives.isEmpty()) {
			return;
		}
		Map<String, Double> variables = TrimEffects.variables(wearer);
		double total = 0.0D;
		for (TrimEffects.Active active : actives) {
			if (active.entry().rolls() == null) {
				continue;
			}
			total += active.entry().rolls().eval(active.pieces(), variables);
		}
		if (total <= 0.0D) {
			return;
		}
		REROLLING.set(Boolean.TRUE);
		try {
			for (int i = 0; i < (int) Math.floor(total); i++) {
				table.getRandomItemsRaw(context, output);
			}
		} finally {
			REROLLING.set(Boolean.FALSE);
		}
	}

	private static LivingEntity living(LootContext context, ContextKey<?> key) {
		return raw(context, key) instanceof LivingEntity living ? living : null;
	}

	/** The raw parameter, or null when the table was rolled without it. */
	private static Object raw(LootContext context, ContextKey<?> key) {
		try {
			return context.getOptionalParameter(key);
		} catch (RuntimeException exception) {
			return null;
		}
	}

	private LootRollEffect() {
	}
}
