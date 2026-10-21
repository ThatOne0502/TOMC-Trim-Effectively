package io.github.thatone0502.trimeffectively.effect;

import com.mojang.serialization.MapCodec;
import io.github.thatone0502.trimeffectively.Tre;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * {@code trimeffectively:facing_respawn} - true when the wearer is looking towards "home".
 *
 * <p>Home is resolved in the wearer's current dimension, in this order:
 * <ol>
 * <li>a respawn point set in that same dimension (bed, respawn anchor);</li>
 * <li>the dimension's own arrival place - the shared spawn position, which is the obsidian platform in
 * the end and the world spawn in the overworld and the nether;</li>
 * <li>failing both, the position is unknown and the condition is false rather than "always true".</li>
 * </ol>
 *
 * <p>The nether portal the wearer came through is deliberate: vanilla keeps no queryable record of
 * "which portal did this player use", so promising that in data would be a lie. The shared spawn plus
 * the respawn anchor covers everything the game can actually be asked.
 */
public record FacingRespawnCondition() implements LootItemCondition {
	public static final MapCodec<FacingRespawnCondition> CODEC =
			MapCodec.unit(new FacingRespawnCondition());
	public static final double MAX_ANGLE_DEGREES = 45.0D;

	public static LootItemConditionType register() {
		return Registry.register(BuiltInRegistries.LOOT_CONDITION_TYPE, Tre.id("facing_respawn"),
				new LootItemConditionType(CODEC));
	}

	@Override
	public LootItemConditionType getType() {
		return TreConditions.FACING_RESPAWN;
	}

	@Override
	public boolean test(LootContext context) {
		if (!(context.getLevel() instanceof ServerLevel level)) {
			return false;
		}
		if (!(context.getOptionalParameter(LootContextParams.THIS_ENTITY) instanceof ServerPlayer player)) {
			return false;
		}
		BlockPos target = home(player, level);
		if (target == null) {
			return false;
		}
		Vec3 eye = player.getEyePosition();
		Vec3 toTarget = Vec3.atCenterOf(target).subtract(eye);
		if (toTarget.lengthSqr() < 1.0E-4D) {
			return true;
		}
		double cosine = player.getLookAngle().normalize().dot(toTarget.normalize());
		return cosine >= Math.cos(Math.toRadians(MAX_ANGLE_DEGREES));
	}

	/** Where the wearer would come back to in this dimension, or null when there is nothing to face. */
	public static BlockPos home(ServerPlayer player, ServerLevel level) {
		ServerPlayer.RespawnConfig config = player.getRespawnConfig();
		if (config != null) {
			var data = config.respawnData();
			if (data.dimension().equals(level.dimension())) {
				return data.pos();
			}
			// A respawn point in another dimension is not "home" here; fall through to the arrival place
			// of the dimension the player is standing in.
		}
		// The portal is remembered by the mod because vanilla does not: whichever position the player
		// entered this dimension at, overwritten on every new entry. It beats the spawn point because
		// "I came in through this portal" is the more useful direction to point at.
		net.minecraft.core.BlockPos entry = io.github.thatone0502.trimeffectively.engine.PortalTracker
				.entryIn(level, player);
		if (entry != null) {
			return entry;
		}
		return arrival(level);
	}

	/** The place a player arrives at when entering this dimension without a target of their own. */
	private static BlockPos arrival(ServerLevel level) {
		if (level.dimension().equals(Level.END)) {
			// The obsidian platform the end portal drops you on.
			return ServerLevel.END_SPAWN_POINT;
		}
		// The world spawn. 1.21.11 exposes it as respawn data rather than as a "shared spawn position".
		return level.getRespawnData().pos();
	}
}
