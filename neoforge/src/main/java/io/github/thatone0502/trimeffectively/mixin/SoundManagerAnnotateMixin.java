package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.client.SoundAnnotations;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code trimeffectively:annotate_sound} - every sound this client starts playing passes through
 * {@code SoundManager#play}, whether it came from the server or was predicted locally, which makes it the only hook
 * that will not miss sounds. Client only: the whole annotation lives on this client.
 *
 * <p>The handler takes a {@code CallbackInfoReturnable} even though it never sets a return value: {@code play}
 * returns {@code SoundEngine.PlayResult}, and mixin rejects a plain {@code CallbackInfo} for a non-void target
 * before injection is even attempted.
 */
@Mixin(SoundManager.class)
public abstract class SoundManagerAnnotateMixin {

	@Inject(method = "play", at = @At("HEAD"))
	private void trimeffectively$annotateSound(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		SoundAnnotations.onSoundPlayed(instance);
	}
}
