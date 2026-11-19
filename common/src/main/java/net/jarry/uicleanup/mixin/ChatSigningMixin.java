package net.jarry.uicleanup.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.jarry.uicleanup.chat.ChatSigning;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.world.entity.player.ProfileKeyPair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Not applied at all when No Chat Reports is installed (see UiMixinPlugin). */
@Mixin(ClientPacketListener.class)
public abstract class ChatSigningMixin {
	@Shadow private boolean serverEnforcesSecureChat;

	/** No key pair means no chat session: nothing to sign with, and no session packet goes to the server. */
	@Inject(method = "setKeyPair", at = @At("HEAD"), cancellable = true)
	private void jarry$noChatSession(ProfileKeyPair keyPair, CallbackInfo ci) {
		if (ChatSigning.mode(this.serverEnforcesSecureChat).strips()) {
			ChatSigning.blockedSessions++;
			ci.cancel();
		}
	}

	/** Whatever the encoder produced, a stripped packet leaves without a signature, or is held back where it would be refused. */
	@WrapOperation(method = {"sendChat", "sendCommand"}, at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"))
	private void jarry$stripSignature(ClientPacketListener self, Packet<?> packet, Operation<Void> original) {
		ChatSigning.Mode mode = ChatSigning.mode(this.serverEnforcesSecureChat);
		if (mode == ChatSigning.Mode.BLOCKED_HERE && ChatSigning.needsSignature(packet)) {
			ChatSigning.showBlockedSend(Minecraft.getInstance());
			return;
		}
		Packet<?> out = mode.strips() ? ChatSigning.strip(packet) : packet;
		ChatSigning.observe(out);
		original.call(self, out);
	}

	@Inject(method = "handleLogin", at = @At("TAIL"))
	private void jarry$noticeOnJoin(ClientboundLoginPacket packet, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (packet.enforcesSecureChat() && !mc.isLocalServer()) ChatSigning.showJoinNotice(mc, ChatSigning.mode(true));
	}
}
