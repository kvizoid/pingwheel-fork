package nx.pingwheel.common.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.phys.Vec3;
import nx.pingwheel.common.CommonClient;
import nx.pingwheel.common.Global;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static nx.pingwheel.common.CommonClient.Game;

public class RelayClient implements WebSocket.Listener {

	public static final RelayClient INSTANCE = new RelayClient();

	private static final String DEFAULT_RELAY_URL = "ws://relay.bubuntu.tech:8899";

	public static String getRelayUrl() {
		try {
			var configDir = java.nio.file.Paths.get("config");
			var configFile = configDir.resolve("ping-relay.txt");
			if (java.nio.file.Files.exists(configFile)) {
				var url = java.nio.file.Files.readString(configFile).trim();
				if (!url.isEmpty()) {
					return url;
				}
			} else {
				java.nio.file.Files.createDirectories(configDir);
				java.nio.file.Files.writeString(configFile, DEFAULT_RELAY_URL + System.lineSeparator());
			}
		} catch (Throwable ignored) {}
		return DEFAULT_RELAY_URL;
	}

	private final HttpClient httpClient = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.build();

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		var thread = new Thread(r, "PingWheel-RelayThread");
		thread.setDaemon(true);
		return thread;
	});

	private WebSocket webSocket;
	private String userChannel = "";
	private String lastSubscribedChannel = "";
	private boolean active = false;
	private final StringBuilder messageBuffer = new StringBuilder();

	private RelayClient() {}

	public void init() {
		active = true;
		connect();
	}

	public synchronized void connect() {
		if (!active || (webSocket != null && !webSocket.isInputClosed() && !webSocket.isOutputClosed())) {
			return;
		}

		try {
			var url = getRelayUrl();
			var uri = URI.create(url);
			httpClient.newWebSocketBuilder()
				.connectTimeout(Duration.ofSeconds(5))
				.buildAsync(uri, this)
				.whenComplete((ws, throwable) -> {
					if (throwable != null) {
						scheduleReconnect();
					} else {
						synchronized (this) {
							this.webSocket = ws;
						}
						Global.LOGGER.info("[PingWheel] Connected to Relay (" + url + ")");
						resubscribe();
					}
				});
		} catch (Exception e) {
			scheduleReconnect();
		}
	}

	private void scheduleReconnect() {
		if (!active) return;
		scheduler.schedule(this::connect, 5, TimeUnit.SECONDS);
	}

	public static String getServerIdentifier() {
		if (Game == null || Game.level == null) return null;
		if (Game.hasSingleplayerServer()) return null;

		var connection = Game.getConnection();
		if (connection == null) return null;

		try {
			var netConn = connection.getConnection();
			if (netConn != null && netConn.getRemoteAddress() instanceof java.net.InetSocketAddress isa) {
				var addr = isa.getAddress();
				String host = addr != null ? addr.getHostAddress() : isa.getHostString();
				return host + ":" + isa.getPort();
			}
		} catch (Throwable ignored) {}

		try {
			var serverData = Game.getCurrentServer();
			if (serverData != null && serverData.ip != null) {
				return serverData.ip.trim().toLowerCase();
			}
		} catch (Throwable ignored) {}

		return null;
	}

	public String getEffectiveChannel() {
		String serverId = getServerIdentifier();
		if (serverId == null) {
			return "__offline__";
		}
		return (userChannel != null ? userChannel : "") + "@" + serverId;
	}

	public synchronized void updateChannel(String channel) {
		this.userChannel = channel != null ? channel : "";
		resubscribe();
	}

	public synchronized void onLeave() {
		resubscribe();
	}

	public void checkServerState() {
		String target = getEffectiveChannel();
		if (!target.equals(this.lastSubscribedChannel)) {
			resubscribe();
		}
	}

	public synchronized void resubscribe() {
		String target = getEffectiveChannel();
		this.lastSubscribedChannel = target;
		sendSubscribe(target);
	}

	public void sendPing(PingLocationC2SPacket packet, UUID author) {
		String serverId = getServerIdentifier();
		if (serverId == null || webSocket == null || packet == null) {
			return;
		}

		String effChannel = getEffectiveChannel();
		var json = new JsonObject();
		json.addProperty("type", "ping");
		json.addProperty("channel", effChannel);
		json.addProperty("server", serverId);
		json.addProperty("x", packet.pos().x);
		json.addProperty("y", packet.pos().y);
		json.addProperty("z", packet.pos().z);
		if (packet.entity() != null) {
			json.addProperty("entity", packet.entity().toString());
		}
		json.addProperty("seq", packet.sequence());
		json.addProperty("dim", packet.dimension());
		json.addProperty("author", author != null ? author.toString() : UUID.randomUUID().toString());

		String raw = json.toString();
		synchronized (this) {
			if (webSocket != null) {
				webSocket.sendText(raw, true);
			}
		}
	}

	private synchronized void sendSubscribe(String channel) {
		if (webSocket == null) return;
		var json = new JsonObject();
		json.addProperty("type", "subscribe");
		json.addProperty("channel", channel != null ? channel : "");
		webSocket.sendText(json.toString(), true);
	}

	@Override
	public void onOpen(WebSocket webSocket) {
		webSocket.request(1);
	}

	@Override
	public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
		messageBuffer.append(data);
		if (last) {
			String fullMessage = messageBuffer.toString();
			messageBuffer.setLength(0);
			handleIncomingMessage(fullMessage);
		}
		webSocket.request(1);
		return CompletableFuture.completedFuture(null);
	}

	private void handleIncomingMessage(String rawJson) {
		try {
			var element = JsonParser.parseString(rawJson);
			if (!element.isJsonObject()) return;
			var obj = element.getAsJsonObject();

			String type = obj.has("type") ? obj.get("type").getAsString() : "";
			if (!"ping".equals(type)) return;

			String currentServer = getServerIdentifier();
			if (currentServer == null) {
				return;
			}

			// Don't replay our own ping if we already rendered it
			UUID author = null;
			if (obj.has("author") && !obj.get("author").isJsonNull()) {
				author = UUID.fromString(obj.get("author").getAsString());
			} else {
				author = UUID.randomUUID();
			}

			if (Game.player != null && author.equals(Game.player.getUUID())) {
				return;
			}

			// Verify server: check cross-server sync, exact server match, synced server list, or online players list
			boolean sameServer = false;
			var clientConfig = nx.pingwheel.common.config.ClientConfig.HANDLER.getConfig();

			if (clientConfig.isCrossServerSync()) {
				sameServer = true;
			}

			if (!sameServer && obj.has("server")) {
				String pingServer = obj.get("server").getAsString();
				if (currentServer.equalsIgnoreCase(pingServer) || clientConfig.isServerAllowed(pingServer, currentServer)) {
					sameServer = true;
				}
			}

			var conn = Game.getConnection();
			if (!sameServer && conn != null) {
				if (conn.getPlayerInfo(author) != null) {
					sameServer = true;
				}
			}

			if (!sameServer) {
				return;
			}

			double x = obj.get("x").getAsDouble();
			double y = obj.get("y").getAsDouble();
			double z = obj.get("z").getAsDouble();

			UUID entity = null;
			if (obj.has("entity") && !obj.get("entity").isJsonNull()) {
				entity = UUID.fromString(obj.get("entity").getAsString());
			}

			int seq = obj.has("seq") ? obj.get("seq").getAsInt() : 0;
			int dim = obj.has("dim") ? obj.get("dim").getAsInt() : 0;

			var s2c = new PingLocationS2CPacket(userChannel, new Vec3(x, y, z), entity, seq, dim, author);

			if (Game != null) {
				Game.execute(() -> CommonClient.INSTANCE.onPingLocationPacket(s2c));
			}
		} catch (Exception e) {
			Global.LOGGER.warn("[PingWheel] Failed to parse relayed ping: " + e.getMessage());
		}
	}

	@Override
	public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
		synchronized (this) {
			this.webSocket = null;
		}
		scheduleReconnect();
		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void onError(WebSocket webSocket, Throwable error) {
		synchronized (this) {
			this.webSocket = null;
		}
		scheduleReconnect();
	}
}
