package com.jagtaczarmor.registry;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jagtaczarmor.JagTaczArmor;
import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.data.ArmorIndex;
import com.jagtaczarmor.data.ArmorSetIndex;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Mod.EventBusSubscriber(
        modid = JagTaczArmor.MODID,
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

    private static final Gson GSON = new Gson();

    /**
     * IDs динамических предметов, уже зарегистрированных через RegisterEvent.
     */
    private static final Set<ResourceLocation> REGISTERED_DYNAMIC_ITEMS =
            new HashSet<>();

    /**
     * Совместимость со старым API.
     *
     * Здесь находятся RegistryObject для динамических предметов.
     */
    private static final Map<ResourceLocation, RegistryObject<Item>> ADDON_ITEMS =
            new HashMap<>();

    /**
     * Реальные экземпляры CustomGeoArmorItem.
     *
     * CreativeTabRegistry использует именно эту коллекцию.
     */
    private static final Map<ResourceLocation, CustomGeoArmorItem>
            ADDON_ITEM_INSTANCES = new HashMap<>();

    private ItemRegistry() {
    }

    /**
     * Регистрация обычных и динамических предметов.
     */
    @SubscribeEvent
    public static void onItemRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(
                ForgeRegistries.ITEMS.getRegistryKey()
        )) {
            return;
        }

        registerAddonItems(event);
    }

    /**
     * Читает items.json ТОЛЬКО из JagTaczArmor armor-паков.
     *
     * Пак считается armor-паком, если содержит:
     *
     *     armorpack.meta.json
     *
     * Поэтому:
     *
     *     .minecraft/tacz/jag_default_armor/
     *         armorpack.meta.json
     *         items.json
     *
     * будет обработан.
     *
     *     .minecraft/tacz/lrarmor_pack/
     *         armorpack.meta.json
     *         items.json
     *
     * тоже будет обработан.
     *
     * А:
     *
     *     .minecraft/tacz/tacz_default_gun/
     *         gunpack.meta.json
     *         items.json
     *
     * полностью игнорируется.
     */
    private static void registerAddonItems(RegisterEvent event) {

        REGISTERED_DYNAMIC_ITEMS.clear();
        ADDON_ITEMS.clear();
        ADDON_ITEM_INSTANCES.clear();

        Path taczDir = net.minecraftforge.fml.loading.FMLPaths.GAMEDIR
                .get()
                .resolve("tacz");

        File[] packs = taczDir.toFile().listFiles();

        if (packs == null) {
            JagTaczArmor.LOGGER.warn(
                    "No JagTaczArmor packs directory found: {}",
                    taczDir
            );
            return;
        }

        for (File pack : packs) {

            /*
             * Обычная папка.
             */
            if (pack.isDirectory()) {

                Path packPath = pack.toPath();

                /*
                 * Только JagTaczArmor armor-паки.
                 *
                 * Если armorpack.meta.json отсутствует,
                 * это не наш пак и его items.json
                 * вообще не читаем.
                 */
                Path armorMeta = packPath.resolve(
                        "armorpack.meta.json"
                );

                if (!Files.isRegularFile(armorMeta)) {
                    JagTaczArmor.LOGGER.debug(
                            "Skipping non-armor pack: '{}'",
                            packPath
                    );
                    continue;
                }

                registerDirectoryPack(
                        event,
                        packPath
                );

                /*
                 * ZIP-пак.
                 */
            } else if (
                    pack.isFile()
                            && pack.getName()
                            .toLowerCase()
                            .endsWith(".zip")
            ) {

                /*
                 * Перед чтением items.json проверяем,
                 * что ZIP действительно является
                 * armor-паком JagTaczArmor.
                 */
                if (!isArmorZipPack(pack.toPath())) {
                    JagTaczArmor.LOGGER.debug(
                            "Skipping non-armor ZIP pack: '{}'",
                            pack.toPath()
                    );
                    continue;
                }

                registerZipPack(
                        event,
                        pack.toPath()
                );
            }
        }
    }

    /**
     * Проверяет ZIP-пак на наличие armorpack.meta.json.
     *
     * Если файла нет, ZIP не является armor-паком
     * JagTaczArmor и его items.json читать нельзя.
     */
    private static boolean isArmorZipPack(Path packPath) {

        try (
                ZipFile zipFile =
                        new ZipFile(packPath.toFile())
        ) {

            ZipEntry armorMeta =
                    zipFile.getEntry("armorpack.meta.json");

            return armorMeta != null;

        } catch (IOException exception) {

            JagTaczArmor.LOGGER.warn(
                    "Failed to inspect ZIP armor pack '{}'.",
                    packPath,
                    exception
            );

            return false;
        }
    }

    /**
     * Читает items.json из обычной папки armor-пака.
     */
    private static void registerDirectoryPack(
            RegisterEvent event,
            Path packPath
    ) {

        Path itemsPath =
                packPath.resolve("items.json");

        if (!Files.isRegularFile(itemsPath)) {
            return;
        }

        try (
                InputStream stream =
                        Files.newInputStream(itemsPath);

                InputStreamReader reader =
                        new InputStreamReader(
                                stream,
                                StandardCharsets.UTF_8
                        )
        ) {

            JsonObject root =
                    GSON.fromJson(
                            reader,
                            JsonObject.class
                    );

            if (root == null) {
                return;
            }

            registerArmorGroup(
                    event,
                    root,
                    itemsPath.toString()
            );

        } catch (
                IOException
                | RuntimeException exception
        ) {

            JagTaczArmor.LOGGER.error(
                    "Failed to load item manifest '{}'.",
                    itemsPath,
                    exception
            );
        }
    }

    /**
     * Читает items.json из ZIP armor-пака.
     */
    private static void registerZipPack(
            RegisterEvent event,
            Path packPath
    ) {

        try (
                ZipFile zipFile =
                        new ZipFile(packPath.toFile())
        ) {

            ZipEntry manifestEntry =
                    zipFile.getEntry("items.json");

            if (manifestEntry == null) {
                return;
            }

            try (
                    InputStream stream =
                            zipFile.getInputStream(
                                    manifestEntry
                            );

                    InputStreamReader reader =
                            new InputStreamReader(
                                    stream,
                                    StandardCharsets.UTF_8
                            )
            ) {

                JsonObject root =
                        GSON.fromJson(
                                reader,
                                JsonObject.class
                        );

                if (root == null) {
                    return;
                }

                registerArmorGroup(
                        event,
                        root,
                        packPath.toString()
                );
            }

        } catch (
                IOException
                | RuntimeException exception
        ) {

            JagTaczArmor.LOGGER.error(
                    "Failed to load item manifest from '{}'.",
                    packPath,
                    exception
            );
        }
    }

    /**
     * Новый формат items.json:
     *
     * {
     *   "armor": [
     *     {
     *       "item_id": "lrarmor_pack:atf_helmet",
     *       "armor_id": "lrarmor_pack:atf_helmet",
     *       "item_type": "armor"
     *     }
     *   ]
     * }
     */
    private static void registerArmorGroup(
            RegisterEvent event,
            JsonObject root,
            String source
    ) {

        if (!root.has("armor")
                || !root.get("armor").isJsonArray()) {

            JagTaczArmor.LOGGER.warn(
                    "Invalid or missing 'armor' array in items manifest '{}'.",
                    source
            );

            return;
        }

        JsonArray armorArray =
                root.getAsJsonArray("armor");

        for (JsonElement element : armorArray) {

            if (!element.isJsonObject()) {

                JagTaczArmor.LOGGER.warn(
                        "Invalid armor item entry in '{}'.",
                        source
                );

                continue;
            }

            JsonObject itemObject =
                    element.getAsJsonObject();

            String itemType =
                    getString(
                            itemObject,
                            "item_type",
                            "armor"
                    );

            if (!"armor".equals(itemType)) {

                JagTaczArmor.LOGGER.warn(
                        "Unsupported item_type '{}' in '{}'.",
                        itemType,
                        source
                );

                continue;
            }

            String itemIdString =
                    getString(
                            itemObject,
                            "item_id",
                            null
                    );

            String armorIdString =
                    getString(
                            itemObject,
                            "armor_id",
                            null
                    );

            if (itemIdString == null
                    || itemIdString.isBlank()) {

                JagTaczArmor.LOGGER.warn(
                        "Armor entry without 'item_id' in '{}'.",
                        source
                );

                continue;
            }

            if (armorIdString == null
                    || armorIdString.isBlank()) {

                JagTaczArmor.LOGGER.warn(
                        "Armor entry '{}' without 'armor_id' in '{}'.",
                        itemIdString,
                        source
                );

                continue;
            }

            ResourceLocation itemId =
                    ResourceLocation.tryParse(
                            itemIdString
                    );

            ResourceLocation armorId =
                    ResourceLocation.tryParse(
                            armorIdString
                    );

            if (itemId == null) {

                JagTaczArmor.LOGGER.warn(
                        "Invalid item_id '{}' in '{}'.",
                        itemIdString,
                        source
                );

                continue;
            }

            if (armorId == null) {

                JagTaczArmor.LOGGER.warn(
                        "Invalid armor_id '{}' in '{}'.",
                        armorIdString,
                        source
                );

                continue;
            }

            if (REGISTERED_DYNAMIC_ITEMS.contains(itemId)
                    || ForgeRegistries.ITEMS.containsKey(itemId)) {

                JagTaczArmor.LOGGER.warn(
                        "Skipping duplicate item registration '{}'.",
                        itemId
                );

                continue;
            }

            ArmorDefinition definition =
                    findArmorDefinition(armorId);

            if (definition == null) {

                JagTaczArmor.LOGGER.warn(
                        "Armor definition '{}' referenced by item '{}' "
                                + "was not found.",
                        armorId,
                        itemId
                );

                continue;
            }

            CustomGeoArmorItem item =
                    new CustomGeoArmorItem(
                            definition.slot(),
                            new Item.Properties(),
                            definition.index(),
                            itemId.toString()
                    );

            event.register(
                    ForgeRegistries.ITEMS.getRegistryKey(),
                    helper ->
                            helper.register(
                                    itemId,
                                    item
                            )
            );

            REGISTERED_DYNAMIC_ITEMS.add(itemId);

            /*
             * Сохраняем RegistryObject
             * для старого API.
             */
            ADDON_ITEMS.put(
                    itemId,
                    RegistryObject.create(
                            itemId,
                            ForgeRegistries.ITEMS
                    )
            );

            /*
             * Сохраняем реальный экземпляр.
             *
             * CreativeTabRegistry использует
             * именно эту коллекцию.
             */
            ADDON_ITEM_INSTANCES.put(
                    itemId,
                    item
            );

            JagTaczArmor.LOGGER.info(
                    "Registered addon armor item: item_id='{}', "
                            + "armor_id='{}', slot='{}', source='{}'",
                    itemId,
                    armorId,
                    definition.slot(),
                    source
            );
        }
    }

    /**
     * Находит ArmorSetIndex по armor_id.
     */
    private static ArmorDefinition findArmorDefinition(
            ResourceLocation armorId
    ) {

        ArmorSetIndex set =
                AddonPackLoader.ARMOR_SET_INDEXES.get(
                        armorId
                );

        if (set == null) {
            return null;
        }

        /*
         * В текущей системе каждый armor JSON
         * содержит одну конкретную часть брони.
         */
        if (set.helmet != null) {

            return new ArmorDefinition(
                    set.helmet,
                    Type.HELMET
            );
        }

        if (set.chestplate != null) {

            return new ArmorDefinition(
                    set.chestplate,
                    Type.CHESTPLATE
            );
        }

        if (set.leggings != null) {

            return new ArmorDefinition(
                    set.leggings,
                    Type.LEGGINGS
            );
        }

        if (set.boots != null) {

            return new ArmorDefinition(
                    set.boots,
                    Type.BOOTS
            );
        }

        return null;
    }

    private static String getString(
            JsonObject object,
            String key,
            String defaultValue
    ) {

        if (!object.has(key)
                || object.get(key).isJsonNull()) {

            return defaultValue;
        }

        try {

            return object
                    .get(key)
                    .getAsString();

        } catch (RuntimeException exception) {

            return defaultValue;
        }
    }

    /**
     * Возвращает реальные экземпляры
     * addon armor items.
     *
     * Используется CreativeTabRegistry.
     */
    public static Map<ResourceLocation, CustomGeoArmorItem>
    getAddonItems() {

        return ADDON_ITEM_INSTANCES;
    }

    /**
     * Старый API — RegistryObject
     * по ResourceLocation.
     */
    public static RegistryObject<Item> getAddonItem(
            ResourceLocation id
    ) {

        return ADDON_ITEMS.get(id);
    }

    /**
     * Старый API — поиск по строковому ID.
     */
    public static RegistryObject<Item> getAddonItem(
            String id
    ) {

        ResourceLocation resourceLocation =
                ResourceLocation.tryParse(id);

        if (resourceLocation == null) {
            return null;
        }

        return getAddonItem(
                resourceLocation
        );
    }

    public static boolean isAddonArmor(
            ResourceLocation id
    ) {

        return ADDON_ITEM_INSTANCES.containsKey(id);
    }

    public static boolean isAddonArmor(
            String id
    ) {

        ResourceLocation resourceLocation =
                ResourceLocation.tryParse(id);

        return resourceLocation != null
                && isAddonArmor(resourceLocation);
    }

    private record ArmorDefinition(
            ArmorIndex index,
            Type slot
    ) {
    }

    static {

        ITEMS = DeferredRegister.create(
                ForgeRegistries.ITEMS,
                JagTaczArmor.MODID
        );

        CUSTOM_HELMET =
                ITEMS.register(
                        "tk_hm",
                        () -> new CustomGeoArmorItem(
                                Type.HELMET,
                                new Item.Properties()
                        )
                );

        CUSTOM_CHESTPLATE =
                ITEMS.register(
                        "tk_ch",
                        () -> new CustomGeoArmorItem(
                                Type.CHESTPLATE,
                                new Item.Properties()
                        )
                );

        CUSTOM_LEGGINGS =
                ITEMS.register(
                        "tk_lg",
                        () -> new CustomGeoArmorItem(
                                Type.LEGGINGS,
                                new Item.Properties()
                        )
                );

        CUSTOM_BOOTS =
                ITEMS.register(
                        "tk_bt",
                        () -> new CustomGeoArmorItem(
                                Type.BOOTS,
                                new Item.Properties()
                        )
                );

        TAB_ICON =
                ITEMS.register(
                        "tab_icon",
                        () -> new Item(
                                new Item.Properties()
                        )
                );

        PLATE_ARMOR =
                ITEMS.register(
                        "plate_armor",
                        () -> new CustomPlateItem(
                                new Item.Properties()
                                        .stacksTo(4)
                        )
                );
    }
}