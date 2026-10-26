package io.github.thatone0502.trimeffectively.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import io.github.thatone0502.trimeffectively.Tre;
import io.github.thatone0502.trimeffectively.data.TreLookup;
import io.github.thatone0502.trimeffectively.engine.TrimStamp;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.core.component.DataComponents;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Replaces the two vanilla trim lines with the mod's own effect name and flavour text.
 *
 * <p>Collapsed, the trim lines read as a hint that names the reveal key; holding it swaps each line
 * for a name plus a description, both taken straight from the data entry's language keys. Trims with
 * no matching data entry keep the vanilla wording.
 */
public final class TrimTooltip {
	public static final KeyMapping.Category CATEGORY =
			new KeyMapping.Category(Tre.id("main"));
	public static final KeyMapping REVEAL = new KeyMapping(
			"key.trimeffectively.reveal",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_LEFT_SHIFT,
			CATEGORY);

	private static final String PATTERN_PREFIX = "trim_pattern.";
	private static final String MATERIAL_PREFIX = "trim_material.";
	/** Description lines reuse the trim colour at this brightness, so they read as secondary text. */
	private static final float DIM_FACTOR = 0.65F;

	private TrimTooltip() {
	}

	public static void register() {
		// The key mapping itself is added from the register event; the tooltip hook is a game bus event.
		NeoForge.EVENT_BUS.addListener(TrimTooltip::onTooltip);
	}

	/** Adds the reveal key to the controls screen. Key mappings are a registry, so this cannot run at class init. */
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		event.registerCategory(CATEGORY);
		event.register(REVEAL);
	}

	private static void onTooltip(ItemTooltipEvent event) {
		append(event.getItemStack(), event.getContext(), event.getFlags(), event.getToolTip());
	}

	private static void append(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
		ArmorTrim trim = stack.get(DataComponents.TRIM);
		if (trim == null) {
			return;
		}
		RegistryAccess access = registryAccess();
		if (access == null) {
			return;
		}
		boolean disabled = !TrimStamp.enabled(stack);
		boolean reveal = !disabled && isRevealHeld();
		// Collect first, then edit: replacements change the indices of everything after them.
		List<Integer> indices = new ArrayList<>(2);
		List<TreLookup.Hit> hits = new ArrayList<>(2);
		List<Style> styles = new ArrayList<>(2);
		for (int index = 0; index < lines.size(); index++) {
			Component source = translatableComponent(lines.get(index));
			if (source == null) {
				continue;
			}
			String key = ownKey(source);
			Optional<TreLookup.Hit> hit;
			if (key.startsWith(PATTERN_PREFIX)) {
				hit = trim.pattern().unwrapKey()
						.flatMap(pattern -> TreLookup.pattern(access, pattern.identifier()));
			} else if (key.startsWith(MATERIAL_PREFIX)) {
				hit = trim.material().unwrapKey()
						.flatMap(material -> TreLookup.material(access, material.identifier()));
			} else {
				continue;
			}
			if (hit.isPresent()) {
				indices.add(index);
				hits.add(hit.get());
				// Vanilla paints both lines with the trim material's colour; carry it over.
				styles.add(source.getStyle());
			}
		}
		if (hits.isEmpty()) {
			return;
		}
		if (reveal) {
			// Swap each vanilla line for a name line plus a description line, keeping the order.
			for (int position = indices.size() - 1; position >= 0; position--) {
				int index = indices.get(position);
				lines.remove(index);
				lines.addAll(index, render(hits.get(position), styles.get(position)));
			}
		} else {
			// Keep the vanilla lines untouched and add one hint right below the last of them.
			int index = indices.get(indices.size() - 1) + 1;
			// A switched-off trim never expands: the reveal key has nothing to reveal, and the point of the
			// line is to say so rather than to hint at a key that would do nothing.
			lines.add(index, disabled
					? Component.translatable("tooltip.trimeffectively.disabled")
							.withStyle(ChatFormatting.DARK_RED)
					: Component.translatable("tooltip.trimeffectively.hidden",
									REVEAL.getTranslatedKeyMessage())
							.withStyle(ChatFormatting.DARK_GRAY));
		}
	}

	/** Name in the material's colour and bold, description in a dimmer shade of the same colour. */
	private static List<Component> render(TreLookup.Hit hit, Style base) {
		return List.of(
				Component.translatable(hit.langKey("name")).withStyle(base.withBold(true)),
				Component.literal(" · ")
						.append(Component.translatable(hit.langKey("desc")))
						.withStyle(dim(base)));
	}

	/** Same hue as the trim, darkened, so it reads as secondary text without losing the identity. */
	private static Style dim(Style base) {
		TextColor colour = base.getColor();
		if (colour == null) {
			return Style.EMPTY.applyFormat(ChatFormatting.DARK_GRAY);
		}
		int rgb = colour.getValue();
		int red = Math.round(((rgb >> 16) & 0xFF) * DIM_FACTOR);
		int green = Math.round(((rgb >> 8) & 0xFF) * DIM_FACTOR);
		int blue = Math.round((rgb & 0xFF) * DIM_FACTOR);
		return Style.EMPTY.withColor(TextColor.fromRgb((red << 16) | (green << 8) | blue));
	}

	/**
	 * Whether the reveal key is physically held right now.
	 *
	 * <p>{@link KeyMapping#isDown()} is only refreshed by the in-game input handler, so it reads
	 * false while a container screen is open — which is exactly where tooltips are read. Query the
	 * device state instead, so rebinding to a keyboard key or a mouse button both work.
	 */
	private static boolean isRevealHeld() {
		InputConstants.Key key = REVEAL.getKey();
		Window window = Minecraft.getInstance().getWindow();
		if (key.getType() == InputConstants.Type.KEYSYM) {
			return key.getValue() != InputConstants.UNKNOWN.getValue()
					&& InputConstants.isKeyDown(window, key.getValue());
		}
		if (key.getType() == InputConstants.Type.MOUSE) {
			return GLFW.glfwGetMouseButton(window.handle(), key.getValue()) == GLFW.GLFW_PRESS;
		}
		return false;
	}

	/**
	 * Finds the translatable component a vanilla tooltip line really stands for.
	 *
	 * <p>Vanilla builds both trim lines as {@code CommonComponents.space().append(...)}, so the line
	 * itself is a literal space and the real translatable component — the one carrying the trim
	 * material's colour — sits in its siblings.
	 */
	private static Component translatableComponent(Component line) {
		if (ownKey(line) != null) {
			return line;
		}
		for (Component sibling : line.getSiblings()) {
			if (ownKey(sibling) != null) {
				return sibling;
			}
		}
		return null;
	}

	private static String ownKey(Component component) {
		if (component.getContents() instanceof TranslatableContents contents) {
			return contents.getKey();
		}
		return null;
	}

	private static RegistryAccess registryAccess() {
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.getConnection() != null) {
			return minecraft.getConnection().registryAccess();
		}
		return minecraft.level == null ? null : minecraft.level.registryAccess();
	}
}
