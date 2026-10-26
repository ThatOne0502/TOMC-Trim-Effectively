package io.github.thatone0502.trimeffectively.effect;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Who is staring at whom.
 *
 * <p>While something with a clear line of sight to an Endphase victim has that victim's eyes inside a narrow cone in
 * front of it, the dissociation stops draining - see {@code EndphaseEffect#applyEffectTick}.
 *
 * <p>The rules, mirroring the enderman's aggro check: any living creature counts, players always do, and a viewer
 * wearing a carved pumpkin does not (the pumpkin covers the eyes). Players are judged by their reported view
 * direction, mobs by where their head actually points, because a mob can turn its body without turning its head.
 */
public final class Gaze {
	/** How far away a stare still counts, in blocks. */
	public static final double RANGE = 32.0D;
	/** Cosine of the half angle of the cone: about ten degrees off the eye-to-eye axis. */
	private static final double CONE = 0.985D;
	private static final double MIN_DISTANCE = 1.0E-3D;

	private Gaze() {
	}

	/** True when at least one living creature is currently looking at {@code subject}'s eyes. */
	public static boolean isWatched(LivingEntity subject) {
		if (!(subject.level() instanceof ServerLevel level)) {
			return false;
		}
		Vec3 eyes = subject.getEyePosition();
		AABB box = subject.getBoundingBox().inflate(RANGE);
		List<LivingEntity> viewers = level.getEntitiesOfClass(LivingEntity.class, box);
		for (LivingEntity viewer : viewers) {
			if (viewer == subject || !viewer.isAlive() || viewer.isSpectator()) {
				continue;
			}
			if (viewer.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)) {
				continue;
			}
			if (isLookingAt(viewer, subject, eyes)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isLookingAt(LivingEntity viewer, LivingEntity subject, Vec3 target) {
		Vec3 delta = target.subtract(viewer.getEyePosition());
		double distance = delta.length();
		if (distance < MIN_DISTANCE || distance > RANGE) {
			return false;
		}
		Vec3 look = viewer instanceof Player player
				? player.getViewVector(1.0F)
				: viewer.calculateViewVector(viewer.getXRot(), viewer.getYHeadRot());
		if (look.dot(delta.scale(1.0D / distance)) < CONE) {
			return false;
		}
		// Only now, when the cone test has already narrowed it down to a handful of candidates, spend a raycast on it.
		return viewer.hasLineOfSight(subject);
	}
}
