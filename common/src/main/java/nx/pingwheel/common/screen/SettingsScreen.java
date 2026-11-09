package nx.pingwheel.common.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.config.PlayerInfoMode;
import nx.pingwheel.common.config.TeamColorMode;
import nx.pingwheel.common.resource.LanguageUtils;
import nx.pingwheel.common.updater.UpdateManager;

import java.util.ArrayList;
import java.util.List;

import static nx.pingwheel.common.CommonClient.Game;
import static nx.pingwheel.common.config.ClientConfig.*;

public class SettingsScreen extends Screen {

	private static final int WHITE = 0xFFFFFFFF;
	private static final int GRAY = 0xFFA0A0A0;

	private final ClientConfig config;
	private final Screen parent;
	private EditBox channelTextField;
	private int currentTab = 0;
	private final Button[] tabButtons = new Button[4];
	private final List<AbstractWidget> currentTabWidgets = new ArrayList<>();
	private Button doneButton;

	public SettingsScreen() {
		this(null);
	}

	public SettingsScreen(Screen parent) {
		super(LanguageUtils.settings("title").get());
		this.parent = parent;
		this.config = ClientConfig.HANDLER.getConfig();
	}

	@Override
	public void tick() {
		if (this.channelTextField != null && this.channelTextField.isFocused() && this.getFocused() != this.channelTextField) {
			this.setFocused(this.channelTextField);
		}
	}

	@Override
	protected void init() {
		this.clearWidgets();
		this.currentTabWidgets.clear();

		int panelWidth = 380;
		int panelHeight = 240;
		int panelLeft = (this.width - panelWidth) / 2;
		int panelTop = (this.height - panelHeight) / 2;

		int tabW = (panelWidth - 20) / 4;
		for (int i = 0; i < 4; i++) {
			final int tabIndex = i;
			this.tabButtons[i] = Button.builder(getTabTitle(i), btn -> switchTab(tabIndex))
				.bounds(panelLeft + 10 + i * tabW, panelTop + 24, tabW, 20)
				.build();
			this.addRenderableWidget(this.tabButtons[i]);
		}

		this.doneButton = Button.builder(CommonComponents.GUI_DONE, btn -> this.onClose())
			.bounds(panelLeft + (panelWidth - 120) / 2, panelTop + panelHeight - 26, 120, 20)
			.build();
		this.addRenderableWidget(this.doneButton);

		this.buildCurrentTabWidgets();
	}

	private void switchTab(int newTab) {
		if (this.channelTextField != null) {
			config.setChannel(this.channelTextField.getValue());
			ClientConfig.HANDLER.save();
		}

		this.currentTab = newTab;
		for (int i = 0; i < 4; i++) {
			this.tabButtons[i].setMessage(getTabTitle(i));
		}
		for (var widget : this.currentTabWidgets) {
			this.removeWidget(widget);
		}
		this.currentTabWidgets.clear();
		this.buildCurrentTabWidgets();
	}

	private Component getTabTitle(int index) {
		String title = switch (index) {
			case 0 -> "Общие";
			case 1 -> "Визуал";
			case 2 -> "Утилиты";
			case 3 -> "Сеть";
			default -> "";
		};
		if (this.currentTab == index) {
			return Component.literal(title).withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD);
		} else {
			return Component.literal(title).withStyle(ChatFormatting.GRAY);
		}
	}

	private void addTabWidget(AbstractWidget widget) {
		this.currentTabWidgets.add(widget);
		this.addRenderableWidget(widget);
	}

	private void buildCurrentTabWidgets() {
		if (this.minecraft == null) return;

		int panelWidth = 380;
		int panelHeight = 240;
		int panelLeft = (this.width - panelWidth) / 2;
		int panelTop = (this.height - panelHeight) / 2;

		int col1X = panelLeft + 15;
		int col2X = panelLeft + 200;
		int colWidth = 165;
		int row1Y = panelTop + 54;
		int row2Y = row1Y + 24;
		int row3Y = row2Y + 24;
		int row4Y = row3Y + 24;

		if (currentTab == 0) {
			addTabWidget(getPingVolumeOption().createButton(this.minecraft.options, col1X, row1Y, colWidth));
			addTabWidget(getPingDurationOption().createButton(this.minecraft.options, col2X, row1Y, colWidth));

			addTabWidget(getPingDistanceOption().createButton(this.minecraft.options, col1X, row2Y, colWidth));
			addTabWidget(getCorrectionPeriodOption().createButton(this.minecraft.options, col2X, row2Y, colWidth));
		} else if (currentTab == 1) {
			addTabWidget(getPingSizeOption().createButton(this.minecraft.options, col1X, row1Y, colWidth));
			addTabWidget(getDetailDistanceOption().createButton(this.minecraft.options, col2X, row1Y, colWidth));

			addTabWidget(getMaxSimultaneousPingsOption().createButton(this.minecraft.options, col1X, row2Y, colWidth));
			addTabWidget(getCoordinatesVisibleOption().createButton(this.minecraft.options, col2X, row2Y, colWidth));

			addTabWidget(getItemIconsVisibleOption().createButton(this.minecraft.options, col1X, row3Y, colWidth));
			addTabWidget(getDirectionIndicatorVisibleOption().createButton(this.minecraft.options, col2X, row3Y, colWidth));

			addTabWidget(getPlayerInfoModeOption().createButton(this.minecraft.options, col1X, row4Y, colWidth));
			addTabWidget(getTeamColorModeOption().createButton(this.minecraft.options, col2X, row4Y, colWidth));
		} else if (currentTab == 2) {
			addTabWidget(getRulerVisibleOption().createButton(this.minecraft.options, col1X, row1Y, colWidth));
			addTabWidget(getMobRadiusVisibleOption().createButton(this.minecraft.options, col2X, row1Y, colWidth));
		} else if (currentTab == 3) {
			if (this.channelTextField == null) {
				this.channelTextField = new EditBox(this.font, col1X, panelTop + 68, 350, 20, Component.empty());
				this.channelTextField.setMaxLength(MAX_CHANNEL_LENGTH);
				this.channelTextField.setHint(Component.literal("Общий канал (по умолчанию)").withStyle(ChatFormatting.DARK_GRAY));
				this.channelTextField.setValue(config.getChannel());
				this.channelTextField.setResponder(text -> {
					config.setChannel(text);
					ClientConfig.HANDLER.save();
				});
			} else {
				this.channelTextField.setPosition(col1X, panelTop + 68);
			}
			addTabWidget(this.channelTextField);

			var syncServersBtn = Button.builder(
				Component.translatable("settings.pingwheel.server_sync"),
				btn -> {
					if (this.minecraft != null) {
						this.minecraft.gui.setScreen(new ServerSyncScreen(this));
					}
				}
			).bounds(col1X, panelTop + 96, colWidth, 20).build();
			addTabWidget(syncServersBtn);

			var updateBtn = Button.builder(
				LanguageUtils.settings("check_updates").get(),
				btn -> {
					btn.active = false;
					btn.setMessage(Component.translatable("settings.pingwheel.checking"));
					UpdateManager.checkForUpdates(false).whenComplete((updated, t) -> {
						if (this.minecraft != null) {
							this.minecraft.execute(() -> {
								btn.active = true;
								btn.setMessage(LanguageUtils.settings("check_updates").get());
							});
						}
					});
				}
			).bounds(col2X, panelTop + 96, colWidth, 20).build();
			addTabWidget(updateBtn);

			var preLaunchBtn = Button.builder(
				Component.literal("Автообновление на старте: " + (config.isPreLaunchUpdate() ? "Вкл" : "Выкл")),
				btn -> {
					config.setPreLaunchUpdate(!config.isPreLaunchUpdate());
					ClientConfig.HANDLER.save();
					btn.setMessage(Component.literal("Автообновление на старте: " + (config.isPreLaunchUpdate() ? "Вкл" : "Выкл")));
				}
			).bounds(col1X, panelTop + 122, 350, 20).build();
			addTabWidget(preLaunchBtn);
		}
	}

	@Override
	public void onClose() {
		if (this.channelTextField != null) {
			config.setChannel(this.channelTextField.getValue());
		}
		ClientConfig.HANDLER.save();

		if (parent != null && this.minecraft != null) {
			this.minecraft.gui.setScreen(parent);
		} else {
			super.onClose();
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor ctx, int mouseX, int mouseY, float delta) {
		this.extractTransparentBackground(ctx);

		int panelWidth = 380;
		int panelHeight = 240;
		int panelLeft = (this.width - panelWidth) / 2;
		int panelTop = (this.height - panelHeight) / 2;

		ctx.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xE810131B);

		ctx.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + 1, 0xFF353C4E);
		ctx.fill(panelLeft, panelTop + panelHeight - 1, panelLeft + panelWidth, panelTop + panelHeight, 0xFF353C4E);
		ctx.fill(panelLeft, panelTop, panelLeft + 1, panelTop + panelHeight, 0xFF353C4E);
		ctx.fill(panelLeft + panelWidth - 1, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF353C4E);

		ctx.fill(panelLeft + 1, panelTop + 1, panelLeft + panelWidth - 1, panelTop + 3, 0xFF7F63AE);

		var titleComponent = Component.literal("Ping Wheel").withStyle(ChatFormatting.BOLD);
		ctx.text(this.font, titleComponent, panelLeft + 14, panelTop + 8, WHITE, false);

		int titleW = this.font.width(titleComponent);
		int pillX = panelLeft + 14 + titleW + 12;
		int pillY = panelTop + 7;
		int pillW = this.font.width("v1.1.6") + 8;
		int pillH = 11;
		ctx.fill(pillX, pillY, pillX + pillW, pillY + pillH, 0x407F63AE);
		ctx.text(this.font, Component.literal("v1.1.6"), pillX + 4, pillY + 2, 0xFFD1C4E9, false);

		int tabW = (panelWidth - 20) / 4;
		int activeTabX = panelLeft + 10 + currentTab * tabW;
		int activeTabY = panelTop + 45;
		ctx.fill(activeTabX + 2, activeTabY, activeTabX + tabW - 2, activeTabY + 2, 0xFF9D7FE0);

		if (currentTab == 0) {
			int cardX = panelLeft + 15;
			int cardY = panelTop + 108;
			int cardW = 350;
			int cardH = 50;
			ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0x30000000);
			ctx.text(this.font, Component.literal("• Нажмите клавишу пинга для установки метки"), cardX + 8, cardY + 8, 0xFFCBD5E1, false);
			ctx.text(this.font, Component.literal("• Зажмите Shift + взгляд вниз для пинга координат"), cardX + 8, cardY + 22, 0xFF94A3B8, false);
			ctx.text(this.font, Component.literal("• Дальность меток ограничивает дальность на HUD"), cardX + 8, cardY + 36, 0xFF94A3B8, false);
		} else if (currentTab == 2) {
			int cardX = panelLeft + 15;
			int cardY = panelTop + 84;
			int cardW = 350;
			int cardH = 68;
			ctx.fill(cardX, cardY, cardX + cardW, cardY + cardH, 0x30000000);
			ctx.text(this.font, Component.literal("• 3D Линейка: зажмите Shift для замера расстояния"), cardX + 8, cardY + 8, 0xFFCBD5E1, false);
			ctx.text(this.font, Component.literal("• Shift + ПКМ по метке фиксирует линейку постоянно"), cardX + 8, cardY + 22, 0xFFCBD5E1, false);
			ctx.text(this.font, Component.literal("• Радиус мобов: 1px пунктирный круг угрозы"), cardX + 8, cardY + 36, 0xFF94A3B8, false);
			ctx.text(this.font, Component.literal("• Умная окклюзия: скрывается за стенами и блоками"), cardX + 8, cardY + 50, 0xFF94A3B8, false);
		} else if (currentTab == 3) {
			ctx.text(this.font, Component.translatable("settings.pingwheel.channel").withStyle(ChatFormatting.GRAY), panelLeft + 15, panelTop + 54, 0xFFA0AAB8, false);
			ctx.text(this.font, Component.literal("• Проверяет обновления до старта Minecraft").withStyle(ChatFormatting.DARK_GRAY), panelLeft + 15, panelTop + 148, 0xFF808A98, false);
		}

		super.extractRenderState(ctx, mouseX, mouseY, delta);
	}

	private OptionInstance<Integer> getPingVolumeOption() {
		final var text = LanguageUtils.settings("ping_volume");

		return OptionUtils.ofInt(
			text.getKey(),
			0, 100, 1,
			(value) -> {
				if (value == 0) {
					return text.get(CommonComponents.OPTION_OFF);
				}

				return text.get(LanguageUtils.UNIT_PERCENT.get(value));
			},
			config::getPingVolume,
			config::setPingVolume
		);
	}

	private OptionInstance<Integer> getPingDurationOption() {
		final var text = LanguageUtils.settings("ping_duration");

		return OptionUtils.ofInt(
			text.getKey(),
			1, MAX_PING_DURATION, 1,
			(value) -> {
				if (value >= MAX_PING_DURATION) {
					return text.get(LanguageUtils.VALUE_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_SECONDS.get(value));
			},
			config::getPingDuration,
			config::setPingDuration
		);
	}

	private OptionInstance<Integer> getPingDistanceOption() {
		final var text = LanguageUtils.settings("ping_distance");

		return OptionUtils.ofInt(
			text.getKey(),
			0, MAX_PING_DISTANCE, 16,
			(value) -> {
				if (value == 0) {
					return text.get(LanguageUtils.VALUE_HIDDEN);
				} else if (value >= MAX_PING_DISTANCE) {
					return text.get(LanguageUtils.VALUE_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_METERS.get(value));
			},
			config::getPingDistance,
			config::setPingDistance
		);
	}

	private OptionInstance<Float> getCorrectionPeriodOption() {
		final var text = LanguageUtils.settings("correction_period");

		return OptionUtils.ofFloat(
			text.getKey(),
			0.1f, MAX_CORRECTION_PERIOD, 0.1f,
			(value) -> {
				if (value >= MAX_CORRECTION_PERIOD) {
					return text.get(LanguageUtils.VALUE_INFINITE);
				}

				return text.get(LanguageUtils.UNIT_SECONDS.get("%.1f".formatted(value)));
			},
			config::getCorrectionPeriod,
			config::setCorrectionPeriod
		);
	}

	private OptionInstance<Boolean> getItemIconsVisibleOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("item_icon_visible").getKey(),
			config::isItemIconVisible,
			config::setItemIconVisible
		);
	}

	private OptionInstance<Boolean> getDirectionIndicatorVisibleOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("direction_indicator_visible").getKey(),
			config::isDirectionIndicatorVisible,
			config::setDirectionIndicatorVisible
		);
	}

	private OptionInstance<PlayerInfoMode> getPlayerInfoModeOption() {
		return OptionUtils.ofEnum(
			LanguageUtils.settings("player_info_mode").getKey(),
			PlayerInfoMode.class,
			(mode) -> LanguageUtils.of("value", mode.toString()).get(),
			(mode) -> {
				if (mode != PlayerInfoMode.HOLD) return Component.empty();

				final var kayPlayerListTitle = Component.translatable(Game.options.keyPlayerList.getName());
				final var kayPlayerListName = Game.options.keyPlayerList.getTranslatedKeyMessage();

				return LanguageUtils.settings("player_info_mode")
					.path("hold", "tooltip")
					.get(kayPlayerListTitle, kayPlayerListName);
			},
			config::getPlayerInfoMode,
			config::setPlayerInfoMode
		);
	}

	private OptionInstance<TeamColorMode> getTeamColorModeOption() {
		return OptionUtils.ofEnum(
			LanguageUtils.settings("team_color_mode").getKey(),
			TeamColorMode.class,
			(mode) -> LanguageUtils.of("value", mode.toString()).get(),
			(mode) -> Component.empty(),
			config::getTeamColorMode,
			config::setTeamColorMode
		);
	}

	private OptionInstance<Integer> getPingSizeOption() {
		final var text = LanguageUtils.settings("ping_size");

		return OptionUtils.ofInt(
			text.getKey(),
			40, 300, 10,
			(value) -> text.get(LanguageUtils.UNIT_PERCENT.get(value)),
			config::getPingSize,
			config::setPingSize
		);
	}

	private OptionInstance<Boolean> getCoordinatesVisibleOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("coordinates_visible").getKey(),
			config::isCoordinatesVisible,
			config::setCoordinatesVisible
		);
	}

	private OptionInstance<Boolean> getRulerVisibleOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("ruler_visible").getKey(),
			config::isRulerVisible,
			config::setRulerVisible
		);
	}

	private OptionInstance<Boolean> getMobRadiusVisibleOption() {
		return OptionUtils.ofBool(
			LanguageUtils.settings("mob_radius_visible").getKey(),
			config::isMobRadiusVisible,
			config::setMobRadiusVisible
		);
	}

	private OptionInstance<Integer> getDetailDistanceOption() {
		final var text = LanguageUtils.settings("detail_distance");

		return OptionUtils.ofInt(
			text.getKey(),
			50, 1000, 25,
			(value) -> text.get(LanguageUtils.UNIT_METERS.get(value)),
			config::getDetailDistance,
			config::setDetailDistance
		);
	}

	private OptionInstance<Integer> getMaxSimultaneousPingsOption() {
		final var text = LanguageUtils.settings("max_simultaneous_pings");

		return OptionUtils.ofInt(
			text.getKey(),
			1, 10, 1,
			(value) -> text.get(value),
			config::getMaxSimultaneousPings,
			config::setMaxSimultaneousPings
		);
	}
}
