package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.TradeKeepEffect;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code trade_keep}: gives a completed trade its use back.
 *
 * <p>{@code MerchantResultSlot#onTake} is the only point in the game where a trade is executed with both
 * the trading player (the wearer) and the offer in hand, and it is shared by villagers and wandering
 * traders. Whether the trade actually went through is read from the offer itself: the use counter is
 * sampled on entry and compared on exit, so a rejected trade (out of stock, wrong payment) never rolls
 * and never gets a use back.
 *
 * <p>The roll runs on the server only and the corrected offers are then re-sent, because the client also
 * executes this method locally for prediction: rolling on both sides would let the predicted use count
 * and the authoritative one disagree, which is exactly the state this effect is supposed to undo.
 */
@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {
	@Shadow
	@Final
	private MerchantContainer slots;

	@Shadow
	@Final
	private Player player;

	@Shadow
	@Final
	private Merchant merchant;

	@Unique
	private int trimeffectively$usesBefore = -1;

	@Inject(method = "onTake", at = @At("HEAD"))
	private void trimeffectively$captureUses(Player taker, ItemStack stack, CallbackInfo ci) {
		MerchantOffer offer = this.slots.getActiveOffer();
		this.trimeffectively$usesBefore = offer == null ? -1 : offer.getUses();
	}

	@Inject(method = "onTake", at = @At("TAIL"))
	private void trimeffectively$keepUse(Player taker, ItemStack stack, CallbackInfo ci) {
		int before = this.trimeffectively$usesBefore;
		this.trimeffectively$usesBefore = -1;
		if (before < 0 || this.player == null || this.player.level().isClientSide()) {
			return;
		}
		MerchantOffer offer = this.slots.getActiveOffer();
		if (offer == null || offer.getUses() != before + 1) {
			return;
		}
		Player wearer = taker == null ? this.player : taker;
		if (!TradeKeepEffect.rolls(wearer)) {
			return;
		}
		offer.resetUses();
		for (int i = 0; i < before; i++) {
			offer.increaseUses();
		}
		if (wearer instanceof ServerPlayer serverPlayer) {
			serverPlayer.sendMerchantOffers(serverPlayer.containerMenu.containerId, this.merchant.getOffers(),
					tradeLevel(this.merchant), this.merchant.getVillagerXp(),
					this.merchant.showProgressBar(), this.merchant.canRestock());
		}
	}

	@Unique
	private static int tradeLevel(Merchant merchant) {
		if (merchant instanceof Villager villager) {
			return villager.getVillagerData().level();
		}
		return 1;
	}
}
