// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.mixin;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.MobInteractionHooks;
import com.lewho.inventory.ChatInventory;
import com.lewho.inventory.MobInventoryMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The {@code MixinMobEntity} mixin class exposes the goalSelector field from the MobEntity class.
 */
@Mixin(Mob.class)
public class MixinMobEntity implements ChatInventory, HasCustomInventoryScreen {

    private final SimpleContainer creaturechat$inventory = new SimpleContainer(15);

    @Override
    public SimpleContainer creaturechat$getInventory() {
        return creaturechat$inventory;
    }

    @Override
    public void openCustomInventoryScreen(Player player) {
        Mob thisEntity = (Mob) (Object) this;
        if (thisEntity instanceof Villager || thisEntity instanceof TamableAnimal) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ExtendedScreenHandlerFactory provider = new ExtendedScreenHandlerFactory() {
                @Override
                public void writeScreenOpeningData(ServerPlayer p, FriendlyByteBuf buf) {
                    buf.writeVarInt(thisEntity.getId());
                }

                @Override
                public Component getDisplayName() {
                    return thisEntity.getDisplayName();
                }

                @Override
                public AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player p) {
                    return new MobInventoryMenu(syncId, playerInventory, creaturechat$inventory, thisEntity, serverPlayer);
                }
            };
            serverPlayer.openMenu(provider);
        }
    }

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void creaturechat$openInventory(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.level().isClientSide()) {
            return;
        }

        if (hand != InteractionHand.MAIN_HAND) {
            return;
        }

        if (!player.isSecondaryUseActive()) {
            return;
        }

        Mob thisEntity = (Mob) (Object) this;

        if (thisEntity instanceof Villager || thisEntity instanceof TamableAnimal) {
            return;
        }

        // Only open the inventory if chat data exists and has been used
        EntityChatData chatData = ChatDataManager.getServerInstance().entityChatDataMap.get(thisEntity.getStringUUID());
        if (chatData == null || chatData.status == ChatDataManager.ChatStatus.NONE) {
            return;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            ExtendedScreenHandlerFactory provider = new ExtendedScreenHandlerFactory() {
                @Override
                public void writeScreenOpeningData(ServerPlayer p, FriendlyByteBuf buf) {
                    buf.writeVarInt(thisEntity.getId());
                }

                @Override
                public Component getDisplayName() {
                    return thisEntity.getDisplayName();
                }

                @Override
                public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player p) {
                    return new MobInventoryMenu(syncId, playerInventory, creaturechat$inventory, thisEntity, serverPlayer);
                }
            };
            serverPlayer.openMenu(provider);
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("RETURN"))
    private void creaturechat$saveInventory(CompoundTag tag, CallbackInfo ci) {
        ListTag listTag = new ListTag();
        for (int i = 0; i < creaturechat$inventory.getContainerSize(); i++) {
            ItemStack stack = creaturechat$inventory.getItem(i);
            if (!stack.isEmpty()) {
                CompoundTag wrapper = new CompoundTag();
                wrapper.putByte("Slot", (byte) i);

                CompoundTag itemTag = new CompoundTag();
                stack.save(itemTag);
                wrapper.put("Item", itemTag);

                listTag.add(wrapper);
            }
        }
        tag.put("CreatureChatInventory", listTag);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("RETURN"))
    private void creaturechat$loadInventory(CompoundTag tag, CallbackInfo ci) {
        ListTag listTag = tag.getList("CreatureChatInventory", 10);
        for (int i = 0; i < listTag.size(); ++i) {
            CompoundTag wrapper = listTag.getCompound(i);
            int slot = wrapper.getByte("Slot") & 255;
            if (slot >= 0 && slot < creaturechat$inventory.getContainerSize()) {
                CompoundTag itemTag;
                if (wrapper.contains("Item", 10)) {
                    itemTag = wrapper.getCompound("Item");
                } else {
                    itemTag = wrapper.copy();
                    itemTag.remove("Slot");
                }
                creaturechat$inventory.setItem(slot, ItemStack.of(itemTag));
            }
        }
    }

    @Inject(method = "interact", at = @At(value = "RETURN"))
    private void onItemGiven(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        MobInteractionHooks.onItemGiven((Mob) (Object) this, player, hand, cir.getReturnValue());
    }
}
