package io.github.thatone0502.trimeffectively.recipe;

import io.github.thatone0502.trimeffectively.Tre;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registers the three smithing recipe serializers.
 *
 * <p>Must run during mod construction, before any datapack is decoded: a recipe JSON naming a serializer that does
 * not exist yet is a load error, not a warning. The serializers themselves are trivial - each recipe is
 * parameterless, so {@code codec()} is a unit codec and the JSON only names the type.
 *
 * <p>The recipe classes are touched here, which is deliberate: it keeps the "when does this class load" question
 * in one place. Their static fields build codecs only; the ingredient tags behind them are resolved when a
 * recipe instance is actually built, at data load.
 */
public final class TreRecipeSerializers {
	public static RecipeSerializer<TrimStripRecipe> STRIP;
	public static RecipeSerializer<TrimStripRecoverRecipe> STRIP_RECOVER;
	public static RecipeSerializer<TrimToggleRecipe> TOGGLE;

	public static void register(RegisterEvent event) {
		Registry<RecipeSerializer<?>> registry = event.getRegistry(BuiltInRegistries.RECIPE_SERIALIZER.key());
		if (registry == null) {
			return;
		}
		STRIP = register(registry, "trim_strip", TrimStripRecipe.CODEC, TrimStripRecipe.STREAM_CODEC);
		STRIP_RECOVER = register(registry, "trim_strip_recover",
				TrimStripRecoverRecipe.CODEC, TrimStripRecoverRecipe.STREAM_CODEC);
		TOGGLE = register(registry, "trim_toggle", TrimToggleRecipe.CODEC, TrimToggleRecipe.STREAM_CODEC);
	}

	private static <T extends Recipe<?>> RecipeSerializer<T> register(
			Registry<RecipeSerializer<?>> registry, String path,
			MapCodec<T> codec, StreamCodec<RegistryFriendlyByteBuf, T> streamCodec) {
		RecipeSerializer<T> serializer = new RecipeSerializer<>(codec, streamCodec);
		@SuppressWarnings("unchecked")
		Registry<RecipeSerializer<T>> typed = (Registry<RecipeSerializer<T>>) (Registry<?>) registry;
		Registry.register(typed, Tre.id(path), serializer);
		return serializer;
	}

	private TreRecipeSerializers() {
	}
}
