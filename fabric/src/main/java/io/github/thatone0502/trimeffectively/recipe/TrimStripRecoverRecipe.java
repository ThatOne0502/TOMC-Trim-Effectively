package io.github.thatone0502.trimeffectively.recipe;

import com.mojang.serialization.MapCodec;
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
 * Strips the trim and hands the template back: the same seven iron nuggets, plus a book and quill to write down
 * which pattern was on the armour.
 *
 * <p>The template is not part of the recipe result - a smithing result is one stack - so it is paid out by
 * {@code SmithingMenuMixin} when the result is taken, before the three input slots are consumed. That mixin is
 * the only reason this recipe is a separate class instead of a flag: the payout has to be keyed to something.
 *
 * <p>Which template comes back is looked up from the armour's pattern among the loaded
 * {@code SmithingTrimRecipe}s, because 1.21.11's {@code TrimPattern} no longer carries a template item. The
 * upshot is that a datapack-defined pattern returns whatever its own trim recipe uses in the template slot,
 * which is the correct answer for it.
 */
public final class TrimStripRecoverRecipe extends TreSmithingRecipe {
	public static final MapCodec<TrimStripRecoverRecipe> CODEC = MapCodec.unit(TrimStripRecoverRecipe::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, TrimStripRecoverRecipe> STREAM_CODEC =
			unitStreamCodec(TrimStripRecoverRecipe::new);

	public TrimStripRecoverRecipe() {
		super(Optional.of(Ingredient.of(Items.IRON_NUGGET)), TreSmithingRecipe::trimmable,
				Optional.of(Ingredient.of(Items.WRITABLE_BOOK)));
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
		ItemStack result = input.base().copyWithCount(1);
		strip(result);
		return result;
	}

	@Override
	public RecipeSerializer<? extends SmithingRecipe> getSerializer() {
		return TreRecipeSerializers.STRIP_RECOVER;
	}
}
