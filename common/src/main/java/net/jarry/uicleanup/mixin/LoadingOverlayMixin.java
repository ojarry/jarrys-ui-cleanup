package net.jarry.uicleanup.mixin;

import java.util.Optional;
import net.jarry.uicleanup.config.UiConfig;
import net.jarry.uicleanup.overlay.JarryOverlay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the red Mojang loading/reload overlay with Jarry's near-black crimson one. Timing follows vanilla
 * (fade in on reloads, fade out when done) unless "faster loading" is on, which skips the fade-in and its one-second
 * minimum and shortens the fade-out.
 */
@Mixin(LoadingOverlay.class)
abstract class LoadingOverlayMixin {
	@Shadow @Final private Minecraft minecraft;
	@Shadow @Final private ReloadInstance reload;
	@Shadow @Final private boolean fadeIn;
	@Shadow private float currentProgress;
	@Shadow private long fadeOutStart;
	@Shadow private long fadeInStart;

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void jarrys_ui_cleanup$render(GuiGraphicsExtractor g, int mouseX, int mouseY, float a, CallbackInfo ci) {
		UiConfig cfg = UiConfig.get();
		if (!cfg.customLoadingScreen) return;
		ci.cancel();
		boolean fast = cfg.fasterLoading;
		float fadeOutMs = fast ? 250.0F : 1000.0F, fadeInMs = 500.0F;
		long now = Util.getMillis();
		if (this.fadeIn && this.fadeInStart == -1L) this.fadeInStart = now;
		float fadeOut = this.fadeOutStart > -1L ? (now - this.fadeOutStart) / fadeOutMs : -1.0F;
		float fadeInAnim = this.fadeInStart > -1L ? (now - this.fadeInStart) / fadeInMs : -1.0F;
		if (fast && this.fadeIn) fadeInAnim = 1.0F;
		float bgAlpha, contentAlpha;
		if (fadeOut >= 1.0F) {
			jarrys_ui_cleanup$drawBehind(g, 0, 0, a);
			g.nextStratum();
			bgAlpha = 1.0F - Mth.clamp(fadeOut - 1.0F, 0.0F, 1.0F);
			contentAlpha = bgAlpha;
		} else if (this.fadeIn) {
			if (fadeInAnim < 1.0F) jarrys_ui_cleanup$drawBehind(g, mouseX, mouseY, a);
			else this.minecraft.gui.hud.extractDeferredSubtitles();
			g.nextStratum();
			bgAlpha = Mth.clamp(fadeInAnim, 0.15F, 1.0F);
			contentAlpha = Mth.clamp(fadeInAnim, 0.0F, 1.0F);
		} else {
			bgAlpha = 1.0F;
			contentAlpha = 1.0F;
		}
		float actual = this.reload.getActualProgress();
		this.currentProgress = Mth.clamp(this.currentProgress * 0.95F + actual * 0.050000012F, 0.0F, 1.0F);
		float barAlpha = fadeOut < 1.0F ? contentAlpha * (1.0F - Mth.clamp(fadeOut, 0.0F, 1.0F)) : 0.0F;
		JarryOverlay.draw(g, bgAlpha, contentAlpha, this.currentProgress, barAlpha > 0.0F);
		if (fadeOut >= 2.0F) this.minecraft.gui.setOverlay(null);
	}

	private void jarrys_ui_cleanup$drawBehind(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		if (this.minecraft.gui.screen() != null) {
			this.minecraft.gui.screen().extractRenderStateWithTooltipAndSubtitles(g, mouseX, mouseY, a);
		} else {
			this.minecraft.gui.hud.extractDeferredSubtitles();
		}
	}

	@Inject(method = "isReadyToFadeOut", at = @At("HEAD"), cancellable = true)
	private void jarrys_ui_cleanup$noMinimumFade(CallbackInfoReturnable<Boolean> cir) {
		if (UiConfig.get().fasterLoading) cir.setReturnValue(true);
	}
}
