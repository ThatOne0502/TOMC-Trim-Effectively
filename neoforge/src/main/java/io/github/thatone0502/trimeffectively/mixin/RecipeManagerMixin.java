package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.engine.TreRecipes;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The single funnel for the recipe table: {@code apply} is called by the reload listener with whatever was
 * parsed from the data packs, on the initial load and again on every {@code /reload}. Rewriting the map here
 * means both the stored table and the copy synced to clients are the rewritten one, and no other entry point
 * has to be covered.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
	@ModifyVariable(
			method = "apply(Lnet/minecraft/world/item/crafting/RecipeMap;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
			at = @At("HEAD"),
			argsOnly = true)
	private RecipeMap trimeffectively$hijackTrimDuplication(RecipeMap map) {
		return TreRecipes.hijack(map);
	}
}
