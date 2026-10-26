package io.github.thatone0502.trimeffectively.effect;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Multi-jump implementation for {@code trimeffectively:extra_jumps}.
 *
 * <p>The grant itself is a real attribute modifier on {@link TreAttributes#EXTRA_JUMPS_ID}, applied by
 * {@code AttributeEngine} from the {@code extra_jumps} data entries, so this class only reads the attribute
 * value and spends charges.
 *
 * <p>Charges are counted server-side only — the attribute is syncable so clients and other mods can read it,
 * but jump authorisation never trusts the client. The resulting upward velocity is pushed to the client
 * through the vanilla entity-motion packet (for players explicitly, for mobs through the ordinary
 * {@code hurtMarked} tracker sync), so no custom payload is needed and the two sides cannot disagree about
 * how many charges are left. A jump input is only spent on a fresh press after leaving the ground, so the
 * normal take-off (which is still "jump held" one tick later) never eats a charge.
 */
public final class ExtraJumps {
	private static final double BASE_JUMP = 0.42D;
	private static final double JUMP_BOOST_PER_LEVEL = 0.1D;
	/** ticks within which a client jump packet still counts as "this press". */
	private static final long REQUEST_WINDOW = 3L;
	private static final Map<UUID, Integer> SPENT = new ConcurrentHashMap<>();
	private static final Map<UUID, Boolean> WAS_JUMPING = new ConcurrentHashMap<>();
	private static final Map<UUID, Long> REQUESTED = new ConcurrentHashMap<>();

	/** A fresh jump key press arrived from a client; only interesting while airborne. */
	public static void requestJump(ServerPlayer player) {
		if (player.onGround()) {
			return;
		}
		REQUESTED.put(player.getUUID(), player.level().getGameTime());
	}

	public static void aiStep(LivingEntity entity) {
		if (!(entity.level() instanceof ServerLevel level)) {
			return;
		}
		UUID id = entity.getUUID();
		boolean jumping = entity.isJumping();
		boolean wasJumping = WAS_JUMPING.getOrDefault(id, false);
		WAS_JUMPING.put(id, jumping);
		boolean requested = takeRequest(entity);
		if (entity.onGround()) {
			SPENT.remove(id);
			return;
		}
		boolean pressed = requested || (jumping && !wasJumping);
		if (!pressed) {
			return;
		}
		int available = TreAttributes.extraJumps(entity);
		int spent = SPENT.getOrDefault(id, 0);
		if (available <= 0 || spent >= available) {
			return;
		}
		SPENT.put(id, spent + 1);
		Vec3 motion = entity.getDeltaMovement();
		entity.setDeltaMovement(motion.x, BASE_JUMP + jumpBoost(entity), motion.z);
		entity.hurtMarked = true;
		if (entity instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
		burst(level, entity);
	}

	/**
	 * The visible half of an air jump: a burst of wind at the feet.
	 *
	 * <p>Server side, because this is where the jump is authorised - the client is told about the new velocity
	 * but never decides anything, so it must not be the side that decides there was a gust either. Sound and
	 * particles both reach every player who can see the jumper, which is the point: a jump that gives no sign is
	 * a jump nobody can react to.
	 */
	private static void burst(ServerLevel level, LivingEntity entity) {
		level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
				SoundEvents.WIND_CHARGE_BURST, SoundSource.PLAYERS, 1.0F, 1.0F);
		level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL,
				entity.getX(), entity.getY(), entity.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
	}

	private static boolean takeRequest(LivingEntity entity) {
		Long requestedAt = REQUESTED.remove(entity.getUUID());
		return requestedAt != null && entity.level().getGameTime() - requestedAt <= REQUEST_WINDOW;
	}

	private static double jumpBoost(LivingEntity entity) {
		MobEffectInstance instance = entity.getEffect(MobEffects.JUMP_BOOST);
		if (instance == null) {
			return 0.0D;
		}
		return JUMP_BOOST_PER_LEVEL * (instance.getAmplifier() + 1);
	}

	private ExtraJumps() {
	}
}
