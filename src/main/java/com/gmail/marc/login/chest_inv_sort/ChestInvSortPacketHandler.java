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
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;


public class ChestInvSortPacketHandler {
    static void handlePacket(ServerPlayer player, boolean isChest) {
        ChestInvSort.LOGGER.debug("[{}] Handling Packet @ Server!", ChestInvSort.MODID);
        List<ItemStack> sortedItemStackList = new ArrayList<>();
        if (isChest == true) {
            if (player.containerMenu instanceof ChestMenu)
                sortedItemStackList = sortContainer((ChestMenu)player.containerMenu);
            // else if (player.containerMenu instanceof HorseInventoryMenu) // TODO
            //     sortChest((HorseInventoryMenu)player.containerMenu);
            else if (player.containerMenu instanceof ShulkerBoxMenu) {
                // sortedItemStackList = sortContainer((ShulkerBoxMenu)player.containerMenu);
            }
        }
        else {
            // sortedItemStackList = sortContainer(player.inventoryMenu);
        }
        // Send a packet to update all slots in the container to the client
        NonNullList<ItemStack> unsortedItemStackList = player.containerMenu.getItems();
        for (int i = 0; i < sortedItemStackList.size(); i++) {
            unsortedItemStackList.set(i, sortedItemStackList.get(i));
        }
        unsortedItemStackList.forEach(stack -> ChestInvSort.LOGGER.debug("[{}] Item: {}", ChestInvSort.MODID, stack.getDisplayName().getString()));
        player.containerMenu.getItems().forEach(stack -> ChestInvSort.LOGGER.debug("[{}] Item: {}", ChestInvSort.MODID, stack.getDisplayName().getString()));
        player.connection.send(new ClientboundContainerSetContentPacket(player.containerMenu.containerId, player.containerMenu.incrementStateId(), unsortedItemStackList, ItemStack.EMPTY));
    }

    static NonNullList<ItemStack> sortContainer(ChestMenu containerMenu) {
        ChestInvSort.LOGGER.debug("[{}] Sorting ChestMenu!", ChestInvSort.MODID);
        
        // NonNullList<Slot> containerSlots = containerMenu.slots;
        // int slotCount = 9 * containerMenu.getRowCount();
        // sortSlots(containerSlots, 0, slotCount - 1);
        // containerMenu.broadcastChanges();
        NonNullList<ItemStack> chestInventory = containerMenu.getItems();
        int slotCount = 9 * containerMenu.getRowCount();
        sortItems(chestInventory, 0, slotCount - 1);
        chestInventory.forEach(stack -> ChestInvSort.LOGGER.debug("[{}] Item: {}", ChestInvSort.MODID, stack.getDisplayName().getString()));
        containerMenu.getContainer().setChanged();
        containerMenu.slots.forEach((slot -> slot.setChanged()));
        containerMenu.broadcastChanges();
        return chestInventory;
    }
    static void sortContainer(ShulkerBoxMenu containerMenu) {
        ChestInvSort.LOGGER.debug("[{}] Sorting ShulkerBoxMenu!", ChestInvSort.MODID);
        NonNullList<Slot> containerSlots = containerMenu.slots;
        int slotCount = 27; // Shulker boxes always have 27 slots
        sortSlots(containerSlots, 0, slotCount - 1);
        containerMenu.broadcastChanges();
    }
    static void sortContainer(InventoryMenu inventory) {
        ChestInvSort.LOGGER.debug("[{}] Sorting InventoryMenu!", ChestInvSort.MODID);
        NonNullList<Slot> inventorySlots = inventory.slots;
        // for (Slot slot : sortSlots(inventorySlots)) {
        //     LOGGER.debug("Player Inventory Item >> {}", slot.getItem().toString());
        // }
        sortSlots(inventorySlots, InventoryMenu.INV_SLOT_START, InventoryMenu.INV_SLOT_END);
        inventory.broadcastChanges();
    }

    private static void sortSlots(NonNullList<Slot> unsortedSlots, int startSlot, int endSlot) {
        
        List<ItemStack> items = new ArrayList<>(unsortedSlots.size());
        // Get all non-empty slots
        for (int i = startSlot; i <= endSlot; i++) {
            Slot slot = unsortedSlots.get(i);
            if (!slot.hasItem()) continue;
            ItemStack stack = slot.getItem();
            items.add(stack);
            // LOGGER.debug("Item id in slot {} : {}", i, stack.getDescriptionId());
        }

        // Group items by their type and stack them
        Map<Item, List<ItemStack>> groupedItems = new HashMap<>();
        for (ItemStack stack : items) {
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
        
        // Populate returnable slots with stackedItems
        List<Slot> sortedSlots = new ArrayList<>(unsortedSlots.size());

        // for (int i = 0; i < sortedSlots.size(); i++) {
        //     Slot slot = sortedSlots.get(i);
        //     // Skip all slots that are not part of the player inventory (e. g. toolbar, crafting slots, hands)
        //     if (i < InventoryMenu.INV_SLOT_START || i > InventoryMenu.INV_SLOT_END)
        //         continue;
        //     // Set all slots, that were stacked up or empty before to empty
        //     else if (i >= stackedItems.size())
        //         slot.set(ItemStack.EMPTY);
        //     else
        //         slot.set(stackedItems.get(i));
        // }

        for (int i = startSlot; i <= endSlot; i++) {
            Slot slot = sortedSlots.get(i);
            if (i >= stackedItems.size())
                slot.set(ItemStack.EMPTY);
            else
                slot.set(stackedItems.get(i));
        }
    }

    private static void sortItems(NonNullList<ItemStack> unsortedItems, int startSlot, int endSlot) {
        
        List<ItemStack> nonEmptyItems = new ArrayList<>(endSlot - startSlot + 1);
        // Get all non-empty slots
        for (int i = startSlot; i <= endSlot; i++) {
            if (unsortedItems.get(i) != ItemStack.EMPTY)
                nonEmptyItems.add(unsortedItems.get(i));
            // LOGGER.debug("Item id in slot {} : {}", i, stack.getDescriptionId());
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
        for (int i = startSlot; i <= endSlot; i++) {
            if (i >= stackedItems.size())
                unsortedItems.set(i, ItemStack.EMPTY);
            else
                unsortedItems.set(i, stackedItems.get(i));
            
            ChestInvSort.LOGGER.debug("Item: {}", unsortedItems.get(i).getDisplayName().getString());
        }
        ChestInvSort.LOGGER.debug("DONE SORTING!");
    }
}
