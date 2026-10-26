package io.github.thatone0502.trimeffectively.recipe;

import io.github.thatone0502.trimeffectively.data.TrimComponents;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Shared skeleton for the smithing-table recipes this mod adds.
 *
 * <p>Vanilla decides nothing about these for us: {@code SmithingMenu#createResult} asks the recipe table for
 * whatever recipe of type {@code minecraft:smithing} matches the three inputs and then calls
 * {@code assemble} on it. It never checks which class it got, so implementing {@link SmithingRecipe} is all a
 * recipe needs to be usable at a smithing table - and the slot predicates follow automatically, because
 * {@code RecipeManager} builds the three {@code RecipePropertySet}s by asking every smithing recipe for its
 * ingredients and taking what it finds there.
 *
 * <p>All three recipes are parameterless: their inputs are fixed. So the codecs are unit codecs and the JSON
 * files carry nothing but the recipe type.
 *
 * <p>The base slot is "anything with a trim can go here", which is a tag, and a tag cannot be read while the
 * recipe is being decoded: the recipe reload runs before item tags are bound to their holders, so a lookup at
 * construction time throws {@code Missing tag}. The base ingredient is therefore resolved on first use and
 * memoised - the first use is {@code RecipeManager#apply}, which runs after the tags are in place. Because the
 * memo holds the tag's live {@link HolderSet} rather than the items it contained, a datapack that edits
 * {@code minecraft:trimmable_armor} still takes effect.
 *
 * <p>They are registered as {@link PlacementInfo#NOT_PLACEABLE} and flagged special: they are things a player
 * discovers at the table, not grid recipes the recipe book could lay out.
 */
public abstract class TreSmithingRecipe implements SmithingRecipe {
	private final Optional<Ingredient> template;
	private final Optional<Ingredient> addition;
	private final Supplier<Ingredient> baseSource;
	private volatile Ingredient base;

	protected TreSmithingRecipe(Optional<Ingredient> template, Supplier<Ingredient> base, Optional<Ingredient> addition) {
		this.template = template;
		this.baseSource = base;
		this.addition = addition;
	}

	@Override
	public final Optional<Ingredient> templateIngredient() {
		return template;
	}

	@Override
	public final Ingredient baseIngredient() {
		Ingredient resolved = base;

		if (resolved == null) {
			synchronized (this) {
				resolved = base;

				if (resolved == null) {
					resolved = baseSource.get();
					base = resolved;
				}
			}
		}

		return resolved;
	}

	@Override
	public final Optional<Ingredient> additionIngredient() {
		return addition;
	}

	@Override
	public final PlacementInfo placementInfo() {
		return PlacementInfo.NOT_PLACEABLE;
	}

	@Override
	public final boolean isSpecial() {
		return true;
	}

	/** Nothing to announce: these recipes are not surfaced by the recipe book at all. */
	@Override
	public final boolean showNotification() {
		return false;
	}

	/**
	 * The recipe book's grouping key. Every recipe in this family is special and not placeable, so there is no
	 * book group to file it under and the empty group - vanilla's own default - is the whole answer.
	 */
	@Override
	public final String group() {
		return "";
	}

	/** The gate every recipe in this family shares: something has to be trimmed before there is work to do. */
	protected static boolean trimmed(SmithingRecipeInput input) {
		return input.base().has(DataComponents.TRIM);
	}

	/**
	 * Everything a trim can be applied to.
	 *
	 * <p>Resolved through the registry's tags, so it stays a live view. If the tag is somehow absent - another
	 * mod's datapack can remove it - the ingredient matches nothing, which disables the recipe instead of
	 * throwing inside a menu update.
	 */
	protected static Ingredient trimmable() {
		return BuiltInRegistries.ITEM.get(ItemTags.TRIMMABLE_ARMOR)
				.<Ingredient>map(Ingredient::of)
				.orElseGet(() -> Ingredient.of(Stream.<Item>empty()));
	}

	/** A stream codec for a recipe that carries no data and can be rebuilt from thin air on the other side. */
	protected static <T> StreamCodec<RegistryFriendlyByteBuf, T> unitStreamCodec(Supplier<T> factory) {
		return new StreamCodec<>() {
			@Override
			public T decode(RegistryFriendlyByteBuf buffer) {
				return factory.get();
			}

			@Override
			public void encode(RegistryFriendlyByteBuf buffer, T value) {
				// Nothing to send: the type of the recipe is the whole message.
			}
		};
	}

	/** Removes everything the mod and vanilla keep about a trim, leaving the bare item behind. */
	protected static void strip(ItemStack stack) {
		stack.remove(DataComponents.TRIM);
		stack.remove(TrimComponents.TRIM_PROPERTIES);
		stack.remove(TrimComponents.TRE_ENABLED);
	}

	/**
	 * The vanilla three-slot test plus "the base is actually trimmed".
	 *
	 * <p>{@code SmithingRecipe} declares this as a default that compares the three slots against the three
	 * ingredients, so the family only has to add its own gate. The slot test alone would also match a plain
	 * untrimmed item, and stripping nothing is not a recipe result anyone asked for.
	 */
	@Override
	public boolean matches(SmithingRecipeInput input, Level level) {
		return SmithingRecipe.super.matches(input, level) && trimmed(input);
	}
}
