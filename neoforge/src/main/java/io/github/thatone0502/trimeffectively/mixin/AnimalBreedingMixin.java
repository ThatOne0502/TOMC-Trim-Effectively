package io.github.thatone0502.trimeffectively.mixin;

import io.github.thatone0502.trimeffectively.effect.BreedCooldownEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code breed_cooldown}: rescales the breeding cooldown vanilla hands to both parents.
 *
 * <p>{@code Animal#finalizeSpawnChildFromBreeding} sets both parents' age to vanilla's cooldown and then
 * clears the love cause, so the breeder has to be sampled on entry and the new ages are read back on
 * exit. Reading the age vanilla wrote (instead of assuming the constant) keeps this correct for animals
 * that override the cooldown, and both parents are scaled because both were bred by the same player.
 */
@Mixin(Animal.class)
public abstract class AnimalBreedingMixin {
	@Unique
	private ServerPlayer trimeffectively$loveCause;

	@Inject(method = "finalizeSpawnChildFromBreeding", at = @At("HEAD"))
	private void trimeffectively$captureBreedCause(ServerLevel level, Animal partner, AgeableMob child, CallbackInfo ci) {
		this.trimeffectively$loveCause = ((Animal) (Object) this).getLoveCause();
	}

	@Inject(method = "finalizeSpawnChildFromBreeding", at = @At("TAIL"))
	private void trimeffectively$breedCooldown(ServerLevel level, Animal partner, AgeableMob child, CallbackInfo ci) {
		ServerPlayer cause = this.trimeffectively$loveCause;
		this.trimeffectively$loveCause = null;
		if (cause == null) {
			return;
		}
		Animal self = (Animal) (Object) this;
		rescale(self, cause);
		if (partner != null && partner != self) {
			rescale(partner, cause);
		}
	}

	@Unique
	private static void rescale(Animal animal, ServerPlayer breeder) {
		int current = animal.getAge();
		int scaled = BreedCooldownEffect.scale(breeder, current);
		if (scaled != current) {
			animal.setAge(scaled);
		}
	}
}
