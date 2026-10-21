package io.github.thatone0502.trimeffectively.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.thatone0502.trimeffectively.Tre;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Keeps {@link PortalEntry} up to date, one record per player, saved with the player.
 *
 * <p>The record lives on the entity through Fabric's attachment API so it survives relogging and death
 * without a custom {@code SavedData} or a mixin into the player's NBT.
 *
 * <p>The update is event driven rather than polled: {@code AFTER_PLAYER_CHANGE_WORLD} fires once per
 * dimension change and only for the player who changed, which is the entire condition this needs. The
 * event is handled by parking the player on {@link #PENDING} and flushing on the next server tick, because
 * the event fires while the destination is still being set up - a block position read at that moment is
 * not reliably the one the player materialises at. One tick later the position is final and stable.
 */
public final class PortalTracker {
	public static final AttachmentType<PortalEntry> ENTRY = AttachmentRegistry.<PortalEntry>builder()
			.persistent(PortalEntry.CODEC)
			.copyOnDeath()
			.buildAndRegister(Tre.id("portal_entry"));

	/** Players whose dimension just changed; read one tick later, when the position has settled. */
	private static final Map<UUID, ServerPlayer> PENDING = new HashMap<>();

	public static void register() {
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			PENDING.put(player.getUUID(), player);
		});
		// A player logging in is already standing somewhere: record it once so the first dimension they
		// log into has an entry point too. Later logins in the same dimension do not overwrite it.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.player;
			PortalEntry existing = player.getAttached(ENTRY);
			if (existing == null) {
				record(player);
			}
		});
	}

	/** Writes the entry for every player whose dimension changed since the last tick. */
	public static void flush(MinecraftServer server) {
		if (PENDING.isEmpty()) {
			return;
		}
		for (ServerPlayer player : PENDING.values()) {
			if (player != null && !player.isRemoved()) {
				record(player);
			}
		}
		PENDING.clear();
	}

	private static void record(ServerPlayer player) {
		Identifier dimension = player.level().dimension().identifier();
		// The place the player came in at replaces whatever was recorded for the dimension before: you
		// arrived through *this* portal, not the previous one.
		player.setAttached(ENTRY, new PortalEntry(dimension, player.getX(), player.getY(), player.getZ()));
	}

	/** The recorded entry position when it belongs to this dimension, otherwise null. */
	public static BlockPos entryIn(ServerLevel level, net.minecraft.world.entity.Entity entity) {
		PortalEntry entry = entity.getAttached(ENTRY);
		if (entry == null || !entry.dimension().equals(level.dimension().identifier())) {
			return null;
		}
		return BlockPos.containing(entry.x(), entry.y(), entry.z());
	}

	public static void forget(UUID uuid) {
		PENDING.remove(uuid);
	}

	private PortalTracker() {
	}
}
