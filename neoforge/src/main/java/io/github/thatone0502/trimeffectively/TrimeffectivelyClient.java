package io.github.thatone0502.trimeffectively.client;

import io.github.thatone0502.trimeffectively.Tre;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client-only entry point. Loaded only on the physical client, so nothing here needs a dist check.
 */
@Mod(value = TrimeffectivelyClient.MOD_ID, dist = Dist.CLIENT)
public final class TrimeffectivelyClient {
	public static final String MOD_ID = Tre.MOD_ID;
	public static final Logger LOGGER = LoggerFactory.getLogger("trimeffectively/client");

	public TrimeffectivelyClient(IEventBus modBus, ModContainer container) {
		TrimTooltip.register();
		SoundAnnotations.register(modBus);
		TreClientConfig.register();
		// Key mappings live in a registry, so they are added from its register event rather than at class init.
		modBus.addListener(TrimTooltip::registerKeyMappings);

		// Config screen: NeoForge's counterpart of Fabric's Mod Menu entrypoint is a per-mod extension point,
		// which the mod list picks up on its own and turns into a "Config" button.
		//
		// Cloth is optional, and this class must never load without it: the factory is only registered when the
		// mod is actually present, so TreConfigScreen (which references Cloth types) stays unloaded otherwise.
		if (ModList.get().isLoaded("cloth_config")) {
			container.registerExtensionPoint(IConfigScreenFactory.class,
					(IConfigScreenFactory) (modContainer, parent) -> TreConfigScreen.create(parent));
		} else {
			LOGGER.info("cloth-config is not installed, so no config screen is offered; "
					+ "edit config/trimeffectively.json by hand instead");
		}

		LOGGER.info("TOMC-Trim Effectively client ready");
	}
}
