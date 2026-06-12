/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.systems.RenderSystem
 *  net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
 *  net.fabricmc.fabric.api.networking.v1.PacketByteBufs
 *  net.minecraft.class_1661
 *  net.minecraft.class_1703
 *  net.minecraft.class_2540
 *  net.minecraft.class_2561
 *  net.minecraft.class_2960
 *  net.minecraft.class_332
 *  net.minecraft.class_342
 *  net.minecraft.class_364
 *  net.minecraft.class_4185
 *  net.minecraft.class_465
 *  net.minecraft.class_5348
 *  net.minecraft.class_757
 */
package com.morwapi.aivillager.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.morwapi.aivillager.networking.ModMessages;
import com.morwapi.aivillager.screen.VillagerControlScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.class_1661;
import net.minecraft.class_1703;
import net.minecraft.class_2540;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_364;
import net.minecraft.class_4185;
import net.minecraft.class_465;
import net.minecraft.class_5348;
import net.minecraft.class_757;

public class VillagerControlScreen
extends class_465<VillagerControlScreenHandler> {
    private static final class_2960 TEXTURE = new class_2960("minecraft", "textures/gui/container/generic_54.png");
    private class_342 instructionField;
    private final int villagerId;

    public VillagerControlScreen(VillagerControlScreenHandler handler, class_1661 inventory, class_2561 title) {
        super((class_1703)handler, inventory, title);
        this.field_2779 = 172;
        this.field_25270 = 73;
        this.villagerId = handler.getVillagerId();
    }

    protected void method_25426() {
        super.method_25426();
        this.field_25267 = (this.field_2792 - this.field_22793.method_27525((class_5348)this.field_22785)) / 2;
        int x = this.field_2776 + 8;
        int y = this.field_2800 + 18 + 18 + 5;
        this.instructionField = new class_342(this.field_22793, x, y, 100, 20, class_2561.method_30163((String)"Instruction"));
        this.instructionField.method_1880(256);
        this.method_37063((class_364)this.instructionField);
        this.method_37063((class_364)class_4185.method_46430((class_2561)class_2561.method_30163((String)"Send"), button -> {
            this.sendInstruction(this.instructionField.method_1882());
            this.method_25419();
        }).method_46434(x + 105, y, 50, 20).method_46431());
    }

    public void method_25394(class_332 context, int mouseX, int mouseY, float delta) {
        this.method_25420(context);
        super.method_25394(context, mouseX, mouseY, delta);
        this.method_2380(context, mouseX, mouseY);
    }

    protected void method_2389(class_332 context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(class_757::method_34542);
        RenderSystem.setShaderColor((float)1.0f, (float)1.0f, (float)1.0f, (float)1.0f);
        RenderSystem.setShaderTexture((int)0, (class_2960)TEXTURE);
        int x = (this.field_22789 - this.field_2792) / 2;
        int y = (this.field_22790 - this.field_2779) / 2;
        context.method_25302(TEXTURE, x, y, 0, 0, this.field_2792, 17);
        context.method_25302(TEXTURE, x, y + 17, 0, 17, 7, 18);
        context.method_25302(TEXTURE, x + this.field_2792 - 7, y + 17, 169, 17, 7, 18);
        context.method_25294(x + 7, y + 17, x + this.field_2792 - 7, y + 35, -3750202);
        for (int i = 0; i < 8; ++i) {
            context.method_25302(TEXTURE, x + 17 + i * 18, y + 17, 7, 17, 18, 18);
        }
        int gapY = y + 35;
        int gapHeight = this.field_2779 - 96 - 35;
        context.method_25302(TEXTURE, x, gapY, 0, 35, 7, gapHeight);
        context.method_25302(TEXTURE, x + this.field_2792 - 7, gapY, 169, 35, 7, gapHeight);
        context.method_25294(x + 7, gapY, x + this.field_2792 - 7, gapY + gapHeight, -3750202);
        context.method_25302(TEXTURE, x, y + this.field_2779 - 96, 0, 126, this.field_2792, 96);
    }

    public boolean method_25404(int keyCode, int scanCode, int modifiers) {
        if (this.instructionField.method_20315() && this.field_22787.field_1690.field_1822.method_1417(keyCode, scanCode)) {
            return true;
        }
        if (this.instructionField.method_25404(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.method_25404(keyCode, scanCode, modifiers);
    }

    private void sendInstruction(String instruction) {
        if (this.villagerId == -1) {
            return;
        }
        class_2540 buf = PacketByteBufs.create();
        buf.writeInt(this.villagerId);
        buf.method_10814(instruction);
        ClientPlayNetworking.send((class_2960)ModMessages.VILLAGER_INSTRUCTION_ID, (class_2540)buf);
    }
}

