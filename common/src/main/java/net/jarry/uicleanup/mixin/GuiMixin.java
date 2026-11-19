package net.jarry.uicleanup.mixin;

import java.util.List;
import java.util.function.Function;
import net.jarry.uicleanup.config.UiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Skips the first-launch accessibility onboarding screen (same as pressing Continue on it). */
@Mixin(Gui.class)
abstract class GuiMixin {
	@Inject(method = "addInitialScreens", at = @At("HEAD"))
	private void jarrys_ui_cleanup$skipOnboarding(List<Function<Runnable, Screen>> screens, CallbackInfoReturnable<Boolean> cir) {
		Minecraft mc = Minecraft.getInstance();
		if (UiConfig.get().skipAccessibilityOnboarding && mc.options.onboardAccessibility) {
			mc.options.onboardAccessibility = false;
		}
	}
}
