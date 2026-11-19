package net.jarry.uicleanup.mixin;

import net.jarry.uicleanup.config.UiConfig;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** The "Loading terrain" screen closes as soon as the player's chunks are ready, without the extra fixed delay. */
@Mixin(LevelLoadTracker.class)
abstract class LevelLoadTrackerMixin {
	@ModifyVariable(method = "<init>(J)V", at = @At("HEAD"), argsOnly = true)
	private static long jarrys_ui_cleanup$noCloseDelay(long closeDelayMs) {
		return UiConfig.get().fasterLoading ? 0L : closeDelayMs;
	}
}
