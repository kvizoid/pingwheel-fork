package nx.pingwheel.common.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec2;
import nx.pingwheel.common.math.MathUtils;
import org.joml.Matrix3x2fStack;

import static nx.pingwheel.common.CommonClient.Game;
import static nx.pingwheel.common.resource.ResourceConstants.ARROW_TEXTURE_ID;
import static nx.pingwheel.common.resource.ResourceConstants.PING_TEXTURE_ID;
import static nx.pingwheel.common.resource.ResourceReloadListener.hasCustomTexture;

public class DrawContext {

	private static final int SHADOW_BLACK = ARGB.color(64, 0, 0, 0);

	private GuiGraphicsExtractor guiGraphics;
	private Matrix3x2fStack matrices;

	public Matrix3x2fStack getMatrices() {
		return matrices;
	}

	public DrawContext(GuiGraphicsExtractor guiGraphics) {
		this.guiGraphics = guiGraphics;
		this.matrices = guiGraphics.pose();
	}

	public void renderLabel(Component text, float yOffset, PlayerInfo player, int color) {
		renderLabel(text, yOffset, player, null, color);
	}

	public void renderLabel(Component text, float yOffset, PlayerInfo player, ItemStack icon, int color) {
		var extraWidth = (player != null || icon != null) ? 10 : 0;
		var textMetrics = new Vec2(
			Game.font.width(text) + extraWidth,
			Game.font.lineHeight
		);
		var textOffset = textMetrics.scale(-0.5f).add(new Vec2(0f, textMetrics.y * yOffset));

		matrices.pushMatrix();
		matrices.translate(textOffset.x, textOffset.y);
		guiGraphics.fill(-2, -2, (int)textMetrics.x + 1, (int)textMetrics.y, SHADOW_BLACK);
		guiGraphics.text(Game.font, text, extraWidth, 0, color, false);

		if (player != null) {
			matrices.pushMatrix();
			matrices.translate(-0.5f, -0.5f);
			renderPlayerHead(player);
			matrices.popMatrix();
		} else if (icon != null) {
			matrices.pushMatrix();
			matrices.translate(-0.5f, -0.5f);
			matrices.scale(0.5f, 0.5f);
			guiGraphics.item(icon, 0, 0);
			matrices.popMatrix();
		}

		matrices.popMatrix();
	}

	public void renderPlayerHead(PlayerInfo player) {
		var texture = player.getSkin().body().texturePath();
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 8, 8, 8, 8, 64, 64);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 40, 8, 8, 8, 64, 64); // Overlay (hat)
	}

	public void renderInlinePing(Component leftText, Component rightText, PlayerInfo player, ItemStack itemStack, boolean drawItemIcon, int pingColor, int leftColor, int rightColor) {
		boolean isItem = (itemStack != null && drawItemIcon);
		int iconHalf = isItem ? 9 : 5;
		int padY = isItem ? 9 : 6;

		int extraLeft = player != null ? 10 : 0;
		int leftWidth = Game.font.width(leftText) + extraLeft;
		int rightWidth = rightText != null ? Game.font.width(rightText) : 0;

		float leftStartX = -iconHalf - 4 - leftWidth;
		float rightStartX = iconHalf + 4;
		float textY = -4.5f;

		float totalEndX = rightText != null ? (rightStartX + rightWidth) : iconHalf;
		guiGraphics.fill((int)leftStartX - 2, -padY, (int)totalEndX + 2, padY, SHADOW_BLACK);

		guiGraphics.text(Game.font, leftText, (int)leftStartX + extraLeft, (int)textY, leftColor, false);
		if (player != null) {
			matrices.pushMatrix();
			matrices.translate(leftStartX - 0.5f, textY - 0.5f);
			renderPlayerHead(player);
			matrices.popMatrix();
		}

		renderPing(itemStack, drawItemIcon, pingColor);

		if (rightText != null) {
			guiGraphics.text(Game.font, rightText, (int)rightStartX, (int)textY, rightColor, false);
		}
	}

	public void renderPing(ItemStack itemStack, boolean drawItemIcon, int color) {
		if (itemStack != null && drawItemIcon) {
			renderGuiItemModel(itemStack);
		} else if (hasCustomTexture()) {
			renderTexture(PING_TEXTURE_ID, 12, color);
		} else {
			renderDefaultPingIcon(color);
		}
	}

	public void renderGuiItemModel(ItemStack itemStack) {
		guiGraphics.item(itemStack, -8, -8, -150);
		if (itemStack.getCount() > 1) {
			guiGraphics.itemDecorations(Game.font, itemStack, -8, -8);
		}
	}

	public void renderDefaultPingIcon(int color) {
		matrices.pushMatrix();
		MathUtils.rotateZ(matrices, (float)(Math.PI / 4f));
		matrices.translate(-2.5f, -2.5f);
		guiGraphics.fill(0, 0, 5, 5, color);
		matrices.popMatrix();
	}

	public void renderTexture(Identifier texture, int size, int color) {
		final var offset = size / -2;

		guiGraphics.blit(
			RenderPipelines.GUI_TEXTURED,
			texture,
			offset,
			offset,
			0,
			0,
			size,
			size,
			size,
			size,
			color
		);
	}

	public void renderArrowIcon(int color) {
		renderTexture(ARROW_TEXTURE_ID, 10, color);
	}
}
