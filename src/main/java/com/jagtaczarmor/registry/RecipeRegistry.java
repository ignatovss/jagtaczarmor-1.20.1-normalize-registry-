//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jagtaczarmor.registry;

import com.jagtaczarmor.crafting.CustomArmorRepairSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class RecipeRegistry {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS;
    public static final RegistryObject<RecipeSerializer<?>> ARMOR_REPAIR;

    public RecipeRegistry() {
    }

    static {
        RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, "jagtaczarmor");
        ARMOR_REPAIR = RECIPE_SERIALIZERS.register("armor_repair", CustomArmorRepairSerializer::new);
    }
}