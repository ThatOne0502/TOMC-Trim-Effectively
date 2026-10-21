package io.github.thatone0502.trimeffectively.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.minecraft.client.gui.screens.Screen;

/**
 * Mod Menu integration (soft dependency): puts a "config" button on this mod's entry in the mod list.
 *
 * <p>The class is only instantiated when Mod Menu is present, because it is reached through the {@code modmenu}
 * entrypoint in {@code fabric.mod.json}, and entrypoints whose owning mod is absent are simply never scanned. Mod
 * Menu itself requires Cloth Config at runtime, so referencing the Cloth-based screen from here is safe on any
 * install that has Mod Menu at all.
 */
public class TreMenuIntegration implements ModMenuApi {

	@Override
	@SuppressWarnings("unchecked")
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return (ConfigScreenFactory<Screen>) TreConfigScreen::create;
	}
}
