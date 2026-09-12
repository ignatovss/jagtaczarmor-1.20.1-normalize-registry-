package com.jagtaczarmor.client.renderer;

import com.jagtaczarmor.client.model.CustomGeoArmorModel;
import com.jagtaczarmor.item.CustomGeoArmorItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

public class CustomGeoArmorRenderer extends GeoArmorRenderer<CustomGeoArmorItem> {
    public CustomGeoArmorRenderer() {
        super(new CustomGeoArmorModel());
    }

    public void prepForRender(Entity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> baseModel) {
        if (this.getGeoModel() instanceof CustomGeoArmorModel) {
            ((CustomGeoArmorModel) this.getGeoModel()).currentItemStack = stack;
        }
        super.prepForRender(entity, stack, slot, baseModel);
    }

    public void applyBoneVisibilityBySlot(EquipmentSlot currentSlot) {
        super.applyBoneVisibilityBySlot(currentSlot);
        if (currentSlot == EquipmentSlot.FEET) {
            this.setBoneVisible(this.rightLeg, true);
            this.setBoneVisible(this.leftLeg, true);
        }
    }
}