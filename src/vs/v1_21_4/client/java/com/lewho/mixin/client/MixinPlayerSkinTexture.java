// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.lewho.skin.IPlayerSkinTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(DynamicTexture.class)
public abstract class MixinPlayerSkinTexture implements IPlayerSkinTexture {
    @Shadow private NativeImage pixels;

    @Override
    public NativeImage getLoadedImage() {
        return this.pixels;
    }
}
