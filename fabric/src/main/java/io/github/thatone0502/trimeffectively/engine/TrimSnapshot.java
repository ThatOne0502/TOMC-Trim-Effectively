package io.github.thatone0502.trimeffectively.engine;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.trim.ArmorTrim;

import java.util.ArrayList;
import java.util.List;

/** A snapshot of what a living entity is wearing right now, trim-wise. */
public final class TrimSnapshot {
	public record Piece(EquipmentSlot slot, Identifier pattern, Identifier material, ItemStack stack) {
	}

	private static final List<EquipmentSlot> ARMOR_SLOTS =
			List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);
	private static final List<EquipmentSlot> HAND_SLOTS =
			List.of(EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND);

	public final List<Piece> pieces;

	private TrimSnapshot(List<Piece> pieces) {
		this.pieces = pieces;
	}

	public static TrimSnapshot of(LivingEntity entity) {
		List<Piece> found = new ArrayList<>(6);
		for (EquipmentSlot slot : EquipmentSlot.VALUES) {
			if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND && !slot.isArmor()) {
				continue;
			}
			ItemStack stack = entity.getItemBySlot(slot);
			if (stack.isEmpty()) {
				continue;
			}
			if (entity.level() instanceof ServerLevel) {
				// Cheapest possible stamp check: this loop already visits every slot that could carry a
				// trim, so items that predate the component are adopted here at no extra cost.
				TrimStamp.reconcile(stack);
			}
			// A switched-off trim stops being a piece here rather than anywhere downstream: piece counts,
			// stacking, attribute modifiers and the datapack variables all read this list, so dropping the
			// item once makes all of them agree, and no effect type needs to know the switch exists.
			if (!TrimStamp.enabled(stack)) {
				continue;
			}
			ArmorTrim trim = stack.get(DataComponents.TRIM);
			if (trim == null) {
				continue;
			}
			Identifier pattern = trim.pattern().unwrapKey().map(k -> k.identifier()).orElse(null);
			Identifier material = trim.material().unwrapKey().map(k -> k.identifier()).orElse(null);
			found.add(new Piece(slot, pattern, material, stack));
		}
		return new TrimSnapshot(List.copyOf(found));
	}

	public boolean isEmpty() {
		return pieces.isEmpty();
	}

	/** Number of pieces matching {@code trimId} that sit in one of the given slot groups. */
	public int countByIdAndSlots(Identifier trimId, List<String> slotGroups) {
		int n = 0;
		for (Piece piece : pieces) {
			if (!slotAllowed(piece.slot(), slotGroups)) {
				continue;
			}
			if (trimId.equals(piece.pattern()) || trimId.equals(piece.material())) {
				n++;
			}
		}
		return n;
	}

	/** Total armour points contributed by pieces that carry any trim and sit in the given slot groups. */
	public double trimmedArmor(List<String> slotGroups) {
		double total = 0.0D;
		for (Piece piece : pieces) {
			if (!slotAllowed(piece.slot(), slotGroups)) {
				continue;
			}
			ItemAttributeModifiers modifiers = piece.stack()
					.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
			double[] sum = {0.0D};
			modifiers.forEach(piece.slot(), (holder, modifier) -> {
				if (is(holder, Attributes.ARMOR)
						&& modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
					sum[0] += modifier.amount();
				}
			});
			total += sum[0];
		}
		return total;
	}

	private static boolean is(Holder<Attribute> holder, Holder<Attribute> expected) {
		return holder.value() == expected.value();
	}

	public static boolean slotAllowed(EquipmentSlot slot, List<String> slotGroups) {
		if (slotGroups == null || slotGroups.isEmpty()) {
			slotGroups = List.of("armor", "hand");
		}
		for (String group : slotGroups) {
			switch (group) {
				case "armor" -> {
					if (ARMOR_SLOTS.contains(slot)) {
						return true;
					}
				}
				case "hand" -> {
					if (HAND_SLOTS.contains(slot)) {
						return true;
					}
				}
				case "mainhand" -> {
					if (slot == EquipmentSlot.MAINHAND) {
						return true;
					}
				}
				case "offhand" -> {
					if (slot == EquipmentSlot.OFFHAND) {
						return true;
					}
				}
				default -> {
					if (slot.getName().equals(group)) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
