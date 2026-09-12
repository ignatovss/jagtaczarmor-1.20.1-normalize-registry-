package com.jagtaczarmor.crafting;

import com.jagtaczarmor.item.CustomGeoArmorItem;
import com.jagtaczarmor.registry.RecipeRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class CustomArmorRepairRecipe extends CustomRecipe {
    public CustomArmorRepairRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    public boolean matches(CraftingContainer container, Level level) {
        List<ItemStack> list = new ArrayList<>();

        for(int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack itemstack = container.getItem(i);
            if (!itemstack.isEmpty()) {
                list.add(itemstack);
                if (list.size() > 2) {
                    return false;
                }
            }
        }

        if (list.size() == 2) {
            ItemStack itemstack1 = list.get(0);
            ItemStack itemstack2 = list.get(1);
            if (itemstack1.is(itemstack2.getItem()) && itemstack1.getCount() == 1 && itemstack1.isDamageableItem() && itemstack2.isDamageableItem()) {
                if (itemstack1.getItem() instanceof CustomGeoArmorItem) {
                    String id1 = itemstack1.hasTag() ? itemstack1.getTag().getString("armor_id") : "";
                    String id2 = itemstack2.hasTag() ? itemstack2.getTag().getString("armor_id") : "";
                    return id1.equals(id2);
                }
                return true;
            }
        }
        return false;
    }

    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();

        for(int i = 0; i < container.getContainerSize(); ++i) {
            ItemStack itemstack = container.getItem(i);
            if (!itemstack.isEmpty()) {
                list.add(itemstack);
                if (list.size() > 2) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (list.size() == 2) {
            ItemStack itemstack1 = list.get(0);
            ItemStack itemstack2 = list.get(1);
            if (itemstack1.is(itemstack2.getItem()) && itemstack1.getCount() == 1 && itemstack1.isDamageableItem() && itemstack2.isDamageableItem()) {
                if (itemstack1.getItem() instanceof CustomGeoArmorItem) {
                    String id1 = itemstack1.hasTag() ? itemstack1.getTag().getString("armor_id") : "";
                    String id2 = itemstack2.hasTag() ? itemstack2.getTag().getString("armor_id") : "";
                    if (!id1.equals(id2)) {
                        return ItemStack.EMPTY;
                    }
                }

                Item item = itemstack1.getItem();
                int i = item.getMaxDamage(itemstack1) - itemstack1.getDamageValue();
                int j = item.getMaxDamage(itemstack2) - itemstack2.getDamageValue();
                int k = i + j + item.getMaxDamage(itemstack1) * 10 / 100;
                int l = item.getMaxDamage(itemstack1) - k;
                if (l < 0) {
                    l = 0;
                }

                ItemStack result = new ItemStack(itemstack1.getItem());
                if (itemstack1.getItem() instanceof CustomGeoArmorItem && itemstack1.hasTag()) {
                    if (itemstack1.getTag().contains("armor_id")) {
                        result.getOrCreateTag().putString("armor_id", itemstack1.getTag().getString("armor_id"));
                    }
                    if (itemstack1.getTag().contains("CustomModelData")) {
                        result.getOrCreateTag().putInt("CustomModelData", itemstack1.getTag().getInt("CustomModelData"));
                    }
                }
                result.setDamageValue(l);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    public RecipeSerializer<?> getSerializer() {
        return RecipeRegistry.ARMOR_REPAIR.get();
    }
}