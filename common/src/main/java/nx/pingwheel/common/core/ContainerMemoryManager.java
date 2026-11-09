package nx.pingwheel.common.core;

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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static nx.pingwheel.common.CommonClient.Game;

public class ContainerMemoryManager {
	private ContainerMemoryManager() {}

	public record ItemEntry(Component name, int count) {}

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

	private static final Map<BlockPos, ContainerSnapshot> CACHE = new ConcurrentHashMap<>();
	private static volatile BlockPos lastInteractedPos = null;
	private static volatile long lastInteractedTime = 0;

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

		CACHE.put(pos, snapshot);

		if (level != null) {
			try {
				BlockState state = level.getBlockState(pos);
				if (state.getBlock() instanceof ChestBlock && ChestBlock.getBlockType(state) != DoubleBlockCombiner.BlockType.SINGLE) {
					Direction dir = ChestBlock.getConnectedDirection(state);
					if (dir != null) {
						CACHE.put(pos.relative(dir).immutable(), snapshot);
					}
				}
			} catch (Throwable ignored) {}
		}
	}

	public static ContainerSnapshot get(BlockPos pos) {
		if (pos == null) {
			return null;
		}

		ContainerSnapshot snapshot = CACHE.get(pos);
		if (snapshot != null) {
			return snapshot;
		}

		for (Direction dir : Direction.values()) {
			snapshot = CACHE.get(pos.relative(dir));
			if (snapshot != null) {
				return snapshot;
			}
		}

		return null;
	}
}

