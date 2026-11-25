// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.inventory;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.PlayerData;
import com.lewho.chat.SocialEventRecorder;
import com.lewho.chat.SocialEventType;
import com.lewho.chat.AdvancementHelper;
import com.lewho.network.ServerPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MobInventoryMenu extends MobInventoryTransferMenu {
    private final Container inventory;
    private final Mob mob;
    private final ServerPlayer serverPlayer;
    private final PlayerData playerData;
    private final MobInventorySnapshot initialInventory;
    private final int mobInvSize;
    private final int rows;
    private final int mainHandSlot;
    private final int offHandSlot;

    public MobInventoryMenu(int syncId, Inventory playerInventory, Container inventory, Mob mob, ServerPlayer player) {
        super(ModMenus.MOB_INVENTORY, syncId);
        this.inventory = inventory;
        this.mob = mob;
        this.serverPlayer = player;
        ChatDataManager chatDataManager = player == null ? ChatDataManager.getClientInstance() : ChatDataManager.getServerInstance();
        EntityChatData chatData = chatDataManager.getOrCreateChatData(mob.getStringUUID());
        if (player != null) {
            this.playerData = chatData.getPlayerData(player);
        } else if (playerInventory.player != null) {
            this.playerData = chatData.getPlayerData(playerInventory.player.getStringUUID(), playerInventory.player.getDisplayName().getString());
        } else {
            this.playerData = chatData.getPlayerData("");
        }
        if (player != null && playerData.friendship > 0 && !playerData.openedInventory) {
            playerData.openedInventory = true;
            AdvancementHelper.openSesame(player);
        }
        if (playerData.wordsmithActive) {
            playerData.wordsmithOpenedInventory = true;
        }
        this.mobInvSize = inventory.getContainerSize();
        this.rows = (mobInvSize + 4) / 5;
        int bottomRowStart = (rows - 1) * 5;
        this.mainHandSlot = bottomRowStart;
        this.offHandSlot = bottomRowStart + 1;
        ItemStack mainHand = mob.getMainHandItem();
        ItemStack offHand = mob.getOffhandItem();
        syncHandSlots(inventory, mainHandSlot, offHandSlot, mainHand, offHand, this::dropDisplacedHandStack);
        this.initialInventory = new MobInventorySnapshot(inventory, mainHandSlot, offHandSlot, mainHand, offHand);
        inventory.startOpen(playerInventory.player);

        int slot = 0;
        boolean canAccessInventory = playerData.friendship > 0;
        boolean canAccessHands = playerData.friendship >= 3;
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < 5 && slot < mobInvSize; ++col) {
                int x = 75 + col * 18;
                int y = 18 + row * 18;
                if (slot == mainHandSlot) {
                    this.addSlot(canAccessHands ? new MobHandSlot(inventory, slot++, x, y, mob::canHoldItem,
                                                               stack -> mob.setItemSlot(EquipmentSlot.MAINHAND, stack))
                                               : new LockedSlot(inventory, slot++, x, y));
                } else if (slot == offHandSlot) {
                    this.addSlot(canAccessHands ? new MobHandSlot(inventory, slot++, x, y, mob::canHoldItem,
                                                               stack -> mob.setItemSlot(EquipmentSlot.OFFHAND, stack))
                                               : new LockedSlot(inventory, slot++, x, y));
                } else {
                    this.addSlot(canAccessInventory ? new Slot(inventory, slot++, x, y)
                                                    : new LockedSlot(inventory, slot++, x, y));
                }
            }
        }

        int startY = 18 + rows * 18 + 14 - 2;
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, startY + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, startY + 58));
        }
    }

    public Mob getMob() { return mob; }

    public int getRows() { return rows; }

    @Override
    public boolean stillValid(Player player) {
        return inventory.stillValid(player) && mob.isAlive() && !mob.isRemoved()
                && player.level() == mob.level() && player.distanceToSqr(mob) < 64.0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            if (!slot.mayPickup(player)) {
                return ItemStack.EMPTY;
            }
            ItemStack stackInSlot = slot.getItem();
            itemStack = stackInSlot.copy();
            if (index < mobInvSize) {
                if (!this.moveItemStackTo(stackInSlot, mobInvSize, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stackInSlot, 0, mobInvSize, false)) {
                return ItemStack.EMPTY;
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
            slot.onTake(player, stackInSlot);
        }
        return itemStack;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        inventory.stopOpen(player);
        if (!player.level().isClientSide() && player == serverPlayer) {
            if (playerData.friendship <= 0) {
                return;
            }
            ItemStack finalMain = mob.getMainHandItem();
            ItemStack finalOff = mob.getOffhandItem();
            MobInventorySnapshot current = new MobInventorySnapshot(inventory, mainHandSlot, offHandSlot, finalMain, finalOff);
            MobInventorySnapshot.Changes changes = initialInventory.changesTo(current);
            var added = changes.added();
            var removed = changes.removed();
            List<String> disarmedToInventory = changes.disarmedToInventory();
            List<String> disarmedTaken = changes.disarmedTaken();
            boolean swapped = changes.handsSwapped();
            boolean handChanged = changes.handChanged();
            if (!added.isEmpty() || !removed.isEmpty() || !disarmedToInventory.isEmpty() || !disarmedTaken.isEmpty() || swapped || handChanged) {
                ChatDataManager chatDataManager = ChatDataManager.getServerInstance();
                EntityChatData chatData = chatDataManager.getOrCreateChatData(mob.getStringUUID());
                PlayerData pd = player instanceof ServerPlayer sp
                        ? chatData.getPlayerData(sp)
                        : chatData.getPlayerData(player.getStringUUID(), player.getDisplayName().getString());
                if (pd.wordsmithActive) {
                    pd.wordsmithGaveItem = true;
                }
                String verbBase = pd.friendship >= 3 ? "borrowed" : pd.friendship == 2 ? "took" : "stole";
                String verb = " " + verbBase + " ";
                if (recordTakenItems(chatData, serverPlayer.getStringUUID(), player.getDisplayName().getString(),
                        removed, disarmedTaken)) {
                    AdvancementHelper.itemTaken(serverPlayer, pd);
                    if (changes.diamondsTaken()) {
                        AdvancementHelper.theHeist(serverPlayer);
                    }
                }
                if (!added.isEmpty() && pd.friendship > 0) {
                    SocialEventRecorder.record(chatData, serverPlayer, SocialEventType.GIFT_GIVEN, "Player gave items to inventory.");
                    pd.gaveItem = true;
                    AdvancementHelper.checkSharedStash(serverPlayer);
                }
                if (handChanged && pd.friendship == 3) {
                    AdvancementHelper.sleightOfHand(serverPlayer);
                }
                if (mob.getType() == net.minecraft.world.entity.EntityType.PIG && pd.friendship == 3 && pd.pigProtect &&
                        (finalMain.getItem() == Items.DIAMOND_SWORD || finalMain.getItem() == Items.NETHERITE_SWORD ||
                         finalOff.getItem() == Items.DIAMOND_SWORD || finalOff.getItem() == Items.NETHERITE_SWORD)) {
                    AdvancementHelper.aLegend(serverPlayer);
                    pd.pigProtect = false;
                }
                if (mob.getType() == net.minecraft.world.entity.EntityType.PIG && pd.friendship == 3) {
                    boolean allPotatoes = true;
                    for (int i = 0; i < mobInvSize; i++) {
                        ItemStack stack = inventory.getItem(i);
                        if (stack.isEmpty() || stack.getItem() != Items.POTATO || stack.getCount() < stack.getMaxStackSize()) {
                            allPotatoes = false;
                            break;
                        }
                    }
                    if (allPotatoes) {
                        AdvancementHelper.potatoWar(serverPlayer);
                    }
                }
                StringBuilder msg = new StringBuilder("<" + player.getDisplayName().getString());
                boolean first = true;
                if (swapped) {
                    msg.append(" swapped the items in your hands");
                    first = false;
                } else if (handChanged) {
                    msg.append(" swapped your held items to ").append(formatHands(finalMain, finalOff));
                    first = false;
                }
                if (!added.isEmpty()) {
                    if (!first) { msg.append(", and"); }
                    msg.append(" gave you ").append(joinCounts(added));
                    first = false;
                }
                if (!removed.isEmpty()) {
                    if (!first) { msg.append(", and"); }
                    msg.append(verb).append(joinCounts(removed));
                    first = false;
                }
                if (!disarmedToInventory.isEmpty()) {
                    if (!first) { msg.append(", and"); }
                    msg.append(" disarmed you and placed ").append(String.join(", ", disarmedToInventory)).append(" into your inventory");
                    first = false;
                }
                if (!disarmedTaken.isEmpty()) {
                    if (!first) { msg.append(", and"); }
                    msg.append(" disarmed you and ").append(verbBase).append(" ").append(String.join(", ", disarmedTaken));
                }
                msg.append(">");
                ServerPackets.generate_chat("N/A", chatData, serverPlayer, mob, msg.toString(), true);
            }
        }
    }

    static void syncHandSlots(Container inventory, int mainHandSlot, int offHandSlot,
                              ItemStack mainHand, ItemStack offHand, Consumer<ItemStack> drop) {
        syncHandSlot(inventory, mainHandSlot, offHandSlot, mainHand, drop);
        syncHandSlot(inventory, offHandSlot, mainHandSlot, offHand, drop);
    }

    private static void syncHandSlot(Container inventory, int slot, int otherHandSlot,
                                     ItemStack handStack, Consumer<ItemStack> drop) {
        if (slot < 0 || slot >= inventory.getContainerSize()) {
            return;
        }
        ItemStack current = inventory.getItem(slot);
        boolean knownMirror = inventory instanceof MobInventory mobInventory && mobInventory.isHandMirror(slot);
        if (!current.isEmpty() && !knownMirror && !ItemStackVariant.matches(current, handStack)) {
            inventory.setItem(slot, ItemStack.EMPTY);
            int emptySlot = findEmptyInventorySlot(inventory, slot, otherHandSlot);
            if (emptySlot >= 0) {
                inventory.setItem(emptySlot, current);
            } else {
                drop.accept(current);
            }
        }
        inventory.setItem(slot, handStack.copy());
        if (inventory instanceof MobInventory mobInventory) {
            mobInventory.markHandMirror(slot);
        }
    }

    private void dropDisplacedHandStack(ItemStack stack) {
        if (mob.level() instanceof ServerLevel serverLevel) {
            ItemEntity item = new ItemEntity(serverLevel, mob.getX(), mob.getY(), mob.getZ(), stack);
            item.setDefaultPickUpDelay();
            serverLevel.addFreshEntity(item);
        }
    }

    private static int findEmptyInventorySlot(Container inventory, int handSlot, int otherHandSlot) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (i != handSlot && i != otherHandSlot && inventory.getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private static String joinCounts(Map<MobInventorySnapshot.Variant, Integer> map) {
        List<String> parts = new ArrayList<>();
        map.forEach((variant, count) -> parts.add(variant.format(count, true)));
        return String.join(", ", parts);
    }

    private static String formatHands(ItemStack main, ItemStack off) {
        String mainName = main.isEmpty() ? "" : main.getHoverName().getString();
        String offName = off.isEmpty() ? "" : off.getHoverName().getString();
        if (!mainName.isEmpty() && !offName.isEmpty()) {
            return mainName + " and " + offName;
        }
        return !mainName.isEmpty() ? mainName : offName;
    }

    public static boolean recordTakenItems(EntityChatData data, String playerId, String playerName,
                                           Map<?, Integer> removed, List<String> disarmedTaken) {
        if (removed.isEmpty() && disarmedTaken.isEmpty()) {
            return false;
        }
        SocialEventRecorder.record(data, playerId, playerName, SocialEventType.ITEM_TAKEN,
                "Player took items from inventory.");
        return true;
    }

    static class LockedSlot extends Slot {
        LockedSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
