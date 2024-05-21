package com.gmail.marc.login.chest_inv_sort;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.network.CustomPayloadEvent;

public class ChestInvSortPacket {
    boolean isChest;
    public ChestInvSortPacket(boolean isChest) {
        this.isChest = isChest;
    }

    static ChestInvSortPacket fromBytes(FriendlyByteBuf buf) {
        // Deserialize data from buffer
        return new ChestInvSortPacket(buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        // Serialize data to buffer
        buf.writeBoolean(isChest);
    }

    public static void handle(ChestInvSortPacket msg, CustomPayloadEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            // Handle packet on the server side
            ServerPlayer player = ctx.getSender();
            ChestInvSort.LOGGER.debug("[{}] Received Packet @ Server!", ChestInvSort.MODID);
            if (player != null) {
                ChestInvSort.LOGGER.debug("[{}] Sender of packet is player '{}'!", ChestInvSort.MODID, player.getName());
                ChestInvSortPacketHandler.handlePacket(player, msg.isChest);
            }
        });
        ctx.setPacketHandled(true);
    }

}
