package io.github.thatone0502.trimeffectively.recipe;

import io.github.thatone0502.trimeffectively.Tre;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;

/**
 * Registers the three smithing recipe serializers.
 *
 * <p>Must run during mod init, before any datapack is decoded: a recipe JSON naming a serializer that does not
 * exist yet is a load error, not a warning. The serializers themselves are trivial - each recipe is
 * parameterless, so {@code codec()} is a unit codec and the JSON only names the type.
 *
 * <p>The recipe classes are touched here, which is deliberate: it keeps the "when does this class load" question
 * in one place. Their static fields build codecs only; the ingredient tags behind them are resolved when a
 * recipe instance is actually built, at data load.
 */
public final class TreRecipeSerializers {
	public static final RecipeSerializer<TrimStripRecipe> STRIP =
			register("trim_strip", TrimStripRecipe.CODEC, TrimStripRecipe.STREAM_CODEC);
	public static final RecipeSerializer<TrimStripRecoverRecipe> STRIP_RECOVER =
			register("trim_strip_recover", TrimStripRecoverRecipe.CODEC, TrimStripRecoverRecipe.STREAM_CODEC);
	public static final RecipeSerializer<TrimToggleRecipe> TOGGLE =
			register("trim_toggle", TrimToggleRecipe.CODEC, TrimToggleRecipe.STREAM_CODEC);

	public static void init() {
		// Force class initialisation so the three serializers are registered in mod-init order.
	}

	private static <T extends Recipe<?>> RecipeSerializer<T> register(
			String path, MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
		return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Tre.id(path), new RecipeSerializer<T>() {
			@Override
			public MapCodec<T> codec() {
				return codec;
			}

			@Override
			public StreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
				return streamCodec;
			}
		});
	}

	private TreRecipeSerializers() {
	}
}
