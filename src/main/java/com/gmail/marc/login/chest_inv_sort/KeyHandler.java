package com.gmail.marc.login.chest_inv_sort;

import org.lwjgl.glfw.GLFW;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Lazy;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.PacketDistributor;

public class KeyHandler {
    private static KeyHandler keyHandler;
    public static final Lazy<KeyMapping> SORT_MAPPING = Lazy.of(()-> new KeyMapping(
        "key." + ChestInvSort.MODID + ".sort", // Will be localized using this translation key
        KeyConflictContext.GUI, // Mapping can only be used when a screen is open
        InputConstants.Type.MOUSE, // Default mapping is on the mouse
        GLFW.GLFW_MOUSE_BUTTON_MIDDLE, // Default button is mouse middle
        "key.categories." + ChestInvSort.MODID // Mapping will be in the misc category
    ));
    public KeyHandler() {

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::registerBindings);
    }

    static void init() {
        keyHandler = new KeyHandler();
    }

    public void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(SORT_MAPPING.get());
    }
        
    @SubscribeEvent
    public void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        // Check if the currently open GUI is a container (inventory or chest)  
        boolean isChest = false;
        Screen openedScreen = event.getScreen();
        if (openedScreen instanceof ContainerScreen) {
            isChest = true;
        }
        else if (! (openedScreen instanceof InventoryScreen)) return;

        // Check if player is hovering his own player inventory (and not the chest menu)
        if (openedScreen instanceof AbstractContainerScreen containerScreen) {
            Slot hoveredSlot = containerScreen.getSlotUnderMouse();
            if (hoveredSlot != null && hoveredSlot.container instanceof Inventory)
                isChest = false; // Player is hoevering his own inventory, so sort this instead
        }

        // Check if the sort inventory key is pressed
        if (!SORT_MAPPING.get().isActiveAndMatches(InputConstants.getKey(event.getKeyCode(), event.getScanCode()))) return;
        // ChestInvSort.LOGGER.debug("Correct key pressed! Start sorting...");
        ChestInvSort.CHANNEL.send(new ChestInvSortPacket(isChest), PacketDistributor.SERVER.noArg());
    }

    @SubscribeEvent
    public void onMouseMiddleClicked(ScreenEvent.MouseButtonPressed.Pre event) {
        // Check if the currently open GUI is a container (inventory or chest)  
        boolean isChest = false;
        Screen openedScreen = event.getScreen();
        if (openedScreen instanceof ContainerScreen) {
            isChest = true;
        }
        else if (! (openedScreen instanceof InventoryScreen)) return;

        // Check if player is hovering his own player inventory (and not the chest menu)
        if (openedScreen instanceof AbstractContainerScreen containerScreen) {
            Slot hoveredSlot = containerScreen.getSlotUnderMouse();
            if (hoveredSlot != null && hoveredSlot.container instanceof Inventory)
                isChest = false; // Player is hoevering his own inventory, so sort this instead
        }

        // Check if the sort inventory button is pressed
        if (!SORT_MAPPING.get().isActiveAndMatches(InputConstants.Type.MOUSE.getOrCreate(event.getButton()))) return;
        // ChestInvSort.LOGGER.debug("Correct key pressed! Start sorting...");
        ChestInvSort.CHANNEL.send(new ChestInvSortPacket(isChest), PacketDistributor.SERVER.noArg());
    }
}
