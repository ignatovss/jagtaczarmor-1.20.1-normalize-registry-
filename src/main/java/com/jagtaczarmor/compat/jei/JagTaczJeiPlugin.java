package com.jagtaczarmor.compat.jei;

import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.data.ArmorIndex;
import com.jagtaczarmor.data.ArmorSetIndex;
import com.jagtaczarmor.item.CustomGeoArmorItem;
import com.jagtaczarmor.registry.ItemRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.IIngredientSubtypeInterpreter;
import mezz.jei.api.recipe.vanilla.IJeiAnvilRecipe;
import mezz.jei.api.recipe.vanilla.IVanillaRecipeFactory;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.minecraftforge.registries.ForgeRegistries;

@JeiPlugin
public class JagTaczJeiPlugin implements IModPlugin {

    private static final ResourceLocation PLUGIN_UID =
            new ResourceLocation(
                    "jagtaczarmor",
                    "jei_plugin"
            );

    public JagTaczJeiPlugin() {
    }

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerItemSubtypes(
            ISubtypeRegistration registration
    ) {

        /*
         * Каждый addon armor теперь является отдельным
         * зарегистрированным Item.
         *
         * Например:
         *
         * lrarmor_pack:atf_helmet
         * lrarmor_pack:atf_chestplate
         *
         * Поэтому регистрируем subtype interpreter
         * непосредственно для каждого CustomGeoArmorItem.
         */
        IIngredientSubtypeInterpreter<ItemStack> interpreter =
                (itemStack, context) ->
                        itemStack.hasTag()
                                && itemStack.getTag().contains("armor_id")
                                ? itemStack.getTag().getString("armor_id")
                                : "";

        for (CustomGeoArmorItem item :
                ItemRegistry.getAddonItems().values()) {

            registration.registerSubtypeInterpreter(
                    item,
                    interpreter
            );
        }
    }

    @Override
    public void registerRecipes(
            IRecipeRegistration registration
    ) {

        IVanillaRecipeFactory factory =
                registration.getVanillaRecipeFactory();

        List<IJeiAnvilRecipe> recipes =
                new ArrayList<>();

        List<Map.Entry<ResourceLocation, ArmorSetIndex>> sortedSets =
                new ArrayList<>(
                        AddonPackLoader.ARMOR_SET_INDEXES.entrySet()
                );

        sortedSets.sort((e1, e2) -> {

            String n1 = e1.getValue().name;
            String n2 = e2.getValue().name;

            if (n1 == null) {
                n1 = "";
            }

            if (n2 == null) {
                n2 = "";
            }

            n1 = n1.trim();
            n2 = n2.trim();

            if (n1.isEmpty()) {
                n1 = e1.getKey().toString();
            }

            if (n2.isEmpty()) {
                n2 = e2.getKey().toString();
            }

            int cmp =
                    n1.compareToIgnoreCase(n2);

            if (cmp == 0) {
                cmp =
                        e1.getKey()
                                .toString()
                                .compareToIgnoreCase(
                                        e2.getKey().toString()
                                );
            }

            return cmp;
        });

        for (Map.Entry<ResourceLocation, ArmorSetIndex> entry :
                sortedSets) {

            ResourceLocation armorSetId =
                    entry.getKey();

            ArmorSetIndex setIndex =
                    entry.getValue();

            if (setIndex.helmet != null
                    && setIndex.helmet.repairItem != null
                    && !setIndex.helmet.repairItem.trim().isEmpty()
                    && (setIndex.helmet.armorTag == null
                    || setIndex.helmet.armorTag.trim().isEmpty())) {

                addAnvilRecipe(
                        recipes,
                        factory,
                        armorSetId,
                        setIndex.helmet
                );
            }

            if (setIndex.chestplate != null
                    && setIndex.chestplate.repairItem != null
                    && !setIndex.chestplate.repairItem.trim().isEmpty()
                    && (setIndex.chestplate.armorTag == null
                    || setIndex.chestplate.armorTag.trim().isEmpty())) {

                addAnvilRecipe(
                        recipes,
                        factory,
                        armorSetId,
                        setIndex.chestplate
                );
            }

            if (setIndex.leggings != null
                    && setIndex.leggings.repairItem != null
                    && !setIndex.leggings.repairItem.trim().isEmpty()
                    && (setIndex.leggings.armorTag == null
                    || setIndex.leggings.armorTag.trim().isEmpty())) {

                addAnvilRecipe(
                        recipes,
                        factory,
                        armorSetId,
                        setIndex.leggings
                );
            }

            if (setIndex.boots != null
                    && setIndex.boots.repairItem != null
                    && !setIndex.boots.repairItem.trim().isEmpty()
                    && (setIndex.boots.armorTag == null
                    || setIndex.boots.armorTag.trim().isEmpty())) {

                addAnvilRecipe(
                        recipes,
                        factory,
                        armorSetId,
                        setIndex.boots
                );
            }
        }

        if (!recipes.isEmpty()) {
            registration.addRecipes(
                    RecipeTypes.ANVIL,
                    recipes
            );
        }
    }

    private void addAnvilRecipe(
            List<IJeiAnvilRecipe> recipes,
            IVanillaRecipeFactory factory,
            ResourceLocation armorSetId,
            ArmorIndex pieceIndex
    ) {

        /*
         * armorSetId теперь соответствует item_id
         * динамического предмета.
         *
         * Например:
         *
         * lrarmor_pack:atf_chestplate
         */
        CustomGeoArmorItem item =
                ItemRegistry.getAddonItems().get(armorSetId);

        if (item == null) {
            return;
        }

        if (pieceIndex.repairItem == null
                || pieceIndex.repairItem.trim().isEmpty()) {
            return;
        }

        ResourceLocation repairItemId =
                ResourceLocation.tryParse(
                        pieceIndex.repairItem
                );

        if (repairItemId == null) {
            return;
        }

        Item repairItem =
                ForgeRegistries.ITEMS.getValue(
                        repairItemId
                );

        if (repairItem == Items.AIR) {
            return;
        }

        ItemStack armorStack =
                new ItemStack(item);

        /*
         * Оставляем armor_id в NBT.
         *
         * Это всё ещё используется существующей
         * системой рендера/совместимости.
         */
        armorStack
                .getOrCreateTag()
                .putString(
                        "armor_id",
                        armorSetId.toString()
                );

        Integer customModelData =
                AddonPackLoader.ARMOR_SET_CMD.get(
                        armorSetId
                );

        if (customModelData != null
                && customModelData > 0) {

            armorStack
                    .getOrCreateTag()
                    .putInt(
                            "CustomModelData",
                            customModelData
                    );
        }

        int slotIdx =
                item.getEquipmentSlot()
                        .getIndex();

        int[] maxDamageArray =
                new int[]{
                        13,
                        15,
                        16,
                        11
                };

        if (slotIdx < 0
                || slotIdx >= maxDamageArray.length) {
            return;
        }

        int maxDamage =
                maxDamageArray[slotIdx]
                        * pieceIndex.durabilityMultiplier;

        ItemStack damagedArmor =
                armorStack.copy();

        damagedArmor.setDamageValue(
                maxDamage / 2
        );

        ItemStack repairMaterial =
                new ItemStack(repairItem);

        ItemStack repairedArmor =
                armorStack.copy();

        repairedArmor.setDamageValue(0);

        IJeiAnvilRecipe anvilRecipe =
                factory.createAnvilRecipe(
                        damagedArmor,
                        Collections.singletonList(
                                repairMaterial
                        ),
                        Collections.singletonList(
                                repairedArmor
                        )
                );

        if (anvilRecipe != null) {
            recipes.add(anvilRecipe);
        }
    }
}