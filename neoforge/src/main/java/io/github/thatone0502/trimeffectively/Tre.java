package io.github.thatone0502.trimeffectively;

import io.github.thatone0502.trimeffectively.data.TrimEffect;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

/** Shared constants and registry keys. */
public final class Tre {
	public static final String MOD_ID = "trimeffectively";

	/** {@code tre_pattern} registry - armour trim <em>patterns</em>, read from {@code data/<pack>/trimeffectively/tre_pattern/}. */
	public static final ResourceKey<Registry<TrimEffect>> TRE_PATTERN =
			ResourceKey.createRegistryKey(id("tre_pattern"));
	/** {@code tre_material} registry - armour trim <em>materials</em>, read from {@code data/<pack>/trimeffectively/tre_material/}. */
	public static final ResourceKey<Registry<TrimEffect>> TRE_MATERIAL =
			ResourceKey.createRegistryKey(id("tre_material"));

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	public static Identifier vanilla(String path) {
		return Identifier.fromNamespaceAndPath("minecraft", path);
	}

	private Tre() {
	}
}
