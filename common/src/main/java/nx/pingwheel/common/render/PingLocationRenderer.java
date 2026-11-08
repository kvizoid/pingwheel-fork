package nx.pingwheel.common.render;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.config.PlayerInfoMode;
import nx.pingwheel.common.config.TeamColorMode;
import nx.pingwheel.common.core.PingManager;
import nx.pingwheel.common.core.PingView;
import nx.pingwheel.common.resource.LanguageUtils;

import static nx.pingwheel.common.CommonClient.Game;

public class PingLocationRenderer {
	private PingLocationRenderer() {}

	private static final ClientConfig CLIENT_CONFIG = ClientConfig.HANDLER.getConfig();
	private static final int WHITE = 0xFFFFFFFF;
	private static final int HOSTILE_PING_COLOR = 0xFFFF3333;
	private static final int HOSTILE_LABEL_COLOR = 0xFFFF6666;

	public static void draw(DrawContext ctx, PingView ping) {
		final var screenPos = ping.getScreenPos();

		if (screenPos == null) {
			return;
		}

		final var m = ctx.getMatrices();
		final var pingScale = ping.getScale();

		m.pushMatrix();
		m.translate(screenPos.x, screenPos.y);
		m.scale(pingScale, pingScale);

		final var labelUseTeamColor = CLIENT_CONFIG.getTeamColorMode() == TeamColorMode.FULL || CLIENT_CONFIG.getTeamColorMode() == TeamColorMode.LABELS_ONLY;
		final var pingUseTeamColor = CLIENT_CONFIG.getTeamColorMode() == TeamColorMode.FULL || CLIENT_CONFIG.getTeamColorMode() == TeamColorMode.PING_ONLY;
		int distanceColor = labelUseTeamColor ? ping.getTeamColor() : 0xFFF1F5F9;
		int pingColor = pingUseTeamColor ? ping.getTeamColor() : WHITE;

		if (ping.isHostile()) {
			pingColor = HOSTILE_PING_COLOR;
			distanceColor = 0xFFF1F5F9;
		}

		final boolean isDetailed = ping.getDistance() <= CLIENT_CONFIG.getDetailDistance();
		final boolean isHovered = (ping == PingManager.getHoveredPing());
		final boolean showInlineCoords = (isDetailed || isHovered) && CLIENT_CONFIG.isCoordinatesVisible() && ping.getPos() != null;

		final var author = ping.getPlayerInfo();
		final var compactPlayerInfo = ((isDetailed || isHovered) && CLIENT_CONFIG.getPlayerInfoMode() == PlayerInfoMode.COMPACT) ? author : null;

		final int OVERWORLD_COLOR = 0xFFA8E7FF;
		final int NETHER_COLOR = 0xFFFFA8A8;

		if (showInlineCoords) {
			Component leftText = LanguageUtils.UNIT_METERS.get("%,.1f".formatted(ping.getDistance()));
			int bx = (int)Math.floor(ping.getPos().x);
			int by = (int)Math.floor(ping.getPos().y);
			int bz = (int)Math.floor(ping.getPos().z);

			boolean isNetherCoords = false;
			int targetX = bx;
			int targetY = by;
			int targetZ = bz;

			if (nx.pingwheel.common.core.GameContext.isNether()) {
				if (ping.isAltDimension()) {
					targetX = (int)Math.floor(ping.getPos().x * 8.0);
					targetZ = (int)Math.floor(ping.getPos().z * 8.0);
					isNetherCoords = false;
				} else {
					isNetherCoords = true;
				}
			} else {
				if (ping.isAltDimension()) {
					targetX = (int)Math.floor(ping.getPos().x / 8.0);
					targetZ = (int)Math.floor(ping.getPos().z / 8.0);
					isNetherCoords = true;
				} else {
					isNetherCoords = false;
				}
			}

			var rightText = Component.literal(targetX + " " + targetY + " " + targetZ);
			int coordsColor = isNetherCoords ? NETHER_COLOR : OVERWORLD_COLOR;

			ctx.renderInlinePing(leftText, rightText, compactPlayerInfo, ping.getItemStack(), CLIENT_CONFIG.isItemIconVisible(), pingColor, distanceColor, coordsColor);
		} else {
			Component distanceText;
			if (isDetailed) {
				distanceText = LanguageUtils.UNIT_METERS.get("%,.1f".formatted(ping.getDistance()));
			} else {
				distanceText = LanguageUtils.UNIT_METERS.get("%d".formatted(Math.round(ping.getDistance())));
			}
			ctx.renderLabel(distanceText, -1.6f, compactPlayerInfo, null, distanceColor);
			ctx.renderPing(ping.getItemStack(), isDetailed && CLIENT_CONFIG.isItemIconVisible(), pingColor);
		}

		if (isDetailed || isHovered) {
			float entityYOffset = showInlineCoords ? 1.6f : 1.75f;
			if (ping.getTargetPlayerInfo() != null && ping.getTargetEntityName() != null) {
				ctx.renderLabel(ping.getTargetEntityName(), entityYOffset, ping.getTargetPlayerInfo(), null, WHITE);
			} else if (ping.getTargetEntityName() != null) {
				ctx.renderLabel(ping.getTargetEntityName(), entityYOffset, null, ping.getTargetEntityIcon(), WHITE);
			} else if (ping.getPos() != null) {
				var bPos = net.minecraft.core.BlockPos.containing(ping.getPos().x, ping.getPos().y, ping.getPos().z);
				var snapshot = nx.pingwheel.common.core.ContainerMemoryManager.get(bPos);
				if (snapshot != null) {
					ctx.renderLabel(snapshot.formatSummary(), entityYOffset, null, WHITE);
				} else if (Game.level != null) {
					var state = Game.level.getBlockState(bPos);
					var blk = state.getBlock();
					if (blk instanceof net.minecraft.world.level.block.ChestBlock
						|| blk instanceof net.minecraft.world.level.block.BarrelBlock
						|| blk instanceof net.minecraft.world.level.block.ShulkerBoxBlock) {
						var unopenedText = Component.literal("📦 ").withStyle(ChatFormatting.GOLD)
							.append(Component.translatable("pingwheel.container_unopened").withStyle(ChatFormatting.GRAY));
						ctx.renderLabel(unopenedText, entityYOffset, null, WHITE);
					}
				}
			} else {
				final var isPlayerListHeld = CLIENT_CONFIG.getPlayerInfoMode() == PlayerInfoMode.HOLD && Game.options.keyPlayerList.isDown();
				final var showVerbosePlayerInfo = CLIENT_CONFIG.getPlayerInfoMode() == PlayerInfoMode.ALWAYS || isPlayerListHeld;

				if (showVerbosePlayerInfo && author != null) {
					var displayName = PlayerTeam.formatNameForTeam(author.getTeam(), Component.literal(author.getProfile().name()));
					if (!labelUseTeamColor) displayName = displayName.withStyle(ChatFormatting.RESET);

					ctx.renderLabel(displayName, entityYOffset, author, null, WHITE);
				}
			}
		}

		m.popMatrix();
	}
}
