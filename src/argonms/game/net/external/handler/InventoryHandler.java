/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2012  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.game.net.external.handler;

import argonms.common.GlobalConstants;
import argonms.common.character.inventory.Equip;
import argonms.common.character.inventory.Inventory;
import argonms.common.character.inventory.Inventory.InventoryType;
import argonms.common.character.inventory.InventorySlot;
import argonms.common.character.inventory.InventorySlot.ItemType;
import argonms.common.character.inventory.InventoryTools;
import argonms.common.loading.item.ItemDataLoader;
import argonms.common.loading.item.ItemEffectsData;
import argonms.common.net.external.CheatTracker;
import argonms.common.util.input.LittleEndianReader;
import argonms.game.character.GameCharacter;
import argonms.game.field.MapEntity.EntityType;
import argonms.game.field.entity.ItemDrop;
import argonms.game.net.external.GameClient;
import argonms.game.net.external.GamePackets;

/**
 *
 * @author GoldenKevin
 */
public class InventoryHandler {
	public static void handleItemMove(LittleEndianReader packet, GameClient gc) {
		/*int time = */packet.readInt();
		InventoryType type = InventoryType.valueOf(packet.readByte());
		short src = packet.readShort();
		short dst = packet.readShort();
		short qty = packet.readShort();
		GameCharacter p = gc.getPlayer();
		if (src < 0 && dst > 0) { //unequip
			InventoryTools.unequip(p.getInventory(InventoryType.EQUIPPED), p.getInventory(InventoryType.EQUIP), src, dst);
			gc.getSession().send(GamePackets.writeInventoryMoveItem(InventoryType.EQUIP, src, dst, (byte) 1));
			p.equipChanged((Equip) p.getInventory(InventoryType.EQUIP).get(dst), false);
			p.getMap().sendToAll(GamePackets.writeUpdateAvatar(p), p);
		} else if (dst < 0) { //equip
			short[] result = InventoryTools.equip(p.getInventory(InventoryType.EQUIP), p.getInventory(InventoryType.EQUIPPED), src, dst);
			if (result != null) {
				gc.getSession().send(GamePackets.writeInventoryMoveItem(InventoryType.EQUIP, src, dst, (byte) 2));
				if (result.length == 2)
					gc.getSession().send(GamePackets.writeInventoryMoveItem(InventoryType.EQUIP, result[0], result[1], (byte) 1));
				p.equipChanged((Equip) p.getInventory(InventoryType.EQUIPPED).get(dst), true);
				p.getMap().sendToAll(GamePackets.writeUpdateAvatar(p), p);
			} else {
				gc.getSession().send(GamePackets.writeInventoryNoChange());
			}
		} else if (dst == 0) { //drop
			Inventory inv = p.getInventory(src >= 0 ? type : InventoryType.EQUIPPED);
			InventorySlot item = inv.get(src);
			if (item.getType() == ItemType.PET) {
				//TODO: unequip pet
			}
			InventorySlot toDrop;
			short newQty = (short) (item.getQuantity() - qty);
			if (newQty == 0 || InventoryTools.isRechargeable(item.getDataId())) {
				inv.remove(src);
				toDrop = item;
				gc.getSession().send(GamePackets.writeInventoryClearSlot(type, src));
			} else {
				item.setQuantity(newQty);
				toDrop = item.clone();
				toDrop.setQuantity(qty);
				gc.getSession().send(GamePackets.writeInventoryDropItem(type, src, newQty));
			}
			ItemDrop d = new ItemDrop(toDrop);
			p.getMap().drop(d, 0, p, ItemDrop.PICKUP_ALLOW_ALL, p.getId(), !ItemDataLoader.getInstance().canDrop(toDrop.getDataId()));
		} else { //move item
			Inventory inv = p.getInventory(type);
			InventorySlot move = inv.get(src);
			InventorySlot replace = inv.get(dst);
			short slotMax = ItemDataLoader.getInstance().getSlotMax(move.getDataId());
			if (notStackable(move, replace, slotMax)) { //swap
				exchange(p, type, src, dst);
			} else { //merge!
				short srcQty = move.getQuantity();
				short dstQty = replace.getQuantity();
				int total = srcQty + dstQty;
				if (total > slotMax) { //exchange quantities
					short rest = (short) (total - slotMax);
					move.setQuantity(rest);
					replace.setQuantity(slotMax);
					gc.getSession().send(GamePackets.writeInventoryMoveItemShiftQuantities(type, src, rest, dst, slotMax));
				} else { //combine
					replace.setQuantity((short) total);
					inv.remove(src);
					gc.getSession().send(GamePackets.writeInventoryMoveItemCombineQuantities(type, src, dst, (short) total));
				}
			}
		}
	}

	public static void handleReturnScroll(LittleEndianReader packet, GameClient gc) {
		/*int time =*/packet.readInt();
		short slot = packet.readShort();
		int itemId = packet.readInt();

		GameCharacter p = gc.getPlayer();
		Inventory inv = p.getInventory(InventoryType.USE);
		InventorySlot changed = inv.get(slot);
		if (changed == null || changed.getDataId() != itemId || changed.getQuantity() < 1) {
			CheatTracker.get(gc).suspicious(CheatTracker.Infraction.PACKET_EDITING, "Tried to use nonexistant map return scroll");
			return;
		}
		changed = InventoryTools.takeFromInventory(inv, slot, (short) 1);
		if (changed != null)
			gc.getSession().send(GamePackets.writeInventoryUpdateSlotQuantity(InventoryType.USE, slot, changed));
		else
			gc.getSession().send(GamePackets.writeInventoryClearSlot(InventoryType.USE, slot));
		p.itemCountChanged(itemId);

		ItemEffectsData e = ItemDataLoader.getInstance().getEffect(itemId);
		if (e.getMoveTo() != 0) {
			if (e.getMoveTo() == GlobalConstants.NULL_MAP)
				p.changeMap(p.getMap().getReturnMap());
			else
				p.changeMap(e.getMoveTo());
		}
	}

	public static void handleMesoDrop(LittleEndianReader packet, GameClient gc) {
		/*int time = */packet.readInt();
		int amount = packet.readInt();
		GameCharacter p = gc.getPlayer();
		if (amount <= p.getMesos()) {
			p.gainMesos(-amount, false);
			ItemDrop d = new ItemDrop(amount);
			p.getMap().drop(d, 0, p, ItemDrop.PICKUP_ALLOW_ALL, p.getId(), false);
		}
	}

	public static void handleMapItemPickUp(LittleEndianReader packet, GameClient gc) {
		/*byte mode = */packet.readByte();
		packet.readInt(); //?
		/*short x = */packet.readShort();
		/*short y = */packet.readShort();
		int eid = packet.readInt();
		GameCharacter p = gc.getPlayer();
		//TODO: Synchronize on the item (!d.isAlive and GameMap.pickUpDrop are
		//not thread safe if two players try picking it up at the exact same time).
		ItemDrop d = (ItemDrop) p.getMap().getEntityById(EntityType.DROP, eid);
		if (d == null || !d.isAlive()) {
			gc.getSession().send(GamePackets.writeInventoryNoChange());
			gc.getSession().send(GamePackets.writeShowInventoryFull());
			return;
		}
		p.getMap().pickUpDrop(d, p);
	}

	private static boolean notStackable(InventorySlot src, InventorySlot dst, short slotMax) {
		return dst == null || src.getDataId() != dst.getDataId() || slotMax == 1
				|| InventoryTools.isThrowingStar(src.getDataId())
				|| InventoryTools.isBullet(src.getDataId());
	}

	private static void exchange(GameCharacter p, InventoryType type, short src, short dst) {
		Inventory inv = p.getInventory(type);
		InventorySlot item1 = inv.remove(src);
		InventorySlot item2 = inv.remove(dst);
		inv.put(dst, item1);
		if (item2 != null)
			inv.put(src, item2);
		p.getClient().getSession().send(GamePackets.writeInventoryMoveItem(type, src, dst, (byte) -1));
	}
}
