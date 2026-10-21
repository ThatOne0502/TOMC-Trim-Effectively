package io.github.thatone0502.trimeffectively.data;

import com.mojang.serialization.Codec;
import io.github.thatone0502.trimeffectively.Tre;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;

/**
 * Registers the item components this mod owns.
 *
 * <p>Registered during mod init, before any datapack loads: a component type has to exist before a stack
 * carrying it is ever decoded.
 */
public final class TrimComponents {
	/** Item-scoped properties granted by a trim, stamped onto the item itself. */
	public static final DataComponentType<TrimProperties> TRIM_PROPERTIES =
			Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Tre.id("trim_properties"),
					new DataComponentType.Builder<TrimProperties>()
							.persistent(TrimProperties.CODEC)
							.networkSynchronized(TrimProperties.STREAM_CODEC)
							.build());

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
	public static final DataComponentType<Boolean> TRE_ENABLED =
			Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, Tre.id("tre_enabled"),
					new DataComponentType.Builder<Boolean>()
							.persistent(Codec.BOOL)
							.networkSynchronized(ByteBufCodecs.BOOL)
							.build());

	public static void init() {
		// Force class initialisation so the components are registered in mod-init order.
	}

	private TrimComponents() {
	}
}
