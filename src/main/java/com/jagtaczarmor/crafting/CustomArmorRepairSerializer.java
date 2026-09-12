//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jagtaczarmor.crafting;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class CustomArmorRepairSerializer implements RecipeSerializer<CustomArmorRepairRecipe> {
    public CustomArmorRepairSerializer() {
    }

    public CustomArmorRepairRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
        return new CustomArmorRepairRecipe(recipeId, CraftingBookCategory.MISC);
    }

    public CustomArmorRepairRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        return new CustomArmorRepairRecipe(recipeId, CraftingBookCategory.MISC);
    }

    public void toNetwork(FriendlyByteBuf buffer, CustomArmorRepairRecipe recipe) {
    }
}
