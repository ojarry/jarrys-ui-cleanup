package net.jarry.uicleanup.mixin;

import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import net.jarry.uicleanup.UiCleanup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.social.PlayerEntry;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.world.entity.player.PlayerSkin;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the "Report player" button in the Social Interactions list. Not applied at all when No Chat Reports is
 * installed (see UiMixinPlugin). The hide/show-chat button keeps working.
 */
@Mixin(PlayerEntry.class)
abstract class PlayerEntryMixin {
	@Shadow private @Nullable Button reportButton;
	@Shadow @Final private List<AbstractWidget> children;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void jarrys_ui_cleanup$hideReport(Minecraft minecraft, SocialInteractionsScreen screen, UUID id, String name,
		Supplier<PlayerSkin> skin, boolean chatReportable, CallbackInfo ci) {
		if (this.reportButton != null && UiCleanup.chatReportRemovalActive()) {
			this.reportButton.visible = false;
			this.reportButton.active = false;
			if (this.children instanceof java.util.ArrayList<AbstractWidget>) this.children.remove(this.reportButton);
		}
	}
}
