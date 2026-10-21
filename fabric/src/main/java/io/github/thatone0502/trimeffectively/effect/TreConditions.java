package io.github.thatone0502.trimeffectively.effect;

import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

/** Registry holders for everything this mod adds outside the vanilla namespace. */
public final class TreConditions {
	public static LootItemConditionType FACING_RESPAWN;

	/** Must run before any datapack is loaded, otherwise condition ids fail to decode. */
	public static void init() {
		FACING_RESPAWN = FacingRespawnCondition.register();
		EndphaseEffect.register();
		// Potions refer to the effect holder, so this has to come after the effect itself.
		EndphasePotions.register();
	}

	private TreConditions() {
	}
}
