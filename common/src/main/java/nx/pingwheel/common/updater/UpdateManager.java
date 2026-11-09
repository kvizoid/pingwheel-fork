package nx.pingwheel.common.updater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import nx.pingwheel.common.CommonClient;
import nx.pingwheel.common.Global;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static nx.pingwheel.common.CommonClient.Game;

public class UpdateManager {
	private UpdateManager() {}

	public static final int CURRENT_BUILD = 6;
	public static final String[] UPDATE_URLS = {
		"https://raw.githubusercontent.com/kvizoid/pingwheel-fork/main/version.json",
		"https://bubuntu.tech/pingwheel/version.json",
		"https://up.bubuntu.tech/pingwheel/version.json"
	};

	private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(6))
		.followRedirects(HttpClient.Redirect.NORMAL)
		.build();

	private static volatile boolean updateInProgress = false;
	private static volatile boolean updateAvailable = false;
	private static volatile String latestVersionName = "";

	public static boolean isUpdateAvailable() {
		return updateAvailable;
	}

	public static String getLatestVersionName() {
		return latestVersionName;
	}

	public static boolean isNewerVersion(String remote, String current) {
		if (remote == null || current == null) return false;
		remote = remote.trim().replaceAll("^[vV]", "");
		current = current.trim().replaceAll("^[vV]", "");
		if (remote.equalsIgnoreCase(current) || current.equalsIgnoreCase("unknown")) {
			return false;
		}

		String[] rParts = remote.split("[.-]");
		String[] cParts = current.split("[.-]");
		int maxLen = Math.max(rParts.length, cParts.length);
		for (int i = 0; i < maxLen; i++) {
			int rNum = 0;
			int cNum = 0;
			if (i < rParts.length) {
				try {
					rNum = Integer.parseInt(rParts[i].replaceAll("\\D", ""));
				} catch (Exception ignored) {}
			}
			if (i < cParts.length) {
				try {
					cNum = Integer.parseInt(cParts[i].replaceAll("\\D", ""));
				} catch (Exception ignored) {}
			}
			if (rNum > cNum) return true;
			if (rNum < cNum) return false;
		}
		return false;
	}

	public static void checkOnStartup() {
		CompletableFuture.runAsync(() -> {
			try {
				Thread.sleep(4000);
				checkForUpdates(true);
			} catch (Throwable ignored) {}
		});
	}

	public static CompletableFuture<Boolean> checkForUpdates(boolean silent) {
		if (updateInProgress) {
			return CompletableFuture.completedFuture(false);
		}

		updateInProgress = true;

		return CompletableFuture.supplyAsync(() -> {
			try {
				JsonObject json = null;
				for (String url : UPDATE_URLS) {
					try {
						var request = HttpRequest.newBuilder()
							.uri(URI.create(url))
							.timeout(Duration.ofSeconds(4))
							.header("User-Agent", "PingWheel-AutoUpdater")
							.header("Cache-Control", "no-cache")
							.GET()
							.build();

						var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
						if (response.statusCode() == 200) {
							json = JsonParser.parseString(response.body()).getAsJsonObject();
							break;
						}
					} catch (Exception ignored) {}
				}

				if (json == null) {
					if (!silent && Game != null && Game.player != null) {
						Game.player.sendOverlayMessage(Component.translatable("update.pingwheel.unavailable").withStyle(ChatFormatting.RED));
					}
					return false;
				}

				int remoteBuild = json.get("build").getAsInt();
				String remoteVersion = json.has("version") ? json.get("version").getAsString() : ("b" + remoteBuild);
				String downloadUrl = json.get("url").getAsString();

				boolean hasUpdate = isNewerVersion(remoteVersion, Global.MOD_VERSION);
				if (!hasUpdate && !remoteVersion.equalsIgnoreCase(Global.MOD_VERSION)) {
					if (remoteBuild > CURRENT_BUILD) {
						hasUpdate = true;
					}
				}

				if (hasUpdate) {
					updateAvailable = true;
					latestVersionName = remoteVersion;

					Global.LOGGER.info("[PingWheel] Found new update: v" + remoteVersion + " (build " + remoteBuild + ")");
					return downloadAndInstall(downloadUrl, remoteVersion, silent);
				} else {
					updateAvailable = false;
					if (!silent && Game != null && Game.player != null) {
						Game.player.sendOverlayMessage(Component.translatable("update.pingwheel.up_to_date").withStyle(ChatFormatting.GREEN));
					}
					return false;
				}
			} catch (Exception e) {
				Global.LOGGER.warn("[PingWheel] Update check failed: " + e.getMessage());
				if (!silent && Game != null && Game.player != null) {
					Game.player.sendOverlayMessage(Component.translatable("update.pingwheel.failed").withStyle(ChatFormatting.RED));
				}
				return false;
			} finally {
				updateInProgress = false;
			}
		});
	}

	private static boolean downloadAndInstall(String downloadUrl, String versionName, boolean silent) {
		try {
			var codeSource = CommonClient.class.getProtectionDomain().getCodeSource();
			if (codeSource == null || codeSource.getLocation() == null) {
				return false;
			}

			var currentJar = new File(codeSource.getLocation().toURI());
			File modsDir;
			if (currentJar.isFile() && currentJar.getName().toLowerCase().endsWith(".jar")) {
				modsDir = currentJar.getParentFile();
			} else {
				modsDir = new File("mods");
				if (!modsDir.exists()) modsDir.mkdirs();
				currentJar = new File(modsDir, "Ping-Wheel-" + versionName + "-fabric-26.3.jar");
			}

			File tempFile = new File(modsDir, "Ping-Wheel-update.tmp");
			File targetFile = currentJar.isFile() ? currentJar : new File(modsDir, "Ping-Wheel-" + versionName + "-fabric-26.3.jar");

			java.io.InputStream stream = null;
			String[] urls = {
				downloadUrl,
				downloadUrl.replace("://bubuntu.tech", "://up.bubuntu.tech"),
				"https://bubuntu.tech/pingwheel/Ping-Wheel-" + versionName + "-fabric-26.3.jar",
				"https://github.com/kvizoid/pingwheel-fork/releases/download/v" + versionName + "/Ping-Wheel-" + versionName + "-fabric-26.3.jar",
				"https://github.com/kvizoid/pingwheel-fork/releases/latest/download/Ping-Wheel-" + versionName + "-fabric-26.3.jar"
			};
			for (String u : urls) {
				try {
					var req = HttpRequest.newBuilder()
						.uri(URI.create(u))
						.timeout(Duration.ofSeconds(20))
						.header("User-Agent", "PingWheel-AutoUpdater")
						.GET()
						.build();
					var resp = HTTP_CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
					if (resp.statusCode() == 200) {
						stream = resp.body();
						break;
					}
				} catch (Exception ignored) {}
			}

			if (stream == null) {
				Global.LOGGER.error("[PingWheel] Download failed from all mirrors");
				return false;
			}

			try (var in = stream; var out = new FileOutputStream(tempFile)) {
				in.transferTo(out);
			}

			boolean replaced = false;
			try {
				Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
				replaced = true;
			} catch (Exception e) {
				try {
					Files.move(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					replaced = true;
				} catch (Exception ex) {
					File newJar = new File(modsDir, "Ping-Wheel-" + versionName + ".jar");
					if (tempFile.renameTo(newJar)) {
						currentJar.deleteOnExit();
						replaced = true;
					}
				}
			}

			if (replaced) {
				if (currentJar.isFile() && !currentJar.getAbsolutePath().equals(targetFile.getAbsolutePath())) {
					try {
						currentJar.delete();
					} catch (Exception ignored) {}
				}
				Global.LOGGER.info("[PingWheel] Successfully auto-updated to v" + versionName);
				if (Game != null && Game.player != null) {
					Game.player.sendOverlayMessage(
						Component.translatable("update.pingwheel.success", versionName).withStyle(ChatFormatting.GREEN)
					);
				}
				return true;
			}
		} catch (Exception e) {
			Global.LOGGER.error("[PingWheel] Failed to install update: " + e.getMessage());
		}
		return false;
	}

	public static void runPreLaunchCheck() {
		try {
			var noUpdateFile = new File("config/pingwheel-no-update.txt");
			if (noUpdateFile.exists()) {
				return;
			}

			JsonObject json = null;
			for (String url : UPDATE_URLS) {
				try {
					var request = HttpRequest.newBuilder()
						.uri(URI.create(url))
						.timeout(Duration.ofMillis(2500))
						.header("User-Agent", "PingWheel-AutoUpdater")
						.header("Cache-Control", "no-cache")
						.GET()
						.build();

					var response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
					if (response.statusCode() == 200) {
						json = JsonParser.parseString(response.body()).getAsJsonObject();
						break;
					}
				} catch (Exception ignored) {}
			}

			if (json == null) {
				return;
			}

			int remoteBuild = json.get("build").getAsInt();
			String remoteVersion = json.has("version") ? json.get("version").getAsString() : ("b" + remoteBuild);
			String downloadUrl = json.get("url").getAsString();

			boolean hasUpdate = isNewerVersion(remoteVersion, Global.MOD_VERSION);
			if (!hasUpdate && !remoteVersion.equalsIgnoreCase(Global.MOD_VERSION)) {
				if (remoteBuild > CURRENT_BUILD) {
					hasUpdate = true;
				}
			}

			if (!hasUpdate) {
				return;
			}

			Global.LOGGER.info("[PingWheel] Found new version during preLaunch: v" + remoteVersion + " (installed: v" + Global.MOD_VERSION + ")");
			Global.LOGGER.info("[PingWheel] Downloading update...");

			boolean installed = downloadAndInstall(downloadUrl, remoteVersion, true);
			if (installed) {
				System.err.println("");
				System.err.println("================================================================================");
				System.err.println("[PingWheel] МОД АВТОМАТИЧЕСКИ ОБНОВЛЕН ДО v" + remoteVersion + "!");
				System.err.println("[PingWheel] Файл мода в папке mods/ успешно заменен на актуальную версию.");
				System.err.println("[PingWheel] Minecraft был завершен, чтобы новая версия вступила в силу.");
				System.err.println("[PingWheel] Нажмите «Играть» еще раз в лаунчере для запуска игры с обновленным модом.");
				System.err.println("================================================================================");
				System.err.println("");
				System.exit(0);
			}
		} catch (Throwable t) {
			Global.LOGGER.warn("[PingWheel] PreLaunch update check error: " + t.getMessage());
		}
	}
}

