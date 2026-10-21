package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.Tre;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
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

/**
 * Endphase, in game 相位解离.
 *
 * <p>The remaining duration <em>is</em> the dissociation: the maximum health is
 * {@code base - remaining seconds}, so the effect eats the victim from the inside as it ticks down.
 * Taking damage adds {@code floor(damage)} seconds; healing at full health removes
 * {@code ceil(overflow)} seconds. Reaching a dissociation at or above the base maximum health kills.
 *
 * <p>While active the victim also burns on contact with water, like an enderman.
 */
public class EndphaseEffect extends MobEffect {
	public static final int COLOR = 0xFF00FF;
	public static final Identifier MAX_HEALTH_MODIFIER = Tre.id("endphase_max_health");
	public static final ResourceKey<DamageType> DAMAGE_TYPE_KEY =
			ResourceKey.create(Registries.DAMAGE_TYPE, Tre.id("endphase"));
	public static final EndphaseEffect INSTANCE = new EndphaseEffect();

	private static Holder<MobEffect> holder;

	public EndphaseEffect() {
		super(MobEffectCategory.HARMFUL, COLOR);
	}

	public static void register() {
		holder = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Tre.id("endphase"), INSTANCE);
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
	public void removeAttributeModifiers(AttributeMap attributes) {
		AttributeInstance maxHealth = attributes.getInstance(Attributes.MAX_HEALTH);
		if (maxHealth != null) {
			maxHealth.removeModifier(MAX_HEALTH_MODIFIER);
		}
		super.removeAttributeModifiers(attributes);
	}
}
