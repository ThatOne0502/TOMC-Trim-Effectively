package io.github.thatone0502.trimeffectively.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Read access to the three fields {@code AnvilMenu} inherits from {@code ItemCombinerMenu}.
 *
 * <p>Only reads are exposed: the anvil hook needs the operating player and the input/result containers,
 * all three of which are {@code protected} and therefore out of reach from a mixin that targets the
 * subclass.
 */
@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
	@Accessor("player")
	Player getPlayer();

	@Accessor("inputSlots")
	Container getInputSlots();

	@Accessor("resultSlots")
	ResultContainer getResultSlots();
}
