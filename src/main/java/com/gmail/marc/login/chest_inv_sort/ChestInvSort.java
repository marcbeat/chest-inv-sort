package com.gmail.marc.login.chest_inv_sort;

import com.mojang.logging.LogUtils;

import net.minecraftforge.api.distmarker.Dist;
// import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;

import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
// import net.minecraftforge.items.wrapper.PlayerInvWrapper;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

import org.slf4j.Logger;

import net.minecraft.resources.ResourceLocation;
// import net.minecraft.client.player.LocalPlayer;
// import net.minecraft.world.Container;
// import net.minecraft.world.entity.player.Inventory;
// import net.minecraft.world.inventory.AbstractContainerMenu;
// import net.minecraft.world.inventory.ChestMenu;
// import net.minecraft.world.inventory.InventoryMenu;
// import net.minecraft.world.inventory.Slot;
// import net.minecraft.world.item.BlockItem;
// import net.minecraft.world.item.Item;
// import net.minecraft.world.item.ItemStack;

// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.List;
// import java.util.Map;
// import java.util.Comparator;
import java.util.Objects;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ChestInvSort.MODID)
public class ChestInvSort
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "chestinvsort";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final SimpleChannel CHANNEL = ChannelBuilder
        .named(new ResourceLocation(MODID, "main"))
        .clientAcceptedVersions((status, version) -> Objects.equals(version, 1))
        .serverAcceptedVersions((status, version) -> Objects.equals(version, 1))
        .networkProtocolVersion(1)
        .simpleChannel();


    public ChestInvSort()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        DistExecutor.safeRunWhenOn(Dist.CLIENT, ()->KeyHandler::init);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        LOGGER.info("SORT KEY >> {}", Config.sortKey);

        // int packetId = 0;
        CHANNEL.messageBuilder(ChestInvSortPacket.class, NetworkDirection.PLAY_TO_SERVER)
        .decoder(ChestInvSortPacket::fromBytes)
        .encoder(ChestInvSortPacket::toBytes)
        .consumerMainThread(ChestInvSortPacket::handle)
        .add();
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("SORT KEY >> {}", Config.sortKey);
        }
    }

    // private static void sortContainer(Container container) {
    //     List<ItemStack> items = new ArrayList<>();
        
    //     // Collect all non-empty items
    //     for (int i = 0; i < container.getContainerSize(); i++) {
    //         ItemStack stack = container.getItem(i);
    //         if (!stack.isEmpty()) {
    //             items.add(stack.copy());
    //         }
    //     }

    //     // Group items by their type and stack them
    //     Map<Item, List<ItemStack>> groupedItems = new HashMap<>();
    //     for (ItemStack stack : items) {
    //         groupedItems.computeIfAbsent(stack.getItem(), k -> new ArrayList<>()).add(stack);
    //     }

    //     List<ItemStack> stackedItems = new ArrayList<>();
    //     for (Map.Entry<Item, List<ItemStack>> entry : groupedItems.entrySet()) {
    //         Item item = entry.getKey();
    //         List<ItemStack> itemStacks = entry.getValue();
    //         int maxStackSize = new ItemStack(item).getMaxStackSize();
    //         int totalAmount = itemStacks.stream().mapToInt(ItemStack::getCount).sum();

    //         while (totalAmount > 0) {
    //             int stackSize = Math.min(totalAmount, maxStackSize);
    //             stackedItems.add(new ItemStack(item, stackSize));
    //             totalAmount -= stackSize;
    //         }
    //     }

    //     // Sort items by type first and then by name
    //     stackedItems.sort(Comparator
    //     .comparing((ItemStack stack) -> stack.getItem() instanceof BlockItem ? 0 : 1)
    //     .thenComparing(stack -> stack.getDisplayName().getString()));

    //     // Clear the inventory
    //     for (int i = 0; i < container.getContainerSize(); i++) {
    //         container.setItem(i, ItemStack.EMPTY);
    //     }

    //     // Put sorted items back into the inventory
    //     int index = 0;
    //     for (ItemStack stack : items) {
    //         container.setItem(index++, stack);
    //     }

    //     // Notify the container that its contents have changed
    //     container.setChanged();
    // }
}
