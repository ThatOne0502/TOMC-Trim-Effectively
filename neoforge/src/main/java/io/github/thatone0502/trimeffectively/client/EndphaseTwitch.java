package io.github.thatone0502.trimeffectively.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.thatone0502.trimeffectively.Tre;
import io.github.thatone0502.trimeffectively.effect.EndphaseEffect;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.LivingEntity;

/**
 * The glitch burst: what an Endphase victim does when it is hit while dissociating.
 *
 * <p>Started by {@link io.github.thatone0502.trimeffectively.net.TreGlitchPayload} from the damage hook, never on a
 * schedule. A burst is {@code BURST_TICKS} long: the first two frames replace the skin with the checker, the rest
 * deform the body - whole-body displacement, a twist, a lean, a vertical fold, and a per-part offset that pulls the
 * limbs and the torso apart from each other (see {@link PartGlitch}).
 *
 * <p>Local state, one entry per entity, keyed by entity id because that is what the payload carries. The entry is
 * dropped as soon as it expires, and also the moment the victim stops having Endphase, so nothing can outlive the
 * effect the way the old two-second window schedule did.
 */
public final class EndphaseTwitch {
	/** The checker shown in place of the skin. Our own file, so it looks the same with or without a resource pack. */
	public static final Identifier MISSING_TEXTURE = Tre.id("textures/entity/missing.png");

	/** Frames of checker at the start of a burst. */
	private static final int TEXTURE_TICKS = 2;
	/** Peak whole-body displacement, in blocks. */
	private static final float OFFSET = 0.30F;
	/** Peak twist, in degrees. */
	private static final float YAW = 14.0F;
	/** Peak lean, in degrees. */
	private static final float ROLL = 9.0F;
	/** Peak vertical fold: the body squashes by up to this fraction and stretches by the same. */
	private static final float SQUASH = 0.12F;

	/** A burst in flight: when it started on this client, how long it lasts, and the seed all of its noise uses. */
	private record Burst(long startMillis, int length, long seed) {
	}

	private static final Map<Integer, Burst> BURSTS = new ConcurrentHashMap<>();

	/** Starts (or restarts) a burst on {@code entityId}. Called from the packet handler. */
	public static void burst(int entityId, int length, long seed) {
		BURSTS.put(entityId, new Burst(Util.getMillis(), Math.max(1, length), seed));
	}

	/** Fills {@code state} with this frame's decision. Called from {@code extractRenderState}. */
	public static void update(LivingEntity entity, TreTwitchState state) {
		Burst burst = BURSTS.get(entity.getId());
		if (burst == null) {
			state.trimeffectively$setFrame(null);
			return;
		}
		if (!EndphaseEffect.has(entity)) {
			BURSTS.remove(entity.getId());
			state.trimeffectively$setFrame(null);
			return;
		}
		// Wall clock rather than tick count: the burst may start or end halfway through a client tick, and at
		// twenty ticks a second a rounding error would already be a visible extra frame.
		int tick = (int) ((Util.getMillis() - burst.startMillis()) / 50L);
		if (tick >= burst.length()) {
			BURSTS.remove(entity.getId());
			state.trimeffectively$setFrame(null);
			return;
		}
		if (tick < TEXTURE_TICKS) {
			state.trimeffectively$setFrame(TwitchFrame.checker());
			return;
		}
		RandomSource random = RandomSource.create(burst.seed() ^ (long) tick * 0x9E3779B97F4A7C15L);
		state.trimeffectively$setFrame(new TwitchFrame(false,
				(random.nextFloat() * 2.0F - 1.0F) * OFFSET,
				(random.nextFloat() * 2.0F - 1.0F) * OFFSET,
				(random.nextFloat() * 2.0F - 1.0F) * OFFSET,
				(random.nextFloat() * 2.0F - 1.0F) * YAW,
				(random.nextFloat() * 2.0F - 1.0F) * ROLL,
				1.0F + (random.nextFloat() * 2.0F - 1.0F) * SQUASH,
				burst.seed(),
				tick));
	}

	private EndphaseTwitch() {
	}
}
