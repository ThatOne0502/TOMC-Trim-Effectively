package io.github.thatone0502.trimeffectively.mixin;

import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.ShapedRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the declared result of a shaped recipe without building it.
 *
 * <p>{@code ShapedRecipe} keeps its result in a private field with no getter, and the only ways to reach it from the
 * outside - assembling the recipe or asking for its display - both build an item stack, which is not possible while
 * the recipe manager applies a datapack.
 */
@Mixin(ShapedRecipe.class)
public interface ShapedRecipeAccessor {
	@Accessor("result")
	ItemStackTemplate trimeffectively$result();
}
