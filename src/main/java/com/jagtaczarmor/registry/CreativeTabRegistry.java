package com.jagtaczarmor.registry;

import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.item.CustomGeoArmorItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
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
                                     *
                                     * Теперь каждая плита является отдельным
                                     * Minecraft Item.
                                     *
                                     * Например:
                                     *
                                     * jag_default_armor:ceramic_plate
                                     * lrarmor_pack:steel_plate
                                     *
                                     * Поэтому больше НЕ создаём:
                                     *
                                     * jagtaczarmor:plate_armor
                                     *
                                     * и НЕ используем plate_id для определения
                                     * самого Item.
                                     */
                                    for (
                                            ResourceLocation plateId :
                                            AddonPackLoader.PLATE_INDEXES.keySet()
                                    ) {

                                        Item plateItem =
                                                ItemRegistry.getPlateItem(
                                                        plateId
                                                );

                                        if (plateItem == null) {

                                            continue;
                                        }

                                        ItemStack plate =
                                                new ItemStack(
                                                        plateItem
                                                );

                                        /*
                                         * Оставляем plate_id в NBT
                                         * для совместимости со старой системой
                                         * и существующими сохранёнными предметами.
                                         */
                                        plate.getOrCreateTag().putString(
                                                "plate_id",
                                                plateId.toString()
                                        );

                                        /*
                                         * CustomModelData также сохраняем.
                                         */
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