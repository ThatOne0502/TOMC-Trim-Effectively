package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.SoundMute;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The vibration half of {@code trimeffectively:mute_sound}, at the level the sculk system actually reads.
 *
 * <p>{@code Entity#gameEvent(Holder)} only covers the entity-centric convenience overload. Footsteps do not go
 * through it: {@code Entity#walkingStepSound} calls {@code Level#gameEvent(GameEvent, Vec3, GameEvent.Context)}
 * directly, which is why a fully silenced player still tripped sculk sensors. Every event that carries a position
 * lands on {@code ServerLevel#gameEvent(Holder, Vec3, Context)} - the only place the dispatcher is fed - and the
 * context still names the emitting entity through {@link GameEvent.Context#sourceEntity()}.
 *
 * <p>Events with no source entity (blocks reacting to each other, for instance) are left alone: nobody is wearing
 * anything there.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelGameEventMixin {

	@Inject(method = "gameEvent(Lnet/minecraft/core/Holder;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V",
			at = @At("HEAD"), cancellable = true)
	private void trimeffectively$muteVibration(Holder<GameEvent> event, Vec3 position, GameEvent.Context context,
			CallbackInfo callback) {
		if (context == null) {
			return;
		}
		Entity source = context.sourceEntity();
		if (source != null && SoundMute.mutesVibration(source)) {
			callback.cancel();
		}
	}
}
