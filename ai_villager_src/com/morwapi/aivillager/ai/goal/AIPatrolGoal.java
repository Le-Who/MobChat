/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1314
 *  net.minecraft.class_1352
 *  net.minecraft.class_1352$class_4134
 *  net.minecraft.class_2338
 *  net.minecraft.class_2382
 *  net.minecraft.class_243
 *  net.minecraft.class_5532
 */
package com.morwapi.aivillager.ai.goal;

import java.util.EnumSet;
import net.minecraft.class_1314;
import net.minecraft.class_1352;
import net.minecraft.class_2338;
import net.minecraft.class_2382;
import net.minecraft.class_243;
import net.minecraft.class_5532;

public class AIPatrolGoal
extends class_1352 {
    private final class_1314 mob;
    private final double speed;
    private final int radius;
    private boolean patrolling;
    private class_2338 anchorPos;
    private int idleTicks;

    public AIPatrolGoal(class_1314 mob, double speed, int radius) {
        this.mob = mob;
        this.speed = speed;
        this.radius = radius;
        this.method_6265(EnumSet.of(class_1352.class_4134.field_18405));
    }

    public void setPatrolling(boolean patrolling) {
        this.patrolling = patrolling;
        if (patrolling) {
            this.anchorPos = this.mob.method_24515();
        } else {
            this.anchorPos = null;
            this.mob.method_5942().method_6340();
        }
    }

    public boolean isPatrolling() {
        return this.patrolling;
    }

    public boolean method_6264() {
        return this.patrolling;
    }

    public boolean method_6266() {
        return this.patrolling;
    }

    public void method_6268() {
        if (!this.patrolling) {
            return;
        }
        if (this.mob.method_5942().method_6357()) {
            if (this.idleTicks > 0) {
                --this.idleTicks;
                return;
            }
            class_243 target = null;
            if (this.anchorPos != null) {
                target = this.mob.method_5649((double)this.anchorPos.method_10263(), (double)this.anchorPos.method_10264(), (double)this.anchorPos.method_10260()) > (double)(this.radius * this.radius) ? this.mob.method_19538().method_1019(class_243.method_24953((class_2382)this.anchorPos).method_1020(this.mob.method_19538()).method_1029().method_1021(5.0)) : class_5532.method_31510((class_1314)this.mob, (int)10, (int)7);
            } else {
                this.anchorPos = this.mob.method_24515();
            }
            if (target != null) {
                this.mob.method_5942().method_6337(target.field_1352, target.field_1351, target.field_1350, this.speed);
                this.idleTicks = 40 + this.mob.method_6051().method_43048(60);
            }
        }
    }

    public void method_6270() {
        super.method_6270();
    }
}

