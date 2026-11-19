package net.jarry.uicleanup.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.jarry.uicleanup.config.UiConfig;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Ctrl+B no longer cycles the narrator. The narrator itself still works from Accessibility settings. */
@Mixin(KeyboardHandler.class)
abstract class KeyboardHandlerMixin {
	@ModifyExpressionValue(method = "keyPress", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/GameNarrator;isActive()Z"))
	private boolean jarrys_ui_cleanup$noNarratorHotkey(boolean active) {
		return active && !UiConfig.get().disableNarratorHotkey;
	}
}
