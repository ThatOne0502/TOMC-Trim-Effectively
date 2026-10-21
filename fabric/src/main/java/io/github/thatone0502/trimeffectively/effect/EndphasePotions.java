package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.Tre;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;

/**
 * The Endphase potion family, brewed from a dragon head.
 *
 * <p>Three potions are registered the vanilla way, as {@code BuiltInRegistries.POTION} entries: the base one
 * (20 s), its extended form (40 s, redstone) and its strengthened form (10 s at amplifier I, glowstone). The
 * durations are deliberately the opposite of what one expects from a potion: the remaining seconds <em>are</em>
 * the dissociation, so "longer" buys a smaller maximum-health penalty while "stronger" both ends the victim
 * sooner and doubles the rate of loss. A dragon head is an expensive ingredient, so even the weakest tier
 * starts the victim at one heart of maximum health; that is intended, not a bug.
 *
 * <p>Brewing is not data-driven in 1.21.11 - {@code PotionBrewing} is built in code by
 * {@code addVanillaMixes} - so the mixes are appended from a mixin at that method's tail. Being able to brew
 * the effect is what makes it reachable without a trim, which is how the potion, arrow and stew requirements
 * in the design all hang together.
 */
public final class EndphasePotions {
	/** 20 s: the dissociation clock the victim is handed by the base potion. */
	public static final int BASE_TICKS = 400;
	/** 40 s: redstone lengthens the brew, which is *not* a kindness - it only spreads the loss thinner. */
	public static final int LONG_TICKS = 800;
	/** 10 s at amplifier I: twice the rate, so the total loss matches the base potion. */
	public static final int STRONG_TICKS = 200;

	private static Holder<Potion> base;
	private static Holder<Potion> extended;
	private static Holder<Potion> strong;

	public static void register() {
		base = registerPotion("endphase", BASE_TICKS, 0);
		extended = registerPotion("long_endphase", LONG_TICKS, 0);
		strong = registerPotion("strong_endphase", STRONG_TICKS, 1);
	}

	/** Called from the tail of {@code PotionBrewing.addVanillaMixes}. */
	public static void addMixes(PotionBrewing.Builder builder) {
		if (base == null) {
			return;
		}
		builder.addMix(Potions.AWKWARD, Items.DRAGON_HEAD, base);
		builder.addMix(base, Items.REDSTONE, extended);
		builder.addMix(base, Items.GLOWSTONE_DUST, strong);
	}

	private static Holder<Potion> registerPotion(String name, int ticks, int amplifier) {
		Potion potion = new Potion(name, new MobEffectInstance(EndphaseEffect.holder(), ticks, amplifier));
		return Registry.registerForHolder(BuiltInRegistries.POTION, Tre.id(name), potion);
	}

	private EndphasePotions() {
	}
}
