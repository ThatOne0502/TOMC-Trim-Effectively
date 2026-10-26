package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.engine.SpeedTracker;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Entity-level hooks that are not about living entities.
 *
 * <p>The damage hook used to live here, on {@code Entity#hurt}. That was wrong on two counts: {@code hurt} is
 * <b>not</b> the funnel vanilla uses - {@code Player#attack} calls {@code hurtOrSimulate} and a good deal of
 * vanilla calls {@code hurtServer} straight away, so arrows and melee hits simply never reached our maths -
 * and {@code hurtServer} itself is <b>abstract</b> in {@code Entity}, which is why the first attempt crashed
 * the mixin applicator with {@code Cannot read field "index" because "insnNode" is null}. The damage
 * interception now sits on the concrete {@code LivingEntity#hurtServer}, which every one of those paths
 * funnels through.
 *
 * <p>The burn-time hook that also lived here is gone as well: {@code LivingEntity#igniteForTicks} multiplies
 * by the vanilla {@code minecraft:burning_time} attribute before it ever touches the fire-ticks field, so
 * netherite's contribution is an attribute modifier now - the same thing Fire Protection does - instead of a
 * private effect type fighting the same number from the outside.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
	/** Keeps the recent-velocity window filled; see {@link SpeedTracker}. */
	@Inject(method = "baseTick", at = @At("TAIL"))
	private void trimeffectively$recordSpeed(CallbackInfo ci) {
		SpeedTracker.record((Entity) (Object) this);
	}
}
