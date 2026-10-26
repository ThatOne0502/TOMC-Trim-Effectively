package io.github.thatone0502.trimeffectively.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import io.github.thatone0502.trimeffectively.data.EffectEntry;
import io.github.thatone0502.trimeffectively.engine.TrimEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * {@code trimeffectively:mute_sound} — the wearer's own sounds stop leaving the wearer.
 *
 * <p>{@code scope} selects which half of the emission is silenced, and both halves are independent so a
 * pattern can blind only the sculk system ({@code vibration}) or only the other players ({@code others}) instead of
 * doing both ({@code both}). The audible half is fed by {@code Entity#playSound} and the vibration half by
 * {@code Entity#gameEvent} — the two places an entity-audible sound and an entity-audible game event are created.
 * The sculk system listens to game events, not to sounds, so silencing only {@code playSound} would leave a warden
 * perfectly able to hear the wearer.
 *
 * <p>{@code chance} is rolled once per candidate entry, exactly like the other chance-carrying effects
 * (see {@link MobEffectApplyEffect#afterDamage}); the data rolls {@code per_piece}, so a full four piece set is
 * louder than a single piece.
 */
public final class SoundMute {
	public static final String TYPE = "mute_sound";
	/** The trigger the sound system reports; {@code equipped} is always consulted as well. */
	public static final String TRIGGER_SOUND = "sound_emitted";

	/**
	 * Should this entity's audible sounds be suppressed on this side?
	 *
	 * <p>The audible half has two sides: the server suppresses the broadcast, which is what stops <em>other</em> players
	 * from hearing the wearer, and the client suppresses its own predicted copy, which is what stops the wearer from
	 * hearing himself. {@code scope: "others"} uses the first only.
	 */
	public static boolean mutesAudible(Entity entity, boolean clientSide) {
		return mutes(entity, true, clientSide);
	}

	/** Should this entity's game events (sculk vibrations) be suppressed? */
	public static boolean mutesVibration(Entity entity) {
		return mutes(entity, false, false);
	}

	private static boolean mutes(Entity entity, boolean audible, boolean clientSide) {
		if (entity == null || !(entity instanceof LivingEntity wearer) || wearer.level() == null) {
			return false;
		}
		if (!wearer.isAlive() || wearer.isSpectator()) {
			return false;
		}
		List<TrimEffects.Active> actives = actives(wearer);
		for (TrimEffects.Active active : actives) {
			if (!scopeMatches(active.entry().scope(), audible, clientSide)) {
				continue;
			}
			if (rolls(active)) {
				return true;
			}
		}
		return false;
	}

	private static List<TrimEffects.Active> actives(LivingEntity wearer) {
		List<TrimEffects.Active> equipped = TrimEffects.activeOfType(wearer, EffectEntry.DEFAULT_TRIGGER, TYPE);
		List<TrimEffects.Active> sound = TrimEffects.activeOfType(wearer, TRIGGER_SOUND, TYPE);
		if (sound.isEmpty()) {
			return equipped;
		}
		if (equipped.isEmpty()) {
			return sound;
		}
		List<TrimEffects.Active> both = new ArrayList<>(equipped.size() + sound.size());
		both.addAll(equipped);
		both.addAll(sound);
		return both;
	}

	/**
	 * {@code both} silences everything, {@code audible} and {@code others} silence only the audible half (all of it, or
	 * only the other players' view of it) and {@code vibration} blinds only the sculk system. An absent scope is treated
	 * as {@code both}.
	 */
	private static boolean scopeMatches(String scope, boolean audible, boolean clientSide) {
		String mode = scope == null ? "both" : scope.trim().toLowerCase(Locale.ROOT);
		if (audible) {
			if (mode.equals("both") || mode.equals("audible")) {
				return true;
			}
			return mode.equals("others") && !clientSide;
		}
		return mode.equals("both") || mode.equals("vibration");
	}

	private static boolean rolls(TrimEffects.Active active) {
		EffectEntry entry = active.entry();
		double chance = entry.chance() == null ? 1.0D : entry.chance().eval(active.pieces(), Map.of());
		chance = Math.max(0.0D, Math.min(1.0D, chance));
		if (chance <= 0.0D) {
			return false;
		}
		return chance >= 1.0D || ThreadLocalRandom.current().nextDouble() < chance;
	}

	private SoundMute() {
	}
}
