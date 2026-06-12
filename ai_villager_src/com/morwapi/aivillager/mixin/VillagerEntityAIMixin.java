/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1299
 *  net.minecraft.class_1304
 *  net.minecraft.class_1309
 *  net.minecraft.class_1314
 *  net.minecraft.class_1352
 *  net.minecraft.class_1646
 *  net.minecraft.class_1799
 *  net.minecraft.class_1937
 *  net.minecraft.class_3988
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package com.morwapi.aivillager.mixin;

import com.morwapi.aivillager.access.FollowTargetAccessor;
import com.morwapi.aivillager.ai.goal.AIAttackGoal;
import com.morwapi.aivillager.ai.goal.AIFollowGoal;
import com.morwapi.aivillager.ai.goal.AIPatrolGoal;
import net.minecraft.class_1299;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1314;
import net.minecraft.class_1352;
import net.minecraft.class_1646;
import net.minecraft.class_1799;
import net.minecraft.class_1937;
import net.minecraft.class_3988;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_1646.class})
public abstract class VillagerEntityAIMixin
extends class_3988
implements FollowTargetAccessor {
    @Unique
    private AIFollowGoal aiFollowGoal;
    @Unique
    private AIPatrolGoal aiPatrolGoal;
    @Unique
    private AIAttackGoal aiAttackGoal;
    @Unique
    private class_1799 forcedMainHandStack = class_1799.field_8037;

    public VillagerEntityAIMixin(class_1299<? extends class_3988> entityType, class_1937 world) {
        super(entityType, world);
    }

    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    private void addCustomAIGoals(CallbackInfo ci) {
        this.aiAttackGoal = new AIAttackGoal((class_1314)this, 1.0, true);
        this.field_6201.method_6277(1, (class_1352)this.aiAttackGoal);
        this.aiFollowGoal = new AIFollowGoal((class_1314)this, 0.6, 2.0f, 20.0f);
        this.field_6201.method_6277(2, (class_1352)this.aiFollowGoal);
        this.aiPatrolGoal = new AIPatrolGoal((class_1314)this, 0.5, 15);
        this.field_6201.method_6277(3, (class_1352)this.aiPatrolGoal);
    }

    @Inject(method={"tick"}, at={@At(value="HEAD")})
    private void onTick(CallbackInfo ci) {
        if (!this.forcedMainHandStack.method_7960() && this.method_6047() != this.forcedMainHandStack) {
            this.method_5673(class_1304.field_6173, this.forcedMainHandStack);
        }
    }

    @Override
    public void setAiFollowTarget(class_1309 target) {
        if (this.aiFollowGoal != null) {
            this.aiFollowGoal.setFollowTarget(target);
            if (target != null) {
                this.setAiPatrol(false);
                this.setAiAttackTarget(null);
            }
        }
    }

    @Override
    public class_1309 getAiFollowTarget() {
        return this.aiFollowGoal != null ? this.aiFollowGoal.getFollowTarget() : null;
    }

    @Override
    public void setAiPatrol(boolean patrolling) {
        if (this.aiPatrolGoal != null) {
            this.aiPatrolGoal.setPatrolling(patrolling);
            if (patrolling) {
                this.setAiFollowTarget(null);
                this.setAiAttackTarget(null);
            }
        }
    }

    @Override
    public boolean isAiPatrolling() {
        return this.aiPatrolGoal != null && this.aiPatrolGoal.isPatrolling();
    }

    @Override
    public void setForcedMainHandStack(class_1799 stack) {
        this.forcedMainHandStack = stack;
        if (!stack.method_7960()) {
            this.method_5673(class_1304.field_6173, stack);
        } else {
            this.method_5673(class_1304.field_6173, class_1799.field_8037);
        }
    }

    @Override
    public void setAiAttackTarget(class_1309 target) {
        if (this.aiAttackGoal != null) {
            this.aiAttackGoal.setAttackTarget(target);
            if (target != null) {
                this.setAiPatrol(false);
                this.setAiFollowTarget(null);
            }
        }
    }
}

