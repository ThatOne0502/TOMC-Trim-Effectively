package io.github.thatone0502.trimeffectively.client;

/**
 * The per-frame glitch decision, carried on the render state.
 *
 * <p>A render state is extracted once per entity per frame and then handed to {@code submit}, so it is the only
 * place where the entity and the pose stack are both reachable. {@link EndphaseTwitch} fills this in during
 * {@code extractRenderState}, the renderer mixin reads it back during {@code submit} and {@code getRenderType}.
 */
public interface TreTwitchState {
	/** Sets this frame's frame, or clears it with {@code null}. */
	void trimeffectively$setFrame(TwitchFrame frame);

	/** This frame, or {@code null} when there is nothing to draw. */
	TwitchFrame trimeffectively$frame();
}
