package io.github.thatone0502.trimeffectively.client;

import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;

/**
 * Keeps the client copy of the {@link TrimEffects} index populated.
 *
 * <p>The registries themselves are synced (Fabric {@code DynamicRegistries.registerSynced}), but the index is built
 * by the server-side engine tick and is therefore empty on a dedicated client. Anything that has to resolve a wearer's
 * effects on the client ({@code annotate_sound}) asks this class first; the rebuild is throttled because the data
 * cannot change between datapack reloads and rebuilding it per sound would be wasteful.
 */
public final class TrimClientIndex {
	private static final long REFRESH_MS = 2_000L;
	private static long lastLoad;

	public static void ensure(Minecraft minecraft) {
		if (minecraft == null || minecraft.level == null) {
			return;
		}
		long now = System.currentTimeMillis();
		if (now - lastLoad < REFRESH_MS) {
			return;
		}
		lastLoad = now;
		RegistryAccess access = minecraft.getConnection() == null
				? minecraft.level.registryAccess()
				: minecraft.getConnection().registryAccess();
		if (access == null) {
			return;
		}
		TrimEffects.update(TrimEffects.load(access));
	}

	private TrimClientIndex() {
	}
}
