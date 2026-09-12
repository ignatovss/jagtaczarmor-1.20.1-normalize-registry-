package com.jagtaczarmor.client.renderer;

import com.jagtaczarmor.client.model.CustomGeoArmorModel;
import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.item.CustomGeoArmorItem;
import com.jagtaczarmor.registry.ItemRegistry;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class CustomGeoArmorItemRenderer extends GeoItemRenderer<CustomGeoArmorItem> {
    private static final String[] PIECE_NAMES = new String[]{"tk_hm", "tk_ch", "tk_lg", "tk_bt"};

    public CustomGeoArmorItemRenderer() {
        super(new CustomGeoArmorModel());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        // ===== ДЛЯ АДДОННЫХ ПРЕДМЕТОВ (С ПРЯМЫМ armorIndex) =====
        if (stack.getItem() instanceof CustomGeoArmorItem customItem && customItem.armorIndex != null) {
            String itemId = customItem.getItemId();
            if (itemId != null) {
                ResourceLocation id = new ResourceLocation("jagtaczarmor", itemId);
                int cmd = AddonPackLoader.ARMOR_SET_CMD.getOrDefault(id, 0);
                if (displayContext == ItemDisplayContext.GUI && cmd > 0) {
                    ModelResourceLocation modelLoc = new ModelResourceLocation(
                            new ResourceLocation("jagtaczarmor", "item/" + itemId + "_" + cmd),
                            "inventory"
                    );
                    BakedModel model2D = Minecraft.getInstance().getModelManager().getModel(modelLoc);
                    if (model2D != null && model2D != Minecraft.getInstance().getModelManager().getMissingModel()) {
                        poseStack.pushPose();
                        Minecraft.getInstance().getItemRenderer().render(stack, displayContext, false, poseStack, bufferSource, packedLight, packedOverlay, model2D);
                        poseStack.popPose();
                        return;
                    }
                }
            }
        }

        // ===== ОРИГИНАЛЬНАЯ ЛОГИКА ДЛЯ ШАБЛОННЫХ ПРЕДМЕТОВ =====
        if (displayContext == ItemDisplayContext.GUI && stack.hasTag() && stack.getTag().contains("armor_id")) {
            String armorId = stack.getTag().getString("armor_id");
            ResourceLocation id = new ResourceLocation(armorId);
            int cmd = AddonPackLoader.ARMOR_SET_CMD.getOrDefault(id, 0);
            int pieceIdx = -1;
            if (stack.getItem() == ItemRegistry.CUSTOM_HELMET.get()) {
                pieceIdx = 0;
            } else if (stack.getItem() == ItemRegistry.CUSTOM_CHESTPLATE.get()) {
                pieceIdx = 1;
            } else if (stack.getItem() == ItemRegistry.CUSTOM_LEGGINGS.get()) {
                pieceIdx = 2;
            } else if (stack.getItem() == ItemRegistry.CUSTOM_BOOTS.get()) {
                pieceIdx = 3;
            }

            if (pieceIdx != -1 && cmd > 0) {
                ModelResourceLocation modelLoc = new ModelResourceLocation(
                        new ResourceLocation("jagtaczarmor", "item/" + PIECE_NAMES[pieceIdx] + "_" + cmd),
                        "inventory"
                );
                BakedModel model2D = Minecraft.getInstance().getModelManager().getModel(modelLoc);
                if (model2D != null && model2D != Minecraft.getInstance().getModelManager().getMissingModel()) {
                    poseStack.pushPose();
                    Minecraft.getInstance().getItemRenderer().render(stack, displayContext, false, poseStack, bufferSource, packedLight, packedOverlay, model2D);
                    poseStack.popPose();
                    return;
                }
            }
        }

        // ===== FALLBACK =====
        GeoModel<?> geoModel = this.getGeoModel();
        if (geoModel instanceof CustomGeoArmorModel model) {
            model.currentItemStack = stack;
        }

        poseStack.pushPose();
        if (displayContext == ItemDisplayContext.GUI) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.scale(0.8F, 0.8F, 0.8F);
        } else if (displayContext == ItemDisplayContext.FIXED) {
            poseStack.translate(0.5F, 0.5F, 0.5F);
        } else if (displayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND &&
                displayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND &&
                displayContext != ItemDisplayContext.THIRD_PERSON_LEFT_HAND &&
                displayContext != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            if (displayContext == ItemDisplayContext.GROUND) {
                poseStack.translate(0.5F, 0.5F, 0.5F);
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
        } else {
            poseStack.translate(0.5F, 0.5F, 0.5F);
        }

        super.renderByItem(stack, displayContext, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }
}