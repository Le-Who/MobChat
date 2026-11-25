// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.skin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;

public class SkinUtils {
    public static boolean checkCustomSkinKey(ResourceLocation skinId) {
        AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(skinId);

        if (tex instanceof IPlayerSkinTexture iSkin) {
            NativeImage image = iSkin.getLoadedImage();
            if (image != null) {
                int width = image.getWidth();
                int height = image.getHeight();

                if (width == 64 && height == 64) {
                    int color31_48 = image.getPixel(31, 48);
                    int color32_48 = image.getPixel(32, 48);
                    return (color31_48 == 0xFF000000 && color32_48 == 0xFFFFFFFF);
                }
            }
        }

        return false;
    }
}
