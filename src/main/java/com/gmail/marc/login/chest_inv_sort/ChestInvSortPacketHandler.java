package com.gmail.marc.login.chest_inv_sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;


public class ChestInvSortPacketHandler {
    static void handlePacket(ServerPlayer player, boolean isChest) {
        ChestInvSort.LOGGER.debug("Handling Packet @ Server!");
        NonNullList<ItemStack> sortedItemStackList = player.containerMenu.getItems();
        if (isChest == true) {
            if (player.containerMenu instanceof ChestMenu chestMenu) {
                sortedItemStackList = sortSlots(chestMenu);
            }
            // Send a packet to update all slots in the container to the client
            player.connection.send(new ClientboundContainerSetContentPacket(player.containerMenu.containerId, player.containerMenu.incrementStateId(), sortedItemStackList, ItemStack.EMPTY));
        }
        else {
            sortedItemStackList = sortSlots(player.inventoryMenu);

            // Send a packet to update all slots in the container to the client
            player.connection.send(new ClientboundContainerSetContentPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), sortedItemStackList, ItemStack.EMPTY));
        }
    }

    private static NonNullList<ItemStack> sortSlots(ChestMenu chestMenu) {
        int slotCount = 9 * chestMenu.getRowCount();
        NonNullList<ItemStack> items = NonNullList.create();
        // Extract items from chestMenu
        for (int i = 0; i < slotCount; i++) {
            Slot slot = chestMenu.getSlot(i);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                items.add(stack);
            }
            slot.set(ItemStack.EMPTY); // Clear slot
        }
        NonNullList<ItemStack> sortedItems = stackAndSortItems(items);
        int index = 0;
        for (Slot slot : chestMenu.slots) {
            slot.set(sortedItems.get(index));
            slot.setChanged();
            index++;
        }
        chestMenu.broadcastChanges();
        return sortedItems;
    }

    private static NonNullList<ItemStack> sortSlots(InventoryMenu inventoryMenu) {
        NonNullList<ItemStack> items = NonNullList.create();
        // Extract items from chestMenu
        for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.INV_SLOT_END; i++) {
            Slot slot = inventoryMenu.getSlot(i);
            ItemStack stack = slot.getItem();
            // ChestInvSort.LOGGER.debug(stack.getItem().getDescriptionId());
            if (!stack.isEmpty()) {
                items.add(stack);
            }
            slot.set(ItemStack.EMPTY); // Clear slot
        }
        NonNullList<ItemStack> sortedItems = stackAndSortItems(items);
        int j = 0;
        for (int i = InventoryMenu.INV_SLOT_START; i < InventoryMenu.INV_SLOT_END; i++) {
            Slot slot = inventoryMenu.getSlot(i);
            slot.set(sortedItems.get(j));
            slot.setChanged();
            j++;
        }
        inventoryMenu.broadcastChanges();
        return sortedItems;
    }

    private static NonNullList<ItemStack> stackAndSortItems(NonNullList<ItemStack> items) {
        // Stack items
        Map<String, List<ItemStack>> itemMap = new HashMap<>();
        for (ItemStack item : items) {
            String key = item.getItem().getDescriptionId() + item.getTag();
            itemMap.putIfAbsent(key, new ArrayList<>());
            List<ItemStack> stackList = itemMap.get(key);

            boolean stacked = false;
            for (ItemStack stack : stackList) {
                if (stack.getCount() < stack.getMaxStackSize()) {
                    int newCount = stack.getCount() + item.getCount();
                    int remaining = Math.max(0, newCount - stack.getMaxStackSize());
                    stack.setCount(Math.min(stack.getMaxStackSize(), newCount));
                    item.setCount(remaining);
                    if (remaining == 0) {
                        stacked = true;
                        break;
                    }
                }
            }
            if (!stacked) {
                stackList.add(item);
            }
        }

        // Collect stacked items into a list
        NonNullList<ItemStack> stackedItems = NonNullList.create();
        for (List<ItemStack> stackList : itemMap.values()) {
            stackedItems.addAll(stackList);
        }

        // Sort items: blocks first, then items, then by display name
        stackedItems.sort(Comparator
            .comparing((ItemStack itemStack) -> !(itemStack.getItem() instanceof BlockItem))
            .thenComparing(itemStack -> itemStack.getHoverName().getString()));

        return stackedItems;
    }
}
