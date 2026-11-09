package nx.pingwheel.common.config;

import nx.pingwheel.common.core.GameContext;
import nx.pingwheel.common.network.UpdateChannelC2SPacket;
import nx.pingwheel.common.platform.IPlatformNetworkService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nx.pingwheel.common.CommonClient.Game;

public class ClientConfig implements IConfig {
	int pingVolume = 100;
	int pingDuration = 7;
	int pingDistance = 2048;
	float correctionPeriod = 1f;
	boolean itemIconVisible = true;
	boolean directionIndicatorVisible = true;
	PlayerInfoMode playerInfoMode = PlayerInfoMode.HOLD;
	TeamColorMode teamColorMode = TeamColorMode.FULL;
	int pingSize = 100;
	String channel = "";
	Map<String, String> serverChannels = new HashMap<>();
	boolean crossServerSync = false;
	List<String> syncedServers = new ArrayList<>(List.of("play.k1nggl1nt.ru"));
	boolean coordinatesVisible = true;
	boolean rulerVisible = true;
	boolean mobRadiusVisible = true;
	boolean autoDropPing = true;
	int detailDistance = 200;
	int maxSimultaneousPings = 5;

	// hidden from the settings screen
	int removeRadius = 10;
	int raycastDistance = 1024;
	int safeZoneLeft = 5;
	int safeZoneRight = 5;
	int safeZoneTop = 5;
	int safeZoneBottom = 60;

	public static final int TPS = 20;
	public static final int MAX_PING_DURATION = 60;
	public static final int MAX_PING_DISTANCE = 2048;
	public static final float MAX_CORRECTION_PERIOD = 5f;
	public static final int MAX_CHANNEL_LENGTH = 128;

	public int getPingVolume() { return pingVolume; }
	public void setPingVolume(int pingVolume) { this.pingVolume = pingVolume; }

	public int getPingDuration() { return pingDuration; }
	public void setPingDuration(int pingDuration) { this.pingDuration = pingDuration; }

	public int getPingDistance() { return pingDistance; }
	public void setPingDistance(int pingDistance) { this.pingDistance = pingDistance; }

	public float getCorrectionPeriod() { return correctionPeriod; }
	public void setCorrectionPeriod(float correctionPeriod) { this.correctionPeriod = correctionPeriod; }

	public boolean isItemIconVisible() { return itemIconVisible; }
	public void setItemIconVisible(boolean itemIconVisible) { this.itemIconVisible = itemIconVisible; }

	public boolean isDirectionIndicatorVisible() { return directionIndicatorVisible; }
	public void setDirectionIndicatorVisible(boolean directionIndicatorVisible) { this.directionIndicatorVisible = directionIndicatorVisible; }

	public PlayerInfoMode getPlayerInfoMode() { return playerInfoMode; }
	public void setPlayerInfoMode(PlayerInfoMode playerInfoMode) { this.playerInfoMode = playerInfoMode; }

	public TeamColorMode getTeamColorMode() { return teamColorMode; }
	public void setTeamColorMode(TeamColorMode teamColorMode) { this.teamColorMode = teamColorMode; }

	public int getPingSize() { return pingSize; }
	public void setPingSize(int pingSize) { this.pingSize = pingSize; }

	public Map<String, String> getServerChannels() { return serverChannels; }
	public void setServerChannels(Map<String, String> serverChannels) { this.serverChannels = serverChannels; }

	public boolean isCrossServerSync() { return crossServerSync; }
	public void setCrossServerSync(boolean crossServerSync) { this.crossServerSync = crossServerSync; }

	public boolean isCoordinatesVisible() { return coordinatesVisible; }
	public void setCoordinatesVisible(boolean coordinatesVisible) { this.coordinatesVisible = coordinatesVisible; }

	public boolean isRulerVisible() { return rulerVisible; }
	public void setRulerVisible(boolean rulerVisible) { this.rulerVisible = rulerVisible; }

	public boolean isMobRadiusVisible() { return mobRadiusVisible; }
	public void setMobRadiusVisible(boolean mobRadiusVisible) { this.mobRadiusVisible = mobRadiusVisible; }

	public boolean isAutoDropPing() { return autoDropPing; }
	public void setAutoDropPing(boolean autoDropPing) { this.autoDropPing = autoDropPing; }

	public int getDetailDistance() { return detailDistance; }
	public void setDetailDistance(int detailDistance) { this.detailDistance = detailDistance; }

	public int getMaxSimultaneousPings() { return maxSimultaneousPings; }
	public void setMaxSimultaneousPings(int maxSimultaneousPings) { this.maxSimultaneousPings = maxSimultaneousPings; }

	public List<String> getSyncedServers() { return syncedServers; }
	public void setSyncedServers(List<String> syncedServers) { this.syncedServers = syncedServers; }

	public int getRemoveRadius() { return removeRadius; }
	public void setRemoveRadius(int removeRadius) { this.removeRadius = removeRadius; }

	public int getRaycastDistance() { return raycastDistance; }
	public void setRaycastDistance(int raycastDistance) { this.raycastDistance = raycastDistance; }

	public int getSafeZoneLeft() { return safeZoneLeft; }
	public void setSafeZoneLeft(int safeZoneLeft) { this.safeZoneLeft = safeZoneLeft; }

	public int getSafeZoneRight() { return safeZoneRight; }
	public void setSafeZoneRight(int safeZoneRight) { this.safeZoneRight = safeZoneRight; }

	public int getSafeZoneTop() { return safeZoneTop; }
	public void setSafeZoneTop(int safeZoneTop) { this.safeZoneTop = safeZoneTop; }

	public int getSafeZoneBottom() { return safeZoneBottom; }
	public void setSafeZoneBottom(int safeZoneBottom) { this.safeZoneBottom = safeZoneBottom; }

	public String getChannel() {
		final var ip = GameContext.getCurrentServerIp();

		if (ip.isPresent()) {
			String serverChan = serverChannels.get(ip.get());
			if (serverChan != null && !serverChan.isEmpty()) {
				return serverChan;
			}
		}

		return channel != null ? channel : "";
	}

	public void setChannel(String newChannel) {
		if (newChannel == null) newChannel = "";
		this.channel = newChannel;

		final var ip = GameContext.getCurrentServerIp();
		if (ip.isPresent()) {
			serverChannels.put(ip.get(), newChannel);
		}
	}

	public boolean isServerAllowed(String incomingServer, String currentServer) {
		if (crossServerSync) return true;
		if (incomingServer == null || currentServer == null) return false;
		String inc = incomingServer.trim().toLowerCase();
		String cur = currentServer.trim().toLowerCase();
		if (inc.equals(cur)) return true;

		if (syncedServers == null) return false;
		for (String s : syncedServers) {
			if (s == null) continue;
			String norm = s.trim().toLowerCase();
			if (norm.isEmpty()) continue;
			if (inc.equals(norm) || inc.contains(norm) || norm.contains(inc)) return true;
			if (cur.equals(norm) || cur.contains(norm) || norm.contains(cur)) return true;
		}
		return false;
	}

	@Override
	public void validate() {
		if (maxSimultaneousPings <= 0) {
			maxSimultaneousPings = 5;
		}

		if (detailDistance <= 0) {
			detailDistance = 200;
		}

		if (syncedServers == null) {
			syncedServers = new ArrayList<>(List.of("play.k1nggl1nt.ru"));
		}

		if (channel == null) {
			channel = "";
		} else if (channel.length() > MAX_CHANNEL_LENGTH) {
			channel = channel.substring(0, MAX_CHANNEL_LENGTH);
		}

		for (Map.Entry<String, String> entry : serverChannels.entrySet()) {
			final var ch = entry.getValue();

			if (ch != null && ch.length() > MAX_CHANNEL_LENGTH) {
				entry.setValue(ch.substring(0, MAX_CHANNEL_LENGTH));
			}
		}
	}

	@Override
	public void onUpdate() {
		String ch = getChannel();
		nx.pingwheel.common.network.RelayClient.INSTANCE.updateChannel(ch);

		if (Game != null) {
			IPlatformNetworkService.INSTANCE.sendToServer(new UpdateChannelC2SPacket(ch));
		}
	}

	public static final ConfigHandler<ClientConfig> HANDLER = ConfigHandler.of(ClientConfig.class, ".json");
}
