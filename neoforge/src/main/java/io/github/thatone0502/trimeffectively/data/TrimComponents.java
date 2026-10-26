package io.github.thatone0502.trimeffectively.data;

import com.mojang.serialization.Codec;
import io.github.thatone0502.trimeffectively.Tre;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers the item components this mod owns.
 *
 * <p>Registered from {@link RegisterEvent} during mod construction, before any datapack loads: a component type has
 * to exist before a stack carrying it is ever decoded, and NeoForge freezes the vanilla registries before mod code
 * runs, so the types cannot be created by a static initialiser any more.
 */
public final class TrimComponents {
	/** Item-scoped properties granted by a trim, stamped onto the item itself. */
	public static DataComponentType<TrimProperties> TRIM_PROPERTIES;

	/**
	 * Per-item kill switch for everything the trim grants.
	 *
	 * <p>Absent means enabled, and the component is removed rather than set to {@code true}: an item that was
	 * never switched off should not carry a marker saying so, so the normal case adds no NBT at all. It is
	 * written only when a player explicitly disables the trim with the glass-pane recipe.
	 *
	 * <p>The flag is read in one place - {@code TrimSnapshot#of} skips the item and {@code TrimStamp} empties
	 * the stamped properties - so disabling a trim removes the piece from stacking counts, from attribute
	 * modifiers and from the item's own properties all at once, and re-enabling recomputes them.
	 */
	public static DataComponentType<Boolean> TRE_ENABLED;

	public static void register(RegisterEvent event) {
		Registry<DataComponentType<?>> registry = event.getRegistry(BuiltInRegistries.DATA_COMPONENT_TYPE.key());
		if (registry == null) {
			return;
		}

		TRIM_PROPERTIES = register(registry, "trim_properties",
				new DataComponentType.Builder<TrimProperties>()
						.persistent(TrimProperties.CODEC)
						.networkSynchronized(TrimProperties.STREAM_CODEC)
						.build());

		TRE_ENABLED = register(registry, "tre_enabled",
				new DataComponentType.Builder<Boolean>()
						.persistent(Codec.BOOL)
						.networkSynchronized(ByteBufCodecs.BOOL)
						.build());
	}

	private static <T> DataComponentType<T> register(
			Registry<DataComponentType<?>> registry, String path, DataComponentType<T> type) {
		@SuppressWarnings("unchecked")
		Registry<DataComponentType<T>> typed = (Registry<DataComponentType<T>>) (Registry<?>) registry;
		Registry.register(typed, Tre.id(path), type);
		return type;
	}

	private TrimComponents() {
	}
}
