// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.mixin;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.inventory.ChatInventory;
import com.lewho.inventory.MobInventoryMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinOnChat {

    @Inject(method = "handlePlayerCommand", at = @At("HEAD"), cancellable = true)
    private void creaturechat$openMobInventory(ServerboundPlayerCommandPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ServerboundPlayerCommandPacket.Action.OPEN_INVENTORY) {
            return;
        }

        ServerGamePacketListenerImpl handler = (ServerGamePacketListenerImpl) (Object) this;
        ServerPlayer serverPlayer = handler.player;
        MinecraftServer server = serverPlayer.getServer();
        if (server == null || !server.isSameThread()) {
            return;
        }

        if (!(serverPlayer.getVehicle() instanceof Mob mob)) {
            return;
        }

        if (mob instanceof Villager || mob instanceof TamableAnimal) {
            return;
        }

        EntityChatData chatData = ChatDataManager.getServerInstance().entityChatDataMap.get(mob.getStringUUID());
        if (chatData == null || chatData.status == ChatDataManager.ChatStatus.NONE) {
            return;
        }

        ExtendedScreenHandlerFactory provider = new ExtendedScreenHandlerFactory() {
            @Override
            public void writeScreenOpeningData(ServerPlayer p, FriendlyByteBuf buf) {
                buf.writeVarInt(mob.getId());
            }

            @Override
            public Component getDisplayName() {
                return mob.getDisplayName();
            }

            @Override
            public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player p) {
                return new MobInventoryMenu(syncId, playerInventory, ((ChatInventory) mob).creaturechat$getInventory(), mob, serverPlayer);
            }
        };

        serverPlayer.openMenu(provider);
        ci.cancel();
    }
}
