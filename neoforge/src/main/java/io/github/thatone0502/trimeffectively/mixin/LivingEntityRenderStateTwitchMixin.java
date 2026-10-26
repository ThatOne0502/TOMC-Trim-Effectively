package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.client.TreTwitchState;
import io.github.thatone0502.trimeffectively.client.TwitchFrame;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Storage for {@link TreTwitchState} on the vanilla render state.
 *
 * <p>The field has to live here rather than in a side table because the render state is reused per entity per
 * frame: whatever {@code extractRenderState} writes is read back by {@code submit} later in the same frame, and a
 * map keyed by the state would only add allocation and a leak to be cleaned up.
 *
 * <p>One field, not eight: the frame is one immutable value, and null is "nothing to draw", which is also the
 * answer for every entity that is not mid-burst - the common case, and the one that must cost nothing.
 */
@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateTwitchMixin implements TreTwitchState {
	@Unique
	private TwitchFrame trimeffectively$frame;

	@Override
	public void trimeffectively$setFrame(TwitchFrame frame) {
		this.trimeffectively$frame = frame;
	}

	@Override
	public TwitchFrame trimeffectively$frame() {
		return this.trimeffectively$frame;
	}
}
