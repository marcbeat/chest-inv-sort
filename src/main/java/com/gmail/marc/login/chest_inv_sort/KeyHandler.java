package com.gmail.marc.login.chest_inv_sort;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.network.PacketDistributor;

public class KeyHandler {
    private static KeyHandler keyHandler;
    public KeyHandler() {

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
    }

    static void init() {
        keyHandler = new KeyHandler();
    }
    
    @SubscribeEvent
    public void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        // Check if the currently open GUI is a container (inventory or chest)  
        boolean isChest = false;
        Screen openedScreen = event.getScreen();
        if (openedScreen instanceof ContainerScreen) {
            ContainerScreen containerScreen = (ContainerScreen) openedScreen;
            isChest = true;
            ChestInvSort.LOGGER.debug("Container is {}! Checking pressed key...", containerScreen.getMenu().getClass().getSimpleName());
        }
        else if (openedScreen instanceof InventoryScreen) {
            InventoryScreen inventoryScreen = (InventoryScreen) openedScreen;
            ChestInvSort.LOGGER.debug("Container is {}! Checking pressed key...", inventoryScreen.getMenu().getClass().getSimpleName());
        }
        else return;

        // Check if the sort inventory key is pressed (defined via config)
        String pressedKey = GLFW.glfwGetKeyName(event.getKeyCode(), event.getScanCode());
        ChestInvSort.LOGGER.debug("Pressed key >> {}" , GLFW.glfwGetKeyName(event.getKeyCode(), event.getScanCode()));
        if (! ( pressedKey != null && Config.sortKey.toLowerCase().equals(pressedKey) ) ) return;
        
        ChestInvSort.LOGGER.debug("Correct key pressed! Start sorting...");
        ChestInvSort.CHANNEL.send(new ChestInvSortPacket(isChest), PacketDistributor.SERVER.noArg());
    }
}
