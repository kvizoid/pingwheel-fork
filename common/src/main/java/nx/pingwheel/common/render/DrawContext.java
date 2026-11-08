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
		int textWidth = Game.font.width(text);
		int totalWidth = textWidth + extraWidth;
		int halfWidth = (totalWidth / 2) + 4;

		int padY = 6;
		float textY = -3.5f;

		matrices.pushMatrix();
		matrices.translate(0f, 9f * yOffset);

		guiGraphics.fill(-halfWidth, -padY, halfWidth, padY, SHADOW_BLACK);

		int startX = -totalWidth / 2;
		guiGraphics.text(Game.font, text, startX + extraWidth, (int)textY, color, false);

		if (player != null) {
			matrices.pushMatrix();
			matrices.translate(startX, textY + 0.5f);
			renderPlayerHead(player);
			matrices.popMatrix();
		} else if (icon != null) {
			matrices.pushMatrix();
			matrices.translate(startX - 1f, textY - 0.5f);
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
		int iconHalf = isItem ? 9 : 4;
		int padY = isItem ? 10 : 7;

		int extraLeft = player != null ? 10 : 0;
		int leftWidth = Game.font.width(leftText) + extraLeft;
		int rightWidth = rightText != null ? Game.font.width(rightText) : 0;

		// Symmetrically center the entire pill around x = 0 so the diamond is in the dead center
		int halfWidth = Math.max(leftWidth, rightWidth) + iconHalf + 6;

		guiGraphics.fill(-halfWidth, -padY, halfWidth, padY, SHADOW_BLACK);

		// Center left text in left wing [-halfWidth, -iconHalf - 2]
		int leftAreaWidth = halfWidth - iconHalf - 2;
		int leftStartX = -halfWidth + (leftAreaWidth - leftWidth) / 2;
		float textY = -3.5f;

		guiGraphics.text(Game.font, leftText, leftStartX + extraLeft, (int)textY, leftColor, false);
		if (player != null) {
			matrices.pushMatrix();
			matrices.translate(leftStartX, textY + 0.5f);
			renderPlayerHead(player);
			matrices.popMatrix();
		}

		// Ping icon / diamond in the exact center (0, 0)
		renderPing(itemStack, drawItemIcon, pingColor);

		// Center right text in right wing [iconHalf + 2, halfWidth]
		if (rightText != null) {
			int rightAreaWidth = halfWidth - iconHalf - 2;
			int rightStartX = iconHalf + 2 + (rightAreaWidth - rightWidth) / 2;
			guiGraphics.text(Game.font, rightText, rightStartX, (int)textY, rightColor, false);
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
