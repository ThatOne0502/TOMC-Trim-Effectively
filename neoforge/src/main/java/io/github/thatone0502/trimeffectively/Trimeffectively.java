package io.github.thatone0502.trimeffectively;

import io.github.thatone0502.trimeffectively.command.TreCommand;
import io.github.thatone0502.trimeffectively.data.TrimComponents;
import io.github.thatone0502.trimeffectively.data.TreRegistries;
import io.github.thatone0502.trimeffectively.effect.EndphaseEffect;
import io.github.thatone0502.trimeffectively.effect.EndphasePotions;
import io.github.thatone0502.trimeffectively.effect.TreAttributes;
import io.github.thatone0502.trimeffectively.effect.TreConditions;
import io.github.thatone0502.trimeffectively.engine.PortalTracker;
import io.github.thatone0502.trimeffectively.engine.TreEngine;
import io.github.thatone0502.trimeffectively.net.TreConfigSync;
import io.github.thatone0502.trimeffectively.net.TreGlitchSync;
import io.github.thatone0502.trimeffectively.recipe.TreRecipeSerializers;
import io.github.thatone0502.trimeffectively.sound.TreSounds;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Trimeffectively.MOD_ID)
public class Trimeffectively {
	public static final String MOD_ID = Tre.MOD_ID;
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public Trimeffectively(IEventBus modBus) {
		// NeoForge freezes the vanilla registries before mod code runs, so nothing that lives in one of them can
		// be registered by a static initialiser any more: every element is created from the register event of its
		// own registry instead. The order in which these run is the registry order, and our own effect is
		// registered before the potions that hold it because the effect registry is created first.
		modBus.addListener(TreConditions::register);
		modBus.addListener(TreAttributes::register);
		modBus.addListener(TreAttributes::modifyAttributes);
		modBus.addListener(TreSounds::register);
		modBus.addListener(TrimComponents::register);
		modBus.addListener(TreRecipeSerializers::register);
		modBus.addListener(EndphaseEffect::register);
		modBus.addListener(EndphasePotions::register);
		modBus.addListener(PortalTracker::register);
		// Datapack registries first, then the payloads, so a joining client can decode both as soon as it is told.
		modBus.addListener(TreRegistries::register);
		modBus.addListener(TreConfigSync::registerPayloads);
		modBus.addListener(TreGlitchSync::registerPayloads);

		// Game bus: world lifecycle, player lifecycle and the per-tick pass.
		NeoForge.EVENT_BUS.addListener(PortalTracker::onChangedDimension);
		NeoForge.EVENT_BUS.addListener(PortalTracker::onLogin);
		NeoForge.EVENT_BUS.addListener(TreConfigSync::onDatapackSync);
		NeoForge.EVENT_BUS.addListener(TreCommand::init);
		TreEngine.init();

		LOGGER.info("TOMC-Trim Effectively loaded");
	}
}
