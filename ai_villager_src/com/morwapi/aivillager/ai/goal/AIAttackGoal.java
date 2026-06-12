/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1309
 *  net.minecraft.class_1314
 *  net.minecraft.class_1366
 */
package com.morwapi.aivillager.ai.goal;

import net.minecraft.class_1309;
import net.minecraft.class_1314;
import net.minecraft.class_1366;

public class AIAttackGoal
extends class_1366 {
    private final class_1314 mob;
    private class_1309 target;

    public AIAttackGoal(class_1314 mob, double speed, boolean pauseWhenMobIdle) {
        super(mob, speed, pauseWhenMobIdle);
        this.mob = mob;
    }

    public void setAttackTarget(class_1309 target) {
        this.target = target;
        this.mob.method_5980(target);
    }

    public boolean method_6264() {
        return this.target != null && this.target.method_5805();
    }

    public boolean method_6266() {
        return super.method_6266() && this.target != null && this.target.method_5805();
    }

    public void method_6270() {
        super.method_6270();
        this.target = null;
        this.mob.method_5980(null);
    }
}

