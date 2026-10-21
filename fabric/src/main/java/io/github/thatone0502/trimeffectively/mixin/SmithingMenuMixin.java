package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.engine.TrimStamp;
import io.github.thatone0502.trimeffectively.recipe.TrimTemplates;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Stamps the component of an item the moment a smithing table hands it over, and pays out a recovered template.
 *
 * <p>Forge is the one place where a trim is born, so it is the only place that sees every trimmed item in the
 * exact moment it exists. Doing it here means a freshly forged piece already answers "am I gold?" before it is
 * ever equipped - which matters, because a dropped item and a piglin's decision about it happen with no wearer
 * in sight.
 *
 * <p>{@code onTake} rather than {@code createResult}: the result slot is recomputed on every menu change, so
 * stamping there would stamp previews that are never taken. The same reasoning decides where the template
 * payout goes: at HEAD the three input slots are still full, which is the only moment the book and quill and the
 * armour's pattern are both still readable. By the time this method returns vanilla has already emptied them.
 *
 * <p>The payout is keyed off the slots rather than off the recipe object, because vanilla does not keep the
 * matched recipe around after assembling the result: {@code onTake} is handed the player and the result and
 * nothing else. "Book and quill in the addition slot, trimmed armour in the base, untrimmed result" is exactly
 * the recover recipe and cannot be produced by any other.
 */
@Mixin(SmithingMenu.class)
public class SmithingMenuMixin {
	@Inject(method = "onTake", at = @At("HEAD"))
	private void tre$stampForgedItem(Player player, ItemStack stack, CallbackInfo info) {
		TrimStamp.reconcile(stack);
	}

	@Inject(method = "onTake", at = @At("HEAD"))
	private void tre$returnStrippedTemplate(Player player, ItemStack result, CallbackInfo info) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		// The slots rather than the input container field: that field lives on ItemCombinerMenu, and reaching
		// it through the public slot list is the same read without depending on how the menu is built.
		SmithingMenu self = (SmithingMenu) (Object) this;
		ItemStack base = self.getSlot(SmithingMenu.BASE_SLOT).getItem();
		ItemStack addition = self.getSlot(SmithingMenu.ADDITIONAL_SLOT).getItem();

		if (!addition.is(Items.WRITABLE_BOOK) || result.has(DataComponents.TRIM)) {
			return;
		}

		ArmorTrim trim = base.get(DataComponents.TRIM);

		if (trim == null) {
			return;
		}

		ItemStack template = TrimTemplates.forPattern(level.getServer(), trim.pattern());

		if (template.isEmpty()) {
			// A pattern with no trim recipe behind it - a datapack can remove the recipe and leave the pattern.
			// Nothing to give back, and nothing to warn about: the strip itself still worked.
			return;
		}

		if (!player.getInventory().add(template)) {
			player.drop(template, false);
		}
	}
}
