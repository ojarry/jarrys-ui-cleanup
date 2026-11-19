package net.jarry.uicleanup.mixin;

import java.util.List;
import java.util.Set;
import net.jarry.uicleanup.platform.LoaderBridge;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Leaves the chat-report and chat-signature mixins out entirely when No Chat Reports is installed, so the two mods never overlap. */
public final class UiMixinPlugin implements IMixinConfigPlugin {
	@Override public void onLoad(String mixinPackage) { }
	@Override public String getRefMapperConfig() { return null; }

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (mixinClassName.endsWith(".PlayerEntryMixin") || mixinClassName.endsWith(".ChatSigningMixin")) {
			boolean ncr = LoaderBridge.isModLoaded("nochatreports");
			if (ncr) org.slf4j.LoggerFactory.getLogger("Jarry's UI Cleanup").info("No Chat Reports found: {} not applied", mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1));
			return !ncr;
		}
		return true;
	}

	@Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }
	@Override public List<String> getMixins() { return null; }
	@Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
	@Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
