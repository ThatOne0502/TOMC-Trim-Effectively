package io.github.thatone0502.trimeffectively;

import io.github.thatone0502.trimeffectively.command.TreCommand;
import io.github.thatone0502.trimeffectively.data.TrimComponents;
import io.github.thatone0502.trimeffectively.data.TreRegistries;
import io.github.thatone0502.trimeffectively.effect.DamageEffects;
import io.github.thatone0502.trimeffectively.effect.TreAttributes;
import io.github.thatone0502.trimeffectively.effect.TreConditions;
import io.github.thatone0502.trimeffectively.engine.PortalTracker;
import io.github.thatone0502.trimeffectively.engine.TreEngine;
import io.github.thatone0502.trimeffectively.net.TreConfigSync;
import io.github.thatone0502.trimeffectively.recipe.TreRecipeSerializers;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Trimeffectively implements ModInitializer {
	public static final String MOD_ID = Tre.MOD_ID;
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// Order matters: the custom loot condition and the mob effect have to exist before any datapack
		// (including our own, and including other mods' data) is decoded.
		TreConditions.init();
		TreAttributes.init();
		TrimComponents.init();
		// Must precede the datapack load for the same reason: a recipe JSON naming a serializer that does not
		// exist yet fails to decode.
		TreRecipeSerializers.init();
		TreRegistries.init();
		PortalTracker.register();
		TreEngine.init();
		TreCommand.init();
		// Reads the config before anything can consult it (TreRecipes asks on every recipe reload anyway), and
		// registers the S2C payload so a joining client learns what this world is running.
		TreConfigSync.register();
		LOGGER.info("TOMC-Trim Effectively loaded");
	}
}
