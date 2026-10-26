package io.github.thatone0502.trimeffectively.engine;

import io.github.thatone0502.trimeffectively.Tre;
import io.github.thatone0502.trimeffectively.data.TrimEffect;
import io.github.thatone0502.trimeffectively.Trimeffectively;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Drives the per-entity recalculation.
 *
 * <p>Attribute modifiers persist by themselves, so a low refresh rate is enough: we only need to
 * recompute when the worn gear (or one of the variables a value depends on) changes. Ten ticks keeps
 * health-ratio driven effects responsive without walking every entity five times a second.
 */
public final class TreEngine {
	private static final int PERIOD = 10;
	private static int counter;
	private static final Set<UUID> SEEN = new HashSet<>();

	public static void init() {
		NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
			describe(event.getServer(), "server started");
		});

		NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> {
			net.minecraft.server.MinecraftServer server = event.getServer();
			PortalTracker.flush(server);
			sweepInventories(server);
			if (++counter % PERIOD != 0) {
				return;
			}
			RegistryAccess access = server.registryAccess();
			TrimEffects.update(TrimEffects.load(access));
			List<AttributeEngine.TrimEntry> entries = AttributeEngine.load(access);
			if (entries.isEmpty() && !complained) {
				complained = true;
				describe(server, "engine saw no entries");
			}
			SEEN.clear();
			for (ServerLevel level : server.getAllLevels()) {
				for (Entity entity : level.getAllEntities()) {
					if (entity instanceof LivingEntity living) {
						SEEN.add(living.getUUID());
						AttributeEngine.update(living, entries);
					}
				}
			}
			if (++counter % (PERIOD * 20) == 0) {
				AttributeEngine.prune(SEEN);
			}
		});
	}

	private static boolean complained;

	/**
	 * Adopts items that predate the stamped component.
	 *
	 * <p>One slot per player per tick, so a full inventory is covered in a couple of seconds while a single tick
	 * only ever pays for one stack. Equipment is covered separately by the attribute pass, and containers in the
	 * world are deliberately not scanned - an item is adopted the moment it passes through a player's hands.
	 */
	private static void sweepInventories(net.minecraft.server.MinecraftServer server) {
		List<net.minecraft.server.level.ServerPlayer> players = server.getPlayerList().getPlayers();
		if (players.isEmpty()) {
			return;
		}
		int cursor = sweepCursor++;
		net.minecraft.world.entity.player.Inventory inventory =
				players.get(cursor % players.size()).getInventory();
		int slots = inventory.getContainerSize();
		if (slots <= 0) {
			return;
		}
		TrimStamp.reconcile(inventory.getItem((cursor / players.size()) % slots));
	}

	private static int sweepCursor;

	/** Logs how many effect entries of each kind survived loading, and which of them are attribute effects. */
	public static void describe(net.minecraft.server.MinecraftServer server, String reason) {
		RegistryAccess access = server.registryAccess();
		int patterns = size(access, Tre.TRE_PATTERN);
		int materials = size(access, Tre.TRE_MATERIAL);
		Trimeffectively.LOGGER.info("[tre] {} - tre_pattern={} tre_material={}", reason, patterns, materials);
	}

	private static int size(RegistryAccess access, net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<io.github.thatone0502.trimeffectively.data.TrimEffect>> key) {
		try {
			return access.lookupOrThrow(key).size();
		} catch (RuntimeException exception) {
			Trimeffectively.LOGGER.warn("[tre] registry {} lookup failed: {}", key.identifier(), exception.toString());
			return -1;
		}
	}

	/** Reloads datapacks (and therefore both of our dynamic registries). */
	public static void reload(net.minecraft.server.MinecraftServer server) {
		server.reloadResources(server.getPackRepository().getSelectedIds());
	}

	private TreEngine() {
	}
}

