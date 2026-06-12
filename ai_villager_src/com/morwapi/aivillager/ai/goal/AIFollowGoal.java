/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1297
 *  net.minecraft.class_1309
 *  net.minecraft.class_1314
 *  net.minecraft.class_1352
 *  net.minecraft.class_1352$class_4134
 */
package com.morwapi.aivillager.ai.goal;

import java.util.EnumSet;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1314;
import net.minecraft.class_1352;

public class AIFollowGoal
extends class_1352 {
    private final class_1314 mob;
    private class_1309 target;
    private final double speed;
    private final float minDistance;
    private final float maxDistance;
    private class_1309 externalTarget;

    public AIFollowGoal(class_1314 mob, double speed, float minDistance, float maxDistance) {
        this.mob = mob;
        this.speed = speed;
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        this.method_6265(EnumSet.of(class_1352.class_4134.field_18405, class_1352.class_4134.field_18406));
    }

    public void setFollowTarget(class_1309 target) {
        this.externalTarget = target;
    }

    public class_1309 getFollowTarget() {
        return this.externalTarget;
    }

    public boolean method_6264() {
        if (this.externalTarget == null) {
            return false;
        }
        if (!this.externalTarget.method_5805()) {
            this.externalTarget = null;
            return false;
        }
        double sqDist = this.mob.method_5858((class_1297)this.externalTarget);
        return sqDist > (double)(this.minDistance * this.minDistance);
    }

    public boolean method_6266() {
        return this.externalTarget != null && this.externalTarget.method_5805() && this.mob.method_5858((class_1297)this.externalTarget) > (double)(this.minDistance * this.minDistance);
    }

    public void method_6269() {
        this.target = this.externalTarget;
    }

    public void method_6270() {
        this.target = null;
        this.mob.method_5942().method_6340();
    }

    public void method_6268() {
        if (this.target != null) {
            this.mob.method_5988().method_6226((class_1297)this.target, 10.0f, (float)this.mob.method_5978());
            if (--this.mob.field_6012 % 10 == 0) {
                this.mob.method_5942().method_6335((class_1297)this.target, this.speed);
            }
        }
    }
}

