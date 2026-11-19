package net.jarry.uicleanup.mixin;

import net.jarry.uicleanup.ui.DarkUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Marks the span in which container labels are drawn, so the Dark UI text fix only touches those labels. */
@Mixin(AbstractContainerScreen.class)
abstract class AbstractContainerScreenMixin {
	@Inject(method = "extractContents", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
	private void jarrys_ui_cleanup$before(GuiGraphicsExtractor g, int mx, int my, float a, CallbackInfo ci) {
		DarkUi.beginLabels();
	}

	@Inject(method = "extractContents", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
		target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractLabels(Lnet/minecraft/client/gui/GuiGraphicsExtractor;II)V"))
	private void jarrys_ui_cleanup$after(GuiGraphicsExtractor g, int mx, int my, float a, CallbackInfo ci) {
		DarkUi.endLabels();
	}
}
