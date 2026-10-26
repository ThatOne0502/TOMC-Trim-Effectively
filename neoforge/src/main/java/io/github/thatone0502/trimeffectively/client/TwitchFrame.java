package io.github.thatone0502.trimeffectively.client;

/**
 * One frame of a glitch burst, carried on the render state.
 *
 * <p>Either the whole skin is replaced by the checker ({@link #texture()}) or the body is deformed by the numbers
 * here; the pose work itself happens in the renderer mixin, which is the only place the pose stack is reachable.
 *
 * @param texture show the missing-texture checker instead of the skin
 * @param x       whole-body displacement, in blocks
 * @param y       whole-body displacement, in blocks
 * @param z       whole-body displacement, in blocks
 * @param yaw     twist around the vertical axis, in degrees
 * @param roll    lean around the view axis, in degrees
 * @param squash  vertical scale factor; below 1 folds the body, above 1 stretches it
 * @param seed    the burst's seed, shared by the per-part offsets
 * @param tick    how far into the burst this frame is, in client ticks
 */
public record TwitchFrame(boolean texture, float x, float y, float z, float yaw, float roll, float squash,
		long seed, int tick) {

	/** A frame that replaces the skin with the checker and touches nothing else. */
	public static TwitchFrame checker() {
		return new TwitchFrame(true, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0L, 0);
	}
}
