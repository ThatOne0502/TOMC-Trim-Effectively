package io.github.thatone0502.trimeffectively.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github.thatone0502.trimeffectively.Tre;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Keeps {@link PortalEntry} up to date, one record per player, saved with the player.
 *
 * <p>The record lives on the entity through the attachment API so it survives relogging and death
 * without a custom {@code SavedData} or a mixin into the player's NBT.
 *
 * <p>The update is event driven rather than polled: the dimension change event fires once per
 * dimension change and only for the player who changed, which is the entire condition this needs. The
 * event is handled by parking the player on {@link #PENDING} and flushing on the next server tick, because
 * the event fires while the destination is still being set up - a block position read at that moment is
 * not reliably the one the player materialises at. One tick later the position is final and stable.
 */
public final class PortalTracker {
	public static AttachmentType<PortalEntry> ENTRY;

	/** Players whose dimension just changed; read one tick later, when the position has settled. */
	private static final Map<UUID, ServerPlayer> PENDING = new HashMap<>();

	public static void register(RegisterEvent event) {
		Registry<AttachmentType<?>> registry = event.getRegistry(NeoForgeRegistries.ATTACHMENT_TYPES.key());
		if (registry == null) {
			return;
		}
		ENTRY = AttachmentType.<PortalEntry>builder(() -> null)
				.serialize(PortalEntry.MAP_CODEC)
				.copyOnDeath()
				.build();
		Registry.register(registry, Tre.id("portal_entry"), ENTRY);
	}

	public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof ServerPlayer player) {
			PENDING.put(player.getUUID(), player);
		}
	}

	// A player logging in is already standing somewhere: record it once so the first dimension they
	// log into has an entry point too. Later logins in the same dimension do not overwrite it.
	public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
		if (!(event.getEntity() instanceof ServerPlayer player)) {
			return;
		}
		if (player.getExistingDataOrNull(ENTRY) == null) {
			record(player);
		}
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
		player.setData(ENTRY, new PortalEntry(dimension, player.getX(), player.getY(), player.getZ()));
	}

	/** The recorded entry position when it belongs to this dimension, otherwise null. */
	public static BlockPos entryIn(ServerLevel level, Entity entity) {
		PortalEntry entry = entity.getExistingDataOrNull(ENTRY);
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
