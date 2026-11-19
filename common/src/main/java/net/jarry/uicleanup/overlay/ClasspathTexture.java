package net.jarry.uicleanup.overlay;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.renderer.texture.MipmapStrategy;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * A texture read straight from this mod's jar, like vanilla does for its own loading logo, so it can be shown
 * while the resource manager itself is still loading. Smooth (linear) filtering so the logo scales cleanly.
 */
final class ClasspathTexture extends ReloadableTexture {
	private final String path;

	ClasspathTexture(Identifier id, String path) {
		super(id);
		this.path = path;
	}

	@Override
	public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
		try (InputStream in = ClasspathTexture.class.getResourceAsStream(path)) {
			if (in == null) throw new IOException("missing " + path);
			return new TextureContents(NativeImage.read(in), new TextureMetadataSection(true, true, MipmapStrategy.MEAN, 0.0F));
		}
	}
}
