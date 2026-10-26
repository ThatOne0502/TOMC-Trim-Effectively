package io.github.thatone0502.trimeffectively.mixin;

import java.util.Map;

import io.github.thatone0502.trimeffectively.sound.TreSounds;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Never two copies of the same sting at once.
 *
 * <p>The stare loop re-asks for the sound every half second while the effect is frozen, because that is the only way
 * to make the loop endless without knowing how long the file is - and it cannot be known, since a resource pack may
 * replace it with something of any length. So the decision is made here instead, where it can be answered exactly:
 * {@code instanceToChannel} holds every sound that is actually sounding right now, so a request for a sound whose
 * event is already in that map is dropped. Whatever length the pack ships, the loop is continuous and never doubles.
 */
@Mixin(SoundEngine.class)
public abstract class SoundEngineDedupeMixin {
	/**
	 * Erased to {@code Map}: the value type is a package-private channel handle that this mixin has no reason to name,
	 * and mixin matches a shadow by name and erased descriptor.
	 */
	@Shadow
	@Final
	private Map<SoundInstance, ?> instanceToChannel;

	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void trimeffectively$skipDuplicate(SoundInstance instance,
			CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		Identifier wanted = instance.getIdentifier();
		if (!TreSounds.isDeduplicated(wanted)) {
			return;
		}
		for (SoundInstance active : this.instanceToChannel.keySet()) {
			if (active != instance && wanted.equals(active.getIdentifier())) {
				cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
				return;
			}
		}
	}
}
