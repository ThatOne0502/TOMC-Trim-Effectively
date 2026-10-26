package io.github.thatone0502.trimeffectively.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import io.github.thatone0502.trimeffectively.data.EffectEntry;
import io.github.thatone0502.trimeffectively.engine.AttributeEngine;
import io.github.thatone0502.trimeffectively.engine.TrimSnapshot;
import io.github.thatone0502.trimeffectively.engine.TrimStamp;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.List;

/** {@code /tre dump} and {@code /tre reload}. */
public final class TreCommand {
	public static void init(RegisterCommandsEvent event) {
		register(event.getDispatcher());
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(net.minecraft.commands.Commands.literal("tre")
				.requires(source -> source.permissions()
						.hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))
				.then(net.minecraft.commands.Commands.literal("dump")
						.executes(TreCommand::dump))
				.then(net.minecraft.commands.Commands.literal("reload")
						.executes(TreCommand::reload)));
	}

	private static int dump(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		List<AttributeEngine.TrimEntry> entries = AttributeEngine.load(source.getServer().registryAccess());
		source.sendSuccess(() -> Component.literal("[TRE] effect entries loaded: " + entries.size()), false);

		ServerPlayer player = source.getPlayer();
		if (player == null) {
			for (AttributeEngine.TrimEntry entry : entries) {
				source.sendSuccess(() -> Component.literal("  - " + entry.entryId()
						+ " -> " + entry.effect().appliesTo()), false);
			}
			return 1;
		}

		TrimSnapshot snapshot = TrimSnapshot.of(player);
		source.sendSuccess(() -> Component.literal("[TRE] trimmed pieces worn: " + snapshot.pieces.size()), false);
		snapshot.pieces.forEach(piece -> source.sendSuccess(() -> Component.literal(
				"  " + piece.slot().getName() + "  " + piece.pattern() + " / " + piece.material()), false));

		for (AttributeEngine.TrimEntry entry : entries) {
			int pieces = 0;
			for (Identifier target : entry.effect().appliesTo()) {
				pieces = Math.max(pieces,
						snapshot.countByIdAndSlots(target, entry.effect().slots()));
			}
			if (pieces <= 0) {
				continue;
			}
			int finalPieces = pieces;
			source.sendSuccess(() -> Component.literal("[TRE] " + entry.entryId()
					+ "  pieces=" + finalPieces + "  slots=" + entry.effect().slots()), false);
			for (EffectEntry effect : entry.effect().effects()) {
				String details = "    " + effect.type() + " trigger=" + effect.triggerName()
						+ (effect.attribute() == null ? "" : " attribute=" + effect.attribute())
						+ (effect.amount() == null ? "" : " amount=" + effect.amount().value()
						+ (effect.amount().per().isEmpty() ? "" : " per=" + effect.amount().per()))
						+ (effect.conditions().isEmpty() ? "" : " [conditions NOT evaluated yet: "
						+ effect.conditions().size() + "]");
				source.sendSuccess(() -> Component.literal(details), false);
			}
		}
		return 1;
	}

	private static int reload(CommandContext<CommandSourceStack> context) {
		CommandSourceStack source = context.getSource();
		MinecraftServer server = source.getServer();
		server.reloadResources(server.getPackRepository().getSelectedIds())
				.thenRun(() -> server.execute(() -> {
					int items = restamp(server);
					source.sendSuccess(() -> Component.literal(
							"[TRE] datapacks reloaded; re-stamped " + items + " item(s) held by online players"),
							false);
				}))
				.exceptionally(error -> {
					source.sendFailure(Component.literal("[TRE] reload failed: " + error.getMessage()));
					return null;
				});
		source.sendSuccess(() -> Component.literal("[TRE] datapacks reloading"), false);
		return 1;
	}

	/**
	 * Recomputes the stamp of everything online players carry.
	 *
	 * <p>This is the escape hatch for the one case a stamp cannot handle by itself: the datapack changed and the
	 * admin wants existing items to follow. Containers in the world are still not touched - those items are
	 * adopted when they next pass through a player's hands.
	 */
	private static int restamp(MinecraftServer server) {
		int count = 0;
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			for (EquipmentSlot slot : EquipmentSlot.VALUES) {
				ItemStack stack = player.getItemBySlot(slot);
				if (!stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.TRIM)) {
					TrimStamp.force(stack);
					count++;
				}
			}
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack stack = player.getInventory().getItem(i);
				if (!stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.TRIM)) {
					TrimStamp.force(stack);
					count++;
				}
			}
		}
		return count;
	}

	private TreCommand() {
	}
}
