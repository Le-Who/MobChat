// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TextureLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("creaturechat");
    private static final Set<String> missing = new HashSet<>();
    public static GpuTextureView lastTextureView = null;
    public static ResourceLocation lastTextureId = null;

    public TextureLoader() {}

    public ResourceLocation GetUI(String name) {
        return load(new ResourceLocation("creaturechat", "textures/ui/" + name + ".png"));
    }

    public ResourceLocation GetEntity(String texturePath) {
        ResourceLocation id = new ResourceLocation("creaturechat", texturePath);
        ResourceManager rm = Minecraft.getInstance().getResourceManager();
        if (rm.getResource(id).isPresent()) {
            return load(id);
        } else {
            if (missing.add(texturePath)) {
                LOGGER.info("Missing texture: {}", texturePath);
            }
            return load(new ResourceLocation("creaturechat", "textures/entity/not_found.png"));
        }
    }

    private ResourceLocation load(ResourceLocation id) {
        Minecraft client = Minecraft.getInstance();
        ResourceManager rm = client.getResourceManager();
        Optional<Resource> res = rm.getResource(id);

        if (res.isEmpty()) {
            if (missing.add(id.toString())) {
                LOGGER.info("Missing texture: {}", id);
            }
            return null;
        }
        return id;
    }

    public static void bind(int unit, ResourceLocation id) {
        Minecraft client = Minecraft.getInstance();
        TextureManager tm = client.getTextureManager();
        AbstractTexture tex = tm.getTexture(id);
        if (tex == null) {
            tex = new SimpleTexture(id);
            tm.register(id, tex);
        }

        GpuTextureView gpu = tex.getTextureView();
        lastTextureView = gpu;
        lastTextureId = id;

        RenderSystem.setShaderTexture(unit, gpu);
    }
}
