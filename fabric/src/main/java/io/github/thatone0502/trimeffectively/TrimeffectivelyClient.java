package io.github.thatone0502.trimeffectively;

import io.github.thatone0502.trimeffectively.client.SoundAnnotations;
import io.github.thatone0502.trimeffectively.client.TreClientConfig;
import io.github.thatone0502.trimeffectively.client.TrimTooltip;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entrypoint: tooltip replacement and the reveal key binding. */
public class TrimeffectivelyClient implements ClientModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("trimeffectively/client");

	@Override
	public void onInitializeClient() {
		TrimTooltip.register();
		SoundAnnotations.register();
		TreClientConfig.register();
		LOGGER.info("TOMC-Trim Effectively client ready");
	}
}
