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

	public GuiGraphicsExtractor getGuiGraphics() {
		return guiGraphics;
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
		int iconHalf = isItem ? 8 : 3;
		int iconWidth = isItem ? 16 : 6;
		int padY = isItem ? 9 : 6;
		int gap = 4;

		int extraLeft = player != null ? 10 : 0;
		int leftWidth = Game.font.width(leftText) + extraLeft;
		int rightWidth = rightText != null ? Game.font.width(rightText) : 0;

		int contentWidth = leftWidth + gap + iconWidth + (rightText != null ? (gap + rightWidth) : 0);
		int halfWidth = (contentWidth / 2) + 4;

		guiGraphics.fill(-halfWidth, -padY, halfWidth, padY, SHADOW_BLACK);

		int startX = -contentWidth / 2;
		float textY = -3.5f;

		// 1. Left text (Distance)
		guiGraphics.text(Game.font, leftText, startX + extraLeft, (int)textY, leftColor, false);
		if (player != null) {
			matrices.pushMatrix();
			matrices.translate(startX, textY + 0.5f);
			renderPlayerHead(player);
			matrices.popMatrix();
		}

		// 2. Icon (Diamond) placed tightly between left and right text
		int iconCenterX = startX + leftWidth + gap + iconHalf;
		matrices.pushMatrix();
		matrices.translate(iconCenterX, 0);
		renderPing(itemStack, drawItemIcon, pingColor);
		matrices.popMatrix();

		// 3. Right text (Coordinates)
		if (rightText != null) {
			int rightStartX = startX + leftWidth + gap + iconWidth + gap;
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

	public void renderLine(float x1, float y1, float x2, float y2, float thickness, int color) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float length = (float)Math.hypot(dx, dy);
		if (length < 1f) {
			return;
		}
		float angle = (float)Math.atan2(dy, dx);
		matrices.pushMatrix();
		matrices.translate(x1, y1);
		MathUtils.rotateZ(matrices, angle);
		int halfThick = Math.max(1, (int)(thickness / 2f + 0.5f));
		guiGraphics.fill(0, -halfThick, (int)length, halfThick, color);
		matrices.popMatrix();
	}

	public void renderDashedLine(float x1, float y1, float x2, float y2, float thickness, float dashLen, float gapLen, int color) {
		float dx = x2 - x1;
		float dy = y2 - y1;
		float length = (float)Math.hypot(dx, dy);
		if (length < 2f) {
			return;
		}
		float angle = (float)Math.atan2(dy, dx);
		matrices.pushMatrix();
		matrices.translate(x1, y1);
		MathUtils.rotateZ(matrices, angle);

		boolean isHairline = thickness <= 1.0f;
		int halfThick = Math.max(1, (int)(thickness / 2f + 0.5f));

		float cur = 0f;
		int maxDashes = 250;
		int count = 0;
		while (cur < length && count++ < maxDashes) {
			float next = Math.min(cur + dashLen, length);
			int start = (int)cur;
			int end = Math.max(start + 1, (int)next);
			if (isHairline) {
				guiGraphics.fill(start, 0, end, 1, color);
			} else {
				guiGraphics.fill(start, -halfThick, end, halfThick, color);
			}
			cur += dashLen + gapLen;
		}

		matrices.popMatrix();
	}
}
