package io.github.thatone0502.trimeffectively.data;

import io.github.thatone0502.trimeffectively.Tre;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;

/**
 * Registers the two data-driven registries. Synced dynamic registries give us datapack
 * loading, {@code /reload} and server-to-client sync for free.
 *
 * <p>A dynamic registry reads its directory from the <em>namespace and path</em> of its key, so
 * {@code trimeffectively:tre_pattern} is fed by {@code data/<pack>/trimeffectively/tre_pattern/**.json}
 * and the entry id is {@code <pack>:<file name>}. The entry id therefore cannot be the vanilla trim id;
 * which trims an entry serves is decided by its {@code pattern} / {@code material} field instead
 * ({@code "pattern": "minecraft:sentry"}). That keeps third-party trims working: ship a file with the
 * vanilla id in {@code pattern} and the mod picks it up without any configuration.
 */
public final class TreRegistries {
	public static void register(DataPackRegistryEvent.NewRegistry event) {
		// Passing a network codec makes the registry synced, the same way the Fabric original did.
		event.dataPackRegistry(Tre.TRE_PATTERN, TrimEffect.CODEC, TrimEffect.CODEC);
		event.dataPackRegistry(Tre.TRE_MATERIAL, TrimEffect.CODEC, TrimEffect.CODEC);
	}

	private TreRegistries() {
	}
}
