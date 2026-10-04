// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.mixin;

import com.lewho.chat.ChatDataManager;
import com.lewho.chat.EntityChatData;
import com.lewho.chat.LivingEntityChatHooks;
import com.lewho.network.ServerPackets;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The {@code MixinLivingEntity} class modifies the behavior of {@link LivingEntity} to integrate
 * custom friendship, chat, and death message mechanics. It prevents friendly entities from targeting players,
 * generates contextual chat messages on attacks, and broadcasts custom death messages for named entities.
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntity {

    private EntityChatData getChatData(LivingEntity entity) {
        ChatDataManager chatDataManager = ChatDataManager.getServerInstance();
        return chatDataManager.getOrCreateChatData(entity.getStringUUID());
    }

    @Inject(method = "canAttack(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true)
    private void modifyCanTarget(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (LivingEntityChatHooks.preventsFriendlyAttack((LivingEntity) (Object) this, target)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At("RETURN"))
    private void onDamage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        LivingEntityChatHooks.onDamage((LivingEntity) (Object) this, source, cir.getReturnValue());
    }

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At("HEAD"))
    private void onDeath(DamageSource source, CallbackInfo info) {
        LivingEntity entity = (LivingEntity) (Object) this;
        Level world = entity.level();

        if (!world.isClientSide() && entity.hasCustomName()) {
            // Skip tamed entities and players
            if (entity instanceof TamableAnimal && ((TamableAnimal) entity).isTame()) {
                return;
            }

            if (entity instanceof Player) {
                return;
            }

            // Get chatData for the entity
            EntityChatData chatData = getChatData(entity);
            if (chatData != null && !chatData.characterSheet.isEmpty()) {
                // Get the original death message
                Component deathMessage = entity.getCombatTracker().getDeathMessage();
                // Broadcast the death message to all players in the world
                ServerPackets.BroadcastMessage(deathMessage);
            }
        }
    }
}
