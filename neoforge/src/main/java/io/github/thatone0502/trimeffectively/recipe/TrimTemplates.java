package io.github.thatone0502.trimeffectively.recipe;

import io.github.thatone0502.trimeffectively.mixin.SmithingTrimRecipeAccessor;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.equipment.trim.TrimPattern;

import java.util.Optional;

/**
 * Which item is the template for a given trim pattern.
 *
 * <p>1.21.11 does not answer this directly: {@code TrimPattern} carries an asset id, a description and a decal
 * flag, and nothing else. The template item lives in the {@code SmithingTrimRecipe} that applies the pattern, in
 * its template slot. So the answer is a lookup over the loaded recipes rather than a field read.
 *
 * <p>Doing it this way also means a datapack pattern works without special handling: whatever its trim recipe
 * puts in the template slot is what comes back when the trim is stripped.
 */
public final class TrimTemplates {
	/** Recovered from the recipe table the same way the trim itself was; empty when nothing matches. */
	public static ItemStack forPattern(MinecraftServer server, Holder<TrimPattern> pattern) {
		Optional<ResourceKey<TrimPattern>> key = pattern.unwrapKey();

		if (key.isEmpty()) {
			// A pattern that is not in the registry has no recipe that could name it.
			return ItemStack.EMPTY;
		}

		for (RecipeHolder<?> holder : server.getRecipeManager().getRecipes()) {
			if (holder.value() instanceof SmithingTrimRecipe recipe
					&& ((SmithingTrimRecipeAccessor) recipe).trimeffectively$pattern().unwrapKey().equals(key)) {
				return recipe.templateIngredient()
						.flatMap(ingredient -> ingredient.items().findFirst())
						.map(ItemStack::new)
						.orElse(ItemStack.EMPTY);
			}
		}

		return ItemStack.EMPTY;
	}

	private TrimTemplates() {
	}
}
