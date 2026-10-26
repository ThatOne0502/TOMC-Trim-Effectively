package io.github.thatone0502.trimeffectively.effect;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

import java.util.List;

/**
 * The single answer to "does the game consider this item gold?".
 *
 * <p>A gold-material trim makes the trimmed item count as {@code #piglin_loved} for every purpose the
 * game has: piglins are attracted to it, pick it up when it is dropped, admire it when it is held, and
 * the "is the target wearing gold armour" test accepts it. Keeping the predicate here - instead of
 * inside a mixin - lets other effects that count gold the wearer carries (for example {@code mammonism}
 * on the {@code snout} pattern) ask the same question and get the same answer.
 *
 * <p>The trim material is matched by its path, so both the vanilla {@code minecraft:gold} material and a
 * datapack material that reuses the {@code gold} name are recognised.
 */
public final class GoldRegard {
	/** The trim material that turns any trimmed item into a piglin-loved item. */
	public static final String GOLD_MATERIAL_PATH = "gold";

	private static final List<EquipmentSlot> ARMOR_SLOTS =
			List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

	/** True when the item is gold for the game's purposes: vanilla gold, or trimmed with gold. */
	public static boolean isGolden(ItemStack stack) {
		return isVanillaGolden(stack) || hasGoldTrim(stack);
	}

	/** True when the item itself is in {@code #piglin_loved}, ignoring any trim it carries. */
	public static boolean isVanillaGolden(ItemStack stack) {
		return stack != null && !stack.isEmpty() && stack.is(ItemTags.PIGLIN_LOVED);
	}

	/** True when the item carries a trim whose material is the gold material. */
	public static boolean hasGoldTrim(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		// The stamped property is the answer of record; the raw trim is only read for items forged before
		// they were ever stamped, so a gold trim works even before the item has been picked up.
		if (ItemPropertyEffect.piglinLoved(stack)) {
			return true;
		}
		ArmorTrim trim = stack.get(DataComponents.TRIM);
		if (trim == null) {
			return false;
		}
		return trim.material().unwrapKey()
				.map(key -> GOLD_MATERIAL_PATH.equals(key.identifier().getPath()))
				.orElse(false);
	}

	/**
	 * True when the item is piglin-loved <em>and</em> trimmed with gold.
	 *
	 * <p>This is the stricter tier {@code piglin_neutral}'s {@code also_brutes} flag asks for: the
	 * author's intent is that only an item which was already gold in vanilla, then trimmed with gold,
	 * reaches the brutes.
	 */
	public static boolean isGoldenTrimmedGold(ItemStack stack) {
		return isVanillaGolden(stack) && hasGoldTrim(stack);
	}

	/** True when any armour slot holds an item that is gold under {@link #isGolden(ItemStack)}. */
	public static boolean wearsGolden(LivingEntity entity) {
		if (entity == null) {
			return false;
		}
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (isGolden(entity.getItemBySlot(slot))) {
				return true;
			}
		}
		return false;
	}

	/** True when any armour slot holds an item that is gold under {@link #isGoldenTrimmedGold(ItemStack)}. */
	public static boolean wearsGoldenTrimmedGold(LivingEntity entity) {
		if (entity == null) {
			return false;
		}
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (isGoldenTrimmedGold(entity.getItemBySlot(slot))) {
				return true;
			}
		}
		return false;
	}

	private GoldRegard() {
	}
}
