package io.github.thatone0502.trimeffectively.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.thatone0502.trimeffectively.effect.SummonEffect;
import net.minecraft.world.entity.Entity;

/**
 * Summon lifetime: {@code baseTick} runs once per entity tick on both sides, so the expiry table is consulted
 * there instead of a periodic server-wide scan. Only entities carrying the summon tag with a recorded expiry
 * do any work, so the cost is one set lookup plus (for summons) one map lookup.
 */
@Mixin(Entity.class)
public abstract class EntityEffectsMixin {
	@Inject(method = "baseTick", at = @At("HEAD"))
	private void trimeffectively$summonLifetime(CallbackInfo ci) {
		SummonEffect.tickEntity((Entity) (Object) this);
	}
}
