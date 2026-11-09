package nx.pingwheel.common;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import nx.pingwheel.common.compat.LegacyMigrationHandler;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.core.GameContext;
import nx.pingwheel.common.core.PingController;
import nx.pingwheel.common.core.PingManager;
import nx.pingwheel.common.network.PingLocationS2CPacket;
import nx.pingwheel.common.network.UpdateChannelC2SPacket;
import nx.pingwheel.common.platform.IPlatformClientEventService;
import nx.pingwheel.common.platform.IPlatformContextService;
import nx.pingwheel.common.platform.IPlatformNetworkService;
import nx.pingwheel.common.render.OverlayRenderer;
import nx.pingwheel.common.render.WorldRenderContext;
import nx.pingwheel.common.screen.SettingsScreen;
import nx.pingwheel.common.util.InputUtils;

import static nx.pingwheel.common.util.InputUtils.KEY_BINDING_PING;
import static nx.pingwheel.common.util.InputUtils.KEY_BINDING_SETTINGS;

public class CommonClient {

	public static final CommonClient INSTANCE = new CommonClient();
	public static Minecraft Game = null;

	private CommonClient() {}

	public void onInit() {
		ClientConfig.HANDLER.load();

		IPlatformClientEventService.INSTANCE.registerTickStartEvent(this::onTickStart);
		IPlatformClientEventService.INSTANCE.registerJoinServerEvent(this::onJoinServer);
		IPlatformClientEventService.INSTANCE.registerLeaveServerEvent(this::onLeaveServer);

		IPlatformContextService.INSTANCE.registerKeyMapping(KEY_BINDING_PING);
		IPlatformContextService.INSTANCE.registerKeyMapping(KEY_BINDING_SETTINGS);
		IPlatformContextService.INSTANCE.registerKeyMapping(InputUtils.KEY_BINDING_PING_SELF);

		LegacyMigrationHandler.migrateKeyMappings();

		nx.pingwheel.common.network.RelayClient.INSTANCE.init();
		nx.pingwheel.common.updater.UpdateManager.checkOnStartup();
	}

	public void onJoinServer() {
		String ch = ClientConfig.HANDLER.getConfig().getChannel();
		nx.pingwheel.common.network.RelayClient.INSTANCE.updateChannel(ch);
		IPlatformNetworkService.INSTANCE.sendToServer(new UpdateChannelC2SPacket(ch));
	}

	public void onLeaveServer() {
		PingManager.clearPings();
		nx.pingwheel.common.network.RelayClient.INSTANCE.onLeave();
	}

	private int checkServerStateCooldown = 0;

	public void onTickStart() {
		Game = Minecraft.getInstance();
		GameContext.updateDimension();

		LegacyMigrationHandler.onTick();
		if (--checkServerStateCooldown <= 0) {
			checkServerStateCooldown = 20;
			nx.pingwheel.common.network.RelayClient.INSTANCE.checkServerState();
		}

		if (InputUtils.consumePingHotkey()) {
			PingController.queuePingAction();
		}

		if (InputUtils.consumePingSelfHotkey()) {
			PingController.queueSelfPingAction();
		}

		if (KEY_BINDING_SETTINGS.consumeClick()) {
			Game.gui.setScreen(new SettingsScreen());
		}

		if (Game.gui != null && Game.gui.screen() == null) {
			var hovered = PingManager.getHoveredPing();
			if (hovered != null && Game.options.keyUse.consumeClick()) {
				PingManager.copyPingCoordinates(hovered);
			}
		}
	}

	public void onRenderWorld(WorldRenderContext ctx) {
		PingManager.updatePings(ctx);
		PingController.pollPingAction(ctx.tickDelta);
	}

	public void onRenderGUI(GuiGraphicsExtractor guiGraphics, float tickDelta) {
		OverlayRenderer.draw(guiGraphics, tickDelta);
	}

	public void onPingLocationPacket(PingLocationS2CPacket packet) {
		PingManager.acceptPingPacket(packet);
	}
}
