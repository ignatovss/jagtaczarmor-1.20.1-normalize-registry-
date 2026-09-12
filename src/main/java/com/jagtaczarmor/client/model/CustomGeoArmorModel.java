//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jagtaczarmor.client.model;

import com.jagtaczarmor.data.ArmorIndex;
import com.jagtaczarmor.item.CustomGeoArmorItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.model.GeoModel;

public class CustomGeoArmorModel extends GeoModel<CustomGeoArmorItem> {
    public ItemStack currentItemStack;

    public CustomGeoArmorModel() {
    }

    public ResourceLocation getModelResource(CustomGeoArmorItem object) {
        if (this.currentItemStack != null) {
            ArmorIndex index = CustomGeoArmorItem.getIndex(this.currentItemStack);
            if (index != null && index.getModelLocation() != null) {
                return index.getModelLocation();
            }
        }

        return new ResourceLocation("jagtaczarmor", "geo/custom_armor.geo.json");
    }

    public ResourceLocation getTextureResource(CustomGeoArmorItem object) {
        if (this.currentItemStack != null) {
            ArmorIndex index = CustomGeoArmorItem.getIndex(this.currentItemStack);
            if (index != null && index.getTextureLocation() != null) {
                return index.getTextureLocation();
            }
        }

        return new ResourceLocation("jagtaczarmor", "textures/models/armor/custom_armor.png");
    }

    public ResourceLocation getAnimationResource(CustomGeoArmorItem animatable) {
        return new ResourceLocation("jagtaczarmor", "animations/custom_armor.animation.json");
    }
}
