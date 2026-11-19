package net.jarry.uicleanup.mixin;

import net.jarry.uicleanup.ui.DarkUi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Applies the Dark UI label colour (only inside container label drawing, only with the pack enabled). */
@Mixin(GuiGraphicsExtractor.class)
abstract class GuiGraphicsExtractorMixin {
	@ModifyVariable(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",
		at = @At("HEAD"), argsOnly = true, ordinal = 2)
	private int jarrys_ui_cleanup$labelColor(int color) {
		return DarkUi.labelColor(color);
	}
}
