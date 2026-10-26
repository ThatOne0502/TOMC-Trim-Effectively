package io.github.thatone0502.trimeffectively.net;

import io.github.thatone0502.trimeffectively.Tre;
import io.github.thatone0502.trimeffectively.config.TreConfig;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The host's configuration, sent to every client so the config screen can show what the world actually runs
 * with instead of what the player's own (frequently irrelevant) file says.
 *
 * <p>Server to client only: a client never sends settings back, and the packet is not a request - the host
 * pushes it when a player joins and again after every data pack reload, so the displayed state cannot drift from
 * the values the recipe table was built from.
 *
 * <p>Every field of {@link TreConfig.Values} gets one field here, written in the same order; the codec is built
 * with {@link StreamCodec#composite} so adding a setting means adding one argument in one place.
 */
public record TreConfigPayload(boolean hijackTrimDuplicationRecipes) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TreConfigPayload> TYPE =
			new CustomPacketPayload.Type<>(Tre.id("config"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TreConfigPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.BOOL, TreConfigPayload::hijackTrimDuplicationRecipes,
					TreConfigPayload::new);

	public static TreConfigPayload of(TreConfig.Values values) {
		return new TreConfigPayload(values.hijackTrimDuplicationRecipes());
	}

	public TreConfig.Values toValues() {
		return new TreConfig.Values(hijackTrimDuplicationRecipes);
	}

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
