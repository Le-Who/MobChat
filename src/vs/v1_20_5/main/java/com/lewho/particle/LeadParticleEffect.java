// SPDX-FileCopyrightText: 2025 lewho LLC
// SPDX-License-Identifier: GPL-3.0-or-later
// Assets CC-BY-NC-SA-4.0; CreatureChat™ trademark © lewho LLC - unauthorized use prohibited
package com.lewho.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record LeadParticleEffect(float angle) implements ParticleOptions {
    public static final MapCodec<LeadParticleEffect> MAP_CODEC = RecordCodecBuilder.mapCodec(inst ->
            inst.group(Codec.FLOAT.fieldOf("angle").forGetter(LeadParticleEffect::angle)).apply(inst, LeadParticleEffect::new)
    );

    public static final StreamCodec<FriendlyByteBuf, LeadParticleEffect> PACKET_CODEC =
            StreamCodec.ofMember(
                    LeadParticleEffect::write,
                    buf -> new LeadParticleEffect(buf.readFloat())
            );

    public static final ParticleType<LeadParticleEffect> TYPE =
            FabricParticleTypes.complex(
                    type -> MAP_CODEC,
                    type -> PACKET_CODEC
            );

    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(angle);
    }

    @Override
    public ParticleType<LeadParticleEffect> getType() {
        return TYPE;
    }
}
