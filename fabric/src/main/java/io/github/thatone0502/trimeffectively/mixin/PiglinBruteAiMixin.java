package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.PiglinNeutralEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.piglin.PiglinBruteAi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * {@code piglin_neutral} with {@code also_brutes}: brutes never acquire the wearer as a target.
 *
 * <p>A brute's target is not a mob field, it is the brain's {@code ATTACK_TARGET} memory, and that memory
 * is written from exactly one place: {@code StartAttacking} in the idle activity, fed by
 * {@code PiglinBruteAi#findNearestValidAttackTarget}. The memory is read back by everything that makes a
 * brute look dangerous - the {@code FIGHT} activity's melee and walk-towards behaviours, the
 * {@code setAggressive(brain.hasMemoryValue(ATTACK_TARGET))} call that drives the arm pose - and the fight
 * activity erases it again whenever {@code StopAttackingIfTargetInvalid} says the target is gone.
 *
 * <p>That pair is why filtering only one side flickers instead of pacifying: vetoing
 * {@code isNearestValidAttackTarget} makes the fight behaviour erase the memory, but the idle target search
 * looks at {@code ANGRY_AT} / {@code NEAREST_VISIBLE_ATTACKABLE_PLAYER} / {@code NEAREST_VISIBLE_NEMESIS}
 * instead and simply re-acquires the wearer on the next tick. The memory is added and removed every tick,
 * the aggression flag follows it, and the brute raises and lowers its axe in place.
 *
 * <p>So both ends answer false for a pacified wearer. The empty finder result stops the memory from ever
 * being written; the invalid-target answer clears one that a previous hit already wrote. With the memory
 * permanently absent the brute stays in its idle activity and holds its arms in the default pose, which is
 * exactly how it looks to a player it was never hostile to.
 *
 * <p>The predicate is the strict tier: the entry must set {@code also_brutes} and the wearer must carry an
 * item that was already {@code #piglin_loved} in vanilla and is trimmed with gold, so a plain gold-trimmed
 * iron helmet never calms a brute.
 */
@Mixin(PiglinBruteAi.class)
public abstract class PiglinBruteAiMixin {
	@Inject(method = "findNearestValidAttackTarget", at = @At("RETURN"), cancellable = true)
	private static void trimeffectively$bruteSkipsPacified(ServerLevel level, AbstractPiglin piglin,
			CallbackInfoReturnable<Optional<? extends LivingEntity>> cir) {
		Optional<? extends LivingEntity> found = cir.getReturnValue();
		if (found == null || found.isEmpty()) {
			return;
		}
		if (PiglinNeutralEffect.coversBrutes(found.get())) {
			cir.setReturnValue(Optional.empty());
		}
	}

	@Inject(method = "isNearestValidAttackTarget", at = @At("HEAD"), cancellable = true)
	private static void trimeffectively$bruteDropsPacified(ServerLevel level, AbstractPiglin piglin,
			LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
		if (target != null && PiglinNeutralEffect.coversBrutes(target)) {
			cir.setReturnValue(Boolean.FALSE);
		}
	}
}
