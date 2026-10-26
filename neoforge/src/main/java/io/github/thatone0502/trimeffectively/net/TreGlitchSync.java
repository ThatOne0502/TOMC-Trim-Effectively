package io.github.thatone0502.trimeffectively.net;

import io.github.thatone0502.trimeffectively.client.EndphaseTwitch;
import io.github.thatone0502.trimeffectively.effect.EndphaseEffect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server half of the glitch burst: the payload type, and the one place that sends it.
 *
 * <p>Sent from the damage hook, so a glitch happens exactly when the hit lands and never on a timer. That is the
 * whole reason the old per-window schedule is gone: a schedule cannot know when the effect ends on somebody else's
 * client, and kept twitching at victims that no longer had Endphase at all.
 */
public final class TreGlitchSync {
	/** How long a burst lasts, in client ticks - a bit over half a second. */
	public static final int BURST_TICKS = 12;

	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
		event.registrar("1").playToClient(
				TreGlitchPayload.TYPE, TreGlitchPayload.STREAM_CODEC, TreGlitchSync::handle);
	}

	private static void handle(TreGlitchPayload payload, IPayloadContext context) {
		// Only ever invoked on the client; the payload is registered as client-bound.
		context.enqueueWork(() -> EndphaseTwitch.burst(payload.entityId(), payload.length(), payload.seed()));
	}

	/** Starts a burst on every client that can see {@code entity}, including its own if it is a player. */
	public static void send(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		PacketDistributor.sendToPlayersTrackingEntityAndSelf(entity,
				new TreGlitchPayload(entity.getId(), BURST_TICKS, level.getRandom().nextLong()));
	}

	private TreGlitchSync() {
	}
}
