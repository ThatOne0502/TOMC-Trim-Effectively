package io.github.thatone0502.trimeffectively.sound;

import io.github.thatone0502.trimeffectively.Tre;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * The two sounds Endphase needs.
 *
 * <p>{@link #PHASE_COLLAPSE} is played while something is staring the victim in the eye; its file is a little over
 * thirteen seconds long, which is why the client skips a repeat while the previous copy is still audible (see
 * {@code SoundEngineDedupeMixin}). {@link #GLITCH} is a variant pool on purpose: one registry entry, a list of files
 * in {@code sounds.json}, and the sound manager picks one at random every time it is played.
 */
public final class TreSounds {
	/** Played while an Endphase victim is being watched. Subtitle: 波函数坍缩. */
	public static final Holder.Reference<SoundEvent> PHASE_COLLAPSE = register("wavefunction_collapse");
	/** Played when a hit converts into more dissociation. Subtitle: 故障. */
	public static final Holder.Reference<SoundEvent> GLITCH = register("glitch");

	private TreSounds() {
	}

	/** Registers both sounds; must run before any datapack touches them. */
	public static void init() {
	}

	private static Holder.Reference<SoundEvent> register(String path) {
		Identifier id = Tre.id(path);
		return Registry.registerForHolder(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	/** True for the sounds that must never overlap with a copy of themselves. */
	public static boolean isDeduplicated(Identifier id) {
		return PHASE_COLLAPSE.value().location().equals(id);
	}
}
