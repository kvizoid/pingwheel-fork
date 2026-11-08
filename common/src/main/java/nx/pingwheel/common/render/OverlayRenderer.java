package nx.pingwheel.common.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.core.GameContext;
import nx.pingwheel.common.core.PingManager;

import static nx.pingwheel.common.CommonClient.Game;

public class OverlayRenderer {
	private OverlayRenderer() {}

	private static final ClientConfig CLIENT_CONFIG = ClientConfig.HANDLER.getConfig();

	public static void draw(GuiGraphicsExtractor guiGraphics, float tickDelta) {
		final var pingRepo = PingManager.PING_REPO;

		if (Game.player == null || pingRepo.isEmpty()) {
			return;
		}

		final var m = guiGraphics.pose();
		final var ctx = new DrawContext(guiGraphics);
		final var showDirectionIndicator = CLIENT_CONFIG.isDirectionIndicatorVisible();

		if (showDirectionIndicator) {
			DirectionIndicatorRenderer.prepareSafeZone();
		}

		m.pushMatrix();

		if (CLIENT_CONFIG.isRulerVisible() && pingRepo.size() >= 2) {
			drawRulerLines(ctx);
		}

		for (var ping : pingRepo) {
			final var screenPos = ping.getScreenPos();

			if (screenPos == null || ping.dimension != GameContext.getDimension()) {
				continue;
			}

			final var behindCamera = screenPos.isBehindCamera();

			if (behindCamera && !showDirectionIndicator) {
				continue;
			}

			if (showDirectionIndicator) {
				DirectionIndicatorRenderer.draw(ctx, ping);
			}

			if (!behindCamera) {
				PingLocationRenderer.draw(ctx, ping);
			}
		}

		if (CLIENT_CONFIG.isRulerVisible() && pingRepo.size() >= 2) {
			drawRulerBadges(ctx);
		}

		m.popMatrix();
	}

	private static void drawRulerLines(DrawContext ctx) {
		final var pingRepo = PingManager.PING_REPO;
		if (pingRepo.size() < 2) {
			return;
		}

		int currentDim = GameContext.getDimension();
		java.util.List<nx.pingwheel.common.core.PingView> rulerPings = new java.util.ArrayList<>();
		for (var p : pingRepo) {
			if (p != null && p.dimension == currentDim && p.getPos() != null && p.isRuler()) {
				rulerPings.add(p);
			}
		}

		if (rulerPings.size() < 2) {
			return;
		}

		rulerPings.sort(java.util.Comparator.comparingInt(p -> Math.abs(p.sequence)));

		var wnd = Game.getWindow();
		float screenW = (float)wnd.getGuiScaledWidth();
		float screenH = (float)wnd.getGuiScaledHeight();
		float margin = 20f;

		for (int i = 0; i < rulerPings.size() - 1; i++) {
			var p1 = rulerPings.get(i);
			var p2 = rulerPings.get(i + 1);

			var s1 = p1.getScreenPos();
			var s2 = p2.getScreenPos();

			if (s1 == null || s2 == null || s1.isBehindCamera() || s2.isBehindCamera() || s1.depth < 0.15f || s2.depth < 0.15f) {
				continue;
			}

			// 2D clipping against visible screen bounds: eliminates any off-screen lag or huge loops!
			float[] clipped = nx.pingwheel.common.math.MathUtils.clipLine(s1.x, s1.y, s2.x, s2.y, -margin, -margin, screenW + margin, screenH + margin);
			if (clipped == null) {
				continue;
			}

			// Ultra-thin 1px dashed white hairline underneath all pings and text
			ctx.renderDashedLine(clipped[0], clipped[1], clipped[2], clipped[3], 1.0f, 4.0f, 3.0f, 0x75FFFFFF);

			// Minimalist anchor ticks at endpoints
			if (s1.x >= 0 && s1.x <= screenW && s1.y >= 0 && s1.y <= screenH) {
				ctx.getGuiGraphics().fill((int)s1.x - 1, (int)s1.y - 1, (int)s1.x + 2, (int)s1.y + 2, 0x90FFFFFF);
			}
			if (s2.x >= 0 && s2.x <= screenW && s2.y >= 0 && s2.y <= screenH) {
				ctx.getGuiGraphics().fill((int)s2.x - 1, (int)s2.y - 1, (int)s2.x + 2, (int)s2.y + 2, 0x90FFFFFF);
			}
		}
	}

	private static void drawRulerBadges(DrawContext ctx) {
		final var pingRepo = PingManager.PING_REPO;
		if (pingRepo.size() < 2) {
			return;
		}

		int currentDim = GameContext.getDimension();
		java.util.List<nx.pingwheel.common.core.PingView> rulerPings = new java.util.ArrayList<>();
		for (var p : pingRepo) {
			if (p != null && p.dimension == currentDim && p.getPos() != null && p.isRuler()) {
				rulerPings.add(p);
			}
		}

		if (rulerPings.size() < 2) {
			return;
		}

		rulerPings.sort(java.util.Comparator.comparingInt(p -> Math.abs(p.sequence)));

		var wnd = Game.getWindow();
		float screenW = (float)wnd.getGuiScaledWidth();
		float screenH = (float)wnd.getGuiScaledHeight();
		float cx = screenW * 0.5f;
		float cy = screenH * 0.5f;

		for (int i = 0; i < rulerPings.size() - 1; i++) {
			var p1 = rulerPings.get(i);
			var p2 = rulerPings.get(i + 1);

			var s1 = p1.getScreenPos();
			var s2 = p2.getScreenPos();

			if (s1 == null || s2 == null || s1.isBehindCamera() || s2.isBehindCamera() || s1.depth < 0.15f || s2.depth < 0.15f) {
				continue;
			}

			float midX = (s1.x + s2.x) * 0.5f;
			float midY = (s1.y + s2.y) * 0.5f;

			if (midX < -30 || midX > screenW + 30 || midY < -30 || midY > screenH + 30) {
				continue;
			}

			var b1 = net.minecraft.core.BlockPos.containing(p1.getPos().x, p1.getPos().y, p1.getPos().z);
			var b2 = net.minecraft.core.BlockPos.containing(p2.getPos().x, p2.getPos().y, p2.getPos().z);

			int bdx = Math.abs(b2.getX() - b1.getX());
			int bdy = Math.abs(b2.getY() - b1.getY());
			int bdz = Math.abs(b2.getZ() - b1.getZ());

			int sx = bdx + 1;
			int sy = bdy + 1;
			int sz = bdz + 1;

			int activeDims = (sx > 1 ? 1 : 0) + (sy > 1 ? 1 : 0) + (sz > 1 ? 1 : 0);

			double realDist = p1.getPos().distanceTo(p2.getPos());
			String mainStr;
			String subStr;

			if (activeDims <= 1) {
				// 1D line along an axis (inclusive block count)
				int totalBlocks = Math.max(sx, Math.max(sy, sz));
				mainStr = totalBlocks + " б";

				int innerGap = Math.max(0, totalBlocks - 2);
				if (totalBlocks <= 2) {
					subStr = String.format(java.util.Locale.ROOT, "%.1f м", realDist);
				} else {
					subStr = String.format(java.util.Locale.ROOT, "просвет: %d б • %.1f м", innerGap, realDist);
				}
			} else if (activeDims == 2) {
				// 2D plane (floor, ceiling, wall)
				int d1, d2;
				if (sy == 1) {
					d1 = sx;
					d2 = sz;
				} else if (sz == 1) {
					d1 = sx;
					d2 = sy;
				} else {
					d1 = sz;
					d2 = sy;
				}
				mainStr = d1 + " × " + d2 + " б";
				long area = (long)d1 * d2;
				subStr = String.format(java.util.Locale.ROOT, "S = %d б² • %.1f м", area, realDist);
			} else {
				// 3D volume
				mainStr = sx + " × " + sy + " × " + sz + " б";
				long volume = (long)sx * sy * sz;
				subStr = String.format(java.util.Locale.ROOT, "V = %d б³ • %.1f м", volume, realDist);
			}

			boolean isHovered = Math.hypot(cx - midX, cy - midY) < 32.0;

			var badgeText = net.minecraft.network.chat.Component.literal(mainStr).withStyle(net.minecraft.ChatFormatting.WHITE);

			var m = ctx.getMatrices();
			m.pushMatrix();
			m.translate(midX, midY);
			m.scale(0.85f, 0.85f);

			int textWidth = Game.font.width(badgeText);
			int shadowColor = net.minecraft.util.ARGB.color(160, 0, 0, 0);

			if (isHovered) {
				var subComponent = net.minecraft.network.chat.Component.literal(subStr).withStyle(net.minecraft.ChatFormatting.GRAY);
				int subWidth = Game.font.width(subComponent);
				int maxWidth = Math.max(textWidth, subWidth);
				int halfWidth = (maxWidth / 2) + 4;

				ctx.getGuiGraphics().fill(-halfWidth, -9, halfWidth, 9, shadowColor);
				ctx.getGuiGraphics().text(Game.font, badgeText, -textWidth / 2, -8, 0xFFFFFFFF, false);
				ctx.getGuiGraphics().text(Game.font, subComponent, -subWidth / 2, 1, 0xFF94A3B8, false);
			} else {
				int halfWidth = (textWidth / 2) + 3;
				int padY = 4;
				ctx.getGuiGraphics().fill(-halfWidth, -padY, halfWidth, padY, shadowColor);
				ctx.getGuiGraphics().text(Game.font, badgeText, -textWidth / 2, -3, 0xFFFFFFFF, false);
			}

			m.popMatrix();
		}
	}
}
