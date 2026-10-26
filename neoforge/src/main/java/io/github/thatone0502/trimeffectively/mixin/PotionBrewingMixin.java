package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.EndphasePotions;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Brewing recipes for Endphase, appended to the vanilla set.
 *
 * <p>{@code PotionBrewing#addVanillaMixes} is where the vanilla mixes are declared; it is a static method taking
 * the builder that becomes the brewing registry, so its tail is the one place where extra mixes belong without
 * re-implementing any of the brewing logic.
 */
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {

	@Inject(method = "addVanillaMixes", at = @At("TAIL"))
	private static void trimeffectively$endphaseMixes(PotionBrewing.Builder builder, CallbackInfo ci) {
		EndphasePotions.addMixes(builder);
	}
}
