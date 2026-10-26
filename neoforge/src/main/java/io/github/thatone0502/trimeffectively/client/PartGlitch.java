package io.github.thatone0502.trimeffectively.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.RandomSource;

/**
 * Dislocates individual model parts while a glitch burst is being drawn.
 *
 * <p>{@code ModelPart#translateAndRotate} is called once per part per draw, after the part's own offset and rotation
 * have been applied, so translating there moves that part and everything below it while leaving its siblings where
 * they are. That is what turns a sliding whole body into a body whose arm, head and legs disagree about where the
 * body is.
 *
 * <p>The window is a static pair of flags set around the entity's own {@code submit} call and cleared in a
 * {@code finally}, so a part drawn for any other entity - or for the same entity outside a burst - is untouched. The
 * render thread is the only thread that reaches here.
 *
 * <p>The offsets are derived from the part's identity hash and the burst seed, so a part keeps its own direction for
 * the whole burst (it is the <em>same</em> arm that keeps being in the wrong place) while each frame picks a new
 * distance, which reads as a broken rig rather than as noise.
 */
public final class PartGlitch {
	/** Peak per-part displacement, in blocks: 0.06 is a little under one model pixel. */
	private static final float OFFSET = 0.06F;

	private static boolean active;
	private static long seed;
	private static int tick;

	/** Opens the window for one entity's draw. */
	public static void begin(long seed, int tick) {
		PartGlitch.active = true;
		PartGlitch.seed = seed;
		PartGlitch.tick = tick;
	}

	/** Closes it again. */
	public static void end() {
		PartGlitch.active = false;
	}

	/** Applied at the end of {@code ModelPart#translateAndRotate}; a no-op outside a burst. */
	public static void apply(ModelPart part, PoseStack pose) {
		if (!active) {
			return;
		}
		long partSeed = seed ^ (long) System.identityHashCode(part) * 0x9E3779B97F4A7C15L;
		// Direction is fixed for the burst, distance is not: the same arm keeps being in the wrong place, but not
		// by the same amount, which is what a broken rig looks like. A fresh direction every frame reads as noise.
		RandomSource direction = RandomSource.create(partSeed);
		float dx = direction.nextFloat() * 2.0F - 1.0F;
		float dy = direction.nextFloat() * 2.0F - 1.0F;
		float dz = direction.nextFloat() * 2.0F - 1.0F;
		RandomSource jitter = RandomSource.create(partSeed ^ (long) tick * 0xC2B2AE3D27D4EB4FL);
		float distance = (0.4F + jitter.nextFloat() * 0.6F) * OFFSET;
		// Mostly sideways: vertical tearing reads as parts falling off, horizontal tearing as the model splitting,
		// and the latter is the effect being asked for.
		pose.translate(dx * distance, dy * distance * 0.6F, dz * distance);
	}

	private PartGlitch() {
	}
}
