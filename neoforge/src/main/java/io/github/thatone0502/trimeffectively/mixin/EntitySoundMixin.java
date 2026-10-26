package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.SoundMute;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trimeffectively:mute_sound} — the two emissions a wearer can be silenced at.
 *
 * <p>{@code Entity#playSound} is where an entity turns a sound event into an actual world sound: the server side of it
 * broadcasts to every player except the wearer's own client, so cancelling here keeps the sound from being heard by
 * others, while the same injection on a client cancels the locally predicted sound of the local player — together those
 * are both halves of "the wearer's own sounds are silent". Cancelling at the level instead would not work: the level
 * overloads that carry coordinates have no idea which entity emitted the sound.
 *
 * <p>{@code Entity#gameEvent} is what the sculk system actually listens to (the warden's vibration listener consumes
 * game events, never sounds), so {@code scope: "both"} and {@code scope: "vibration"} are honoured there.
 *
 * <p>Both targets are named by full descriptor, not by bare name: {@code playSound} and {@code gameEvent} each have
 * two overloads in 1.21.11, and a bare name makes mixin resolve the wrong one and then reject our handler's
 * parameter list.
 */
@Mixin(Entity.class)
public abstract class EntitySoundMixin {

	@Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V", at = @At("HEAD"), cancellable = true)
	private void trimeffectively$muteOwnSound(SoundEvent sound, float volume, float pitch, CallbackInfo callback) {
		Entity self = (Entity) (Object) this;
		boolean clientSide = self.level() != null && self.level().isClientSide();
		if (SoundMute.mutesAudible(self, clientSide)) {
			callback.cancel();
		}
	}

	@Inject(method = "gameEvent(Lnet/minecraft/core/Holder;)V", at = @At("HEAD"), cancellable = true)
	private void trimeffectively$muteOwnGameEvent(Holder<GameEvent> event, CallbackInfo callback) {
		Entity self = (Entity) (Object) this;
		if (SoundMute.mutesVibration(self)) {
			callback.cancel();
		}
	}
}
