package io.github.thatone0502.trimeffectively.mixin;

import net.minecraft.core.Holder;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the trim pattern a jewellery recipe stamps on.
 *
 * <p>The field is package private, and the alternative - re-reading the recipe JSON - would mean keeping a
 * second copy of the loader's job. An accessor is honest about what it is: the same field, one visibility
 * level wider.
 */
@Mixin(SmithingTrimRecipe.class)
public interface SmithingTrimRecipeAccessor {
	@Accessor("pattern")
	Holder<TrimPattern> trimeffectively$pattern();
}
