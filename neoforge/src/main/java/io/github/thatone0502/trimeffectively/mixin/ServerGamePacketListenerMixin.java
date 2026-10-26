package io.github.thatone0502.trimeffectively.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.thatone0502.trimeffectively.effect.ExtraJumps;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

/**
 * Feeds the multi-jump with the player's real jump key state: the client only sends this packet when its input
 * changes, so a packet with {@code jump} set while airborne is exactly one press of the key. Sampling the
 * vanilla {@code isJumping} flag in {@code aiStep} alone would miss presses that the server never mirrors.
 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
	@Shadow
	@Final
	private ServerPlayer player;

	@Inject(method = "handlePlayerInput", at = @At("TAIL"))
	private void trimeffectively$jumpRequest(ServerboundPlayerInputPacket packet, CallbackInfo ci) {
		if (packet.input().jump()) {
			ExtraJumps.requestJump(player);
		}
	}
}
