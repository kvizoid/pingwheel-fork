package nx.pingwheel.common.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import nx.pingwheel.common.core.PingManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

	@Shadow @Final private Minecraft minecraft;

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		if (nx.pingwheel.common.CommonClient.Game != null
			&& nx.pingwheel.common.CommonClient.Game.gui != null
			&& nx.pingwheel.common.CommonClient.Game.gui.screen() == null
			&& Math.abs(vertical) > 0.01) {
			var hovered = PingManager.getHoveredPing();
			if (hovered != null) {
				hovered.toggleAltDimension();
				try {
					nx.pingwheel.common.CommonClient.Game.getSoundManager().play(
						SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.6f)
					);
				} catch (Throwable ignored) {}
				ci.cancel();
			}
		}
	}
}
