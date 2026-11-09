package nx.pingwheel.common.core;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.platform.IPlatformNetworkService;
import nx.pingwheel.common.network.PingLocationC2SPacket;

import java.util.UUID;

import static nx.pingwheel.common.CommonClient.Game;

public class DropPingManager {
	private DropPingManager() {}

	private static volatile long pendingTime = 0;
	private static volatile UUID pendingPlayer = null;
	private static volatile ItemStack pendingStack = ItemStack.EMPTY;

	public static void markPending(LocalPlayer player, ItemStack stack) {
		if (player == null || stack == null || stack.isEmpty()) return;
		pendingTime = System.currentTimeMillis();
		pendingPlayer = player.getUUID();
		pendingStack = stack.copy();
	}

	public static void onEntityAdded(Entity entity) {
		if (!(entity instanceof ItemEntity itemEntity)) return;

		var config = ClientConfig.HANDLER.getConfig();
		if (!config.isAutoDropPing()) return;

		long now = System.currentTimeMillis();
		if (now - pendingTime > 1500) return;

		if (Game == null || Game.player == null) return;
		if (pendingPlayer == null || !pendingPlayer.equals(Game.player.getUUID())) return;

		double dist = itemEntity.distanceTo(Game.player);
		if (dist > 3.5) return;

		pendingTime = 0;
		pendingPlayer = null;

		int seq = 1;
		var pos = itemEntity.position().add(0, 0.3, 0);
		var packet = new PingLocationC2SPacket(config.getChannel(), pos, itemEntity.getUUID(), seq, GameContext.getDimension());
		IPlatformNetworkService.INSTANCE.sendToServer(packet);
	}
}
