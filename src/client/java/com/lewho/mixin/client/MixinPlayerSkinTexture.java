// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.lewho.skin.IPlayerSkinTexture;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HttpTexture.class)
public abstract class MixinPlayerSkinTexture extends SimpleTexture implements IPlayerSkinTexture {

    @Unique
    private NativeImage cachedSkinImage;

    public MixinPlayerSkinTexture(ResourceLocation location) {
        super(location);
    }

    @Inject(method = "loadCallback(Lcom/mojang/blaze3d/platform/NativeImage;)V", at = @At("HEAD"))
    private void captureNativeImage(NativeImage image, CallbackInfo ci) {
        this.cachedSkinImage = cloneNativeImage(image);
    }

    @Override
    public NativeImage getLoadedImage() {
        return this.cachedSkinImage;
    }

    private static NativeImage cloneNativeImage(NativeImage source) {
        NativeImage copy = new NativeImage(
                source.format(),
                source.getWidth(),
                source.getHeight(),
                false
        );
        copy.copyFrom(source);
        return copy;
    }
}

