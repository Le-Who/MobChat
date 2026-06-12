/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1263
 *  net.minecraft.class_1277
 *  net.minecraft.class_1657
 *  net.minecraft.class_1661
 *  net.minecraft.class_1703
 *  net.minecraft.class_1735
 *  net.minecraft.class_1799
 *  net.minecraft.class_2540
 */
package com.morwapi.aivillager.screen;

import com.morwapi.aivillager.AIVillagerMod;
import net.minecraft.class_1263;
import net.minecraft.class_1277;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_1735;
import net.minecraft.class_1799;
import net.minecraft.class_2540;

public class VillagerControlScreenHandler
extends class_1703 {
    private final class_1263 inventory;
    private final int villagerId;

    public VillagerControlScreenHandler(int syncId, class_1661 playerInventory, class_2540 buf) {
        super(AIVillagerMod.VILLAGER_CONTROL_SCREEN_HANDLER, syncId);
        this.inventory = new class_1277(8);
        this.villagerId = buf.readInt();
        this.inventory.method_5435(playerInventory.field_7546);
        this.initSlots(playerInventory);
    }

    public VillagerControlScreenHandler(int syncId, class_1661 playerInventory, class_1263 inventory) {
        super(AIVillagerMod.VILLAGER_CONTROL_SCREEN_HANDLER, syncId);
        VillagerControlScreenHandler.method_17359((class_1263)inventory, (int)8);
        this.inventory = inventory;
        this.villagerId = -1;
        inventory.method_5435(playerInventory.field_7546);
        this.initSlots(playerInventory);
    }

    private void initSlots(class_1661 playerInventory) {
        int startX = 18;
        for (int i = 0; i < 8; ++i) {
            this.method_7621(new class_1735(this.inventory, i, startX + i * 18, 18));
        }
        int playerInvY = 90;
        for (int r = 0; r < 3; ++r) {
            for (int c = 0; c < 9; ++c) {
                this.method_7621(new class_1735((class_1263)playerInventory, c + r * 9 + 9, 8 + c * 18, playerInvY + r * 18));
            }
        }
        for (int i = 0; i < 9; ++i) {
            this.method_7621(new class_1735((class_1263)playerInventory, i, 8 + i * 18, playerInvY + 58));
        }
    }

    public int getVillagerId() {
        return this.villagerId;
    }

    public boolean method_7597(class_1657 player) {
        return this.inventory.method_5443(player);
    }

    public class_1799 method_7601(class_1657 player, int invSlot) {
        class_1799 newStack = class_1799.field_8037;
        class_1735 slot = (class_1735)this.field_7761.get(invSlot);
        if (slot != null && slot.method_7681()) {
            class_1799 originalStack = slot.method_7677();
            newStack = originalStack.method_7972();
            if (invSlot < this.inventory.method_5439() ? !this.method_7616(originalStack, this.inventory.method_5439(), this.field_7761.size(), true) : !this.method_7616(originalStack, 0, this.inventory.method_5439(), false)) {
                return class_1799.field_8037;
            }
            if (originalStack.method_7960()) {
                slot.method_48931(class_1799.field_8037);
            } else {
                slot.method_7668();
            }
        }
        return newStack;
    }
}

