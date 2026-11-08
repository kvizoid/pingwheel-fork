package nx.pingwheel.common.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import nx.pingwheel.common.core.ContainerMemoryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> {

	@Shadow public abstract T getMenu();

	@Inject(method = "containerTick", at = @At("TAIL"))
	private void onContainerTick(CallbackInfo ci) {
		var pos = ContainerMemoryManager.getLastInteractedPos();
		if (pos != null) {
			var menu = this.getMenu();
			if (menu != null && menu.slots != null && menu.slots.size() > 36) {
				ContainerMemoryManager.saveSnapshot(pos, menu.slots, Minecraft.getInstance().level);
			}
		}
	}

	@Inject(method = "onClose", at = @At("HEAD"))
	private void onContainerClose(CallbackInfo ci) {
		var pos = ContainerMemoryManager.getLastInteractedPos();
		if (pos != null) {
			var menu = this.getMenu();
			if (menu != null && menu.slots != null && menu.slots.size() > 36) {
				ContainerMemoryManager.saveSnapshot(pos, menu.slots, Minecraft.getInstance().level);
			}
		}
	}
}
