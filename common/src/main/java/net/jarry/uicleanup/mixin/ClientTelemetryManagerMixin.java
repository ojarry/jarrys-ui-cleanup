package net.jarry.uicleanup.mixin;

import net.jarry.uicleanup.config.UiConfig;
import net.minecraft.client.telemetry.ClientTelemetryManager;
import net.minecraft.client.telemetry.TelemetryEventSender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Telemetry off: the game gets vanilla's own "disabled" sender, the same one it uses when telemetry is not allowed,
 * so no telemetry session is opened and no events are built, logged or sent. Nothing else about networking changes.
 */
@Mixin(ClientTelemetryManager.class)
abstract class ClientTelemetryManagerMixin {
	@Inject(method = "createEventSender", at = @At("HEAD"), cancellable = true)
	private void jarrys_ui_cleanup$disabled(CallbackInfoReturnable<TelemetryEventSender> cir) {
		if (UiConfig.get().disableTelemetry) cir.setReturnValue(TelemetryEventSender.DISABLED);
	}
}
