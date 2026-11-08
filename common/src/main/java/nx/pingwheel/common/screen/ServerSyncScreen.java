package nx.pingwheel.common.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.core.GameContext;

import java.util.ArrayList;
import java.util.List;

import static nx.pingwheel.common.CommonClient.Game;

public class ServerSyncScreen extends OptionsSubScreen {

	private final ClientConfig config;
	private EditBox serverInput;
	private String inputDraft = "";

	public ServerSyncScreen(Screen parent) {
		super(parent, Game != null ? Game.options : null, Component.literal("Синхронизация серверов").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
		this.config = ClientConfig.HANDLER.getConfig();
	}

	@Override
	protected void addOptions() {
		// 1. Cross-Server Sync Mode Button
		boolean isGlobal = config.isCrossServerSync();
		String modeText = isGlobal
			? "§aГлобальный (Все серверы на канале)"
			: "§eПо списку (Только серверы из списка ниже)";

		String tooltipText = isGlobal
			? "Метки передаются между ЛЮБЫМИ серверами с одинаковым номером канала. В одиночной игре метки всегда отключены."
			: "Метки передаются только если сервер отправителя или получателя добавлен в список ниже.";

		var toggleBtn = Button.builder(
			Component.literal("Режим: " + modeText),
			btn -> {
				config.setCrossServerSync(!config.isCrossServerSync());
				ClientConfig.HANDLER.save();
				this.rebuildWidgets();
			}
		).tooltip(Tooltip.create(Component.literal(tooltipText))).width(310).build();

		this.list.addBig(toggleBtn);

		// 2. Add server section
		this.list.addHeader(Component.literal("Добавить сервер в синхронизацию").withStyle(ChatFormatting.YELLOW));

		this.serverInput = new EditBox(this.font, 0, 0, 205, 20, Component.literal("Адрес сервера"));
		this.serverInput.setHint(Component.literal("play.example.com или IP:порт").withStyle(ChatFormatting.DARK_GRAY));
		this.serverInput.setMaxLength(128);
		this.serverInput.setValue(inputDraft);
		this.serverInput.setResponder(text -> this.inputDraft = text);

		var addBtn = Button.builder(
			Component.literal("§a+ Добавить"),
			btn -> {
				String text = this.serverInput.getValue().trim();
				if (!text.isEmpty()) {
					if (!config.getSyncedServers().contains(text)) {
						config.getSyncedServers().add(text);
						ClientConfig.HANDLER.save();
					}
					this.inputDraft = "";
					this.rebuildWidgets();
				}
			}
		).width(95).build();

		this.list.addSmall(this.serverInput, addBtn);

		// 3. Quick-add current server button (if playing on a multiplayer server)
		var currentIpOpt = GameContext.getCurrentServerIp();
		if (currentIpOpt.isPresent()) {
			String currentIp = currentIpOpt.get();
			boolean alreadyInList = config.getSyncedServers().contains(currentIp);

			if (!alreadyInList) {
				var addCurrentBtn = Button.builder(
					Component.literal("§b+ Добавить текущий сервер: §f" + currentIp),
					btn -> {
						config.getSyncedServers().add(currentIp);
						ClientConfig.HANDLER.save();
						this.rebuildWidgets();
					}
				).width(310).build();
				this.list.addBig(addCurrentBtn);
			}
		}

		// 4. List of synced servers
		List<String> servers = config.getSyncedServers();
		this.list.addHeader(Component.literal("Связанные серверы (" + servers.size() + ")").withStyle(ChatFormatting.AQUA));

		if (servers.isEmpty()) {
			var emptyBtn = Button.builder(
				Component.literal("§7(Список пуст — добавьте адреса серверов выше)"),
				btn -> {}
			).width(310).build();
			emptyBtn.active = false;
			this.list.addBig(emptyBtn);
		} else {
			String curIp = currentIpOpt.orElse("");
			for (String srv : new ArrayList<>(servers)) {
				boolean isCurrent = !curIp.isEmpty() && srv.equalsIgnoreCase(curIp);
				String display = "🌐 " + srv + (isCurrent ? " §a(текущий)" : "");

				var infoBtn = Button.builder(
					Component.literal(display),
					btn -> {}
				).width(225).build();
				infoBtn.active = false;

				var delBtn = Button.builder(
					Component.literal("§c✕ Удалить"),
					btn -> {
						config.getSyncedServers().remove(srv);
						ClientConfig.HANDLER.save();
						this.rebuildWidgets();
					}
				).width(75).build();

				this.list.addSmall(infoBtn, delBtn);
			}
		}
	}

	@Override
	public void onClose() {
		ClientConfig.HANDLER.save();
		if (this.lastScreen != null && this.minecraft != null) {
			this.minecraft.gui.setScreen(this.lastScreen);
		} else {
			super.onClose();
		}
	}
}
