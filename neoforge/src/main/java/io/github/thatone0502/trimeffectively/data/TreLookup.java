package io.github.thatone0502.trimeffectively.data;

import io.github.thatone0502.trimeffectively.Tre;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;

import java.util.Optional;

/** Finds the data-driven entry that applies to a given trim pattern or material. */
public final class TreLookup {
	private TreLookup() {
	}

	public record Hit(Identifier entryId, TrimEffect effect) {
		public String langKey(String suffix) {
			return "trim_effect.trimeffectively." + entryId.getPath() + "." + suffix;
		}
	}

	public static Optional<Hit> pattern(RegistryAccess access, Identifier trim) {
		return find(access, Tre.TRE_PATTERN, trim);
	}

	public static Optional<Hit> material(RegistryAccess access, Identifier trim) {
		return find(access, Tre.TRE_MATERIAL, trim);
	}

	private static Optional<Hit> find(RegistryAccess access, ResourceKey<Registry<TrimEffect>> key, Identifier trim) {
		try {
			Registry<TrimEffect> registry = access.lookupOrThrow(key);
			return registry.entrySet().stream()
					.filter(entry -> entry.getValue().appliesTo().contains(trim))
					.findFirst()
					.map(entry -> new Hit(entry.getKey().identifier(), entry.getValue()));
		} catch (RuntimeException exception) {
			return Optional.empty();
		}
	}
}
