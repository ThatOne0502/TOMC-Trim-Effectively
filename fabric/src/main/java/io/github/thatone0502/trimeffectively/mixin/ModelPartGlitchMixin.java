package io.github.thatone0502.trimeffectively.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.thatone0502.trimeffectively.client.PartGlitch;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the per-part dislocation of a glitch burst.
 *
 * <p>The hook is at the end of {@code translateAndRotate}, which is the one method every model part runs through on
 * its way to the screen - both the immediate {@code render} path and the {@code visit} path used when a model is
 * submitted into a node collector. Hooking it once covers every mob and every layer (armour, held items, elytra)
 * without a mixin per renderer.
 */
@Mixin(ModelPart.class)
public class ModelPartGlitchMixin {

	@Inject(method = "translateAndRotate(Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("TAIL"))
	private void trimeffectively$dislocate(PoseStack pose, CallbackInfo ci) {
		PartGlitch.apply((ModelPart) (Object) this, pose);
	}
}
