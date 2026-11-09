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
		super(parent, Game != null ? Game.options : null, Component.translatable("settings.pingwheel.server_sync.title").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD));
		this.config = ClientConfig.HANDLER.getConfig();
	}

	@Override
	protected void addOptions() {

		boolean isGlobal = config.isCrossServerSync();
		Component modeText = Component.translatable(isGlobal
			? "settings.pingwheel.server_sync.mode.global"
			: "settings.pingwheel.server_sync.mode.whitelist");

		Component tooltipText = Component.translatable(isGlobal
			? "settings.pingwheel.server_sync.mode.global.tooltip"
			: "settings.pingwheel.server_sync.mode.whitelist.tooltip");

		var toggleBtn = Button.builder(
			Component.translatable("settings.pingwheel.server_sync.mode", modeText),
			btn -> {
				config.setCrossServerSync(!config.isCrossServerSync());
				ClientConfig.HANDLER.save();
				this.rebuildWidgets();
			}
		).tooltip(Tooltip.create(tooltipText)).width(310).build();

		this.list.addBig(toggleBtn);


		this.list.addHeader(Component.translatable("settings.pingwheel.server_sync.add_header").withStyle(ChatFormatting.YELLOW));

		this.serverInput = new EditBox(this.font, 0, 0, 205, 20, Component.translatable("settings.pingwheel.server_sync.input_label"));
		this.serverInput.setHint(Component.translatable("settings.pingwheel.server_sync.input_hint").withStyle(ChatFormatting.DARK_GRAY));
		this.serverInput.setMaxLength(128);
		this.serverInput.setValue(inputDraft);
		this.serverInput.setResponder(text -> this.inputDraft = text);

		var addBtn = Button.builder(
			Component.translatable("settings.pingwheel.server_sync.add_button"),
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


		var currentIpOpt = GameContext.getCurrentServerIp();
		if (currentIpOpt.isPresent()) {
			String currentIp = currentIpOpt.get();
			boolean alreadyInList = config.getSyncedServers().contains(currentIp);

			if (!alreadyInList) {
				var addCurrentBtn = Button.builder(
					Component.translatable("settings.pingwheel.server_sync.add_current", currentIp),
					btn -> {
						config.getSyncedServers().add(currentIp);
						ClientConfig.HANDLER.save();
						this.rebuildWidgets();
					}
				).width(310).build();
				this.list.addBig(addCurrentBtn);
			}
		}


		List<String> servers = config.getSyncedServers();
		this.list.addHeader(Component.translatable("settings.pingwheel.server_sync.linked_header", servers.size()).withStyle(ChatFormatting.AQUA));

		if (servers.isEmpty()) {
			var emptyBtn = Button.builder(
				Component.translatable("settings.pingwheel.server_sync.empty_list"),
				btn -> {}
			).width(310).build();
			emptyBtn.active = false;
			this.list.addBig(emptyBtn);
		} else {
			String curIp = currentIpOpt.orElse("");
			for (String srv : new ArrayList<>(servers)) {
				boolean isCurrent = !curIp.isEmpty() && srv.equalsIgnoreCase(curIp);
				var display = Component.literal("🌐 " + srv);
				if (isCurrent) {
					display.append(Component.translatable("settings.pingwheel.server_sync.current_badge").withStyle(ChatFormatting.GREEN));
				}

				var infoBtn = Button.builder(
					display,
					btn -> {}
				).width(225).build();
				infoBtn.active = false;

				var delBtn = Button.builder(
					Component.translatable("settings.pingwheel.server_sync.delete_button"),
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

