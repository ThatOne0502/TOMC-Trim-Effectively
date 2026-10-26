package io.github.thatone0502.trimeffectively;

import io.github.thatone0502.trimeffectively.client.EndphaseTwitch;
import io.github.thatone0502.trimeffectively.client.SoundAnnotations;
import io.github.thatone0502.trimeffectively.client.TreClientConfig;
import io.github.thatone0502.trimeffectively.client.TrimTooltip;
import io.github.thatone0502.trimeffectively.net.TreGlitchPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Client entrypoint: tooltip replacement, the reveal key binding, sound annotations, and glitch bursts. */
public class TrimeffectivelyClient implements ClientModInitializer {

	public static final Logger LOGGER = LoggerFactory.getLogger("trimeffectively/client");

	@Override
	public void onInitializeClient() {
		TrimTooltip.register();
		SoundAnnotations.register();
		TreClientConfig.register();
		// The payload type itself is registered on both sides in the common initialiser; only the handler is here,
		// because only the client has a render state to fill.
		ClientPlayNetworking.registerGlobalReceiver(TreGlitchPayload.TYPE, (payload, context) ->
				context.client().execute(() ->
						EndphaseTwitch.burst(payload.entityId(), payload.length(), payload.seed())));
		LOGGER.info("TOMC-Trim Effectively client ready");
	}
}
