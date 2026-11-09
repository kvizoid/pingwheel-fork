package nx.pingwheel.fabric;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import nx.pingwheel.common.Global;
import nx.pingwheel.common.config.ClientConfig;
import nx.pingwheel.common.updater.UpdateManager;

public class PreLaunchUpdater implements PreLaunchEntrypoint {

	@Override
	public void onPreLaunch() {
		try {
			ClientConfig.HANDLER.load();
			var config = ClientConfig.HANDLER.getConfig();
			if (!config.isPreLaunchUpdate()) {
				return;
			}

			UpdateManager.runPreLaunchCheck();
		} catch (Throwable t) {
			Global.LOGGER.warn("[PingWheel] PreLaunch updater failed: " + t.getMessage());
		}
	}
}
