package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.engine.TrimStamp;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code keep_repair_cost} ("Excelsior"): the prior-work penalty stops growing.
 *
 * <p>The decision belongs to the <b>item</b>, not to the player. An earlier version looked the effect up on the
 * wearer, which can never work: while an anvil is open the item sits in the input slot and nobody is wearing it.
 * The property is therefore stamped onto the item itself (see {@code item_property} in the datapack) and read
 * back here, which also means a player can lend such an item out and it keeps working.
 *
 * <p>{@code createResult} computes the new penalty and leaves it in the result slot, so the tail sees both the
 * incoming {@code minecraft:repair_cost} of the inputs and the increased value vanilla just wrote. Keeping the
 * prior cost is what "no longer accumulates" means; the grindstone clearing it to zero is untouched vanilla
 * behaviour and stays available as a way out.
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
	private static final String PROPERTY = "keep_repair_cost";

	@Inject(method = "createResult", at = @At("RETURN"))
	private void trimeffectively$keepRepairCost(CallbackInfo ci) {
		ItemCombinerMenuAccessor accessor = (ItemCombinerMenuAccessor) (Object) this;
		Player player = accessor.getPlayer();
		ItemStack result = accessor.getResultSlots().getItem(0);
		if (player == null || result == null || result.isEmpty()) {
			return;
		}
		ItemStack first = accessor.getInputSlots().getItem(0);
		ItemStack second = accessor.getInputSlots().getItem(1);
		if (!TrimStamp.flag(first, PROPERTY) && !TrimStamp.flag(second, PROPERTY)) {
			return;
		}
		int prior = Math.max(repairCost(first), repairCost(second));
		int increased = result.getOrDefault(DataComponents.REPAIR_COST, 0);
		if (increased != prior) {
			result.set(DataComponents.REPAIR_COST, prior);
		}
	}

	@Unique
	private static int repairCost(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}
		return stack.getOrDefault(DataComponents.REPAIR_COST, 0);
	}
}
