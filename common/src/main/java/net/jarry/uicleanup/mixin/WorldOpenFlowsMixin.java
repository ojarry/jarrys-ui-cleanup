package net.jarry.uicleanup.mixin;

import com.mojang.serialization.Lifecycle;
import net.jarry.uicleanup.UiCleanup;
import net.jarry.uicleanup.config.UiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldOpenFlows;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skips the experimental-settings warnings: the "are you sure / I know what I'm doing" prompt when creating a world
 * with experimental settings, and the backup prompt when opening a world that uses experimental or old customized
 * settings. We simply continue exactly as if "Proceed" / "Continue without backup" had been clicked.
 * The separate prompt shown when a world was saved in a different Minecraft version is NOT skipped: that one
 * protects worlds during version changes, so it is left alone.
 */
@Mixin(WorldOpenFlows.class)
abstract class WorldOpenFlowsMixin {
	@Inject(method = "confirmWorldCreation", at = @At("HEAD"), cancellable = true)
	private static void jarrys_ui_cleanup$skipCreateWarning(Minecraft minecraft, CreateWorldScreen parent, Lifecycle lifecycle,
		Runnable task, boolean skipWarning, CallbackInfo ci) {
		if (UiConfig.get().skipWorldWarnings && !skipWarning && lifecycle != Lifecycle.stable()) {
			UiCleanup.skippedCreateWarnings++;
			UiCleanup.LOG.info("Skipped the experimental-settings warning for a new world");
			ci.cancel();
			task.run();
		}
	}

	@Inject(method = "askForBackup", at = @At("HEAD"), cancellable = true)
	private void jarrys_ui_cleanup$skipBackupPrompt(LevelStorageSource.LevelStorageAccess access, boolean oldCustomized,
		Runnable proceed, Runnable cancel, CallbackInfo ci) {
		if (UiConfig.get().skipWorldWarnings) {
			UiCleanup.skippedBackupPrompts++;
			UiCleanup.LOG.info("Skipped the experimental-settings backup prompt for world {}", access.getLevelId());
			ci.cancel();
			proceed.run();
		}
	}
}
