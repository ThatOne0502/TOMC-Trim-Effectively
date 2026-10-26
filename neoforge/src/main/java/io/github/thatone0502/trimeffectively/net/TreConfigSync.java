package io.github.thatone0502.trimeffectively.net;

import io.github.thatone0502.trimeffectively.client.TreClientConfig;
import io.github.thatone0502.trimeffectively.config.TreConfig;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server half of the config sync.
 *
 * <p>The payload type is registered on both sides here (the registry is common code; only the handler lives on
 * the client), and the actual sending happens at the two moments the data can be needed: when the datapacks are
 * synced to a joining player - which is the same point the recipe table is sent - and again after a reload, when
 * the recipe table was just rebuilt from the file and the values a client is showing could have become stale.
 *
 * <p>Nothing is sent the other way. A client cannot ask for the values and cannot propose new ones: the host
 * owns the file, and this is the only direction in which it is ever read by somebody else.
 */
public final class TreConfigSync {
	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
		event.registrar("1").playToClient(
				TreConfigPayload.TYPE, TreConfigPayload.STREAM_CODEC, TreConfigSync::handle);
	}

	private static void handle(TreConfigPayload payload, IPayloadContext context) {
		// Only ever invoked on the client; the payload is registered as client-bound.
		context.enqueueWork(() -> TreClientConfig.accept(payload));
	}

	/** Fires for the joining player and for everyone when the datapacks are reloaded. */
	public static void onDatapackSync(OnDatapackSyncEvent event) {
		TreConfigPayload payload = TreConfigPayload.of(TreConfig.read());
		event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
	}

	/** Sends the current file contents to everyone connected. */
	public static void broadcast(MinecraftServer server) {
		PacketDistributor.sendToAllPlayers(TreConfigPayload.of(TreConfig.read()));
	}

	private TreConfigSync() {
	}
}
