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
		int distanceColor = labelUseTeamColor ? ping.getTeamColor() : WHITE;
		int pingColor = pingUseTeamColor ? ping.getTeamColor() : WHITE;

		if (ping.isHostile()) {
			pingColor = HOSTILE_PING_COLOR;
			distanceColor = HOSTILE_LABEL_COLOR;
		}

		final boolean isDetailed = ping.getDistance() <= CLIENT_CONFIG.getDetailDistance();
		final boolean isHovered = (ping == PingManager.getHoveredPing());
		final boolean showInlineCoords = (isDetailed || isHovered) && CLIENT_CONFIG.isCoordinatesVisible() && ping.getPos() != null;

		final var author = ping.getPlayerInfo();
		final var compactPlayerInfo = ((isDetailed || isHovered) && CLIENT_CONFIG.getPlayerInfoMode() == PlayerInfoMode.COMPACT) ? author : null;

		if (showInlineCoords) {
			Component leftText = LanguageUtils.UNIT_METERS.get("%,.1f".formatted(ping.getDistance()));
			int bx = (int)Math.floor(ping.getPos().x);
			int by = (int)Math.floor(ping.getPos().y);
			int bz = (int)Math.floor(ping.getPos().z);

			var rightText = Component.literal(bx + " " + by + " " + bz);
			int coordsColor = isHovered ? 0xFFFFFF55 : 0xFFFFD700;
			if (isHovered) {
				rightText = rightText.append(Component.translatable("pingwheel.rmb_badge").withStyle(ChatFormatting.AQUA));
			}

			ctx.renderInlinePing(leftText, rightText, compactPlayerInfo, ping.getItemStack(), CLIENT_CONFIG.isItemIconVisible(), pingColor, distanceColor, coordsColor);
		} else {
			Component distanceText;
			if (isDetailed) {
				distanceText = LanguageUtils.UNIT_METERS.get("%,.1f".formatted(ping.getDistance()));
			} else {
				distanceText = LanguageUtils.UNIT_METERS.get("%d".formatted(Math.round(ping.getDistance())));
			}
			ctx.renderLabel(distanceText, -1.5f, compactPlayerInfo, null, distanceColor);
			ctx.renderPing(ping.getItemStack(), isDetailed && CLIENT_CONFIG.isItemIconVisible(), pingColor);
		}

		if (isDetailed) {
			if (ping.getTargetPlayerInfo() != null && ping.getTargetEntityName() != null) {
				ctx.renderLabel(ping.getTargetEntityName(), 1.75f, ping.getTargetPlayerInfo(), null, WHITE);
			} else if (ping.getTargetEntityName() != null) {
				ctx.renderLabel(ping.getTargetEntityName(), 1.75f, null, ping.getTargetEntityIcon(), WHITE);
			} else {
				final var isPlayerListHeld = CLIENT_CONFIG.getPlayerInfoMode() == PlayerInfoMode.HOLD && Game.options.keyPlayerList.isDown();
				final var showVerbosePlayerInfo = CLIENT_CONFIG.getPlayerInfoMode() == PlayerInfoMode.ALWAYS || isPlayerListHeld;

				if (showVerbosePlayerInfo && author != null) {
					var displayName = PlayerTeam.formatNameForTeam(author.getTeam(), Component.literal(author.getProfile().name()));
					if (!labelUseTeamColor) displayName = displayName.withStyle(ChatFormatting.RESET);

					ctx.renderLabel(displayName, 1.75f, author, null, WHITE);
				}
			}
		}

		m.popMatrix();
	}
}
