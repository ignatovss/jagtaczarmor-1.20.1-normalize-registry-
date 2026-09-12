package com.jagtaczarmor.registry;

import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.item.CustomGeoArmorItem;
import com.jagtaczarmor.item.CustomPlateItem;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(
        modid = "jagtaczarmor",
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class ItemRegistry {

    public static final DeferredRegister<Item> ITEMS;

    public static final RegistryObject<Item> CUSTOM_HELMET;
    public static final RegistryObject<Item> CUSTOM_CHESTPLATE;
    public static final RegistryObject<Item> CUSTOM_LEGGINGS;
    public static final RegistryObject<Item> CUSTOM_BOOTS;
    public static final RegistryObject<Item> TAB_ICON;
    public static final RegistryObject<Item> PLATE_ARMOR;

    /**
     * Динамические armor items из armor pack'ов.
     *
     * Ключ карты — полный Minecraft registry ID.
     *
     * Например:
     *
     * jag_default_armor:tactical_armor_helmet
     * jag_default_armor:tactical_armor_chestplate
     */
    private static final Map<ResourceLocation, CustomGeoArmorItem> ADDON_ITEMS =
            new HashMap<>();

    private ItemRegistry() {
    }

    /**
     * Сохраняет динамический armor item в нашей карте.
     *
     * Фактическая регистрация в Forge registry происходит через RegisterEvent.
     */
    public static void registerAddonArmor(
            ResourceLocation itemId,
            CustomGeoArmorItem item
    ) {
        if (itemId == null || item == null) {
            return;
        }

        ADDON_ITEMS.put(itemId, item);
    }

    /**
     * Получить динамический armor item по полному registry ID.
     */
    public static CustomGeoArmorItem getAddonItem(
            ResourceLocation itemId
    ) {
        if (itemId == null) {
            return null;
        }

        return ADDON_ITEMS.get(itemId);
    }

    /**
     * Проверить, является ли ID динамическим armor item.
     */
    public static boolean isAddonArmor(
            ResourceLocation itemId
    ) {
        return itemId != null && ADDON_ITEMS.containsKey(itemId);
    }

    /**
     * Получить все динамические armor items.
     */
    public static Map<ResourceLocation, CustomGeoArmorItem> getAddonItems() {
        return ADDON_ITEMS;
    }

    /**
     * Forge registry event.
     *
     * Здесь AddonPackLoader читает items.json и регистрирует
     * конкретные Minecraft Item ID.
     *
     * Важно:
     *
     * items.json теперь является источником уникальных registry ID.
     * Armor JSON больше не используется как Item ID.
     */
    @SubscribeEvent
    public static void onItemRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(
                ForgeRegistries.ITEMS.getRegistryKey()
        )) {
            return;
        }

        AddonPackLoader.registerAddonItems(event);
    }

    static {
        ITEMS = DeferredRegister.create(
                ForgeRegistries.ITEMS,
                "jagtaczarmor"
        );

        /*
         * Старые встроенные предметы оставляем.
         *
         * Они являются частью самого мода и не относятся
         * к новой динамической системе armor pack items.json.
         */

        CUSTOM_HELMET = ITEMS.register(
                "tk_hm",
                () -> new CustomGeoArmorItem(
                        Type.HELMET,
                        new Item.Properties()
                )
        );

        CUSTOM_CHESTPLATE = ITEMS.register(
                "tk_ch",
                () -> new CustomGeoArmorItem(
                        Type.CHESTPLATE,
                        new Item.Properties()
                )
        );

        CUSTOM_LEGGINGS = ITEMS.register(
                "tk_lg",
                () -> new CustomGeoArmorItem(
                        Type.LEGGINGS,
                        new Item.Properties()
                )
        );

        CUSTOM_BOOTS = ITEMS.register(
                "tk_bt",
                () -> new CustomGeoArmorItem(
                        Type.BOOTS,
                        new Item.Properties()
                )
        );

        TAB_ICON = ITEMS.register(
                "tab_icon",
                () -> new Item(
                        new Item.Properties()
                )
        );

        PLATE_ARMOR = ITEMS.register(
                "plate_armor",
                () -> new CustomPlateItem(
                        new Item.Properties().stacksTo(4)
                )
        );
    }
}