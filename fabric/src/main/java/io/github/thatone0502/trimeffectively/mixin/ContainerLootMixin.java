package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.LootRollEffect;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Attributes a container's loot roll to the player who opened it.
 *
 * <p>{@code RandomizableContainer#unpackLootTable(Player)} is the single place a chest, barrel, shulker
 * box or hopper cart fills itself, and it does receive the player - but the loot parameters it builds for
 * the roll do not carry them, so a roll that arrives in {@code LootRollEffect} looks like it belongs to
 * nobody. Parking the player on the thread for the duration of that call is enough for the roll to be
 * attributed, and the extra items travel through the container's own insertion callback, which drops
 * whatever does not fit at the container's position - exactly the requested behaviour.
 */
@Mixin(RandomizableContainer.class)
public interface ContainerLootMixin {
	@Inject(method = "unpackLootTable", at = @At("HEAD"))
	private void trimeffectively$beginContainer(Player player, CallbackInfo ci) {
		LootRollEffect.beginContainer(player);
	}

	@Inject(method = "unpackLootTable", at = @At("RETURN"))
	private void trimeffectively$endContainer(Player player, CallbackInfo ci) {
		LootRollEffect.endContainer();
	}
}
