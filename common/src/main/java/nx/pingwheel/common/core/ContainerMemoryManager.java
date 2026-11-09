package nx.pingwheel.common.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.state.BlockState;
import nx.pingwheel.common.platform.IPlatformContextService;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static nx.pingwheel.common.CommonClient.Game;

public class ContainerMemoryManager {
	private ContainerMemoryManager() {}

	public record ItemEntry(Component name, int count) {}

	public record StoredItem(String name, int count) {}

	public record StoredContainer(
		long timestamp,
		List<StoredItem> topItems,
		int totalItems,
		int distinctItems,
		boolean empty
	) {}

	public record ContainerSnapshot(
		long timestamp,
		List<ItemEntry> topItems,
		int totalItems,
		int distinctItems,
		boolean empty
	) {
		public Component formatSummary() {
			if (empty) {
				return Component.literal("📦 ").withStyle(ChatFormatting.GOLD)
					.append(Component.translatable("pingwheel.container_empty").withStyle(ChatFormatting.GRAY));
			}

			MutableComponent result = Component.literal("📦 ").withStyle(ChatFormatting.GOLD);
			for (int i = 0; i < topItems.size(); i++) {
				var entry = topItems.get(i);
				if (i > 0) {
					result.append(Component.literal("  ").withStyle(ChatFormatting.DARK_GRAY));
				}
				result.append(Component.literal(entry.count() + "x ").withStyle(ChatFormatting.YELLOW))
					.append(entry.name().copy().withStyle(ChatFormatting.WHITE));
			}

			int remaining = distinctItems - topItems.size();
			if (remaining > 0) {
				result.append(Component.literal(" ")
					.append(Component.translatable("pingwheel.container_more", remaining).withStyle(ChatFormatting.GRAY)));
			}

			return result;
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<String, ContainerSnapshot> CACHE = new ConcurrentHashMap<>();
	private static final ExecutorService IO_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "PingWheel-ContainerIO");
		t.setDaemon(true);
		return t;
	});

	private static volatile BlockPos lastInteractedPos = null;
	private static volatile long lastInteractedTime = 0;
	private static boolean loaded = false;

	private static synchronized void ensureLoaded() {
		if (loaded) return;
		loaded = true;
		try {
			Path path = IPlatformContextService.INSTANCE.resolveConfigDir("pingwheel_containers.json");
			if (Files.exists(path)) {
				try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
					Type type = new TypeToken<Map<String, StoredContainer>>(){}.getType();
					Map<String, StoredContainer> data = GSON.fromJson(reader, type);
					if (data != null) {
						for (var entry : data.entrySet()) {
							var sc = entry.getValue();
							List<ItemEntry> topList = new ArrayList<>();
							if (sc.topItems() != null) {
								for (var it : sc.topItems()) {
									topList.add(new ItemEntry(Component.literal(it.name()), it.count()));
								}
							}
							CACHE.put(entry.getKey(), new ContainerSnapshot(
								sc.timestamp(),
								topList,
								sc.totalItems(),
								sc.distinctItems(),
								sc.empty()
							));
						}
					}
				}
			}
		} catch (Throwable ignored) {}
	}

	private static void saveToDiskAsync() {
		IO_EXECUTOR.submit(() -> {
			try {
				Path path = IPlatformContextService.INSTANCE.resolveConfigDir("pingwheel_containers.json");
				Map<String, StoredContainer> toSave = new HashMap<>();
				for (var entry : CACHE.entrySet()) {
					var cs = entry.getValue();
					List<StoredItem> items = new ArrayList<>();
					for (var ie : cs.topItems()) {
						items.add(new StoredItem(ie.name().getString(), ie.count()));
					}
					toSave.put(entry.getKey(), new StoredContainer(
						cs.timestamp(),
						items,
						cs.totalItems(),
						cs.distinctItems(),
						cs.empty()
					));
				}
				if (path.getParent() != null) {
					Files.createDirectories(path.getParent());
				}
				try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
					GSON.toJson(toSave, writer);
				}
			} catch (Throwable ignored) {}
		});
	}

	private static String makeKey(BlockPos pos) {
		if (pos == null) return null;
		String server = GameContext.getCurrentServerIp().orElse("singleplayer");
		int dim = GameContext.getDimension();
		return server + "#" + dim + "#" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
	}

	public static void recordInteraction(BlockPos pos) {
		if (pos != null) {
			lastInteractedPos = pos.immutable();
			lastInteractedTime = System.currentTimeMillis();
		}
	}

	public static BlockPos getLastInteractedPos() {
		if (System.currentTimeMillis() - lastInteractedTime < 4000) {
			return lastInteractedPos;
		}
		return null;
	}

	public static void saveSnapshot(BlockPos pos, NonNullList<Slot> slots, ClientLevel level) {
		if (pos == null || slots == null || slots.size() <= 36) {
			return;
		}

		ensureLoaded();

		int containerSlots = slots.size() - 36;
		Map<Item, Integer> counts = new LinkedHashMap<>();
		Map<Item, Component> names = new HashMap<>();
		int total = 0;

		for (int i = 0; i < containerSlots; i++) {
			ItemStack stack = slots.get(i).getItem();
			if (!stack.isEmpty()) {
				Item item = stack.getItem();
				int count = stack.getCount();
				counts.merge(item, count, Integer::sum);
				names.putIfAbsent(item, stack.getHoverName());
				total += count;
			}
		}

		boolean isEmpty = counts.isEmpty();
		List<ItemEntry> topList = new ArrayList<>();

		if (!isEmpty) {
			counts.entrySet().stream()
				.sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
				.limit(3)
				.forEach(e -> topList.add(new ItemEntry(names.get(e.getKey()), e.getValue())));
		}

		var snapshot = new ContainerSnapshot(
			System.currentTimeMillis(),
			topList,
			total,
			counts.size(),
			isEmpty
		);

		String key = makeKey(pos);
		if (key != null) {
			CACHE.put(key, snapshot);
		}

		if (level != null) {
			try {
				BlockState state = level.getBlockState(pos);
				if (state.getBlock() instanceof ChestBlock && ChestBlock.getBlockType(state) != DoubleBlockCombiner.BlockType.SINGLE) {
					Direction dir = ChestBlock.getConnectedDirection(state);
					if (dir != null) {
						String secondKey = makeKey(pos.relative(dir));
						if (secondKey != null) {
							CACHE.put(secondKey, snapshot);
						}
					}
				}
			} catch (Throwable ignored) {}
		}

		saveToDiskAsync();
	}

	public static ContainerSnapshot get(BlockPos pos) {
		if (pos == null) {
			return null;
		}

		ensureLoaded();

		String key = makeKey(pos);
		if (key == null) {
			return null;
		}

		ContainerSnapshot snapshot = CACHE.get(key);
		if (snapshot != null) {
			return snapshot;
		}

		for (Direction dir : Direction.values()) {
			String neighborKey = makeKey(pos.relative(dir));
			if (neighborKey != null) {
				snapshot = CACHE.get(neighborKey);
				if (snapshot != null) {
					return snapshot;
				}
			}
		}

		return null;
	}
}
