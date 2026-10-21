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
 * Strips the trim off a piece of armour: seven iron nuggets of template work and the trim is gone.
 *
 * <p>Cheap on purpose. A trim is cosmetic, and the only reason to take one off is to put a different one on,
 * so the cost should not be a second thought. The pattern is <em>destroyed</em>, not returned - use
 * {@link TrimStripRecoverRecipe} when the template matters.
 *
 * <p>What "gone" means: the {@code minecraft:trim} component, the stamped properties, and the kill switch.
 * Removing the trim is also what the effects read, so a stripped item stops contributing anything without any
 * effect-specific code noticing.
 */
public final class TrimStripRecipe extends TreSmithingRecipe {
	public static final MapCodec<TrimStripRecipe> CODEC = MapCodec.unit(TrimStripRecipe::new);
	public static final StreamCodec<RegistryFriendlyByteBuf, TrimStripRecipe> STREAM_CODEC =
			unitStreamCodec(TrimStripRecipe::new);

	public TrimStripRecipe() {
		super(Optional.of(Ingredient.of(Items.IRON_NUGGET)), TreSmithingRecipe::trimmable, Optional.empty());
	}

	@Override
	public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
		ItemStack result = input.base().copyWithCount(1);
		strip(result);
		return result;
	}

	@Override
	public RecipeSerializer<? extends SmithingRecipe> getSerializer() {
		return TreRecipeSerializers.STRIP;
	}
}
