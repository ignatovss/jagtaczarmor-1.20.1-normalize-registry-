package com.jagtaczarmor.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS;
    public static final RegistryObject<SoundEvent> ARMOR_HIT_1;
    public static final RegistryObject<SoundEvent> ARMOR_HIT_2;
    public static final RegistryObject<SoundEvent> ARMOR_HIT_3;
    public static final RegistryObject<SoundEvent> ARMOR_HIT_4;
    public static final RegistryObject<SoundEvent> ARMOR_HIT_5;
    public static final RegistryObject<SoundEvent> HURT_1;
    public static final RegistryObject<SoundEvent> HURT_2;
    public static final RegistryObject<SoundEvent> HURT_3;
    public static final RegistryObject<SoundEvent> HURT_4;
    public static final RegistryObject<SoundEvent> HURT_5;
    public static final RegistryObject<SoundEvent> HURT_6;

    public SoundRegistry() {
    }

    private static RegistryObject<SoundEvent> registerSoundEvent(String name) {
        ResourceLocation id = new ResourceLocation("jagtaczarmor", name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    static {
        SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, "jagtaczarmor");
        ARMOR_HIT_1 = registerSoundEvent("armor_hit_1");
        ARMOR_HIT_2 = registerSoundEvent("armor_hit_2");
        ARMOR_HIT_3 = registerSoundEvent("armor_hit_3");
        ARMOR_HIT_4 = registerSoundEvent("armor_hit_4");
        ARMOR_HIT_5 = registerSoundEvent("armor_hit_5");
        HURT_1 = registerSoundEvent("hurt_1");
        HURT_2 = registerSoundEvent("hurt_2");
        HURT_3 = registerSoundEvent("hurt_3");
        HURT_4 = registerSoundEvent("hurt_4");
        HURT_5 = registerSoundEvent("hurt_5");
        HURT_6 = registerSoundEvent("hurt_6");
    }
}