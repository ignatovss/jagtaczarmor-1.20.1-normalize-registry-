package com.jagtaczarmor.item;

import net.minecraft.Util;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

public class CustomArmorMaterial implements ArmorMaterial {
    public static final CustomArmorMaterial INSTANCE = new CustomArmorMaterial();
    private static final int[] HEALTH_PER_SLOT = new int[]{13, 15, 16, 11};

    public CustomArmorMaterial() {
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type pType) {
        return HEALTH_PER_SLOT[pType.getSlot().getIndex()] * 33;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type pType) {
        switch (pType) {
            case HELMET -> {
                return 3;
            }
            case CHESTPLATE -> {
                return 8;
            }
            case LEGGINGS -> {
                return 6;
            }
            case BOOTS -> {
                return 3;
            }
            default -> {
                return 0;
            }
        }
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.EMPTY;
    }

    @Override
    public String getName() {
        return "jagtaczarmor:custom";
    }

    @Override
    public float getToughness() {
        return 2.0F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1F;
    }
}