package nx.pingwheel.common.core;

import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.network.PingLocationS2CPacket;
import nx.pingwheel.common.render.WorldRenderContext;

import java.util.ArrayList;
import java.util.Objects;

import static nx.pingwheel.common.CommonClient.Game;
import static nx.pingwheel.common.Global.LOGGER;
import static nx.pingwheel.common.config.ClientConfig.MAX_PING_DISTANCE;

public class PingManager {
	private PingManager() {}

	private static final ClientConfig CLIENT_CONFIG = ClientConfig.HANDLER.getConfig();

	public static final ArrayList<PingView> PING_REPO = new ArrayList<>();

	public static void clearPings() {
		PING_REPO.clear();
	}

	public static void addOrReplacePing(PingView newPing) {
		int index = -1;

		for (int i = 0; i < PING_REPO.size(); i++) {
			var entry = PING_REPO.get(i);

			if (Objects.equals(entry.authorId, newPing.authorId) && entry.sequence == newPing.sequence) {
				index = i;
				break;
			}
		}

		if (index != -1) {
			PING_REPO.set(index, newPing);
		} else {
			int maxPings = CLIENT_CONFIG.getMaxSimultaneousPings();
			int authorCount = 0;
			int oldestIndex = -1;
			int maxAge = -1;

			for (int i = 0; i < PING_REPO.size(); i++) {
				var entry = PING_REPO.get(i);
				if (Objects.equals(entry.authorId, newPing.authorId)) {
					authorCount++;
					if (entry.getAge() > maxAge) {
						maxAge = entry.getAge();
						oldestIndex = i;
					}
				}
			}

			if (authorCount >= maxPings && oldestIndex != -1) {
				PING_REPO.remove(oldestIndex);
			}

			PING_REPO.add(newPing);
		}
	}

	public static void updatePings(WorldRenderContext ctx) {
		if (Game == null || Game.level == null || PING_REPO.isEmpty()) {
			return;
		}

		final var time = (int)Game.level.getGameTime();
		PingView target = null;

		for (var iter = PING_REPO.iterator(); iter.hasNext(); ) {
			final var ping = iter.next();

			ping.update(ctx, time);

			if (ping.isExpired()) {
				iter.remove();
			} else if (PingController.isPingQueued() && ping.isRemovable() && ping.isCloserToCenter(target)) {
				target = ping;
			}
		}

		if (target != null && PING_REPO.remove(target)) {
			PingController.revokePingAction();
		}

		PING_REPO.sort((a, b) -> Double.compare(b.getDistance(), a.getDistance()));
	}

	public static void acceptPingPacket(PingLocationS2CPacket packet) {
		if (packet.isCorrupt()) {
			LOGGER.warn("received invalid ping location from server");
			return;
		}

		final var connection = Game.getConnection();

		if (Game.player == null || Game.level == null || connection == null) {
			return;
		}

		if (!packet.channel().equals(CLIENT_CONFIG.getChannel())) {
			return;
		}

		if (CLIENT_CONFIG.getPingDistance() < MAX_PING_DISTANCE) {
			var vecToPing = Game.player.position().vectorTo(packet.pos());

			if (vecToPing.length() > CLIENT_CONFIG.getPingDistance()) {
				return;
			}
		}

		Game.execute(() -> {
			final var newPing = PingView.from(packet);

			PingManager.addOrReplacePing(newPing);
			newPing.playSoundInDimension();
		});
	}

	public static PingView getHoveredPing() {
		if (Game == null || Game.getWindow() == null || PING_REPO.isEmpty()) {
			return null;
		}

		final var wnd = Game.getWindow();
		float px = wnd.getGuiScaledWidth() * 0.5f;
		float py = wnd.getGuiScaledHeight() * 0.5f;

		if (Game.gui != null && Game.gui.screen() != null) {
			px = (float)(Game.mouseHandler.xpos() * (double)wnd.getGuiScaledWidth() / (double)Math.max(1, wnd.getScreenWidth()));
			py = (float)(Game.mouseHandler.ypos() * (double)wnd.getGuiScaledHeight() / (double)Math.max(1, wnd.getScreenHeight()));
		}

		PingView closest = null;
		float minDistance = Float.MAX_VALUE;

		for (var ping : PING_REPO) {
			if (ping == null || ping.getScreenPos() == null || ping.getScreenPos().isBehindCamera()) {
				continue;
			}
			if (ping.dimension != GameContext.getDimension()) {
				continue;
			}

			float scale = Math.max(0.6f, ping.getScale());
			float dx = Math.abs(px - ping.getScreenPos().x);
			float dy = Math.abs(py - ping.getScreenPos().y);

			float maxDx = Math.max(45f, 65f * scale);
			float maxDy = Math.max(22f, 32f * scale);

			if (dx <= maxDx && dy <= maxDy) {
				float dist = dx + dy;
				if (dist < minDistance) {
					minDistance = dist;
					closest = ping;
				}
			}
		}

		return closest;
	}

	public static void copyPingCoordinates(PingView ping) {
		if (ping == null || ping.getPos() == null || Game == null) {
			return;
		}

		int x = (int)Math.floor(ping.getPos().x);
		int y = (int)Math.floor(ping.getPos().y);
		int z = (int)Math.floor(ping.getPos().z);
		String coordsStr = x + " " + y + " " + z;

		try {
			Game.keyboardHandler.setClipboard(coordsStr);
		} catch (Throwable t) {
			LOGGER.error("Failed to copy coordinates to clipboard", t);
		}

		if (Game.player != null) {
			Game.player.sendOverlayMessage(
				Component.literal("§a").append(Component.translatable("pingwheel.coords_copied", "§6" + coordsStr))
			);
		}

		try {
			Game.getSoundManager().play(
				SimpleSoundInstance.forUI(
					SoundEvents.UI_BUTTON_CLICK,
					1.3f
				)
			);
		} catch (Throwable ignored) {}
	}
}
