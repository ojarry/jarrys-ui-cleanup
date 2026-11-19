package net.jarry.uicleanup.mixin;

import net.jarry.uicleanup.config.UiConfig;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No Realms notification dot or news icon on the title screen (and no Realms polling for them). */
@Mixin(TitleScreen.class)
abstract class TitleScreenMixin {
	@Inject(method = "realmsNotificationsEnabled", at = @At("HEAD"), cancellable = true)
	private void jarrys_ui_cleanup$noRealmsNotifications(CallbackInfoReturnable<Boolean> cir) {
		if (UiConfig.get().removeRealms) cir.setReturnValue(false);
	}
}
