package net.jarry.uicleanup.mixin;

import net.jarry.uicleanup.ui.ToastFilter;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drops tutorial toasts and the chat-security ("unsecure server") toast before they are queued. */
@Mixin(ToastManager.class)
abstract class ToastManagerMixin {
	@Inject(method = "addToast", at = @At("HEAD"), cancellable = true)
	private void jarrys_ui_cleanup$filter(Toast toast, CallbackInfo ci) {
		if (ToastFilter.blocks(toast)) ci.cancel();
	}
}
