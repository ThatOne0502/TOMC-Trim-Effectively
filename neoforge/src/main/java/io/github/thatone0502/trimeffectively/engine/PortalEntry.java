package io.github.thatone0502.trimeffectively.engine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;

/**
 * Where the entity stood when it entered the dimension it is currently in.
 *
 * <p>Vanilla keeps no queryable record of "which portal did this player come through", so the mod keeps
 * its own: the first tick a player is seen in a dimension that differs from the recorded one, the current
 * position overwrites the record. Entering the nether again overwrites the previous entry, which is what
 * the wayfinder effect wants - the portal is always the most recent one.
 */
public record PortalEntry(Identifier dimension, double x, double y, double z) {
	public static final MapCodec<PortalEntry> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Identifier.CODEC.fieldOf("dimension").forGetter(PortalEntry::dimension),
			Codec.DOUBLE.fieldOf("x").forGetter(PortalEntry::x),
			Codec.DOUBLE.fieldOf("y").forGetter(PortalEntry::y),
			Codec.DOUBLE.fieldOf("z").forGetter(PortalEntry::z))
			.apply(instance, PortalEntry::new));
	public static final Codec<PortalEntry> CODEC = MAP_CODEC.codec();
}
