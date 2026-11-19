package net.jarry.uicleanup.mixin;

import net.jarry.uicleanup.ui.DarkUi;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** This toast draws black and dark purple text on its background; lift it while Dark UI is on. */
@Mixin(RecipeToast.class)
abstract class RecipeToastMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;J)V", at = @At("HEAD"))
	private void jarrys_ui_cleanup$before(GuiGraphicsExtractor g, Font font, long ms, CallbackInfo ci) {
		DarkUi.beginLabels();
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;J)V", at = @At("RETURN"))
	private void jarrys_ui_cleanup$after(GuiGraphicsExtractor g, Font font, long ms, CallbackInfo ci) {
		DarkUi.endLabels();
	}
}
