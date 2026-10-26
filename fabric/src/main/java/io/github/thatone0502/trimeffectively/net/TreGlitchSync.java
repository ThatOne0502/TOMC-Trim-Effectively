package io.github.thatone0502.trimeffectively.net;

import io.github.thatone0502.trimeffectively.effect.EndphaseEffect;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

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

	public static void register() {
		PayloadTypeRegistry.playS2C().register(TreGlitchPayload.TYPE, TreGlitchPayload.STREAM_CODEC);
	}

	/** Starts a burst on every client that can see {@code entity}, including its own if it is a player. */
	public static void send(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		TreGlitchPayload payload =
				new TreGlitchPayload(entity.getId(), BURST_TICKS, level.getRandom().nextLong());
		for (ServerPlayer player : PlayerLookup.tracking(entity)) {
			ServerPlayNetworking.send(player, payload);
		}
		if (entity instanceof ServerPlayer self) {
			ServerPlayNetworking.send(self, payload);
		}
	}

	private TreGlitchSync() {
	}
}
