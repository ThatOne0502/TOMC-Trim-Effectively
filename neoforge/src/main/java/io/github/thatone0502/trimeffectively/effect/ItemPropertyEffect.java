package io.github.thatone0502.trimeffectively.effect;

import io.github.thatone0502.trimeffectively.data.TrimComponents;
import io.github.thatone0502.trimeffectively.data.TrimProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Reads the item-scoped properties a trim stamped onto an item.
 *
 * <p>This type is the only one that answers a question about <em>the item</em> rather than its wearer: "how much
 * durability does this stack have", "does it burn", "would a piglin want it". Those questions get asked from
 * places with no wearer and no registry access, so the answers are stamped onto the stack at forging time (see
 * {@link io.github.thatone0502.trimeffectively.engine.TrimStamp}) and read back here from the component.
 *
 * <p>The consequence worth knowing: the value is frozen when the item is stamped. A datapack edit does not
 * retroactively change items people already own - the author's call, to keep old items stable.
 */
public final class ItemPropertyEffect {
	public static final String TYPE = "item_property";
	public static final String PROPERTY_DURABILITY = "durability_multiplier";
	public static final String PROPERTY_FIRE_RESISTANT = "fire_resistant";
	public static final String PROPERTY_PIGLIN_LOVED = "piglin_loved";
	public static final String PROPERTY_REPAIR_COST = "repair_cost";

	public static double durabilityMultiplier(ItemStack stack) {
		TrimProperties properties = properties(stack);
		return properties == null ? 1.0D : properties.number(PROPERTY_DURABILITY, 1.0D);
	}

	public static boolean fireResistant(ItemStack stack) {
		TrimProperties properties = properties(stack);
		return properties != null && properties.flag(PROPERTY_FIRE_RESISTANT);
	}

	/** Whether the item counts as gold for piglin purposes, on top of being made of gold already. */
	public static boolean piglinLoved(ItemStack stack) {
		TrimProperties properties = properties(stack);
		return properties != null && properties.flag(PROPERTY_PIGLIN_LOVED);
	}

	/** Extra anvil work penalty this item carries; negative values remove some. */
	public static double repairCost(ItemStack stack) {
		TrimProperties properties = properties(stack);
		return properties == null ? 0.0D : properties.number(PROPERTY_REPAIR_COST, 0.0D);
	}

	private static TrimProperties properties(ItemStack stack) {
		return stack == null || stack.isEmpty() ? null : stack.get(TrimComponents.TRIM_PROPERTIES);
	}

	private ItemPropertyEffect() {
	}
}
