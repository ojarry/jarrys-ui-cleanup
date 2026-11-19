package net.jarry.uicleanup.mixin;

import net.jarry.uicleanup.ui.DarkUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The advancements window title is drawn in dark grey on the window frame; lift it while Dark UI is on. */
@Mixin(AdvancementsScreen.class)
abstract class AdvancementsScreenMixin {
	@Inject(method = "extractWindow", at = @At("HEAD"))
	private void jarrys_ui_cleanup$before(GuiGraphicsExtractor g, int mx, int my, CallbackInfo ci) {
		DarkUi.beginLabels();
	}

	@Inject(method = "extractWindow", at = @At("RETURN"))
	private void jarrys_ui_cleanup$after(GuiGraphicsExtractor g, int mx, int my, CallbackInfo ci) {
		DarkUi.endLabels();
	}
}
