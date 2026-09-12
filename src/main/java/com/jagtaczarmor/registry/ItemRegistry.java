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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
     * Динамические предметы из armor pack'ов.
     *
     * Ключ:
     *     полный registry ID предмета
     *
     * Например:
     *     my_pack:tactical_helmet
     */
    private static final Map<ResourceLocation, Item> ADDON_ITEMS = new HashMap<>();

    /**
     * Уже зарегистрированные динамические предметы.
     */
    private static final Set<ResourceLocation> REGISTERED_DYNAMIC_ITEMS = new HashSet<>();

    public ItemRegistry() {
    }

    /**
     * Регистрация динамического armor item.
     *
     * В отличие от старой версии здесь namespace больше
     * не привязан к "jagtaczarmor".
     *
     * Реальный ID приходит из items.json.
     */
    public static void registerAddonArmor(
            ResourceLocation itemId,
            CustomGeoArmorItem item
    ) {
        if (itemId == null || item == null) {
            return;
        }

        if (!REGISTERED_DYNAMIC_ITEMS.add(itemId)) {
            return;
        }

        ADDON_ITEMS.put(itemId, item);
    }

    /**
     * Получить динамический предмет по полному registry ID.
     */
    public static Item getAddonItem(ResourceLocation id) {
        return ADDON_ITEMS.get(id);
    }

    /**
     * Проверка динамического armor item.
     */
    public static boolean isAddonArmor(ResourceLocation id) {
        return id != null && ADDON_ITEMS.containsKey(id);
    }

    /**
     * Регистрация динамических предметов из armor pack'ов.
     *
     * Сам список предметов берётся из items.json через AddonPackLoader.
     */
    @SubscribeEvent
    public static void onItemRegister(RegisterEvent event) {

        if (!event.getRegistryKey().equals(ForgeRegistries.ITEMS.getRegistryKey())) {
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
         * Они нужны для совместимости со старой системой
         * и существующими ItemStack.
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
                () -> new Item(new Item.Properties())
        );

        PLATE_ARMOR = ITEMS.register(
                "plate_armor",
                () -> new CustomPlateItem(
                        new Item.Properties().stacksTo(4)
                )
        );
    }
}