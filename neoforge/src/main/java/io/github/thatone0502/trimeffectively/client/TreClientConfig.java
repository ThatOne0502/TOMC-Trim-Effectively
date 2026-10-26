package io.github.thatone0502.trimeffectively.client;

import io.github.thatone0502.trimeffectively.config.TreConfig;
import io.github.thatone0502.trimeffectively.net.TreConfigPayload;

import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.common.NeoForge;

import net.minecraft.client.Minecraft;

/**
 * Client half of the config sync: keeps whatever the host last told us.
 *
 * <p>Two states matter, and they are decided differently:
 *
 * <ul>
 *   <li><b>What to show.</b> Unless a remote host owns the truth, the local file
 *       <em>is</em> the truth and is read directly, so the screen shows what will be written. Otherwise the
 *       values come from {@link TreConfigPayload}; before the first payload arrives the defaults are shown but the
 *       screen is read-only anyway, so nothing can be mistaken for a decision.</li>
 *   <li><b>Whether it can be edited.</b> Only when the local file is the copy that counts - that is, in the main
 *       menu (no world at all) and while hosting a world. Connected to somebody else's server the entries are
 *       greyed out and the save button does nothing, because the host's file is then the only copy that counts:
 *       the screen is a viewer, not an editor.</li>
 * </ul>
 */
public final class TreClientConfig {
	private static TreConfig.Values fromServer;

	public static void register() {
		// The payload type itself is registered by TreConfigSync on the mod bus; all that is left for the client
		// is forgetting one server's values as soon as that connection ends.
		// Leaving the world must not leave one server's settings behind for the next one.
		NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> fromServer = null);
	}

	/** Stores the values just sent by the host, called from the payload handler. */
	public static void accept(TreConfigPayload payload) {
		fromServer = payload.toValues();
	}

	/**
	 * Whether this client is looking at a remote host's settings, and therefore may not change them.
	 *
	 * <p>Deliberately phrased as "connected to a world that we are not hosting" rather than "hosting or not":
	 * the main menu has no world and no host, and that is exactly when a player wants to change the defaults
	 * before ever opening a save.
	 */
	public static boolean readOnly() {
		Minecraft minecraft = Minecraft.getInstance();
		return minecraft.level != null && !minecraft.hasSingleplayerServer();
	}

	/** Whether the local file is the copy that counts, so the screen may write to it. */
	public static boolean editable() {
		return !readOnly();
	}

	/** Whether a payload has been received from a remote host. */
	public static boolean receivedFromServer() {
		return fromServer != null;
	}

	/** The values to display: the local file unless a remote host owns the truth. */
	public static TreConfig.Values values() {
		if (readOnly()) {
			return fromServer == null ? TreConfig.Values.DEFAULT : fromServer;
		}
		return TreConfig.read();
	}

	private TreClientConfig() {
	}
}
