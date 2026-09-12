package com.jagtaczarmor.registry;

import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.item.CustomGeoArmorItem;
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
        CREATIVE_MODE_TABS =
                DeferredRegister.create(
                        Registries.CREATIVE_MODE_TAB,
                        "jagtaczarmor"
                );

        TAB = CREATIVE_MODE_TABS.register(
                "tab",
                () -> CreativeModeTab.builder()
                        .title(
                                Component.translatable(
                                        "itemGroup.jagtaczarmor"
                                )
                        )
                        .icon(
                                () -> new ItemStack(
                                        ItemRegistry.TAB_ICON.get()
                                )
                        )
                        .displayItems(
                                (parameters, output) -> {

                                    /*
                                     * =====================================================
                                     * ARMOR ITEMS FROM ADDON PACKS
                                     * =====================================================
                                     *
                                     * Теперь каждый armor item имеет собственный
                                     * Minecraft registry ID.
                                     *
                                     * Например:
                                     *
                                     * jag_default_armor:tactical_armor_helmet
                                     * jag_default_armor:tactical_armor_chestplate
                                     * jag_default_armor:tactical_armor_leggings
                                     * jag_default_armor:tactical_armor_boots
                                     *
                                     * Поэтому больше нельзя строить item ID из
                                     * ArmorSetIndex.getPath().
                                     *
                                     * Берём непосредственно зарегистрированные
                                     * addon items из ItemRegistry.
                                     */
                                    for (
                                            Map.Entry<ResourceLocation, CustomGeoArmorItem> entry :
                                            ItemRegistry.getAddonItems().entrySet()
                                    ) {
                                        CustomGeoArmorItem item =
                                                entry.getValue();

                                        if (item == null) {
                                            continue;
                                        }

                                        /*
                                         * Armor с armorTag не добавляем,
                                         * сохраняя старую логику Creative Tab.
                                         */
                                        if (
                                                item.armorIndex != null
                                                        && item.armorIndex.armorTag != null
                                                        && !item.armorIndex.armorTag
                                                        .trim()
                                                        .isEmpty()
                                        ) {
                                            continue;
                                        }

                                        output.accept(
                                                new ItemStack(item)
                                        );
                                    }

                                    /*
                                     * =====================================================
                                     * PLATES
                                     * =====================================================
                                     */
                                    for (
                                            ResourceLocation plateId :
                                            AddonPackLoader.PLATE_INDEXES.keySet()
                                    ) {
                                        ItemStack plate =
                                                new ItemStack(
                                                        ItemRegistry.PLATE_ARMOR.get()
                                                );

                                        plate.getOrCreateTag().putString(
                                                "plate_id",
                                                plateId.toString()
                                        );

                                        Integer cmd =
                                                AddonPackLoader.PLATE_CMD.get(
                                                        plateId
                                                );

                                        if (cmd != null) {
                                            plate.getOrCreateTag().putInt(
                                                    "CustomModelData",
                                                    cmd
                                            );
                                        }

                                        output.accept(
                                                plate
                                        );
                                    }
                                }
                        )
                        .build()
        );
    }
}