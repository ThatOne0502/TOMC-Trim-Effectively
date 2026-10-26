package io.github.thatone0502.trimeffectively.effect;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github.thatone0502.trimeffectively.Tre;
import io.github.thatone0502.trimeffectively.mixin.MobEffectInstanceAccessor;
import io.github.thatone0502.trimeffectively.net.TreGlitchSync;
import io.github.thatone0502.trimeffectively.sound.TreSounds;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Endphase, in game 相位解离.
 *
 * <p>The remaining duration <em>is</em> the dissociation: the maximum health is
 * {@code base - remaining seconds}, so the effect eats the victim from the inside as it ticks down.
 * Taking damage adds {@code floor(damage)} seconds; healing at full health removes
 * {@code ceil(overflow)} seconds. Reaching a dissociation at or above the base maximum health kills.
 *
 * <p>While active the victim also burns on contact with water, like an enderman.
 *
 * <p>And it is an effect that can be watched. Anything that looks the victim in the eye - any living creature, any
 * player, unless it is wearing a carved pumpkin - holds the countdown still for as long as it keeps looking
 * ({@link Gaze}), which turns the effect into something that can be interrupted rather than merely outrun.
 */
public class EndphaseEffect extends MobEffect {
	public static final int COLOR = 0xFF00FF;
	public static final Identifier MAX_HEALTH_MODIFIER = Tre.id("endphase_max_health");
	public static final ResourceKey<DamageType> DAMAGE_TYPE_KEY =
			ResourceKey.create(Registries.DAMAGE_TYPE, Tre.id("endphase"));
	public static final EndphaseEffect INSTANCE = new EndphaseEffect();

	/** How often the client is told the frozen duration again while somebody is staring. */
	private static final int HOLD_RESYNC_TICKS = 5;
	/** How often the stare loop re-asks for its sound. The client drops the request while a copy is still sounding. */
	private static final int HOLD_SOUND_TICKS = 10;
	/** Volume of the stare sound; at 2.0 it carries twice the usual sixteen blocks. */
	private static final float HOLD_SOUND_VOLUME = 2.0F;

	private static Holder<MobEffect> holder;

	/**
	 * Entity -> the entity tick on which the clock was last held. Another modifier may shorten harmful effects
	 * afterwards ({@code HarmfulEffectRate}, the {@code tide} trim), which would drain the dissociation while the
	 * stare is supposed to be holding it still; that code asks this map and stands down for exactly that tick.
	 */
	private static final Map<UUID, Integer> HELD = new ConcurrentHashMap<>();

	public EndphaseEffect() {
		super(MobEffectCategory.HARMFUL, COLOR);
	}

	public static void register(RegisterEvent event) {
		Registry<MobEffect> registry = event.getRegistry(BuiltInRegistries.MOB_EFFECT.key());
		if (registry == null) {
			return;
		}
		holder = Registry.registerForHolder(registry, Tre.id("endphase"), INSTANCE);
	}

	public static Holder<MobEffect> holder() {
		return holder;
	}

	public static boolean has(LivingEntity entity) {
		return holder != null && entity.hasEffect(holder);
	}

	/** Builds a damage source of our own type, keeping the original attacker. */
	public static DamageSource source(ServerLevel level, Entity direct, Entity causing) {
		Registry<DamageType> registry = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE);
		Holder.Reference<DamageType> type = registry.getOrThrow(DAMAGE_TYPE_KEY);
		return new DamageSource(type, direct, causing);
	}

	/** Current dissociation in seconds, zero when the effect is absent. */
	public static double dissociation(LivingEntity entity) {
		if (holder == null) {
			return 0.0D;
		}
		MobEffectInstance instance = entity.getEffect(holder);
		return instance == null ? 0.0D : instance.getDuration() / 20.0D;
	}

	/** Adds {@code floor(amount)} seconds of dissociation; called from the damage hook. */
	public static void onDamageTaken(LivingEntity entity, float amount) {
		if (holder == null) {
			return;
		}
		MobEffectInstance current = entity.getEffect(holder);
		if (current == null) {
			return;
		}
		double seconds = Math.floor(amount);
		if (seconds <= 0.0D) {
			return;
		}
		long extended = current.getDuration() + (long) seconds * 20L;
		int duration = (int) Math.min(extended, Integer.MAX_VALUE - 1);
		entity.forceAddEffect(new MobEffectInstance(holder, duration, current.getAmplifier()), null);
		// The sound of a wavefunction failing to hold: one of five files, picked by the sound manager. Played at the
		// victim, so everyone nearby hears the hit land the way it actually landed.
		if (entity.level() instanceof ServerLevel level) {
			level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
					TreSounds.GLITCH, SoundSource.PLAYERS, 1.0F, 1.0F);
		}
		// And the victim tears itself apart for a moment. Triggered here rather than on a timer because this is
		// exactly the instant the sound is chosen: the two are the same event, so they cannot drift apart, and a
		// victim that is no longer dissociating can never glitch again.
		TreGlitchSync.send(entity);
	}

	/** Removes {@code ceil(overflow)} seconds of dissociation when healing at full health. */
	public static void onHealed(LivingEntity entity, float amount) {
		if (holder == null || amount <= 0.0F) {
			return;
		}
		MobEffectInstance current = entity.getEffect(holder);
		if (current == null) {
			return;
		}
		float overflow = amount - (entity.getMaxHealth() - entity.getHealth());
		if (overflow <= 0.0F) {
			return;
		}
		int removed = (int) Math.ceil(overflow) * 20;
		int duration = current.getDuration() - removed;
		if (duration <= 0) {
			entity.removeEffect(holder);
		} else {
			entity.forceAddEffect(new MobEffectInstance(holder, duration, current.getAmplifier()), null);
		}
	}

	@Override
	public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
		return true;
	}

	@Override
	public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
		MobEffectInstance self = holder == null ? null : entity.getEffect(holder);
		if (self == null) {
			return true;
		}
		// Being watched holds the dissociation. The effect ticks before its own duration is decremented, so writing
		// "one more tick" here cancels exactly this tick's decrement - and only this one, which is why the effect
		// resumes the moment the stare breaks. A whole second at a time would be wrong: the dissociation is displayed
		// floored to seconds, so a jump of twenty ticks would drop the heart row by one and put it back, a visible
		// stutter twenty times a second.
		if (Gaze.isWatched(entity)) {
			((MobEffectInstanceAccessor) self).trimeffectively$setDuration(self.getDuration() + 1);
			HELD.put(entity.getUUID(), entity.tickCount);
			if (level.getGameTime() % HOLD_RESYNC_TICKS == 0L) {
				// The client counts its own copy down; without this it would lose the effect entirely on a long stare.
				resend(entity, self);
			}
			if (level.getGameTime() % HOLD_SOUND_TICKS == 0L && entity instanceof ServerPlayer victim) {
				// Only the player being stared at hears its own wavefunction collapse. Not a broadcast: the sound is
				// a private report that somebody is looking, and a bystander learning it from across the room would
				// be a different - and much cheaper - version of the same mechanic.
				victim.connection.send(new ClientboundSoundPacket(TreSounds.PHASE_COLLAPSE, SoundSource.PLAYERS,
						entity.getX(), entity.getY(), entity.getZ(), HOLD_SOUND_VOLUME, 1.0F,
						level.getRandom().nextLong()));
			}
		} else {
			HELD.remove(entity.getUUID());
		}
		double seconds = self.getDuration() / 20.0D;
		applyMaxHealthPenalty(entity, seconds, self.getAmplifier());
		if (entity.isInWaterOrRain()) {
			entity.hurtServer(level, level.damageSources().drown(), 1.0F);
		}
		// There is deliberately no scripted execution here. Death comes from the ordinary damage the victim
		// takes: the damage hook rewrites the source of any hit landed while dissociated, so the death message
		// is already the right one, and the max health attribute floors at 1.0 - a victim past the threshold is
		// one hit from anything, which is sharper and more honest than a hidden kill.
		return true;
	}

	/** True when the dissociation on {@code entity} was held still during this very entity tick. */
	public static boolean isHeld(LivingEntity entity) {
		Integer stamp = HELD.get(entity.getUUID());
		return stamp != null && stamp == entity.tickCount;
	}

	/** Tells the viewers (and the victim, when it is a player) what the frozen duration really is. */
	private static void resend(LivingEntity entity, MobEffectInstance instance) {
		// The trailing flag is the "blend" bit (FLAG_BLEND). Vanilla passes false here - ServerPlayer's
		// addEffect path uses the same false - so the effect only blends into the HUD when the pack asks it to.
		ClientboundUpdateMobEffectPacket packet =
				new ClientboundUpdateMobEffectPacket(entity.getId(), instance, false);
		if (entity.level() instanceof ServerLevel level) {
			// Same set vanilla broadcasts entity packets to: everyone tracking the chunk the entity is in.
			for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(entity.chunkPosition(), false)) {
				player.connection.send(packet);
			}
		}
		if (entity instanceof ServerPlayer self) {
			self.connection.send(packet);
		}
	}

	private static void applyMaxHealthPenalty(LivingEntity entity, double seconds, int amplifier) {
		AttributeInstance maxHealth = entity.getAttributes().getInstance(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return;
		}
		// Whole seconds, not fractional. The duration ticks down every tick, so a fractional penalty rewrote
		// the modifier twenty times a second - enough for the client to re-render the heart row continuously,
		// and for a hit to visibly reflow it. Quantised, the value changes once per second, on heart
		// boundaries, and reads as the wound draining instead of the bar twitching.
		double reduction = -Math.floor(seconds) * (amplifier + 1);
		AttributeModifier existing = maxHealth.getModifier(MAX_HEALTH_MODIFIER);
		if (existing != null && existing.amount() == reduction) {
			return;
		}
		maxHealth.addOrReplacePermanentModifier(new AttributeModifier(
				MAX_HEALTH_MODIFIER, reduction, AttributeModifier.Operation.ADD_VALUE));
	}

	@Override
	public void onMobRemoved(ServerLevel level, LivingEntity entity, int amplifier, Entity.RemovalReason reason) {
		// The hold is remembered per entity, and an entity that loses the effect never reports in again.
		HELD.remove(entity.getUUID());
		super.onMobRemoved(level, entity, amplifier, reason);
	}

	@Override
	public void removeAttributeModifiers(AttributeMap attributes) {
		AttributeInstance maxHealth = attributes.getInstance(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.removeModifier(MAX_HEALTH_MODIFIER);
		}
		super.removeAttributeModifiers(attributes);
	}
}
