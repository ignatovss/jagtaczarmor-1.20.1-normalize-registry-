package com.jagtaczarmor.client.renderer;

import com.jagtaczarmor.client.model.CustomGeoArmorModel;
import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.item.CustomGeoArmorItem;
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

    public CustomGeoArmorItemRenderer() {
        super(new CustomGeoArmorModel());
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext displayContext,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {

        /*
         * Все новые addon armor items имеют:
         *
         *     CustomGeoArmorItem.armorIndex
         *     CustomGeoArmorItem.itemId
         *
         * Например:
         *
         *     lrarmor_pack:atf_chestplate
         *
         * Поэтому больше не используем старые:
         *
         *     tk_hm
         *     tk_ch
         *     tk_lg
         *     tk_bt
         *
         * и не читаем armor_id из NBT для определения предмета.
         */
        if (stack.getItem() instanceof CustomGeoArmorItem customItem
                && customItem.armorIndex != null) {

            String itemId = customItem.getItemId();

            if (itemId != null) {

                ResourceLocation itemResourceId =
                        ResourceLocation.tryParse(itemId);

                if (itemResourceId != null) {

                    int cmd =
                            AddonPackLoader.ARMOR_SET_CMD.getOrDefault(
                                    itemResourceId,
                                    0
                            );

                    if (displayContext == ItemDisplayContext.GUI
                            && cmd > 0) {

                        /*
                         * Модель GUI использует реальный item ID.
                         *
                         * Например:
                         *
                         * jagtaczarmor:item/lrarmor_pack:atf_chestplate_123
                         *
                         * Однако ResourceLocation не позволяет ':' внутри
                         * path, поэтому для имени модели используем
                         * namespace и path отдельно.
                         */
                        ResourceLocation modelId =
                                new ResourceLocation(
                                        itemResourceId.getNamespace(),
                                        "item/"
                                                + itemResourceId.getPath()
                                                + "_"
                                                + cmd
                                );

                        ModelResourceLocation modelLoc =
                                new ModelResourceLocation(
                                        modelId,
                                        "inventory"
                                );

                        BakedModel model2D =
                                Minecraft.getInstance()
                                        .getModelManager()
                                        .getModel(modelLoc);

                        if (model2D != null
                                && model2D != Minecraft.getInstance()
                                .getModelManager()
                                .getMissingModel()) {

                            poseStack.pushPose();

                            Minecraft.getInstance()
                                    .getItemRenderer()
                                    .render(
                                            stack,
                                            displayContext,
                                            false,
                                            poseStack,
                                            bufferSource,
                                            packedLight,
                                            packedOverlay,
                                            model2D
                                    );

                            poseStack.popPose();

                            return;
                        }
                    }
                }
            }
        }

        /*
         * FALLBACK
         */
        GeoModel<?> geoModel = this.getGeoModel();

        if (geoModel instanceof CustomGeoArmorModel model) {
            model.currentItemStack = stack;
        }

        poseStack.pushPose();

        if (displayContext == ItemDisplayContext.GUI) {

            poseStack.translate(
                    0.5F,
                    0.5F,
                    0.5F
            );

            poseStack.scale(
                    0.8F,
                    0.8F,
                    0.8F
            );

        } else if (displayContext == ItemDisplayContext.FIXED) {

            poseStack.translate(
                    0.5F,
                    0.5F,
                    0.5F
            );

        } else if (
                displayContext != ItemDisplayContext.FIRST_PERSON_LEFT_HAND
                        && displayContext != ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                        && displayContext != ItemDisplayContext.THIRD_PERSON_LEFT_HAND
                        && displayContext != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
        ) {

            if (displayContext == ItemDisplayContext.GROUND) {

                poseStack.translate(
                        0.5F,
                        0.5F,
                        0.5F
                );

                poseStack.scale(
                        0.5F,
                        0.5F,
                        0.5F
                );
            }

        } else {

            poseStack.translate(
                    0.5F,
                    0.5F,
                    0.5F
            );
        }

        super.renderByItem(
                stack,
                displayContext,
                poseStack,
                bufferSource,
                packedLight,
                packedOverlay
        );

        poseStack.popPose();
    }
}