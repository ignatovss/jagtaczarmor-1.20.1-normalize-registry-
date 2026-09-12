package com.jagtaczarmor.registry;

import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.data.ArmorSetIndex;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

public class CreativeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS;
    public static final RegistryObject<CreativeModeTab> TAB;

    public CreativeTabRegistry() {
    }

    static {
        CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "jagtaczarmor");
        TAB = CREATIVE_MODE_TABS.register("tab", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.jagtaczarmor"))
                .icon(() -> new ItemStack(ItemRegistry.TAB_ICON.get()))
                .displayItems((parameters, output) -> {

                    // ===== ВСЕ ПРЕДМЕТЫ ИЗ АДДОНОВ =====
                    for (Map.Entry<ResourceLocation, ArmorSetIndex> entry : AddonPackLoader.ARMOR_SET_INDEXES.entrySet()) {
                        ResourceLocation id = entry.getKey();
                        ArmorSetIndex setIndex = entry.getValue();
                        String baseName = id.getPath();

                        // Проверяем, есть ли зарегистрированный предмет для этой части
                        if (setIndex.helmet != null && (setIndex.helmet.armorTag == null || setIndex.helmet.armorTag.trim().isEmpty())) {
                            String itemId = baseName;
                            RegistryObject<net.minecraft.world.item.Item> regItem = ItemRegistry.getAddonItem(itemId);
                            if (regItem != null) {
                                output.accept(new ItemStack(regItem.get()));
                            }
                        }
                        if (setIndex.chestplate != null && (setIndex.chestplate.armorTag == null || setIndex.chestplate.armorTag.trim().isEmpty())) {
                            String itemId = baseName;
                            RegistryObject<net.minecraft.world.item.Item> regItem = ItemRegistry.getAddonItem(itemId);
                            if (regItem != null) {
                                output.accept(new ItemStack(regItem.get()));
                            }
                        }
                        if (setIndex.leggings != null && (setIndex.leggings.armorTag == null || setIndex.leggings.armorTag.trim().isEmpty())) {
                            String itemId = baseName;
                            RegistryObject<net.minecraft.world.item.Item> regItem = ItemRegistry.getAddonItem(itemId);
                            if (regItem != null) {
                                output.accept(new ItemStack(regItem.get()));
                            }
                        }
                        if (setIndex.boots != null && (setIndex.boots.armorTag == null || setIndex.boots.armorTag.trim().isEmpty())) {
                            String itemId = baseName;
                            RegistryObject<net.minecraft.world.item.Item> regItem = ItemRegistry.getAddonItem(itemId);
                            if (regItem != null) {
                                output.accept(new ItemStack(regItem.get()));
                            }
                        }
                    }

                    // ===== ПЛАСТИНЫ =====
                    for (ResourceLocation plateId : AddonPackLoader.PLATE_INDEXES.keySet()) {
                        ItemStack plate = new ItemStack(ItemRegistry.PLATE_ARMOR.get());
                        plate.getOrCreateTag().putString("plate_id", plateId.toString());
                        Integer cmd = AddonPackLoader.PLATE_CMD.get(plateId);
                        if (cmd != null) {
                            plate.getOrCreateTag().putInt("CustomModelData", cmd);
                        }
                        output.accept(plate);
                    }

                })
                .build());
    }
}