package net.jarry.uicleanup.chat;

import java.util.Optional;
import net.jarry.uicleanup.UiCleanup;
import net.jarry.uicleanup.config.UiConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;

/**
 * "Strip Chat Signatures" (on by default, can be turned off): the client does not open a signed chat session and
 * sends chat and commands without signatures, so they can't be tied to the player in a report.
 * Servers that enforce secure chat refuse unsigned chat; "Allow signed chat on servers that require it" (on by
 * default) signs normally there, otherwise chat is blocked client-side with a clear notice (never a kick).
 * Steps aside completely when No Chat Reports is installed.
 */
public final class ChatSigning {
	public enum Mode {
		/** Feature off or No Chat Reports installed: vanilla behaviour. */
		VANILLA,
		/** Normal server: nothing is signed. */
		STRIP,
		/** Server enforces secure chat and the player allows signing there: vanilla signing. */
		SIGNED_HERE,
		/** Server enforces secure chat and signing is not allowed: chat that needs a signature is held back. */
		BLOCKED_HERE;

		public boolean strips() { return this == STRIP || this == BLOCKED_HERE; }
	}

	/** Self-test/log counters, never saved. */
	public static int strippedChats, strippedCommands, blockedSessions, blockedSends, notices, unsignedChatsSent, signedChatsSent;
	public static volatile boolean lastOutgoingChatSigned;

	private ChatSigning() { }

	/** Pure decision used everywhere (unit tested). */
	public static Mode decide(boolean stripToggle, boolean noChatReports, boolean allowSignedWhereRequired, boolean serverEnforces) {
		if (!stripToggle || noChatReports) return Mode.VANILLA;
		if (!serverEnforces) return Mode.STRIP;
		return allowSignedWhereRequired ? Mode.SIGNED_HERE : Mode.BLOCKED_HERE;
	}

	public static Mode mode(boolean serverEnforces) {
		UiConfig c = UiConfig.get();
		return decide(c.stripChatSignatures, UiCleanup.noChatReportsPresent(), c.allowSignedChatOnRequiredServers, serverEnforces);
	}

	/** True when this outgoing packet would need a signature to be accepted. */
	public static boolean needsSignature(Packet<?> packet) {
		return packet instanceof ServerboundChatPacket || packet instanceof ServerboundChatCommandSignedPacket;
	}

	/** Returns the packet unchanged, or an identical copy with every signature removed. */
	public static Packet<?> strip(Packet<?> packet) {
		if (packet instanceof ServerboundChatPacket p && signed(p)) {
			strippedChats++;
			return chatPacket(p.message(), p.timeStamp(), p.salt(), null, p.lastSeenMessages());
		}
		if (packet instanceof ServerboundChatCommandSignedPacket p && !p.argumentSignatures().entries().isEmpty()) {
			strippedCommands++;
			return new ServerboundChatCommandSignedPacket(p.command(), p.timeStamp(), p.salt(), ArgumentSignatures.EMPTY, p.lastSeenMessages());
		}
		return packet;
	}

	/** 26.3 stores the signature as an Optional, 26.2 as a nullable field; handle both from one source. */
	public static boolean signed(ServerboundChatPacket p) {
		Object sig = p.signature();
		return sig instanceof Optional<?> o ? o.isPresent() : sig != null;
	}

	public static ServerboundChatPacket chatPacket(String msg, java.time.Instant time, long salt,
			net.minecraft.network.chat.MessageSignature sig, net.minecraft.network.chat.LastSeenMessages.Update seen) {
		try {
			java.lang.reflect.Constructor<?> c = ServerboundChatPacket.class.getDeclaredConstructors()[0];
			for (java.lang.reflect.Constructor<?> k : ServerboundChatPacket.class.getDeclaredConstructors()) if (k.getParameterCount() == 5) c = k;
			Object s = c.getParameterTypes()[3] == Optional.class ? Optional.ofNullable(sig) : sig;
			return (ServerboundChatPacket) c.newInstance(msg, time, salt, s, seen);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Records what actually leaves the client (used by the self-test). */
	public static void observe(Packet<?> packet) {
		if (packet instanceof ServerboundChatPacket p) {
			lastOutgoingChatSigned = signed(p);
			if (lastOutgoingChatSigned) signedChatsSent++; else unsignedChatsSent++;
		}
	}

	/** Shown on joining a server that enforces secure chat. */
	public static void showJoinNotice(Minecraft mc, Mode mode) {
		notices++;
		if (mode == Mode.SIGNED_HERE) {
			UiCleanup.LOG.info("Server requires signed chat; signing allowed here by config");
			SystemToast.addOrUpdate(mc.gui.toastManager(), Ids.NOTICE, Component.translatable("jarrys_ui_cleanup.chat.signedHere.title"),
				Component.translatable("jarrys_ui_cleanup.chat.signedHere.body"));
		} else if (mode == Mode.BLOCKED_HERE) {
			UiCleanup.LOG.info("Server requires signed chat; signing not allowed by config, chat is blocked here");
			SystemToast.addOrUpdate(mc.gui.toastManager(), Ids.NOTICE, Component.translatable("jarrys_ui_cleanup.chat.blocked.title"),
				Component.translatable("jarrys_ui_cleanup.chat.blocked.body"));
			mc.gui.hud.getChat().addClientSystemMessage(Component.translatable("jarrys_ui_cleanup.chat.blocked.body").withStyle(ChatFormatting.GOLD));
		}
	}

	/** Shown instead of sending a message the server would refuse. */
	public static void showBlockedSend(Minecraft mc) {
		blockedSends++;
		mc.gui.hud.getChat().addClientSystemMessage(Component.translatable("jarrys_ui_cleanup.chat.blocked.send").withStyle(ChatFormatting.GOLD));
	}

	private static final class Ids {
		static final SystemToast.SystemToastId NOTICE = new SystemToast.SystemToastId(8000L);
	}
}
