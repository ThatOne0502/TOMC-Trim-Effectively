package io.github.thatone0502.trimeffectively.recipe;

import com.mojang.serialization.MapCodec;
import io.github.thatone0502.trimeffectively.engine.TrimStamp;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;

import java.util.Optional;

/**
 * Switches the trim's effects off, or back on, with a pane of glass as the template.
 *
 * <p>This exists because "I like how this looks but not what it does" is a legitimate preference, and the
 * alternative - take the trim off and re-apply it - destroys the pattern. The item keeps its trim, its name and
 * its appearance; the effects stop.
 *
 * <p>Off is a component on the item, on enables by its absence. Switching back on re-derives the stamped
 * properties from the datapack, so a trim switched off before a datapack change comes back with the current
 * numbers rather than the ones it was switched off with.
 */
public final class TrimToggleRecipe extends TreSmithingRecipe {
	public static final MapCodec<TrimToggleRecipe> CODEC = MapCodec.unit(TrimToggleRecipe::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, TrimToggleRecipe> STREAM_CODEC =
			unitStreamCodec(TrimToggleRecipe::new);

	public TrimToggleRecipe() {
		super(Optional.of(Ingredient.of(Items.GLASS_PANE)), TreSmithingRecipe::trimmable, Optional.empty());
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input) {
		ItemStack result = input.base().copyWithCount(1);
		boolean enabled = !TrimStamp.enabled(input.base());
		TrimStamp.setEnabled(result, enabled);
		if (enabled) {
			// Freshly derived, not restored: this is the one moment where "recompute" is the right answer,
			// because the player is explicitly asking for the trim to act again.
			TrimStamp.force(result);
		} else {
			// Empties the stamped values while keeping the trim's identity.
			TrimStamp.reconcile(result);
		}
		return result;
	}

	@Override
	public RecipeSerializer<? extends SmithingRecipe> getSerializer() {
		return TreRecipeSerializers.TOGGLE;
	}
}
