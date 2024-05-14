package com.gmail.marc.login.chest_inv_sort;


import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.slf4j.Logger;

import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.lwjgl.glfw.GLFW;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ChestInvSort.MODID)
public class ChestInvSort
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "chest_inv_sort";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();
    

    public ChestInvSort()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        forgeEventBus.addListener(ChestInvSort::onKeyPressed);


        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");

        LOGGER.info("SORT KEY >> {}", Config.sortKey);
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
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    private static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        // Check if the currently open GUI is a container (inventory or chest)
        LOGGER.debug("Pressed key >> {}", GLFW.glfwGetKeyName(event.getKeyCode(), event.getScanCode()));
        if (event.getScreen() instanceof ContainerScreen) {
            LOGGER.debug("Opened container >> {}", event.getScreen().getClass().getName());

            // Check if the sort inventory key is pressed (replace GLFW.GLFW_KEY_X with the desired key code)
            String pressedKey = GLFW.glfwGetKeyName(event.getKeyCode(), event.getScanCode());
            if (pressedKey != null && Config.sortKey.toLowerCase().equals(pressedKey) ) {
                LOGGER.debug("Correct key pressed! Sorting...");
                ContainerScreen containerScreen = (ContainerScreen) event.getScreen();
                LocalPlayer player = containerScreen.getMinecraft().player;
                if (player == null) return;
                
                NonNullList<Slot> slots = player.containerMenu.slots;
                List<ItemStack> items = new ArrayList<>(slots.size());
                // Get all non-empty slots
                for (int i = 0; i < slots.size(); i++) {
                    Slot slot = slots.get(i);
                    if (!slot.hasItem()) continue;
                    ItemStack stack = slot.getItem();
                    items.add(stack);
                    LOGGER.debug("Item id in slot {} : {}", i, stack.getDescriptionId());
                }
                // Sort items
                items.sort((ItemStack a, ItemStack b) -> { return a.getDisplayName().getString().compareTo(b.getDisplayName().getString()); });
                // Put sorted items back into the inventory
                for (int i = 0; i < slots.size(); i++) {
                    Slot slot = slots.get(i);
                    if (i >= items.size()) slot.set(ItemStack.EMPTY);
                    else
                        slot.set(items.get(i));
                }
            }
        }
    }
    // private void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
    //     if (event.getScreen() instanceof ContainerScreen) {
            
    //     }
    // }
}
