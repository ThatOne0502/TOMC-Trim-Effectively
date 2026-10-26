package io.github.thatone0502.trimeffectively.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import io.github.thatone0502.trimeffectively.client.EndphaseTwitch;
import io.github.thatone0502.trimeffectively.client.PartGlitch;
import io.github.thatone0502.trimeffectively.client.TreTwitchState;
import io.github.thatone0502.trimeffectively.client.TwitchFrame;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The visible half of a glitch burst: the body tears itself apart for a dozen frames.
 *
 * <p>{@code getRenderType} is the one hook that covers every living renderer - the texture lookup happens inside it,
 * not in each renderer's own code - so the checker swap is a single expression rewrite here rather than a mixin per
 * mob. The deformation is applied around {@code submit}, which is where the pose stack for this entity is still
 * ours to push and pop, and the per-part dislocation is switched on for exactly that call (see {@link PartGlitch}).
 */
@Mixin(LivingEntityRenderer.class)
public class LivingEntityRendererTwitchMixin {

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
			at = @At("TAIL"))
	private void trimeffectively$decideGlitch(LivingEntity entity, LivingEntityRenderState state, float partialTick,
			CallbackInfo ci) {
		EndphaseTwitch.update(entity, (TreTwitchState) (Object) state);
	}

	@ModifyExpressionValue(method = "getRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/renderer/rendertype/RenderType;",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getTextureLocation(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;)Lnet/minecraft/resources/Identifier;"))
	private Identifier trimeffectively$checkerTexture(Identifier original, LivingEntityRenderState state) {
		TwitchFrame frame = ((TreTwitchState) (Object) state).trimeffectively$frame();
		return frame != null && frame.texture() ? EndphaseTwitch.MISSING_TEXTURE : original;
	}

	@WrapMethod(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V")
	private void trimeffectively$glitchPose(LivingEntityRenderState state, PoseStack pose,
			SubmitNodeCollector collector, CameraRenderState camera, Operation<Void> original) {
		TwitchFrame frame = ((TreTwitchState) (Object) state).trimeffectively$frame();
		if (frame == null || frame.texture()) {
			original.call(state, pose, collector, camera);
			return;
		}
		pose.pushPose();
		pose.translate(frame.x(), frame.y(), frame.z());
		pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(frame.yaw())));
		pose.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(frame.roll())));
		// A vertical squash rather than a uniform scale: the body folds in on itself at the moment of the hit and
		// stretches back out, which reads even at a distance where a few centimetres of offset do not.
		pose.scale(1.0F, frame.squash(), 1.0F);
		PartGlitch.begin(frame.seed(), frame.tick());
		try {
			original.call(state, pose, collector, camera);
		} finally {
			PartGlitch.end();
			pose.popPose();
		}
	}
}
