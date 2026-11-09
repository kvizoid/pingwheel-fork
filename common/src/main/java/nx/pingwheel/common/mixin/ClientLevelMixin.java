package nx.pingwheel.common.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import nx.pingwheel.common.core.DropPingManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

	@Inject(method = "addEntity", at = @At("TAIL"))
	private void onAddEntity(Entity entity, CallbackInfo ci) {
		DropPingManager.onEntityAdded(entity);
	}
}
