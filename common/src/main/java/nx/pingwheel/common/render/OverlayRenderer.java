package nx.pingwheel.common.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;
import nx.pingwheel.common.CommonClient;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.core.GameContext;
import nx.pingwheel.common.core.PingManager;
import nx.pingwheel.common.math.MathUtils;

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

		if (CLIENT_CONFIG.isMobRadiusVisible()) {
			drawMobRadii(ctx);
		}

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
		boolean shiftHeld = Game != null && Game.options != null && Game.options.keyShift.isDown();
		java.util.List<nx.pingwheel.common.core.PingView> rulerPings = new java.util.ArrayList<>();
		for (var p : pingRepo) {
			if (p != null && p.dimension == currentDim && p.getPos() != null && (p.isRuler() || shiftHeld)) {
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


			float[] clipped = nx.pingwheel.common.math.MathUtils.clipLine(s1.x, s1.y, s2.x, s2.y, -margin, -margin, screenW + margin, screenH + margin);
			if (clipped == null) {
				continue;
			}

			float midX = (clipped[0] + clipped[2]) * 0.5f;
			float midY = (clipped[1] + clipped[3]) * 0.5f;
			float dx = clipped[2] - clipped[0];
			float dy = clipped[3] - clipped[1];
			float len = (float)Math.hypot(dx, dy);
			float gap = 24.0f;

			if (len > gap * 2.2f) {
				float nx = dx / len;
				float ny = dy / len;
				float p1x = midX - nx * gap;
				float p1y = midY - ny * gap;
				float p2x = midX + nx * gap;
				float p2y = midY + ny * gap;
				ctx.renderDashedLine(clipped[0], clipped[1], p1x, p1y, 1.0f, 4.0f, 3.0f, 0x75FFFFFF);
				ctx.renderDashedLine(p2x, p2y, clipped[2], clipped[3], 1.0f, 4.0f, 3.0f, 0x75FFFFFF);
			} else {
				ctx.renderDashedLine(clipped[0], clipped[1], clipped[2], clipped[3], 1.0f, 4.0f, 3.0f, 0x75FFFFFF);
			}

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
		boolean shiftHeld = Game != null && Game.options != null && Game.options.keyShift.isDown();
		java.util.List<nx.pingwheel.common.core.PingView> rulerPings = new java.util.ArrayList<>();
		for (var p : pingRepo) {
			if (p != null && p.dimension == currentDim && p.getPos() != null && (p.isRuler() || shiftHeld)) {
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

			float[] clipped = nx.pingwheel.common.math.MathUtils.clipLine(s1.x, s1.y, s2.x, s2.y, -margin, -margin, screenW + margin, screenH + margin);
			if (clipped == null) {
				continue;
			}

			float midX = (clipped[0] + clipped[2]) * 0.5f;
			float midY = (clipped[1] + clipped[3]) * 0.5f;

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

			String mainStr;

			if (activeDims <= 1) {
				int totalBlocks = Math.max(sx, Math.max(sy, sz));
				mainStr = totalBlocks + " б";
			} else if (activeDims == 2) {
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
			} else {
				mainStr = sx + " × " + sy + " × " + sz + " б";
			}

			boolean isHovered = Math.hypot(cx - midX, cy - midY) < 32.0;

			var badgeText = net.minecraft.network.chat.Component.literal(mainStr);

			var m = ctx.getMatrices();
			m.pushMatrix();
			m.translate(midX, midY);
			m.scale(0.85f, 0.85f);

			int textWidth = Game.font.width(badgeText);
			int shadowColor = isHovered ? net.minecraft.util.ARGB.color(100, 16, 21, 32) : net.minecraft.util.ARGB.color(64, 0, 0, 0);
			int textColor = isHovered ? 0xFFFFFFFF : net.minecraft.util.ARGB.color(200, 241, 245, 249);
			int halfWidth = (textWidth / 2) + 4;
			int padY = 6;

			ctx.getGuiGraphics().fill(-halfWidth, -padY, halfWidth, padY, shadowColor);
			ctx.getGuiGraphics().text(Game.font, badgeText, -textWidth / 2, -3, textColor, false);

			m.popMatrix();
		}
	}

	private static void drawMobRadii(DrawContext ctx) {
		var worldCtx = CommonClient.LAST_WORLD_CONTEXT;
		if (worldCtx == null || Game == null || Game.level == null) return;

		final var pingRepo = PingManager.PING_REPO;
		if (pingRepo.isEmpty()) return;

		var wnd = Game.getWindow();
		float screenW = (float)wnd.getGuiScaledWidth();
		float screenH = (float)wnd.getGuiScaledHeight();
		float margin = 20f;
		int currentDim = GameContext.getDimension();

		for (var ping : pingRepo) {
			if (ping == null || ping.dimension != currentDim || ping.entityId == null) continue;
			var ent = GameContext.getEntity(ping.entityId);
			if (ent == null || ent.isRemoved() || !(ent instanceof net.minecraft.world.entity.LivingEntity)) continue;

			float radius = getDangerRadius(ent);
			if (radius <= 0.1f) continue;

			double cx = ent.getX();
			double cy = ent.getY() + 0.05;
			double cz = ent.getZ();

			int segments = 32;
			int circleColor = 0x85FFA8A8;

			var camPos = worldCtx.camera.pos;
			var mobBox = ent.getBoundingBox().inflate(0.08);

			for (int i = 0; i < segments; i++) {
				double a1 = (i * 2.0 * Math.PI) / segments;
				double a2 = ((i + 1) * 2.0 * Math.PI) / segments;

				Vec3 p1 = new Vec3(cx + radius * Math.cos(a1), cy, cz + radius * Math.sin(a1));
				Vec3 p2 = new Vec3(cx + radius * Math.cos(a2), cy, cz + radius * Math.sin(a2));

				Vec3 mid = p1.add(p2).scale(0.5);

				if (mobBox.clip(camPos, mid).isPresent() || mobBox.clip(camPos, p1).isPresent() || mobBox.clip(camPos, p2).isPresent()) {
					continue;
				}

				var blockHit = Game.level.clip(new net.minecraft.world.level.ClipContext(
					camPos,
					mid,
					net.minecraft.world.level.ClipContext.Block.COLLIDER,
					net.minecraft.world.level.ClipContext.Fluid.NONE,
					ent
				));
				if (blockHit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
					if (blockHit.getLocation().distanceToSqr(camPos) < mid.distanceToSqr(camPos) - 0.25) {
						continue;
					}
				}

				var s1 = MathUtils.worldToScreen(p1, worldCtx);
				var s2 = MathUtils.worldToScreen(p2, worldCtx);

				if (s1 == null || s2 == null || s1.isBehindCamera() || s2.isBehindCamera() || s1.depth < 0.15f || s2.depth < 0.15f) {
					continue;
				}

				float[] clipped = MathUtils.clipLine(s1.x, s1.y, s2.x, s2.y, -margin, -margin, screenW + margin, screenH + margin);
				if (clipped == null) continue;

				ctx.renderDashedLine(clipped[0], clipped[1], clipped[2], clipped[3], 1.0f, 3.0f, 3.0f, circleColor);
			}
		}
	}

	public static float getDangerRadius(net.minecraft.world.entity.Entity entity) {
		var type = entity.getType();
		if (type == net.minecraft.world.entity.EntityTypes.CREEPER) return 3.2f;
		if (type == net.minecraft.world.entity.EntityTypes.WARDEN) return 16.0f;
		if (type == net.minecraft.world.entity.EntityTypes.WITCH) return 9.0f;
		if (type == net.minecraft.world.entity.EntityTypes.SKELETON
			|| type == net.minecraft.world.entity.EntityTypes.STRAY
			|| type == net.minecraft.world.entity.EntityTypes.BOGGED) return 15.0f;
		if (type == net.minecraft.world.entity.EntityTypes.SPIDER
			|| type == net.minecraft.world.entity.EntityTypes.CAVE_SPIDER) return 5.0f;
		if (type == net.minecraft.world.entity.EntityTypes.BLAZE) return 16.0f;
		if (type == net.minecraft.world.entity.EntityTypes.GHAST) return 24.0f;
		if (type == net.minecraft.world.entity.EntityTypes.ZOMBIE
			|| type == net.minecraft.world.entity.EntityTypes.HUSK
			|| type == net.minecraft.world.entity.EntityTypes.DROWNED
			|| type == net.minecraft.world.entity.EntityTypes.PIGLIN_BRUTE
			|| type == net.minecraft.world.entity.EntityTypes.VINDICATOR
			|| type == net.minecraft.world.entity.EntityTypes.WITHER_SKELETON
			|| type == net.minecraft.world.entity.EntityTypes.IRON_GOLEM) return 2.5f;
		if (entity instanceof net.minecraft.world.entity.monster.Enemy) return 3.0f;
		return 0f;
	}
}

