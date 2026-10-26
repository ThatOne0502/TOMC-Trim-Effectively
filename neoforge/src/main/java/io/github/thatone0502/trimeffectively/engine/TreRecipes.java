package io.github.thatone0502.trimeffectively.engine;

import io.github.thatone0502.trimeffectively.Trimeffectively;
import io.github.thatone0502.trimeffectively.config.TreConfig;
import io.github.thatone0502.trimeffectively.mixin.ShapedRecipeAccessor;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Rewrites the trim template duplication recipes.
 *
 * <p>Every trim template duplication recipe in the game - vanilla ships eighteen of them, one per pattern - is
 * written the same way: a three by three shaped recipe of
 *
 * <pre>
 *   # S #
 *   # C #
 *   # # #
 * </pre>
 *
 * where {@code S} is the template being duplicated, {@code #} is diamond (seven of them) and {@code C} is
 * whatever the pattern is "made of" (one). Squeezing two templates out of seven diamonds is expensive; this
 * swaps what the two keys hold, so the seven slots take the pattern's own material and the single centre slot
 * takes the diamond. {@code S} stays where it is, the output is unchanged, and the recipe keeps its id - which
 * matters, because the recipe book and the server's unlock advancements are keyed by that id.
 *
 * <p>The match is generic rather than a list of vanilla ids, so a datapack or another mod writing the same idiom
 * is rewritten too: the shape has to be three by three, the seven {@code #} slots have to hold the same
 * ingredient, the centre has to hold something else, the result has to be two items, and the top middle slot has
 * to accept that result item. Anything that fails one of those is left exactly as it was.
 *
 * <p>This runs from {@code RecipeManager#apply}, which is the one funnel that sees the whole recipe table both on
 * the initial load and on {@code /reload}; the rewritten table is what gets stored, so it is also what gets
 * synced to clients.
 */
public final class TreRecipes {
	private static final int SIZE = 3;
	private static final int TEMPLATE_SLOT = 1;
	private static final int MATERIAL_SLOT = 4;
	private static final List<Integer> DECORATION_SLOTS = List.of(0, 2, 3, 5, 6, 7, 8);
	private static final int DUPLICATED_COUNT = 2;

	public static RecipeMap hijack(RecipeMap map) {
		if (!TreConfig.hijackTrimDuplicationRecipes()) {
			return map;
		}
		List<RecipeHolder<?>> table = new ArrayList<>();
		int changed = 0;
		for (RecipeHolder<?> holder : map.values()) {
			RecipeHolder<?> replacement;
			try {
				replacement = rewrite(holder);
			} catch (RuntimeException e) {
				// A recipe that cannot be inspected this early - because it reaches for an ingredient tag before the
				// tags are bound, say - is left exactly as it was rather than taking the whole load down with it.
				replacement = null;
			}
			table.add(replacement == null ? holder : replacement);
			if (replacement != null) {
				changed++;
			}
		}
		if (changed == 0) {
			Trimeffectively.LOGGER.info("[tre] recipe hijack: no trim duplication recipe matched");
			return map;
		}
		Trimeffectively.LOGGER.info(
				"[tre] recipe hijack: {} of {} recipes now take 1 diamond and 7 of the pattern's own material",
				changed, table.size());
		return RecipeMap.create(table);
	}

	/** @return the rewritten holder, or {@code null} when this recipe is not a duplication recipe we can swap. */
	private static RecipeHolder<?> rewrite(RecipeHolder<?> holder) {
		if (!(holder.value() instanceof ShapedRecipe recipe)) {
			return null;
		}
		if (recipe.getWidth() != SIZE || recipe.getHeight() != SIZE) {
			return null;
		}
		List<Optional<Ingredient>> grid = recipe.getIngredients();
		if (grid.size() != SIZE * SIZE) {
			return null;
		}
		Optional<Ingredient> template = grid.get(TEMPLATE_SLOT);
		Optional<Ingredient> material = grid.get(MATERIAL_SLOT);
		Optional<Ingredient> decoration = grid.get(0);
		if (template.isEmpty() || material.isEmpty() || decoration.isEmpty()) {
			return null;
		}
		for (int slot : DECORATION_SLOTS) {
			Optional<Ingredient> other = grid.get(slot);
			if (other.isEmpty() || !other.get().equals(decoration.get())) {
				return null;
			}
		}
		// Swapping two identical ingredients would reproduce the very same recipe, so there is nothing to do.
		if (decoration.get().equals(material.get())) {
			return null;
		}
		ItemStackTemplate result = ((ShapedRecipeAccessor) recipe).trimeffectively$result();
		if (result == null) {
			return null;
		}
		if (result.count() != DUPLICATED_COUNT) {
			return null;
		}
		if (!accepts(template.get(), result)) {
			return null;
		}
		Map<Character, Ingredient> keys = new HashMap<>();
		keys.put('S', template.get());
		// D is what the seven surrounding slots used to hold (the diamond), M is what the centre used to hold.
		keys.put('D', decoration.get());
		keys.put('M', material.get());
		ShapedRecipePattern pattern = ShapedRecipePattern.of(keys, List.of("MSM", "MDM", "MMM"));
		ShapedRecipe replacement = new ShapedRecipe(
				new Recipe.CommonInfo(recipe.showNotification()),
				new CraftingRecipe.CraftingBookInfo(recipe.category(), recipe.group()),
				pattern, result.withCount(DUPLICATED_COUNT));
		ResourceKey<Recipe<?>> id = holder.id();
		return new RecipeHolder<>(id, replacement);
	}

	/**
	 * Whether the template slot accepts the item the recipe outputs.
	 *
	 * @return {@code false} when the ingredient cannot be resolved at this point, which happens for an ingredient
	 *         that names a tag - tags are bound at the same late stage as item components
	 */
	private static boolean accepts(Ingredient ingredient, ItemStackTemplate result) {
		try {
			return ingredient.acceptsItem(result.item());
		} catch (RuntimeException e) {
			return false;
		}
	}

	private TreRecipes() {
	}
}
