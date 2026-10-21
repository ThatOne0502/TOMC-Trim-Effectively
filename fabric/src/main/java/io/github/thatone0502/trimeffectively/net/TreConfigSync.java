package io.github.thatone0502.trimeffectively.net;

import io.github.thatone0502.trimeffectively.config.TreConfig;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server half of the config sync.
 *
 * <p>The payload type is registered on both sides here (the registry is common code; only the handler lives on
 * the client), and the actual sending happens at two points:
 *
 * <ul>
 *   <li>on join, so the config screen has real numbers the first time it is opened;</li>
 *   <li>after a data pack reload, because that is exactly when the recipe table was rebuilt from the file and
 *       therefore exactly when the values a client is showing could have become stale.</li>
 * </ul>
 *
 * <p>Nothing is sent the other way. A client cannot ask for the values and cannot propose new ones: the host
 * owns the file, and this is the only direction in which it is ever read by somebody else.
 */
public final class TreConfigSync {
	public static void register() {
		PayloadTypeRegistry.playS2C().register(TreConfigPayload.TYPE, TreConfigPayload.STREAM_CODEC);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
				ServerPlayNetworking.send(handler.player, TreConfigPayload.of(TreConfig.read())));

		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if (success && server != null) {
				broadcast(server);
			}
		});
	}

	/** Sends the current file contents to everyone connected. */
	public static void broadcast(MinecraftServer server) {
		TreConfigPayload payload = TreConfigPayload.of(TreConfig.read());
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			ServerPlayNetworking.send(player, payload);
		}
	}

	private TreConfigSync() {
	}
}
