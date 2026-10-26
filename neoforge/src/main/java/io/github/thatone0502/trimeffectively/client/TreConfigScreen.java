package io.github.thatone0502.trimeffectively.client;

import io.github.thatone0502.trimeffectively.config.TreConfig;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.network.chat.Component;

import java.util.Arrays;

/**
 * The Cloth Config screen behind the mod list's "config" button.
 *
 * <p>Registered from {@link TrimeffectivelyClient} as an {@code IConfigScreenFactory} extension point, which is
 * NeoForge's counterpart of Fabric's Mod Menu entrypoint; the screen itself is the same code as the Fabric side.
 *
 * <p>Layout, per the project standard: <b>one page</b> that scrolls, entries grouped under small section
 * headings, the categories on the left as an index, and a search bar at the very top.
 *
 * <p>All three of "search bar", "left-hand index" and "one scrolling page" come from the same switch:
 * {@code setGlobalized(true)} swaps Cloth's tabbed screen for {@code GlobalizedClothConfigScreen}, which has the
 * search field at the top and lists the categories as references down the left side, with the entry list
 * scrolling beside them. Without it, several categories become <em>top</em> tabs instead, which is the thing we
 * do not want.
 *
 * <p>Read-only mode is the same builder switch: {@code setEditable(false)} greys out every entry and hides the
 * save action. It is used whenever this client is not the host - see {@link TreClientConfig#editable()}.
 *
 * <p>Client-only class, and Cloth is an <b>optional</b> dependency: the factory is only registered when Cloth is
 * present, so this class is never loaded on a client without it.
 */
public final class TreConfigScreen {

	/** Builds the screen; {@code parent} may be null when opened from outside the mod list. */
	public static Screen create(Screen parent) {
		boolean editable = TreClientConfig.editable();
		TreConfig.Values values = TreClientConfig.values();

		// The save consumer writes into this and the saving runnable reads it back: Cloth asks for the screen's
		// save callback once, before any entry has been touched.
		boolean[] hijack = {values.hijackTrimDuplicationRecipes()};

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("config.trimeffectively.title"))
				.setSavingRunnable(() -> save(hijack[0]));
		builder.setGlobalized(true);
		builder.setEditable(editable);

		ConfigEntryBuilder entries = builder.entryBuilder();
		ConfigCategory page = builder.getOrCreateCategory(
				Component.translatable("config.trimeffectively.category.main"));

		// Says out loud whose settings these are, because the same screen is shown in both situations and the
		// greyed-out state alone does not explain itself.
		page.addEntry(entries.startTextDescription(Component.translatable(
				editable ? "config.trimeffectively.scope.local" : "config.trimeffectively.scope.server")).build());

		// ---- 分节：模板配方 ----
		page.addEntry(section(entries, "recipes"));
		page.addEntry(entries.startBooleanToggle(
						label("hijack_trim_duplication_recipes"),
						values.hijackTrimDuplicationRecipes())
				.setDefaultValue(TreConfig.Values.DEFAULT.hijackTrimDuplicationRecipes())
				.setTooltip(tooltip("hijack_trim_duplication_recipes"))
				.setSaveConsumer(value -> hijack[0] = value)
				.build());

		return builder.build();
	}

	/**
	 * Writes the file and, when this client hosts the world, reloads the data packs so the change takes effect
	 * straight away - which is what the entry's tooltip says it will need.
	 */
	private static void save(boolean hijackTrimDuplicationRecipes) {
		boolean written = TreConfig.write(new TreConfig.Values(hijackTrimDuplicationRecipes));
		if (!written) {
			return;
		}
		Minecraft minecraft = Minecraft.getInstance();
		IntegratedServer server = minecraft.getSingleplayerServer();
		if (server != null) {
			// Recipes are rebuilt on the server thread; scheduling there keeps the client thread out of it.
			server.execute(() -> server.reloadResources(server.getPackRepository().getSelectedIds()));
		}
	}

	/** Entry title: {@code config.trimeffectively.<key>}. */
	private static Component label(String key) {
		return Component.translatable("config.trimeffectively." + key);
	}

	/**
	 * Entry tooltip: {@code config.trimeffectively.<key>.tooltip}. A {@code \n} inside the translated text becomes
	 * a real line break: Cloth renders each {@code Component} argument as its own line, so splitting first is the
	 * only way to get a deterministic break rather than relying on the wrapper's handling of {@code '\n'}.
	 */
	private static Component[] tooltip(String key) {
		return Arrays.stream(Component.translatable("config.trimeffectively." + key + ".tooltip").getString().split("\n"))
				.map(Component::literal)
				.toArray(Component[]::new);
	}

	/** Section heading: {@code config.trimeffectively.section.<key>}. */
	private static AbstractConfigListEntry<?> section(ConfigEntryBuilder entries, String key) {
		return entries.startTextDescription(Component.translatable("config.trimeffectively.section." + key)).build();
	}

	private TreConfigScreen() {
	}
}
