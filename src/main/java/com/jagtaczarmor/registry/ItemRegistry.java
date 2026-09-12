package com.jagtaczarmor.registry;

import com.google.gson.Gson;
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

    public static final RegistryObject<Item> TAB_ICON;

    /**
     * Реальные зарегистрированные plate items.
     *
     * Ключ:
     *
     *     lrarmor_pack:ceramic_plate
     *
     * Значение:
     *
     *     конкретный CustomPlateItem
     */
    private static final Map<ResourceLocation, CustomPlateItem>
            PLATE_ITEMS = new HashMap<>();

    private static final Gson GSON = new Gson();

    /**
     * Реальные registry ID addon-предметов,
     * зарегистрированных в текущем RegisterEvent.
     */
    private static final Set<ResourceLocation> REGISTERED_DYNAMIC_ITEMS =
            new HashSet<>();

    /**
     * RegistryObject динамических addon-предметов.
     *
     * Используется старым API и JEI.
     */
    private static final Map<ResourceLocation, RegistryObject<Item>>
            ADDON_ITEMS = new HashMap<>();

    /**
     * Реальные экземпляры CustomGeoArmorItem.
     *
     * Используется CreativeTabRegistry.
     */
    private static final Map<ResourceLocation, CustomGeoArmorItem>
            ADDON_ITEM_INSTANCES = new HashMap<>();

    private ItemRegistry() {
    }

    /**
     * Forge registration event.
     */
    @SubscribeEvent
    public static void onItemRegister(RegisterEvent event) {

        if (!event.getRegistryKey().equals(
                ForgeRegistries.ITEMS.getRegistryKey()
        )) {
            return;
        }

        registerAddonItems(event);
        registerPlateItems(event);
    }

    /**
     * Читает items.json из JagTaczArmor armor-паков.
     *
     * Поддерживаемый формат:
     *
     * {
     *   "armor": {
     *     "atf_helmet": {
     *       "item_type": "armor"
     *     },
     *     "atf_chestplate": {
     *       "item_type": "armor"
     *     }
     *   }
     * }
     *
     * Как в TaCZ:
     *
     * ключ объекта = имя item/armor.
     *
     * Например:
     *
     * atf_helmet
     *
     * превращается в:
     *
     * lrarmor_pack:atf_helmet
     *
     * если соответствующий armor JSON имеет namespace
     * lrarmor_pack.
     */
    private static void registerAddonItems(RegisterEvent event) {

        REGISTERED_DYNAMIC_ITEMS.clear();
        ADDON_ITEMS.clear();
        ADDON_ITEM_INSTANCES.clear();

        Path taczDir =
                net.minecraftforge.fml.loading.FMLPaths.GAMEDIR
                        .get()
                        .resolve("tacz");

        File[] packs =
                taczDir.toFile().listFiles();

        if (packs == null) {

            JagTaczArmor.LOGGER.warn(
                    "No JagTaczArmor packs directory found: {}",
                    taczDir
            );

            return;
        }

        for (File pack : packs) {

            /*
             * =========================================================
             * DIRECTORY PACK
             * =========================================================
             */

            if (pack.isDirectory()) {

                Path packPath =
                        pack.toPath();

                Path armorMeta =
                        packPath.resolve(
                                "armorpack.meta.json"
                        );

                /*
                 * Обычные TaCZ gun-паки игнорируем.
                 */
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
                 * =====================================================
                 * ZIP PACK
                 * =====================================================
                 */

            } else if (
                    pack.isFile()
                            && pack.getName()
                            .toLowerCase()
                            .endsWith(".zip")
            ) {

                if (!isArmorZipPack(
                        pack.toPath()
                )) {

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
     * Проверяет ZIP на наличие armorpack.meta.json.
     */
    private static boolean isArmorZipPack(
            Path packPath
    ) {

        try (
                ZipFile zipFile =
                        new ZipFile(
                                packPath.toFile()
                        )
        ) {

            return zipFile.getEntry(
                    "armorpack.meta.json"
            ) != null;

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
     * Регистрация items.json из directory pack.
     */
    private static void registerDirectoryPack(
            RegisterEvent event,
            Path packPath
    ) {

        Path itemsPath =
                packPath.resolve(
                        "items.json"
                );

        if (!Files.isRegularFile(itemsPath)) {
            return;
        }

        try (
                InputStream stream =
                        Files.newInputStream(
                                itemsPath
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
     * Регистрация items.json из ZIP pack.
     */
    private static void registerZipPack(
            RegisterEvent event,
            Path packPath
    ) {

        try (
                ZipFile zipFile =
                        new ZipFile(
                                packPath.toFile()
                        )
        ) {

            ZipEntry manifestEntry =
                    zipFile.getEntry(
                            "items.json"
                    );

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
     * Читает TaCZ-подобную структуру:
     *
     * "armor": {
     *     "atf_helmet": {
     *         "item_type": "armor"
     *     },
     *     "atf_chestplate": {
     *         "item_type": "armor"
     *     }
     * }
     */
    private static void registerArmorGroup(
            RegisterEvent event,
            JsonObject root,
            String source
    ) {

        if (!root.has("armor")
                || !root.get("armor").isJsonObject()) {

            JagTaczArmor.LOGGER.warn(
                    "Invalid or missing 'armor' object in items manifest '{}'.",
                    source
            );

            return;
        }

        JsonObject armorObject =
                root.getAsJsonObject(
                        "armor"
                );

        for (
                Map.Entry<String, JsonElement> entry :
                armorObject.entrySet()
        ) {

            String armorName =
                    entry.getKey();

            JsonElement element =
                    entry.getValue();

            if (!element.isJsonObject()) {

                JagTaczArmor.LOGGER.warn(
                        "Invalid armor entry '{}' in '{}'.",
                        armorName,
                        source
                );

                continue;
            }

            JsonObject itemObject =
                    element.getAsJsonObject();

            registerSingleArmorItem(
                    event,
                    armorName,
                    itemObject,
                    source
            );
        }
    }

    /**
     * Регистрирует одну броню из entries:
     *
     * "atf_helmet": {
     *     "item_type": "armor"
     * }
     */
    private static void registerSingleArmorItem(
            RegisterEvent event,
            String armorName,
            JsonObject itemObject,
            String source
    ) {

        String itemType =
                getString(
                        itemObject,
                        "item_type",
                        "armor"
                );

        if (!"armor".equalsIgnoreCase(
                itemType
        )) {

            JagTaczArmor.LOGGER.warn(
                    "Unsupported item_type '{}' for '{}' in '{}'.",
                    itemType,
                    armorName,
                    source
            );

            return;
        }

        /*
         * ------------------------------------------------------------
         * Ищем armor JSON по имени ключа.
         *
         * Например:
         *
         * atf_helmet
         *
         * ищется среди:
         *
         * lrarmor_pack:atf_helmet
         * jag_default_armor:atf_helmet
         * и т.д.
         * ------------------------------------------------------------
         */

        ResourceLocation armorId =
                findArmorId(
                        armorName
                );

        if (armorId == null) {

            JagTaczArmor.LOGGER.warn(
                    "Armor definition '{}' referenced by items.json "
                            + "was not found in loaded armor definitions. "
                            + "Source='{}'.",
                    armorName,
                    source
            );

            return;
        }

        /*
         * В новом формате ключ items.json
         * является одновременно item ID.
         *
         * Поэтому:
         *
         * armor_id = lrarmor_pack:atf_helmet
         * item_id  = lrarmor_pack:atf_helmet
         */
        ResourceLocation itemId =
                armorId;

        if (
                REGISTERED_DYNAMIC_ITEMS.contains(
                        itemId
                )
        ) {

            JagTaczArmor.LOGGER.warn(
                    "Skipping duplicate dynamic armor item '{}'.",
                    itemId
            );

            return;
        }

        /*
         * Если такой Minecraft registry ID уже существует,
         * повторно его регистрировать нельзя.
         */
        if (
                ForgeRegistries.ITEMS.containsKey(
                        itemId
                )
        ) {

            JagTaczArmor.LOGGER.warn(
                    "Skipping armor item '{}' because registry ID "
                            + "is already occupied.",
                    itemId
            );

            return;
        }

        ArmorSetIndex set =
                AddonPackLoader.ARMOR_SET_INDEXES.get(
                        armorId
                );

        if (set == null) {

            JagTaczArmor.LOGGER.warn(
                    "Armor definition '{}' not found for item '{}'.",
                    armorId,
                    itemId
            );

            return;
        }

        ArmorDefinition definition =
                findArmorDefinition(
                        set
                );

        if (definition == null) {

            JagTaczArmor.LOGGER.warn(
                    "Armor definition '{}' contains no armor piece.",
                    armorId
            );

            return;
        }

        /*
         * Создаём настоящий Minecraft Item.
         */
        CustomGeoArmorItem item =
                new CustomGeoArmorItem(
                        definition.slot(),
                        new Item.Properties()
                                .stacksTo(1),
                        definition.index(),
                        itemId.toString()
                );

        /*
         * Регистрируем item под настоящим ID.
         */
        event.register(
                ForgeRegistries.ITEMS.getRegistryKey(),
                helper ->
                        helper.register(
                                itemId,
                                item
                        )
        );

        REGISTERED_DYNAMIC_ITEMS.add(
                itemId
        );

        /*
         * RegistryObject для совместимости
         * со старым API / JEI.
         */
        RegistryObject<Item> registryObject =
                RegistryObject.create(
                        itemId,
                        ForgeRegistries.ITEMS
                );

        ADDON_ITEMS.put(
                itemId,
                registryObject
        );

        /*
         * Реальный объект для Creative Tab.
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

    /**
     * Регистрирует каждую armor plate как отдельный Minecraft Item.
     *
     * Например:
     *
     * jag_default_armor:ceramic_plate
     *
     * или:
     *
     * lrarmor_pack:steel_plate
     *
     * становятся настоящими registry ID предметов.
     */
    private static void registerPlateItems(
            RegisterEvent event
    ) {

        PLATE_ITEMS.clear();

        for (
                ResourceLocation plateId :
                AddonPackLoader.PLATE_INDEXES.keySet()
        ) {

            if (plateId == null) {
                continue;
            }

            /*
             * Если такой ID уже существует в Minecraft,
             * повторно регистрировать его нельзя.
             */
            if (
                    ForgeRegistries.ITEMS.containsKey(
                            plateId
                    )
            ) {

                JagTaczArmor.LOGGER.warn(
                        "Skipping plate item '{}' because registry ID "
                                + "is already occupied.",
                        plateId
                );

                continue;
            }

            CustomPlateItem plateItem =
                    new CustomPlateItem(
                            new Item.Properties()
                                    .stacksTo(4),
                            plateId
                    );

            event.register(
                    ForgeRegistries.ITEMS.getRegistryKey(),
                    helper ->
                            helper.register(
                                    plateId,
                                    plateItem
                            )
            );

            PLATE_ITEMS.put(
                    plateId,
                    plateItem
            );

            JagTaczArmor.LOGGER.info(
                    "Registered addon plate item: item_id='{}'",
                    plateId
            );
        }
    }

    /**
     * Находит реальный ResourceLocation armor JSON
     * по имени ключа из items.json.
     *
     * Например:
     *
     * "atf_helmet"
     *
     * -> lrarmor_pack:atf_helmet
     */
    private static ResourceLocation findArmorId(
            String armorName
    ) {

        if (armorName == null
                || armorName.isBlank()) {

            return null;
        }

        /*
         * Сначала ищем точное совпадение path.
         */
        for (
                ResourceLocation id :
                AddonPackLoader.ARMOR_SET_INDEXES.keySet()
        ) {

            if (id.getPath().equals(
                    armorName
            )) {

                return id;
            }
        }

        /*
         * На случай если в items.json
         * каким-либо образом уже указан namespace.
         */
        ResourceLocation explicitId =
                ResourceLocation.tryParse(
                        armorName
                );

        if (explicitId != null
                && AddonPackLoader.ARMOR_SET_INDEXES.containsKey(
                explicitId
        )) {

            return explicitId;
        }

        return null;
    }

    /**
     * Определяет конкретную часть брони.
     */
    private static ArmorDefinition findArmorDefinition(
            ArmorSetIndex set
    ) {

        if (set == null) {
            return null;
        }

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
     * Возвращает настоящий Item плиты
     * по её registry ID.
     *
     * Например:
     *
     * lrarmor_pack:ceramic_plate
     */
    public static Item getPlateItem(
            ResourceLocation id
    ) {

        return PLATE_ITEMS.get(
                id
        );
    }

    /**
     * Реальные экземпляры addon armor.
     *
     * Используется CreativeTabRegistry.
     */
    public static Map<ResourceLocation, CustomGeoArmorItem>
    getAddonItems() {

        return ADDON_ITEM_INSTANCES;
    }

    /**
     * RegistryObject addon armor.
     *
     * Используется старым кодом.
     */
    public static RegistryObject<Item> getAddonItem(
            ResourceLocation id
    ) {

        return ADDON_ITEMS.get(
                id
        );
    }

    /**
     * RegistryObject addon armor по строковому ID.
     */
    public static RegistryObject<Item> getAddonItem(
            String id
    ) {

        ResourceLocation resourceLocation =
                ResourceLocation.tryParse(
                        id
                );

        if (resourceLocation == null) {
            return null;
        }

        return getAddonItem(
                resourceLocation
        );
    }

    /**
     * Все RegistryObject addon items.
     *
     * Этот метод нужен, в частности, JEI.
     */
    public static Map<ResourceLocation, RegistryObject<Item>>
    getAddonItemRegistryObjects() {

        return ADDON_ITEMS;
    }

    public static boolean isAddonArmor(
            ResourceLocation id
    ) {

        return ADDON_ITEM_INSTANCES.containsKey(
                id
        );
    }

    public static boolean isAddonArmor(
            String id
    ) {

        ResourceLocation resourceLocation =
                ResourceLocation.tryParse(
                        id
                );

        return resourceLocation != null
                && isAddonArmor(
                resourceLocation
        );
    }

    private record ArmorDefinition(
            ArmorIndex index,
            Type slot
    ) {
    }

    static {

        ITEMS =
                DeferredRegister.create(
                        ForgeRegistries.ITEMS,
                        JagTaczArmor.MODID
                );

        /*
         * Старые универсальные:
         *
         * tk_hm
         * tk_ch
         * tk_lg
         * tk_bt
         *
         * БОЛЬШЕ НЕ РЕГИСТРИРУЮТСЯ.
         */

        TAB_ICON =
                ITEMS.register(
                        "tab_icon",
                        () ->
                                new Item(
                                        new Item.Properties()
                                )
                );
    }
}