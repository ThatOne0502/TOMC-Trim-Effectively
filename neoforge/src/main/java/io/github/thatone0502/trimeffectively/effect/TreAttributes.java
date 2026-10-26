package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.Tre;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * The attributes this mod adds on top of vanilla.
 *
 * <p>Each of them exists so a mechanic is not hidden inside a handler: the data entry is only sugar,
 * the grant is a real attribute modifier (applied by {@code AttributeEngine}, same id scheme as every
 * other attribute entry), so a player, a command block or another mod can read the value, stack it or
 * add to it without knowing about trim data.
 *
 * <p>The attributes are meaningless on mobs that never jump or never crit, but registering them for all
 * players costs nothing and keeps "which entity has which attribute" a vanilla question.
 */
public final class TreAttributes {
	public static final Identifier EXTRA_JUMPS_ID = Tre.id("extra_jumps");
	/** The data author asked for 0..8 whole jumps; the attribute is a double so modifiers can stack. */
	public static final double EXTRA_JUMPS_MIN = 0.0D;
	public static final double EXTRA_JUMPS_MAX = 8.0D;
	public static final double EXTRA_JUMPS_DEFAULT = 0.0D;

	/** Vanilla's own critical multiplier, so the attribute reads as "how much a crit multiplies". */
	public static final Identifier CRIT_MULTIPLIER_ID = Tre.id("crit_multiplier");
	public static final double CRIT_MULTIPLIER_DEFAULT = 1.5D;

	private static Holder<Attribute> extraJumps;
	private static Holder<Attribute> critMultiplier;

	public static void register(RegisterEvent event) {
		Registry<Attribute> registry = event.getRegistry(BuiltInRegistries.ATTRIBUTE.key());
		if (registry == null || extraJumps != null) {
			return;
		}
		extraJumps = Registry.registerForHolder(registry, EXTRA_JUMPS_ID,
				new RangedAttribute("attribute.trimeffectively.extra_jumps",
						EXTRA_JUMPS_DEFAULT, EXTRA_JUMPS_MIN, EXTRA_JUMPS_MAX).setSyncable(true));
		critMultiplier = Registry.registerForHolder(registry, CRIT_MULTIPLIER_ID,
				new RangedAttribute("attribute.trimeffectively.crit_multiplier",
						CRIT_MULTIPLIER_DEFAULT, 0.0D, 20.0D).setSyncable(true));
	}

	/**
	 * Registering an attribute only makes it *exist*; an entity still has to be given an instance of it, which for
	 * vanilla entities is decided by the AttributeSupplier built in each EntityType's createAttributes. Without this
	 * the attribute is in the registry but no player has it, so /attribute answers "no such attribute" and the
	 * modifier has nowhere to land.
	 */
	public static void modifyAttributes(EntityAttributeModificationEvent event) {
		if (extraJumps == null) {
			return;
		}
		event.add(EntityType.PLAYER, extraJumps, EXTRA_JUMPS_DEFAULT);
		event.add(EntityType.PLAYER, critMultiplier, CRIT_MULTIPLIER_DEFAULT);
	}

	public static Holder<Attribute> extraJumps() {
		return extraJumps;
	}

	public static Holder<Attribute> critMultiplier() {
		return critMultiplier;
	}

	/** Whole mid-air jumps the entity still has available; fractions from other mods are ignored. */
	public static int extraJumps(LivingEntity entity) {
		return (int) Math.floor(value(entity, extraJumps, EXTRA_JUMPS_DEFAULT));
	}

	/** How much this entity currently multiplies a critical hit by. */
	public static double critMultiplier(LivingEntity entity) {
		return value(entity, critMultiplier, CRIT_MULTIPLIER_DEFAULT);
	}

	private static double value(LivingEntity entity, Holder<Attribute> attribute, double fallback) {
		if (attribute == null || entity == null) {
			return fallback;
		}
		if (!entity.getAttributes().hasAttribute(attribute)) {
			return fallback;
		}
		return entity.getAttributes().getValue(attribute);
	}

	private TreAttributes() {
	}
}
