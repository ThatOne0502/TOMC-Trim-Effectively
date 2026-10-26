package io.github.thatone0502.trimeffectively.effect;

import com.mojang.serialization.MapCodec;
import io.github.thatone0502.trimeffectively.Tre;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registry holders for everything this mod adds outside the vanilla namespace. */
public final class TreConditions {
	public static MapCodec<? extends LootItemCondition> FACING_RESPAWN;

	/** Must run before any datapack is loaded, otherwise condition ids fail to decode. */
	public static void register(RegisterEvent event) {
		// Since 26.1 the condition registry holds the map codecs themselves rather than a wrapper type, so
		// what is registered here is the codec and nothing else.
		Registry<MapCodec<? extends LootItemCondition>> registry =
				event.getRegistry(BuiltInRegistries.LOOT_CONDITION_TYPE.key());
		if (registry == null) {
			return;
		}
		FACING_RESPAWN = Registry.register(registry, Tre.id("facing_respawn"),
				FacingRespawnCondition.CODEC);
	}

	private TreConditions() {
	}
}
