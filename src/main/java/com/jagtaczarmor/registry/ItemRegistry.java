package com.jagtaczarmor.registry;

import com.jagtaczarmor.item.CustomGeoArmorItem;
import com.jagtaczarmor.item.CustomPlateItem;
import com.jagtaczarmor.data.ArmorIndex;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS;
    public static final RegistryObject<Item> CUSTOM_HELMET;
    public static final RegistryObject<Item> CUSTOM_CHESTPLATE;
    public static final RegistryObject<Item> CUSTOM_LEGGINGS;
    public static final RegistryObject<Item> CUSTOM_BOOTS;
    public static final RegistryObject<Item> TAB_ICON;
    public static final RegistryObject<Item> PLATE_ARMOR;

    // ===== НАШЕ ДОПОЛНЕНИЕ =====
    private static final Map<String, RegistryObject<Item>> ADDON_ITEMS = new HashMap<>();

    public static RegistryObject<Item> registerAddonArmor(String id, ArmorIndex index, Type slot) {
        if (ADDON_ITEMS.containsKey(id)) {
            return ADDON_ITEMS.get(id);
        }
        RegistryObject<Item> item = ITEMS.register(id, () -> {
            return new CustomGeoArmorItem(slot, new Item.Properties(), index, id);
        });
        ADDON_ITEMS.put(id, item);
        return item;
    }

    public static RegistryObject<Item> getAddonItem(String id) {
        return ADDON_ITEMS.get(id);
    }

    public static boolean isAddonArmor(String id) {
        return ADDON_ITEMS.containsKey(id);
    }
    // ===== КОНЕЦ НАШЕГО ДОПОЛНЕНИЯ =====

    public ItemRegistry() {
    }

    static {
        ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, "jagtaczarmor");
        CUSTOM_HELMET = ITEMS.register("tk_hm", () -> new CustomGeoArmorItem(Type.HELMET, new Item.Properties()));
        CUSTOM_CHESTPLATE = ITEMS.register("tk_ch", () -> new CustomGeoArmorItem(Type.CHESTPLATE, new Item.Properties()));
        CUSTOM_LEGGINGS = ITEMS.register("tk_lg", () -> new CustomGeoArmorItem(Type.LEGGINGS, new Item.Properties()));
        CUSTOM_BOOTS = ITEMS.register("tk_bt", () -> new CustomGeoArmorItem(Type.BOOTS, new Item.Properties()));
        TAB_ICON = ITEMS.register("tab_icon", () -> new Item(new Item.Properties()));
        PLATE_ARMOR = ITEMS.register("plate_armor", () -> new CustomPlateItem((new Item.Properties()).stacksTo(4)));
    }
}