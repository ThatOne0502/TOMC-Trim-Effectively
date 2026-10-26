package io.github.thatone0502.trimeffectively.engine;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Remembers how fast an entity was moving over the last few ticks.
 *
 * <p>{@code speed_mps} has to mean "how fast this thing is actually travelling", not the movement speed
 * attribute: a player on elytra has an unremarkable movement speed value and a very remarkable velocity.
 * The instantaneous velocity is not good enough either. Collision damage is applied <em>after</em> the crash
 * zeroes the velocity, so by the time the damage pipeline reads {@code getDeltaMovement()} the number that
 * caused the damage is already gone and every impact would look like a standing still hit.
 *
 * <p>A five tick window fixes both: it is short enough that a standing player reads zero and long enough to
 * still hold the pre-impact velocity when the impact itself is being resolved.
 */
public final class SpeedTracker {
	private static final int WINDOW = 5;
	private static final Map<UUID, double[]> RECENT = new ConcurrentHashMap<>();

	public static void record(Entity entity) {
		Vec3 movement = entity.getDeltaMovement();
		double horizontal = Math.sqrt(movement.x * movement.x + movement.z * movement.z);
		// A standing entity still reports the gravity step it was about to take and had cancelled, so the
		// vertical part only counts while it is genuinely in the air. That is also exactly when a fall is
		// what the damage is about.
		double speed = entity.onGround() ? horizontal : movement.length();
		double[] window = RECENT.computeIfAbsent(entity.getUUID(), key -> new double[WINDOW]);
		window[(int) (entity.tickCount % WINDOW)] = speed;
	}

	/** Fastest travel speed, in blocks per second, over the last five ticks. */
	public static double peak(Entity entity) {
		double[] window = RECENT.get(entity.getUUID());
		if (window == null) {
			Vec3 movement = entity.getDeltaMovement();
			return movement.length() * 20.0D;
		}
		double best = 0.0D;
		for (double value : window) {
			if (value > best) {
				best = value;
			}
		}
		return best * 20.0D;
	}

	public static void forget(UUID uuid) {
		RECENT.remove(uuid);
	}

	public static void retain(java.util.Set<UUID> alive) {
		RECENT.keySet().retainAll(alive);
	}

	private SpeedTracker() {
	}
}
