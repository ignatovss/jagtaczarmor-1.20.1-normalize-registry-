package com.jagtaczarmor.registry;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLES;
    public static final RegistryObject<SimpleParticleType> IMPACT_SPARK;
    public static final RegistryObject<SimpleParticleType> BLOOD;

    public ParticleRegistry() {
    }

    static {
        PARTICLES = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, "jagtaczarmor");
        IMPACT_SPARK = PARTICLES.register("impact_spark", () -> new SimpleParticleType(false));
        BLOOD = PARTICLES.register("blood", () -> new SimpleParticleType(false));
    }
}