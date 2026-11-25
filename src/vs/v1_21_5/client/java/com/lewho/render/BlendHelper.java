// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.render;

import com.mojang.blaze3d.opengl.GlStateManager;
import org.lwjgl.opengl.GL11;

public final class BlendHelper {
    private BlendHelper() {}

    public static void enableBlend() {
        GlStateManager._enableBlend();
    }

    public static void defaultBlendFunc() {
        GlStateManager._blendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ZERO
        );
    }

    public static void disableBlend() {
        GlStateManager._disableBlend();
    }

    public static void enableDepthTest() {
        GlStateManager._enableDepthTest();
    }

    public static void disableDepthTest() {
        GlStateManager._disableDepthTest();
    }

    public static void depthMask(boolean write) {
        GlStateManager._depthMask(write);
    }
}
