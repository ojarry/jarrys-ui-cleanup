package net.jarry.uicleanup.selftest;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import net.jarry.uicleanup.UiCleanup;
import net.jarry.uicleanup.chat.ChatSigning;
import net.jarry.uicleanup.config.UiConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.LocalChatSession;
import net.minecraft.network.chat.SignedMessageChain;
import net.minecraft.world.entity.player.ProfileKeyPair;
import net.minecraft.world.entity.player.ProfilePublicKey;

/** Self-test for "Strip Chat Signatures" in singleplayer (dev-only). Uses a locally generated, throwaway key pair. */
final class ChatTest {
	private static ProfileKeyPair fakeKeys;
	private static SignedMessageChain.Encoder realEncoder;

	private ChatTest() { }

	private static Field field(String name) throws Exception {
		Field f = ClientPacketListener.class.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private static void setKeyPair(ClientPacketListener conn, ProfileKeyPair keys) throws Exception {
		Method m = ClientPacketListener.class.getDeclaredMethod("setKeyPair", ProfileKeyPair.class);
		m.setAccessible(true);
		m.invoke(conn, keys);
	}

	static void start(Minecraft mc) throws Exception {
		ClientPacketListener conn = mc.getConnection();
		UiConfig cfg = UiConfig.get();
		if (UiCleanup.noChatReportsPresent()) {
			ClientSelfTest.check(ChatSigning.mode(false) == ChatSigning.Mode.VANILLA, "No Chat Reports present: Strip Chat Signatures steps aside (mode VANILLA)");
			conn.sendChat("jarry ncr test");
			ClientSelfTest.check(ChatSigning.signedChatsSent + ChatSigning.unsignedChatsSent == 0 && ChatSigning.blockedSessions == 0,
				"No Chat Reports present: our chat-signing mixin is not applied (it saw no packets)");
			return;
		}
		java.security.KeyPairGenerator gen = java.security.KeyPairGenerator.getInstance("RSA");
		gen.initialize(2048); // Mojang profile keys are RSA-2048 (256-byte signatures)
		KeyPair kp = gen.generateKeyPair();
		Instant exp = Instant.now().plus(Duration.ofDays(1));
		fakeKeys = new ProfileKeyPair(kp.getPrivate(), new ProfilePublicKey(new ProfilePublicKey.Data(exp, kp.getPublic(), new byte[512])), exp);
		realEncoder = LocalChatSession.create(fakeKeys).createMessageEncoder(mc.getUser().getProfileId());

		// 1. Toggle on: no chat session is created even when a key pair arrives.
		cfg.stripChatSignatures = true;
		int blocked = ChatSigning.blockedSessions;
		setKeyPair(conn, fakeKeys);
		ClientSelfTest.check(field("chatSession").get(conn) == null && ChatSigning.blockedSessions == blocked + 1,
			"strip on: no signed chat session created from a key pair (nothing sent to the server)");

		// 2. Toggle on, even with a real signing encoder in place: the chat packet leaves unsigned.
		field("signedMessageEncoder").set(conn, realEncoder);
		int stripped = ChatSigning.strippedChats;
		conn.sendChat("jarry strip test");
		ClientSelfTest.check(!ChatSigning.lastOutgoingChatSigned && ChatSigning.strippedChats == stripped + 1,
			"strip on: singleplayer chat sent with NO signature");
		conn.sendCommand("msg @s jarry cmd test");
		ClientSelfTest.check(ChatSigning.strippedCommands >= 1, "strip on: /msg argument signature removed");

		// 3. Toggle off: vanilla signing comes back.
		cfg.stripChatSignatures = false;
		conn.sendChat("jarry signed test");
		ClientSelfTest.check(ChatSigning.lastOutgoingChatSigned, "strip off: chat is signed again (vanilla behaviour restored)");
		var validator = mc.getSingleplayerServer() == null ? null : mc.getSingleplayerServer().services().profileKeySignatureValidator();
		if (validator == null) {
			field("signedMessageEncoder").set(conn, SignedMessageChain.Encoder.UNSIGNED);
			setKeyPair(conn, fakeKeys);
			ClientSelfTest.check(field("chatSession").get(conn) != null, "strip off: setKeyPair creates a chat session again (not cancelled)");
		} else {
			UiCleanup.LOG.info("CLIENTTEST skip: integrated server validates profile keys; not sending a fake chat session to it");
		}
		cfg.stripChatSignatures = true;
		field("chatSession").set(conn, null);
		field("signedMessageEncoder").set(conn, SignedMessageChain.Encoder.UNSIGNED);
	}

	static boolean chatContains(Minecraft mc, String text) {
		ChatComponent chat = mc.gui.hud.getChat();
		for (Class<?> c = chat.getClass(); c != null; c = c.getSuperclass()) {
			for (Field f : c.getDeclaredFields()) {
				if (!List.class.isAssignableFrom(f.getType())) continue;
				try {
					f.setAccessible(true);
					for (Object o : (List<?>) f.get(chat)) {
						try {
							Object content = o.getClass().getMethod("content").invoke(o);
							if (content instanceof net.minecraft.network.chat.Component comp && comp.getString().contains(text)) return true;
						} catch (NoSuchMethodException ignored) { }
					}
				} catch (Exception ignored) { }
			}
		}
		return false;
	}

	static void verifyDelivered(Minecraft mc) {
		if (UiCleanup.noChatReportsPresent()) {
			ClientSelfTest.check(chatContains(mc, "jarry ncr test"), "No Chat Reports present: singleplayer chat still delivered");
			return;
		}
		ClientSelfTest.check(chatContains(mc, "jarry strip test"), "unsigned chat accepted and shown by the singleplayer server");
		ClientSelfTest.check(chatContains(mc, "jarry cmd test"), "unsigned /msg command accepted in singleplayer");
		ClientSelfTest.check(chatContains(mc, "jarry signed test"), "signed chat (toggle off) accepted in singleplayer");
	}

	/** Simulates a server that enforces secure chat with "Allow signed chat" turned off. */
	static void blockedHere(Minecraft mc) throws Exception {
		if (UiCleanup.noChatReportsPresent()) return;
		ClientPacketListener conn = mc.getConnection();
		field("serverEnforcesSecureChat").set(conn, true);
		UiConfig.get().allowSignedChatOnRequiredServers = false;
		ClientSelfTest.check(ChatSigning.mode(true) == ChatSigning.Mode.BLOCKED_HERE, "enforcing server + allow off: mode BLOCKED_HERE");
		int sent = ChatSigning.signedChatsSent + ChatSigning.unsignedChatsSent, blocked = ChatSigning.blockedSends;
		conn.sendChat("jarry should not send");
		ClientSelfTest.check(ChatSigning.blockedSends == blocked + 1 && ChatSigning.signedChatsSent + ChatSigning.unsignedChatsSent == sent,
			"enforcing server + allow off: chat held back client-side with a notice (nothing sent, so no kick)");
		ChatSigning.showJoinNotice(mc, ChatSigning.Mode.BLOCKED_HERE);
	}

	/** Same server, "Allow signed chat" on: signing is used there. */
	static void signedHere(Minecraft mc) throws Exception {
		if (UiCleanup.noChatReportsPresent()) return;
		ClientPacketListener conn = mc.getConnection();
		UiConfig.get().allowSignedChatOnRequiredServers = true;
		ClientSelfTest.check(ChatSigning.mode(true) == ChatSigning.Mode.SIGNED_HERE, "enforcing server + allow on: mode SIGNED_HERE");
		field("signedMessageEncoder").set(conn, realEncoder);
		conn.sendChat("jarry signed where required");
		ClientSelfTest.check(ChatSigning.lastOutgoingChatSigned, "enforcing server + allow on: chat is signed there");
		ChatSigning.showJoinNotice(mc, ChatSigning.Mode.SIGNED_HERE);
	}

	static void restore(Minecraft mc) throws Exception {
		if (UiCleanup.noChatReportsPresent()) return;
		ClientPacketListener conn = mc.getConnection();
		field("serverEnforcesSecureChat").set(conn, false);
		field("signedMessageEncoder").set(conn, SignedMessageChain.Encoder.UNSIGNED);
		field("chatSession").set(conn, null);
		UiConfig.get().allowSignedChatOnRequiredServers = true;
		UiConfig.get().stripChatSignatures = true;
	}
}
