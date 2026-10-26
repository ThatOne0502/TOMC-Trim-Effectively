package io.github.thatone0502.trimeffectively.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;

import io.github.thatone0502.trimeffectively.effect.LootRollEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * Extra loot rolls are added at {@code getRandomItemsRaw}, the one method every loot generation funnels through.
 *
 * <p>The overload matters. 1.21.11 has both {@code getRandomItemsRaw(LootParams, Consumer)} and
 * {@code getRandomItemsRaw(LootContext, Consumer)}; every public entry point builds a {@code LootContext} first
 * and then calls the <b>LootContext</b> overload, so the LootParams overload never sees a generation of its own.
 *
 * <p>Wrapping rather than injecting at the head is also deliberate. One table roll can re-enter this method:
 * {@code NestedLootTable#createItemStack} hands the same context back to {@code getRandomItemsRaw} for every
 * {@code minecraft:nested} pool entry, so a single fishing catch rolls {@code gameplay/fishing} and then its
 * nested {@code gameplay/fishing/fish}. Hooked at the head, the outer call has already returned by the time its
 * own pools run, the nesting depth reads zero again, and both layers hand out extra rolls - which is exactly the
 * doubled catch that shipped in 0.1.0.43. A wrapper keeps {@link LootRollEffect#enter()} up for the whole body,
 * and the {@code finally} means an exception cannot leak the counter and silently switch the effect off.
 */
@Mixin(LootTable.class)
public abstract class LootTableEffectsMixin {
	@WrapMethod(method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V")
	private void trimeffectively$extraRolls(LootContext context, Consumer<ItemStack> output, Operation<Void> original) {
		boolean outermost = LootRollEffect.enter();
		try {
			if (outermost) {
				LootRollEffect.onRoll((LootTable) (Object) this, context, output);
			}
			original.call(context, output);
		} finally {
			LootRollEffect.exit();
		}
	}
}
