package nx.pingwheel.common.core;

import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.math.MathUtils;
import nx.pingwheel.common.math.ScreenPos;
import nx.pingwheel.common.network.PingLocationS2CPacket;
import nx.pingwheel.common.render.WorldRenderContext;
import nx.pingwheel.common.util.DirectionalSoundInstance;

import java.util.UUID;

import static nx.pingwheel.common.CommonClient.Game;
import static nx.pingwheel.common.config.ClientConfig.*;
import static nx.pingwheel.common.resource.ResourceConstants.PING_SOUND_EVENT;

public class PingView extends PingData {

	private static final ClientConfig CLIENT_CONFIG = ClientConfig.HANDLER.getConfig();
	private static final int WHITE = 0xFFFFFFFF;

	private int spawnTime;
	private int age;
	private PlayerInfo playerInfo;
	private ItemStack itemStack;
	private ScreenPos screenPos;
	private double distance;
	private float scale;

	private PlayerInfo targetPlayerInfo;
	private Component targetEntityName;
	private ItemStack targetEntityIcon;
	private boolean entityDead;
	private boolean hostile;

	public PlayerInfo getPlayerInfo() {
		return playerInfo;
	}

	public ItemStack getItemStack() {
		return itemStack;
	}

	public ScreenPos getScreenPos() {
		return screenPos;
	}

	public double getDistance() {
		return distance;
	}

	public float getScale() {
		return scale;
	}

	public PlayerInfo getTargetPlayerInfo() {
		return targetPlayerInfo;
	}

	public Component getTargetEntityName() {
		return targetEntityName;
	}

	public ItemStack getTargetEntityIcon() {
		return targetEntityIcon;
	}

	public boolean isEntityDead() {
		return entityDead;
	}

	public boolean isHostile() {
		return hostile;
	}

	public int getAge() {
		return age;
	}

	public Vec3 getPos() {
		return pos;
	}

	private PingView(Vec3 pos, UUID entityId, UUID authorId, int sequence, int dimension) {
		super(pos, entityId, authorId, sequence, dimension);
	}

	public static PingView of(Vec3 pos, UUID entityId, UUID authorId, int sequence, int dimension) {
		return new PingView(
			pos,
			entityId,
			authorId,
			sequence,
			dimension
		);
	}

	public static PingView from(PingLocationS2CPacket packet) {
		return PingView.of(
			packet.pos(),
			packet.entity(),
			packet.author(),
			packet.sequence(),
			packet.dimension()
		);
	}

	public void update(WorldRenderContext ctx, int gameTime) {
		if (this.spawnTime == 0) {
			this.spawnTime = gameTime;
		}

		this.age = gameTime - spawnTime;

		final var connection = Game.getConnection();

		if (connection != null && this.authorId != null) {
			this.playerInfo = connection.getPlayerInfo(this.authorId);
		}

		if (this.entityId != null) {
			if (this.targetPlayerInfo == null && connection != null) {
				var pInfo = connection.getPlayerInfo(this.entityId);
				if (pInfo != null) {
					this.targetPlayerInfo = pInfo;
					this.targetEntityName = Component.literal(pInfo.getProfile().name());
				}
			}

			final var ent = GameContext.getEntity(this.entityId);

			if (ent != null) {
				if (ent.isRemoved() || (ent instanceof LivingEntity && ((LivingEntity)ent).isDeadOrDying())) {
					this.entityDead = true;
				} else {
					if (ent instanceof Enemy) {
						this.hostile = true;
					}

					if (ent instanceof ItemEntity && CLIENT_CONFIG.isItemIconVisible()) {
						this.itemStack = ((ItemEntity)ent).getItem().copy();
						if (this.targetEntityName == null) {
							String name = this.itemStack.getHoverName().getString();
							if (this.itemStack.getCount() > 1) {
								name += " ×" + this.itemStack.getCount();
							}
							this.targetEntityName = Component.literal(name);
						}
					} else if (ent instanceof Player targetPlayer) {
						if (connection != null && this.targetPlayerInfo == null) {
							this.targetPlayerInfo = connection.getPlayerInfo(targetPlayer.getUUID());
						}
						var baseName = targetPlayer.getDisplayName().copy();
						int hp = Math.round(targetPlayer.getHealth());
						int maxHp = Math.round(targetPlayer.getMaxHealth());
						if (hp > 0 && maxHp > 0) {
							int color = getHealthColor(targetPlayer.getHealth(), targetPlayer.getMaxHealth());
							String hpStr = (hp == maxHp) ? (hp + "❤") : (hp + "/" + maxHp + "❤");
							baseName.append(Component.literal("  " + hpStr).withStyle(s -> s.withColor(color)));
							int absorption = Math.round(targetPlayer.getAbsorptionAmount());
							if (absorption > 0) {
								baseName.append(Component.literal(" +" + absorption).withStyle(ChatFormatting.GOLD));
							}
						}
						this.targetEntityName = baseName;
					} else if (ent instanceof LivingEntity mob) {
						var baseName = mob.getDisplayName().copy();
						int hp = Math.round(mob.getHealth());
						int maxHp = Math.round(mob.getMaxHealth());
						if (maxHp > 0) {
							int color = getHealthColor(mob.getHealth(), mob.getMaxHealth());
							String hpStr = (hp == maxHp) ? (hp + "❤") : (hp + "/" + maxHp + "❤");
							baseName.append(Component.literal("  " + hpStr).withStyle(s -> s.withColor(color)));
							int absorption = Math.round(mob.getAbsorptionAmount());
							if (absorption > 0) {
								baseName.append(Component.literal(" +" + absorption).withStyle(ChatFormatting.GOLD));
							}
						}
						this.targetEntityName = baseName;
						if (this.targetEntityIcon == null) {
							this.targetEntityIcon = getMobIcon(mob);
						}
					}

					this.pos = ent.getPosition(ctx.tickDelta).add(0, ent.getBoundingBox().getYsize(), 0);
				}
			} else if (this.age > 40 && this.targetPlayerInfo == null) {
				this.entityDead = true;
			}
		}

		this.screenPos = MathUtils.worldToScreen(pos, ctx);
		this.distance = ctx.camera.pos.distanceTo(pos);
		this.calculateScale();
	}

	private static ItemStack getMobIcon(LivingEntity mob) {
		try {
			var type = mob.getType();
			if (type == EntityTypes.ZOMBIE) return new ItemStack(Items.ZOMBIE_HEAD);
			if (type == EntityTypes.SKELETON) return new ItemStack(Items.SKELETON_SKULL);
			if (type == EntityTypes.WITHER_SKELETON) return new ItemStack(Items.WITHER_SKELETON_SKULL);
			if (type == EntityTypes.CREEPER) return new ItemStack(Items.CREEPER_HEAD);
			if (type == EntityTypes.PIGLIN) return new ItemStack(Items.PIGLIN_HEAD);
			if (type == EntityTypes.ENDER_DRAGON) return new ItemStack(Items.DRAGON_HEAD);
			return SpawnEggItem.byId(type).map(h -> new ItemStack(h.value())).orElse(null);
		} catch (Throwable ignored) {
			return null;
		}
	}

	public static int getHealthColor(float current, float max) {
		if (max <= 0f) return 0xFF5555;
		float ratio = Math.clamp(current / max, 0f, 1f);

		int r, g;
		if (ratio >= 0.5f) {
			float t = (ratio - 0.5f) * 2f;
			r = (int)(0xFF * (1f - t) + 0x55 * t);
			g = 0xFF;
		} else {
			float t = ratio * 2f;
			r = 0xFF;
			g = (int)(0x33 * (1f - t) + 0xFF * t);
		}
		int b = (int)(0x33 * (1f - ratio) + 0x55 * ratio);
		return (r << 16) | (g << 8) | b;
	}

	public void playSoundInDimension() {
		if (this.dimension != GameContext.getDimension()) {
			return;
		}

		Game.getSoundManager().play(
			new DirectionalSoundInstance(
				PING_SOUND_EVENT,
				SoundSource.MASTER,
				CLIENT_CONFIG.getPingVolume() / 100f,
				1f,
				this.pos
			)
		);
	}

	public boolean isExpired() {
		return (this.age > CLIENT_CONFIG.getPingDuration() * TPS && CLIENT_CONFIG.getPingDuration() < MAX_PING_DURATION) || this.entityDead;
	}

	public boolean isRemovable() {
		return (CLIENT_CONFIG.getCorrectionPeriod() >= MAX_CORRECTION_PERIOD || this.age > CLIENT_CONFIG.getCorrectionPeriod() * TPS) && this.distanceToCenter() < CLIENT_CONFIG.getRemoveRadius();
	}

	public float distanceToCenter() {
		if (this.screenPos == null) {
			return 0f;
		}

		final var wnd = Game.getWindow();
		final var center = new Vec2(wnd.getGuiScaledWidth() * 0.5f, wnd.getGuiScaledHeight() * 0.5f);

		return this.screenPos.distanceTo(center);
	}

	public boolean isCloserToCenter(PingView b) {
		if (b == null) {
			return true;
		}

		return this.distanceToCenter() < b.distanceToCenter();
	}

	public int getTeamColor() {
		if (this.playerInfo == null || this.playerInfo.getTeam() == null) return WHITE;

		return this.playerInfo.getTeam().getColor()
			.map(teamColor -> (255 << 24) | teamColor.rgb())
			.orElse(WHITE);
	}

	private void calculateScale() {
		final var scale = 2.0 / Math.pow(distance, 0.3);
		final var pingSize = CLIENT_CONFIG.getPingSize() / 100f;

		this.scale = (float)Math.max(1.0, scale) * 0.5f * pingSize;
	}
}
