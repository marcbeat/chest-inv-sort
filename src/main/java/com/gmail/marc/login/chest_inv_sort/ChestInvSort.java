package com.gmail.marc.login.chest_inv_sort;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

import org.slf4j.Logger;
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
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
        }
    }
}
