package nx.pingwheel.common.core;

import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.integration.ModContext;
import nx.pingwheel.common.math.Raycast;
import nx.pingwheel.common.network.PingLocationC2SPacket;
import nx.pingwheel.common.platform.IPlatformNetworkService;

import java.util.UUID;

import static nx.pingwheel.common.CommonClient.Game;
import static nx.pingwheel.common.config.ClientConfig.MAX_CORRECTION_PERIOD;
import static nx.pingwheel.common.config.ClientConfig.TPS;

public class PingController {
	private PingController() {}

	private static final ClientConfig CLIENT_CONFIG = ClientConfig.HANDLER.getConfig();

	private static boolean pingQueued = false;
	private static boolean selfPingQueued = false;
	private static int pingSequence = 0;
	private static int lastPing = 0;

	public static boolean isPingQueued() {
		return pingQueued || selfPingQueued;
	}

	public static void revokePingAction() {
		pingQueued = false;
		selfPingQueued = false;
	}

	public static void queuePingAction() {
		pingQueued = true;
	}

	public static void queueSelfPingAction() {
		selfPingQueued = true;
	}

	public static void pollPingAction(float tickDelta) {
		if ((!pingQueued && !selfPingQueued) || Game.level == null) {
			return;
		}

		var time = (int)Game.level.getGameTime();

		++pingSequence;

		lastPing = time;
		boolean forceSelf = selfPingQueued;
		pingQueued = false;
		selfPingQueued = false;
		performPingAction(tickDelta, forceSelf);
	}

	private static void performPingAction(float tickDelta, boolean forceSelf) {
		var cameraEntity = Game.getCameraEntity();

		if (cameraEntity == null || Game.level == null) {
			return;
		}

		// 1. Dedicated self-ping hotkey OR Shift + looking down
		if (forceSelf || (cameraEntity.isCrouching() && cameraEntity.getXRot() > 50f)) {
			doSelfPing(cameraEntity);
			return;
		}

		var cameraDirection = cameraEntity.getViewVector(tickDelta);
		var hitResult = Raycast.traceDirectional(
			cameraDirection,
			tickDelta,
			Math.min(CLIENT_CONFIG.getRaycastDistance(), CLIENT_CONFIG.getPingDistance()),
			cameraEntity.isCrouching());

		if (hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
			if (ModContext.HasDistantHorizons) {
				Raycast.traceDistantAsync(cameraDirection, tickDelta, (distantHitResult) -> {
					IPlatformNetworkService.INSTANCE.sendToServer(new PingLocationC2SPacket(CLIENT_CONFIG.getChannel(), distantHitResult.getLocation(), null, pingSequence, GameContext.getDimension()));
				});
			}

			return;
		}

		UUID uuid = null;

		if (hitResult.getType() == HitResult.Type.ENTITY) {
			uuid = ((EntityHitResult)hitResult).getEntity().getUUID();
		}

		IPlatformNetworkService.INSTANCE.sendToServer(new PingLocationC2SPacket(CLIENT_CONFIG.getChannel(), hitResult.getLocation(), uuid, pingSequence, GameContext.getDimension()));
	}

	private static void doSelfPing(net.minecraft.world.entity.Entity cameraEntity) {
		var selfPos = cameraEntity.position().add(0, cameraEntity.getBbHeight() * 0.8, 0);
		IPlatformNetworkService.INSTANCE.sendToServer(
			new PingLocationC2SPacket(CLIENT_CONFIG.getChannel(), selfPos, cameraEntity.getUUID(), pingSequence, GameContext.getDimension())
		);
	}
}
