package io.github.thatone0502.trimeffectively.net;

import io.github.thatone0502.trimeffectively.Tre;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Tells the clients that an entity just glitched, and for how long.
 *
 * <p>The burst is an event, not a state: the server decides the instant the damage lands (that is also the instant
 * the glitch sound is chosen), and every client that can see the entity is told to start the same burst at the same
 * moment. Nothing is stored per client beyond the burst itself, and nothing has to be kept in sync afterwards -
 * when the burst is over it is over, whether or not the effect survived it.
 *
 * <p>{@code seed} is what makes it look like one event instead of one effect per viewer: the displacement, the
 * per-part offsets and the rolling/folding of the body are all derived from it, so two players standing next to
 * each other see the same corpse-like collapse.
 */
public record TreGlitchPayload(int entityId, int length, long seed) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<TreGlitchPayload> TYPE =
			new CustomPacketPayload.Type<>(Tre.id("glitch"));

	public static final StreamCodec<RegistryFriendlyByteBuf, TreGlitchPayload> STREAM_CODEC =
			StreamCodec.composite(
					ByteBufCodecs.VAR_INT, TreGlitchPayload::entityId,
					ByteBufCodecs.VAR_INT, TreGlitchPayload::length,
					ByteBufCodecs.VAR_LONG, TreGlitchPayload::seed,
					TreGlitchPayload::new);

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
