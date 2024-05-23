package com.gmail.marc.login.chest_inv_sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;


public class ChestInvSortPacketHandler {
    static void handlePacket(ServerPlayer player, boolean isChest) {
        ChestInvSort.LOGGER.debug("Handling Packet @ Server!");
        NonNullList<ItemStack> sortedItemStackList = player.containerMenu.getItems(); // Temporary variable fill
        if (isChest == true) {
            if (player.containerMenu instanceof ChestMenu chestMenu) {
                sortedItemStackList = sortContainer(chestMenu);
                Container container = chestMenu.getContainer();
                if (container instanceof ChestBlockEntity chestEntity) {
                    // ChestInvSort.LOGGER.debug("chestEntity size: {}", chestEntity.getContainerSize());
                    for (int i = 0; i < chestEntity.getContainerSize(); i++) {
                        chestEntity.setItem(i, sortedItemStackList.get(i));
                    }
                    chestEntity.setChanged();
                }
                else
                    ChestInvSort.LOGGER.debug("ChestMenu is not in ChestBlockEntity, cannot sort!");
            }
            // else if (player.containerMenu instanceof HorseInventoryMenu) // TODO
            //     sortChest((HorseInventoryMenu)player.containerMenu);
            // else if (player.containerMenu instanceof ShulkerBoxMenu boxMenu) { // TODO
            //     sortedItemStackList = sortContainer(boxMenu);
            //     // Container container = boxMenu
            //     for (int i = 0; i < 27; i++) {
            //         boxMenu.setItem(i, sortedItemStackList.get(i));
            //     }
            //     // boxEntity.setChanged();
            // }

            // Send a packet to update all slots in the container to the client
            player.connection.send(new ClientboundContainerSetContentPacket(player.containerMenu.containerId, player.containerMenu.incrementStateId(), sortedItemStackList, ItemStack.EMPTY));
        }
        else {
            sortedItemStackList = sortContainer(player.inventoryMenu);
            // player.inventoryMenu.clearCraftingContent();
            Inventory playerInventory = player.getInventory();
            // ChestInvSort.LOGGER.debug("Size of inventory items: {}", playerInventory.items.size());
            // playerInventory.items.forEach(stack -> ChestInvSort.LOGGER.debug("playerInventory.items: {}", stack.getDisplayName().getString()));
            for (int i = InventoryMenu.INV_SLOT_START; i <= InventoryMenu.INV_SLOT_END; i++) {
                playerInventory.items.set(i, sortedItemStackList.get(i));
            }
            playerInventory.setChanged();

            // Send a packet to update all slots in the container to the client
            player.connection.send(new ClientboundContainerSetContentPacket(player.inventoryMenu.containerId, player.inventoryMenu.incrementStateId(), sortedItemStackList, ItemStack.EMPTY));
        }
    }

    static NonNullList<ItemStack> sortContainer(ChestMenu containerMenu) {
        ChestInvSort.LOGGER.debug("Sorting ChestMenu!");
        NonNullList<ItemStack> chestInventory = containerMenu.getItems();
        int slotCount = 9 * containerMenu.getRowCount();
        sortItems(chestInventory, 0, slotCount - 1);
        // chestInventory.forEach(stack -> ChestInvSort.LOGGER.debug("Item: {}", stack.getDisplayName().getString()));
        // containerMenu.getContainer().setChanged();
        containerMenu.slots.forEach((slot -> slot.setChanged()));
        containerMenu.broadcastChanges();
        return chestInventory;
    }
    static NonNullList<ItemStack> sortContainer(ShulkerBoxMenu containerMenu) {
        ChestInvSort.LOGGER.debug("Sorting ShulkerBoxMenu!");
        NonNullList<ItemStack> boxInventory = containerMenu.getItems();
        int slotCount = 27; // Shulker boxes always have 27 slots
        sortItems(boxInventory, 0, slotCount - 1);
        containerMenu.slots.forEach((slot -> slot.setChanged()));
        containerMenu.broadcastChanges();
        return boxInventory;
    }
    static NonNullList<ItemStack> sortContainer(InventoryMenu inventory) {
        ChestInvSort.LOGGER.debug("Sorting InventoryMenu!");
        NonNullList<ItemStack> inventoryItems = inventory.getItems();
        // inventoryItems.forEach(stack -> ChestInvSort.LOGGER.debug("Item: {}", stack.getDisplayName().getString())); // DEBUG
        sortItems(inventoryItems, InventoryMenu.INV_SLOT_START, InventoryMenu.INV_SLOT_END - 1);
        inventory.slots.forEach((slot -> slot.setChanged()));
        inventory.broadcastChanges();
        return inventoryItems;
    }
    private static void sortItems(NonNullList<ItemStack> unsortedItems, int startSlot, int endSlot) {
        ChestInvSort.LOGGER.debug("Sorting Items!");
        List<ItemStack> nonEmptyItems = new ArrayList<>(endSlot - startSlot + 1);
        // Get all non-empty slots
        for (int i = startSlot; i <= endSlot; i++) {
            if (unsortedItems.get(i) != ItemStack.EMPTY)
                nonEmptyItems.add(unsortedItems.get(i));
            // ChestInvSort.LOGGER.debug("Item id in slot {} : {}", i, unsortedItems.get(i).getDisplayName().getString());
        }

        // Group items by their type and stack them
        Map<Item, List<ItemStack>> groupedItems = new HashMap<>();
        for (ItemStack stack : nonEmptyItems) {
            groupedItems.computeIfAbsent(stack.getItem(), k -> new ArrayList<>()).add(stack);
        }

        List<ItemStack> stackedItems = new ArrayList<>();
        for (Map.Entry<Item, List<ItemStack>> entry : groupedItems.entrySet()) {
            Item item = entry.getKey();
            List<ItemStack> itemStacks = entry.getValue();
            int maxStackSize = new ItemStack(item).getMaxStackSize();
            int totalAmount = itemStacks.stream().mapToInt(ItemStack::getCount).sum();

            while (totalAmount > 0) {
                int stackSize = Math.min(totalAmount, maxStackSize);
                stackedItems.add(new ItemStack(item, stackSize));
                totalAmount -= stackSize;
            }
        }
            
        // Sort items by type first and then by name
        stackedItems.sort(Comparator
        .comparing((ItemStack stack) -> stack.getItem() instanceof BlockItem ? 0 : 1)
        .thenComparing(stack -> stack.getDisplayName().getString()));
        
        // Populate original container with stackedItems
        int j = 0;
        for (int i = startSlot; i <= endSlot; i++) {
            if (j >= stackedItems.size())
                unsortedItems.set(i, ItemStack.EMPTY);
            else
                unsortedItems.set(i, stackedItems.get(j));
            j++;
            // ChestInvSort.LOGGER.debug("Item: {}", unsortedItems.get(i).getDisplayName().getString());
        }
        ChestInvSort.LOGGER.debug("DONE SORTING!");
    }
}
