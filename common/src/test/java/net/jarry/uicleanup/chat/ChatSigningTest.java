package net.jarry.uicleanup.chat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;
import net.jarry.uicleanup.config.UiConfig;
import net.minecraft.commands.arguments.ArgumentSignatures;
import net.minecraft.network.chat.LastSeenMessages;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import org.junit.jupiter.api.Test;

class ChatSigningTest {
	private static final LastSeenMessages.Update SEEN = new LastSeenMessages.Update(0, new BitSet(), (byte) 0);
	private static final MessageSignature SIG = new MessageSignature(new byte[MessageSignature.BYTES]);

	@Test
	void defaults() {
		UiConfig c = new UiConfig();
		assertTrue(c.stripChatSignatures, "stripping on by default");
		assertTrue(c.allowSignedChatOnRequiredServers, "signed chat allowed where required by default");
	}

	@Test
	void toggleOffRestoresVanillaSigning() {
		for (boolean enforces : new boolean[] {false, true}) for (boolean allow : new boolean[] {false, true})
			assertEquals(ChatSigning.Mode.VANILLA, ChatSigning.decide(false, false, allow, enforces));
		assertFalse(ChatSigning.Mode.VANILLA.strips());
	}

	@Test
	void stepsAsideForNoChatReports() {
		assertEquals(ChatSigning.Mode.VANILLA, ChatSigning.decide(true, true, true, false));
		assertEquals(ChatSigning.Mode.VANILLA, ChatSigning.decide(true, true, false, true));
	}

	@Test
	void serversThatRequireSignatures() {
		assertEquals(ChatSigning.Mode.STRIP, ChatSigning.decide(true, false, true, false));
		assertEquals(ChatSigning.Mode.STRIP, ChatSigning.decide(true, false, false, false));
		assertEquals(ChatSigning.Mode.SIGNED_HERE, ChatSigning.decide(true, false, true, true));
		assertFalse(ChatSigning.Mode.SIGNED_HERE.strips(), "allowed: signs normally there");
		assertEquals(ChatSigning.Mode.BLOCKED_HERE, ChatSigning.decide(true, false, false, true));
		assertTrue(ChatSigning.Mode.BLOCKED_HERE.strips());
	}

	@Test
	void onlyChatAndSignedCommandsNeedSignatures() {
		assertTrue(ChatSigning.needsSignature(ChatSigning.chatPacket("hi", Instant.EPOCH, 0L, null, SEEN)));
		assertTrue(ChatSigning.needsSignature(new ServerboundChatCommandSignedPacket("msg a b", Instant.EPOCH, 0L, ArgumentSignatures.EMPTY, SEEN)));
		assertFalse(ChatSigning.needsSignature(new ServerboundChatCommandPacket("time set day")));
	}

	@Test
	void chatSignatureRemovedEverythingElseKept() {
		Instant t = Instant.ofEpochMilli(1234);
		ServerboundChatPacket in = ChatSigning.chatPacket("hello", t, 42L, SIG, SEEN);
		Packet<?> out = ChatSigning.strip(in);
		ServerboundChatPacket p = (ServerboundChatPacket) out;
		assertFalse(ChatSigning.signed(p));
		assertEquals("hello", p.message());
		assertEquals(t, p.timeStamp());
		assertEquals(42L, p.salt());
		assertSame(SEEN, p.lastSeenMessages());
	}

	@Test
	void commandArgumentSignaturesRemoved() {
		ArgumentSignatures sigs = new ArgumentSignatures(List.of(new ArgumentSignatures.Entry("message", SIG)));
		ServerboundChatCommandSignedPacket in = new ServerboundChatCommandSignedPacket("msg Bob hi", Instant.EPOCH, 7L, sigs, SEEN);
		ServerboundChatCommandSignedPacket p = (ServerboundChatCommandSignedPacket) ChatSigning.strip(in);
		assertTrue(p.argumentSignatures().entries().isEmpty());
		assertEquals("msg Bob hi", p.command());
	}

	@Test
	void unsignedPacketsPassThroughUntouched() {
		ServerboundChatPacket unsigned = ChatSigning.chatPacket("hi", Instant.EPOCH, 0L, null, SEEN);
		assertSame(unsigned, ChatSigning.strip(unsigned));
		ServerboundChatCommandPacket cmd = new ServerboundChatCommandPacket("time set day");
		assertSame(cmd, ChatSigning.strip(cmd));
	}
}
