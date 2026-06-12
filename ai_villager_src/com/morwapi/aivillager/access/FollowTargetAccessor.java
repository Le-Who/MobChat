/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1309
 *  net.minecraft.class_1799
 */
package com.morwapi.aivillager.access;

import net.minecraft.class_1309;
import net.minecraft.class_1799;

public interface FollowTargetAccessor {
    public void setAiFollowTarget(class_1309 var1);

    public class_1309 getAiFollowTarget();

    public void setAiPatrol(boolean var1);

    public boolean isAiPatrolling();

    public void setForcedMainHandStack(class_1799 var1);

    public void setAiAttackTarget(class_1309 var1);
}

