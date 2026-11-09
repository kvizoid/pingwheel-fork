package nx.pingwheel.fabric.platform;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import nx.pingwheel.common.CommonClient;
import nx.pingwheel.common.network.IPacket;
import nx.pingwheel.common.network.PingLocationC2SPacket;
import nx.pingwheel.common.network.PingLocationS2CPacket;
import nx.pingwheel.common.network.RelayClient;
import nx.pingwheel.common.network.UpdateChannelC2SPacket;
import nx.pingwheel.common.platform.IPlatformNetworkService;

import java.util.UUID;

import static nx.pingwheel.common.CommonClient.Game;

public class PlatformNetworkServiceImpl implements IPlatformNetworkService {

	@Override
	public void sendToServer(IPacket packet) {
		if (ClientPlayNetworking.canSend(packet.getId())) {
			ClientPlayNetworking.send(packet);
			return;
		}

		if (packet instanceof UpdateChannelC2SPacket update) {
			RelayClient.INSTANCE.updateChannel(update.channel());
			return;
		}

		if (packet instanceof PingLocationC2SPacket c2s) {
			UUID author = (Game != null && Game.player != null) ? Game.player.getUUID() : UUID.randomUUID();


			RelayClient.INSTANCE.sendPing(c2s, author);


			var s2c = PingLocationS2CPacket.fromClientPacket(c2s, author);
			if (Game != null) {
				Game.execute(() -> CommonClient.INSTANCE.onPingLocationPacket(s2c));
			}
		}
	}

	@Override
	public void sendToClient(IPacket packet, ServerPlayer player) {
		if (!ServerPlayNetworking.canSend(player, packet.getId())) {
			return;
		}

		ServerPlayNetworking.send(player, packet);
	}
}

