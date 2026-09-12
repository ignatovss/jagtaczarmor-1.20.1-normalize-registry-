package com.jagtaczarmor.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagtaczarmor.JagTaczArmor;
import com.jagtaczarmor.config.ArmorConfig;
import com.jagtaczarmor.item.CustomGeoArmorItem;
import com.jagtaczarmor.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.resource.DelegatingPackResources;
import net.minecraftforge.resource.PathPackResources;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@EventBusSubscriber(modid = "jagtaczarmor", bus = Bus.FORGE)
public class AddonPackLoader implements RepositorySource {
    public static final AddonPackLoader INSTANCE = new AddonPackLoader();

    private static final Gson GSON = new GsonBuilder().create();

    /**
     * Все загруженные armor pack'и.
     *
     * Для директории:
     * tacz/jag_default_armor
     *
     * Для zip:
     * tacz/some_pack.zip
     */
    private static final List<Path> LOADED_PACK_PATHS = new ArrayList<>();

    public static final Map<ResourceLocation, ArmorSetIndex> ARMOR_SET_INDEXES =
            new HashMap<>();

    public static final Map<ResourceLocation, Integer> ARMOR_SET_CMD =
            new HashMap<>();

    public static final Map<ResourceLocation, PlateIndex> PLATE_INDEXES =
            new HashMap<>();

    public static final Map<ResourceLocation, Integer> PLATE_CMD =
            new HashMap<>();

    public static final Map<Integer, String> PLATE_CMD_TEXTURES =
            new HashMap<>();

    private static final Map<Integer, ResourceLocation> CMD_TO_ARMOR_ID =
            new HashMap<>();

    private static final Map<Integer, ResourceLocation> CMD_TO_PLATE_ID =
            new HashMap<>();

    private static final List<PackResources> ACTIVE_PACKS =
            new ArrayList<>();

    public static final Map<String, PackMeta> PACK_METAS =
            new HashMap<>();

    public static final Map<String, ArmorIndex> TAGGED_ARMORS =
            new HashMap<>();

    private static final String[] PIECE_NAMES = new String[]{
            "tk_hm",
            "tk_ch",
            "tk_lg",
            "tk_bt"
    };

    private static final Map<Integer, Map<Integer, String>> CMD_TEXTURES =
            new HashMap<>();

    /**
     * Защита от повторной регистрации одного и того же registry ID
     * в рамках одного RegisterEvent.
     */
    private static final Set<ResourceLocation> REGISTERED_ITEM_IDS =
            new HashSet<>();

    public AddonPackLoader() {
    }

    public static int getOrCreateArmorCmd(ResourceLocation id) {
        if (ARMOR_SET_CMD.containsKey(id)) {
            return ARMOR_SET_CMD.get(id);
        }

        int cmd = Math.abs(id.toString().hashCode());

        if (cmd == 0) {
            cmd = 1;
        }

        while (
                CMD_TO_ARMOR_ID.containsKey(cmd)
                        && !CMD_TO_ARMOR_ID.get(cmd).equals(id)
        ) {
            ++cmd;
        }

        CMD_TO_ARMOR_ID.put(cmd, id);
        ARMOR_SET_CMD.put(id, cmd);

        return cmd;
    }

    public static int getOrCreatePlateCmd(ResourceLocation id) {
        if (PLATE_CMD.containsKey(id)) {
            return PLATE_CMD.get(id);
        }

        int cmd = Math.abs(id.toString().hashCode());

        if (cmd == 0) {
            cmd = 1;
        }

        while (
                CMD_TO_PLATE_ID.containsKey(cmd)
                        && !CMD_TO_PLATE_ID.get(cmd).equals(id)
        ) {
            ++cmd;
        }

        CMD_TO_PLATE_ID.put(cmd, id);
        PLATE_CMD.put(id, cmd);

        return cmd;
    }

    public static Map<Integer, Map<Integer, String>> getCmdTextures() {
        return CMD_TEXTURES;
    }

    public static String getCmdTexture(int cmd, int pieceIdx) {
        Map<Integer, String> map = CMD_TEXTURES.get(cmd);
        return map != null ? map.get(pieceIdx) : null;
    }

    public static void init() {
        Path taczDir = FMLPaths.GAMEDIR.get().resolve("tacz");
        Path defaultPackDir = taczDir.resolve("jag_default_armor");

        try {
            if (!Files.exists(taczDir, new LinkOption[0])) {
                Files.createDirectories(taczDir);
            }

            Path legacyGen = taczDir.resolve("jag_generated_resources");

            if (Files.exists(legacyGen, new LinkOption[0])) {
                try (Stream<Path> walk = Files.walk(legacyGen)) {
                    walk.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            }

        } catch (Exception e) {
            JagTaczArmor.LOGGER.error(
                    "Failed to initialize directories",
                    e
            );
        }

        boolean shouldExtract = true;

        if (Files.exists(defaultPackDir, new LinkOption[0])) {
            if (!ArmorConfig.DATA.overwrite_default_pack) {
                shouldExtract = false;
            } else {
                try (Stream<Path> walk = Files.walk(defaultPackDir)) {
                    walk.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (Exception e) {
                    JagTaczArmor.LOGGER.error(
                            "Failed to clean up old default pack",
                            e
                    );
                }
            }
        }

        if (shouldExtract) {
            extractDefaultPack(defaultPackDir);
        }

        reloadPacks();
    }

    public static void reloadPacks() {
        JagTaczArmor.LOGGER.info(
                "Reloading JagTaczArmor packs..."
        );

        LOADED_PACK_PATHS.clear();

        ARMOR_SET_INDEXES.clear();
        ARMOR_SET_CMD.clear();
        CMD_TEXTURES.clear();

        PLATE_INDEXES.clear();
        PLATE_CMD.clear();
        PLATE_CMD_TEXTURES.clear();

        ACTIVE_PACKS.clear();
        PACK_METAS.clear();
        TAGGED_ARMORS.clear();

        CMD_TO_ARMOR_ID.clear();
        CMD_TO_PLATE_ID.clear();

        Path taczDir =
                FMLPaths.GAMEDIR.get().resolve("tacz");

        ACTIVE_PACKS.add(
                new InMemoryPackResources()
        );

        File[] packs =
                taczDir.toFile().listFiles();

        if (packs == null) {
            return;
        }

        Arrays.sort(
                packs,
                (a, b) ->
                        a.getName().compareTo(b.getName())
        );

        for (File pack : packs) {

            /*
             * =========================================================
             * DIRECTORY PACK
             * =========================================================
             */
            if (
                    pack.isDirectory()
                            && !pack.getName().equals(
                            "jag_generated_resources"
                    )
            ) {
                File metaFile =
                        new File(
                                pack,
                                "armorpack.meta.json"
                        );

                if (!metaFile.exists()) {
                    continue;
                }

                PackMeta packMeta = null;

                try (
                        FileReader reader =
                                new FileReader(metaFile)
                ) {
                    packMeta =
                            GSON.fromJson(
                                    reader,
                                    PackMeta.class
                            );

                } catch (Exception e) {
                    JagTaczArmor.LOGGER.error(
                            "Failed to parse armorpack.meta.json for "
                                    + pack.getName(),
                            e
                    );
                }

                if (packMeta == null) {
                    packMeta = new PackMeta();
                }

                if (packMeta.name == null) {
                    packMeta.name = pack.getName();
                }

                PACK_METAS.put(
                        pack.getName(),
                        packMeta
                );

                File packMcMeta =
                        new File(
                                pack,
                                "pack.mcmeta"
                        );

                if (!packMcMeta.exists()) {
                    try {
                        Files.writeString(
                                packMcMeta.toPath(),
                                "{\"pack\":{\"pack_format\":15,\"description\":\"JagTaczArmor Addon\"}}"
                        );
                    } catch (Exception e) {
                        JagTaczArmor.LOGGER.error(
                                "Failed to generate pack.mcmeta for "
                                        + pack.getName(),
                                e
                        );
                    }
                }

                /*
                 * Сохраняем сам pack path.
                 *
                 * Позже registerAddonItems() прочитает
                 * items.json непосредственно из корня этого pack.
                 */
                LOADED_PACK_PATHS.add(
                        pack.toPath()
                );

                loadArmorsFromPack(
                        pack.toPath()
                );

                ACTIVE_PACKS.add(
                        new PathPackResources(
                                "jagtaczarmor_addon_" + pack.getName(),
                                true,
                                pack.toPath()
                        )
                );

                /*
                 * =========================================================
                 * ZIP PACK
                 * =========================================================
                 */
            } else if (
                    pack.isFile()
                            && pack.getName().endsWith(".zip")
            ) {
                String packName =
                        pack.getName().substring(
                                0,
                                pack.getName().length() - 4
                        );

                try (ZipFile zip = new ZipFile(pack)) {

                    ZipEntry metaEntry =
                            zip.getEntry(
                                    "armorpack.meta.json"
                            );

                    if (metaEntry == null) {
                        continue;
                    }

                    PackMeta packMeta = null;

                    try (
                            InputStream in =
                                    zip.getInputStream(metaEntry);

                            InputStreamReader reader =
                                    new InputStreamReader(
                                            in,
                                            StandardCharsets.UTF_8
                                    )
                    ) {
                        packMeta =
                                GSON.fromJson(
                                        reader,
                                        PackMeta.class
                                );

                    } catch (Exception e) {
                        JagTaczArmor.LOGGER.error(
                                "Failed to parse armorpack.meta.json inside "
                                        + pack.getName(),
                                e
                        );
                    }

                    if (packMeta == null) {
                        packMeta = new PackMeta();
                    }

                    if (packMeta.name == null) {
                        packMeta.name = packName;
                    }

                    PACK_METAS.put(
                            packName,
                            packMeta
                    );

                    /*
                     * ВАЖНО:
                     *
                     * ZIP тоже добавляем в LOADED_PACK_PATHS.
                     * registerAddonItems() ниже умеет читать
                     * items.json непосредственно из ZIP.
                     */
                    LOADED_PACK_PATHS.add(
                            pack.toPath()
                    );

                    loadArmorsFromZip(
                            pack,
                            packName
                    );

                    ACTIVE_PACKS.add(
                            new FilePackResources(
                                    "jagtaczarmor_addon_" + packName,
                                    pack,
                                    true
                            )
                    );

                } catch (Exception e) {
                    JagTaczArmor.LOGGER.error(
                            "Failed to process zip pack: "
                                    + pack.getName(),
                            e
                    );
                }
            }
        }
    }

    /**
     * ================================================================
     * НОВАЯ СИСТЕМА РЕГИСТРАЦИИ ПРЕДМЕТОВ
     * ================================================================
     *
     * Читает items.json из каждого armor pack.
     *
     * Пример:
     *
     * {
     *   "armor": {
     *     "tactical_armor_helmet": {
     *       "item_type": "custom_geo_armor",
     *       "armor_id": "jag_default_armor:tactical_armor",
     *       "slot": "helmet"
     *     }
     *   }
     * }
     *
     * В данном случае registry ID:
     *
     * jag_default_armor:tactical_armor_helmet
     *
     * а armor_id:
     *
     * jag_default_armor:tactical_armor
     *
     * является ссылкой на armor definition.
     */
    public static void registerAddonItems(RegisterEvent event) {
        if (
                !event.getRegistryKey().equals(
                        ForgeRegistries.ITEMS.getRegistryKey()
                )
        ) {
            return;
        }

        REGISTERED_ITEM_IDS.clear();

        JagTaczArmor.LOGGER.info(
                "[JagTaczArmor] Registering armor items from items.json..."
        );

        for (Path packPath : LOADED_PACK_PATHS) {
            try {
                if (Files.isDirectory(packPath)) {
                    registerItemsFromDirectory(
                            event,
                            packPath
                    );
                } else if (
                        Files.isRegularFile(packPath)
                                && packPath.getFileName()
                                .toString()
                                .endsWith(".zip")
                ) {
                    registerItemsFromZip(
                            event,
                            packPath
                    );
                }

            } catch (Exception e) {
                JagTaczArmor.LOGGER.error(
                        "[JagTaczArmor] Failed to register items from pack "
                                + packPath,
                        e
                );
            }
        }
    }

    private static void registerItemsFromDirectory(
            RegisterEvent event,
            Path packPath
    ) {
        Path itemsPath =
                packPath.resolve("items.json");

        if (!Files.exists(itemsPath)) {
            JagTaczArmor.LOGGER.info(
                    "[JagTaczArmor] No items.json in pack {}",
                    packPath.getFileName()
            );

            return;
        }

        String packName =
                packPath.getFileName().toString();

        try (
                FileReader reader =
                        new FileReader(
                                itemsPath.toFile()
                        )
        ) {
            JsonElement rootElement =
                    JsonParser.parseReader(reader);

            if (!rootElement.isJsonObject()) {
                JagTaczArmor.LOGGER.error(
                        "[JagTaczArmor] items.json in {} is not an object",
                        packName
                );

                return;
            }

            registerArmorItemsFromJson(
                    event,
                    rootElement.getAsJsonObject(),
                    packName
            );

        } catch (Exception e) {
            JagTaczArmor.LOGGER.error(
                    "[JagTaczArmor] Failed to read items.json from "
                            + packName,
                    e
            );
        }
    }

    private static void registerItemsFromZip(
            RegisterEvent event,
            Path packPath
    ) {
        String fileName =
                packPath.getFileName().toString();

        String packName =
                fileName.endsWith(".zip")
                        ? fileName.substring(
                        0,
                        fileName.length() - 4
                )
                        : fileName;

        try (ZipFile zip = new ZipFile(packPath.toFile())) {

            ZipEntry itemsEntry =
                    zip.getEntry("items.json");

            if (itemsEntry == null) {
                JagTaczArmor.LOGGER.info(
                        "[JagTaczArmor] No items.json in zip pack {}",
                        packName
                );

                return;
            }

            try (
                    InputStream in =
                            zip.getInputStream(itemsEntry);

                    InputStreamReader reader =
                            new InputStreamReader(
                                    in,
                                    StandardCharsets.UTF_8
                            )
            ) {
                JsonElement rootElement =
                        JsonParser.parseReader(reader);

                if (!rootElement.isJsonObject()) {
                    JagTaczArmor.LOGGER.error(
                            "[JagTaczArmor] items.json in zip pack {} is not an object",
                            packName
                    );

                    return;
                }

                registerArmorItemsFromJson(
                        event,
                        rootElement.getAsJsonObject(),
                        packName
                );
            }

        } catch (Exception e) {
            JagTaczArmor.LOGGER.error(
                    "[JagTaczArmor] Failed to read items.json from zip pack "
                            + packName,
                    e
            );
        }
    }

    private static void registerArmorItemsFromJson(
            RegisterEvent event,
            JsonObject root,
            String packName
    ) {
        JsonObject armorObject = null;

        /*
         * Основная группа.
         */
        if (root.has("armor")
                && root.get("armor").isJsonObject()) {

            armorObject =
                    root.getAsJsonObject("armor");
        }

        /*
         * Дополнительно поддерживаем "armors",
         * чтобы не ломать будущие/старые паки.
         */
        if (
                armorObject == null
                        && root.has("armors")
                        && root.get("armors").isJsonObject()
        ) {
            armorObject =
                    root.getAsJsonObject("armors");
        }

        if (armorObject == null) {
            JagTaczArmor.LOGGER.info(
                    "[JagTaczArmor] No 'armor' section in items.json of {}",
                    packName
            );

            return;
        }

        for (Map.Entry<String, JsonElement> entry :
                armorObject.entrySet()) {

            String itemPath =
                    entry.getKey();

            JsonElement value =
                    entry.getValue();

            if (!value.isJsonObject()) {
                JagTaczArmor.LOGGER.error(
                        "[JagTaczArmor] Item '{}' in pack {} is not an object",
                        itemPath,
                        packName
                );

                continue;
            }

            JsonObject itemObject =
                    value.getAsJsonObject();

            registerSingleArmorItem(
                    event,
                    packName,
                    itemPath,
                    itemObject
            );
        }
    }

    private static void registerSingleArmorItem(
            RegisterEvent event,
            String packName,
            String itemPath,
            JsonObject itemObject
    ) {
        String itemType =
                getString(
                        itemObject,
                        "item_type",
                        "custom_geo_armor"
                );

        /*
         * Пока armor group предназначен именно
         * для CustomGeoArmorItem.
         */
        if (!itemType.equalsIgnoreCase("custom_geo_armor")
                && !itemType.equalsIgnoreCase("armor")
                && !itemType.equalsIgnoreCase("custom_armor")) {

            JagTaczArmor.LOGGER.warn(
                    "[JagTaczArmor] Unknown armor item_type '{}' for {}:{}",
                    itemType,
                    packName,
                    itemPath
            );

            return;
        }

        ResourceLocation itemId =
                resolveItemId(
                        packName,
                        itemPath
                );

        if (itemId == null) {
            JagTaczArmor.LOGGER.error(
                    "[JagTaczArmor] Invalid item ID '{}:{}'",
                    packName,
                    itemPath
            );

            return;
        }

        if (REGISTERED_ITEM_IDS.contains(itemId)) {
            JagTaczArmor.LOGGER.warn(
                    "[JagTaczArmor] Duplicate armor item ID {}, skipping",
                    itemId
            );

            return;
        }

        String slot =
                getString(
                        itemObject,
                        "slot",
                        null
                );

        ArmorIndex armorIndex =
                resolveArmorIndex(
                        itemId,
                        itemObject,
                        slot
                );

        if (armorIndex == null) {
            JagTaczArmor.LOGGER.error(
                    "[JagTaczArmor] Could not resolve armor definition for item {}",
                    itemId
            );

            return;
        }

        Type armorType =
                resolveArmorType(
                        armorIndex,
                        slot
                );

        if (armorType == null) {
            JagTaczArmor.LOGGER.error(
                    "[JagTaczArmor] Could not resolve armor slot for item {}",
                    itemId
            );

            return;
        }

        CustomGeoArmorItem item =
                new CustomGeoArmorItem(
                        armorType,
                        new Item.Properties().stacksTo(1),
                        armorIndex,
                        itemId.toString()
                );

        /*
         * Сначала сохраняем item в ItemRegistry.
         */
        ItemRegistry.registerAddonArmor(
                itemId,
                item
        );

        /*
         * Затем регистрируем его непосредственно
         * в Minecraft Forge ITEM registry.
         */
        event.register(
                ForgeRegistries.ITEMS.getRegistryKey(),
                helper ->
                        helper.register(
                                itemId,
                                item
                        )
        );

        REGISTERED_ITEM_IDS.add(
                itemId
        );

        JagTaczArmor.LOGGER.info(
                "[JagTaczArmor] Registered armor item: {} | type={} | armor={} | tag={}",
                itemId,
                armorType,
                getArmorDefinitionId(itemObject, armorIndex),
                armorIndex.armorTag
        );
    }

    private static ResourceLocation resolveItemId(
            String packName,
            String itemPath
    ) {
        /*
         * Разрешаем явный namespace:
         *
         * "some_namespace:item_name"
         */
        if (itemPath.contains(":")) {
            ResourceLocation parsed =
                    ResourceLocation.tryParse(
                            itemPath
                    );

            if (parsed != null) {
                return parsed;
            }

            return null;
        }

        /*
         * Как и в TaCZ:
         *
         * namespace = имя gun/armor pack
         * path = ключ из items.json
         *
         * Например:
         *
         * pack:
         * jag_default_armor
         *
         * key:
         * tactical_armor_helmet
         *
         * result:
         * jag_default_armor:tactical_armor_helmet
         */
        return ResourceLocation.tryBuild(
                packName,
                itemPath
        );
    }

    private static ArmorIndex resolveArmorIndex(
            ResourceLocation itemId,
            JsonObject itemObject,
            String slot
    ) {
        /*
         * ------------------------------------------------------------
         * 1. Явный armor_tag
         * ------------------------------------------------------------
         */
        String armorTag =
                getString(
                        itemObject,
                        "armor_tag",
                        null
                );

        if (armorTag != null
                && !armorTag.trim().isEmpty()) {

            ArmorIndex tagged =
                    TAGGED_ARMORS.get(
                            armorTag.trim()
                    );

            if (tagged != null) {
                return tagged;
            }

            JagTaczArmor.LOGGER.warn(
                    "[JagTaczArmor] armor_tag '{}' was not found for {}",
                    armorTag,
                    itemId
            );
        }

        /*
         * ------------------------------------------------------------
         * 2. Явный armor_id
         * ------------------------------------------------------------
         *
         * Например:
         *
         * "armor_id": "jag_default_armor:tactical_armor"
         */
        String armorIdString =
                getString(
                        itemObject,
                        "armor_id",
                        null
                );

        if (armorIdString != null
                && !armorIdString.trim().isEmpty()) {

            ResourceLocation armorId =
                    ResourceLocation.tryParse(
                            armorIdString
                    );

            if (armorId != null) {
                ArmorSetIndex setIndex =
                        ARMOR_SET_INDEXES.get(
                                armorId
                        );

                if (setIndex != null) {
                    ArmorIndex piece =
                            getArmorPiece(
                                    setIndex,
                                    slot
                            );

                    if (piece != null) {
                        return piece;
                    }

                    /*
                     * Если slot не указан,
                     * попробуем определить часть
                     * по самому ArmorIndex.
                     */
                    ArmorIndex inferred =
                            inferOnlyArmorPiece(
                                    setIndex
                            );

                    if (inferred != null) {
                        return inferred;
                    }
                }

                JagTaczArmor.LOGGER.warn(
                        "[JagTaczArmor] armor_id '{}' was not found for {}",
                        armorId,
                        itemId
                );
            }
        }

        /*
         * ------------------------------------------------------------
         * 3. Автоматический поиск по ID предмета
         * ------------------------------------------------------------
         *
         * Например:
         *
         * tactical_armor_helmet
         *
         * превращается в:
         *
         * tactical_armor
         */
        String baseName =
                removeArmorPieceSuffix(
                        itemId.getPath()
                );

        if (!baseName.equals(itemId.getPath())) {

            ResourceLocation inferredSetId =
                    ResourceLocation.tryBuild(
                            getString(
                                    itemObject,
                                    "armor_namespace",
                                    itemId.getNamespace()
                            ),
                            baseName
                    );

            if (inferredSetId != null) {
                ArmorSetIndex setIndex =
                        ARMOR_SET_INDEXES.get(
                                inferredSetId
                        );

                if (setIndex != null) {
                    ArmorIndex piece =
                            getArmorPiece(
                                    setIndex,
                                    slot
                            );

                    if (piece != null) {
                        return piece;
                    }

                    ArmorIndex inferred =
                            inferArmorPieceFromItemId(
                                    setIndex,
                                    itemId
                            );

                    if (inferred != null) {
                        return inferred;
                    }
                }
            }
        }

        /*
         * Последняя попытка:
         * найти set по armor_id без slot.
         */
        return null;
    }

    private static ArmorIndex getArmorPiece(
            ArmorSetIndex setIndex,
            String slot
    ) {
        if (setIndex == null
                || slot == null
                || slot.trim().isEmpty()) {
            return null;
        }

        switch (slot.toLowerCase()) {
            case "helmet":
            case "head":
                return setIndex.helmet;

            case "chestplate":
            case "chest":
            case "torso":
                return setIndex.chestplate;

            case "leggings":
            case "legs":
                return setIndex.leggings;

            case "boots":
            case "feet":
                return setIndex.boots;

            default:
                return null;
        }
    }

    private static ArmorIndex inferArmorPieceFromItemId(
            ArmorSetIndex setIndex,
            ResourceLocation itemId
    ) {
        String path =
                itemId.getPath();

        if (path.endsWith("_helmet")
                || path.endsWith("_head")) {

            return setIndex.helmet;
        }

        if (path.endsWith("_chestplate")
                || path.endsWith("_chest")
                || path.endsWith("_torso")) {

            return setIndex.chestplate;
        }

        if (path.endsWith("_leggings")
                || path.endsWith("_legs")) {

            return setIndex.leggings;
        }

        if (path.endsWith("_boots")
                || path.endsWith("_feet")) {

            return setIndex.boots;
        }

        return null;
    }

    private static ArmorIndex inferOnlyArmorPiece(
            ArmorSetIndex setIndex
    ) {
        ArmorIndex result = null;
        int count = 0;

        if (setIndex.helmet != null) {
            result = setIndex.helmet;
            ++count;
        }

        if (setIndex.chestplate != null) {
            result = setIndex.chestplate;
            ++count;
        }

        if (setIndex.leggings != null) {
            result = setIndex.leggings;
            ++count;
        }

        if (setIndex.boots != null) {
            result = setIndex.boots;
            ++count;
        }

        /*
         * Возвращаем автоматически только если
         * в set действительно одна часть.
         */
        return count == 1 ? result : null;
    }

    private static String removeArmorPieceSuffix(
            String path
    ) {
        String[] suffixes = new String[]{
                "_chestplate",
                "_helmet",
                "_leggings",
                "_boots",
                "_chest",
                "_torso",
                "_head",
                "_legs",
                "_feet"
        };

        for (String suffix : suffixes) {
            if (path.endsWith(suffix)) {
                return path.substring(
                        0,
                        path.length() - suffix.length()
                );
            }
        }

        return path;
    }

    private static Type resolveArmorType(
            ArmorIndex armorIndex,
            String slot
    ) {
        if (slot != null
                && !slot.trim().isEmpty()) {

            switch (slot.toLowerCase()) {
                case "helmet":
                case "head":
                    return Type.HELMET;

                case "chestplate":
                case "chest":
                case "torso":
                    return Type.CHESTPLATE;

                case "leggings":
                case "legs":
                    return Type.LEGGINGS;

                case "boots":
                case "feet":
                    return Type.BOOTS;
            }
        }

        if (armorIndex != null
                && armorIndex.slot != null) {

            switch (
                    armorIndex.slot.toLowerCase()
            ) {
                case "helmet":
                case "head":
                    return Type.HELMET;

                case "chestplate":
                case "chest":
                case "torso":
                    return Type.CHESTPLATE;

                case "leggings":
                case "legs":
                    return Type.LEGGINGS;

                case "boots":
                case "feet":
                    return Type.BOOTS;
            }
        }

        return null;
    }

    private static String getArmorDefinitionId(
            JsonObject itemObject,
            ArmorIndex armorIndex
    ) {
        String armorId =
                getString(
                        itemObject,
                        "armor_id",
                        null
                );

        if (armorId != null) {
            return armorId;
        }

        if (armorIndex != null
                && armorIndex.name != null) {
            return armorIndex.name;
        }

        return "unknown";
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
            return object.get(key).getAsString();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static void extractDefaultPack(
            Path defaultPackDir
    ) {
        try {
            URL resource =
                    AddonPackLoader.class.getResource(
                            "/default_pack.zip"
                    );

            if (resource != null) {
                File zipFile =
                        File.createTempFile(
                                "default_pack",
                                ".zip"
                        );

                try (InputStream in =
                             resource.openStream()) {

                    Files.copy(
                            in,
                            zipFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                ZipFile zip =
                        new ZipFile(zipFile);

                Enumeration<? extends ZipEntry> entries =
                        zip.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry =
                            entries.nextElement();

                    String name =
                            entry.getName();

                    name =
                            name.replace(
                                    "\\",
                                    "/"
                            );

                    if (name.startsWith("/")) {
                        name =
                                name.substring(1);
                    }

                    File destFile =
                            new File(
                                    defaultPackDir.toFile(),
                                    name
                            );

                    if (
                            !entry.isDirectory()
                                    && !name.endsWith("/")
                    ) {
                        File parent =
                                destFile.getParentFile();

                        if (
                                parent != null
                                        && !parent.exists()
                        ) {
                            parent.mkdirs();
                        }

                        try (
                                InputStream in =
                                        zip.getInputStream(entry)
                        ) {
                            Files.copy(
                                    in,
                                    destFile.toPath(),
                                    StandardCopyOption.REPLACE_EXISTING
                            );
                        }

                    } else if (!destFile.exists()) {
                        destFile.mkdirs();
                    }
                }

                zip.close();

                if (zipFile.exists()) {
                    zipFile.delete();
                }

                JagTaczArmor.LOGGER.info(
                        "Successfully extracted default_pack.zip to "
                                + defaultPackDir
                );

            } else {
                JagTaczArmor.LOGGER.error(
                        "Could not find default_pack.zip in resources!"
                );
            }

        } catch (Exception e) {
            JagTaczArmor.LOGGER.error(
                    "Failed to extract default pack",
                    e
            );
        }
    }

    private static void loadArmorsFromPack(
            Path packPath
    ) {
        Path dataPath =
                packPath.resolve("data");

        if (
                !Files.exists(
                        dataPath,
                        new LinkOption[0]
                )
                        || !Files.isDirectory(
                        dataPath,
                        new LinkOption[0]
                )
        ) {
            return;
        }

        File[] namespaceDirs =
                dataPath.toFile().listFiles(
                        File::isDirectory
                );

        if (namespaceDirs == null) {
            return;
        }

        String packName =
                packPath.getFileName().toString();

        for (File namespaceDir :
                namespaceDirs) {

            String namespace =
                    namespaceDir.getName();

            File dataSubDir =
                    new File(
                            namespaceDir,
                            "data"
                    );

            File[] armorDirs;

            if (
                    dataSubDir.exists()
                            && dataSubDir.isDirectory()
            ) {
                armorDirs = new File[]{
                        new File(
                                dataSubDir,
                                "armors"
                        ),
                        new File(
                                dataSubDir,
                                "armor"
                        )
                };
            } else {
                armorDirs = new File[]{
                        new File(
                                namespaceDir,
                                "armors"
                        ),
                        new File(
                                namespaceDir,
                                "armor"
                        )
                };
            }

            for (File armorsDir :
                    armorDirs) {

                if (
                        !armorsDir.exists()
                                || !armorsDir.isDirectory()
                ) {
                    continue;
                }

                File[] jsonFiles =
                        armorsDir.listFiles(
                                (dir, name) ->
                                        name.endsWith(".json")
                        );

                if (jsonFiles == null) {
                    continue;
                }

                Arrays.sort(
                        jsonFiles,
                        (a, b) ->
                                a.getName().compareTo(
                                        b.getName()
                                )
                );

                for (File jsonFile :
                        jsonFiles) {

                    try (
                            FileReader reader =
                                    new FileReader(jsonFile)
                    ) {
                        JsonElement element =
                                JsonParser.parseReader(
                                        reader
                                );

                        if (!element.isJsonObject()) {
                            continue;
                        }

                        JsonObject obj =
                                element.getAsJsonObject();

                        ArmorSetIndex index;

                        String armorSetName =
                                jsonFile.getName()
                                        .replace(
                                                ".json",
                                                ""
                                        );

                        ResourceLocation id =
                                new ResourceLocation(
                                        namespace,
                                        armorSetName
                                );

                        if (!obj.has("slot")) {
                            index =
                                    GSON.fromJson(
                                            obj,
                                            ArmorSetIndex.class
                                    );

                        } else {
                            ArmorIndex piece =
                                    GSON.fromJson(
                                            obj,
                                            ArmorIndex.class
                                    );

                            index =
                                    new ArmorSetIndex();

                            String nameToUse =
                                    piece.displayName;

                            if (
                                    (nameToUse == null
                                            || "Custom Armor".equals(
                                            nameToUse))
                                            && piece.name != null
                            ) {
                                nameToUse =
                                        piece.name;
                            }

                            index.name =
                                    nameToUse;

                            if (piece.slot != null) {
                                switch (
                                        piece.slot.toLowerCase()
                                ) {
                                    case "helmet":
                                    case "head":
                                        index.helmet =
                                                piece;
                                        break;

                                    case "chestplate":
                                    case "torso":
                                    case "chest":
                                        index.chestplate =
                                                piece;
                                        break;

                                    case "leggings":
                                    case "legs":
                                        index.leggings =
                                                piece;
                                        break;

                                    case "boots":
                                    case "feet":
                                        index.boots =
                                                piece;
                                        break;
                                }
                            }
                        }

                        ARMOR_SET_INDEXES.put(
                                id,
                                index
                        );

                        if (index.helmet != null) {
                            index.helmet.plateSlot =
                                    false;

                            index.helmet.packNamespace =
                                    namespace;
                        }

                        if (index.chestplate != null) {
                            index.chestplate.packNamespace =
                                    namespace;
                        }

                        if (index.leggings != null) {
                            index.leggings.plateSlot =
                                    false;

                            index.leggings.packNamespace =
                                    namespace;
                        }

                        if (index.boots != null) {
                            index.boots.plateSlot =
                                    false;

                            index.boots.packNamespace =
                                    namespace;
                        }

                        registerTaggedPiece(
                                index.helmet,
                                packName
                        );

                        registerTaggedPiece(
                                index.chestplate,
                                packName
                        );

                        registerTaggedPiece(
                                index.leggings,
                                packName
                        );

                        registerTaggedPiece(
                                index.boots,
                                packName
                        );

                        int cmd =
                                getOrCreateArmorCmd(
                                        id
                                );

                        collectItemTextures(
                                index,
                                cmd
                        );

                        /*
                         * НИКАКОЙ регистрации Item здесь больше нет.
                         *
                         * Раньше здесь был:
                         *
                         * ItemRegistry.registerAddonArmor(...)
                         *
                         * Теперь Item создаётся только из items.json
                         * во время RegisterEvent.
                         */

                        JagTaczArmor.LOGGER.info(
                                "Loaded armor definition: "
                                        + id
                                        + " (CMD="
                                        + cmd
                                        + ")"
                        );

                    } catch (Exception e) {
                        JagTaczArmor.LOGGER.error(
                                "Failed to load armor definition: "
                                        + jsonFile.getName(),
                                e
                        );
                    }
                }
            }
        }

        loadPlatesFromPack(
                packPath
        );
    }

    private static void loadPlatesFromPack(
            Path packPath
    ) {
        Path dataPath =
                packPath.resolve("data");

        if (
                !Files.exists(
                        dataPath,
                        new LinkOption[0]
                )
                        || !Files.isDirectory(
                        dataPath,
                        new LinkOption[0]
                )
        ) {
            return;
        }

        File[] namespaceDirs =
                dataPath.toFile().listFiles(
                        File::isDirectory
                );

        if (namespaceDirs == null) {
            return;
        }

        for (File namespaceDir :
                namespaceDirs) {

            String namespace =
                    namespaceDir.getName();

            File dataSubDir =
                    new File(
                            namespaceDir,
                            "data"
                    );

            File[] plateDirs;

            if (
                    dataSubDir.exists()
                            && dataSubDir.isDirectory()
            ) {
                plateDirs = new File[]{
                        new File(
                                dataSubDir,
                                "plate"
                        ),
                        new File(
                                dataSubDir,
                                "plates"
                        )
                };
            } else {
                plateDirs = new File[]{
                        new File(
                                namespaceDir,
                                "plate"
                        ),
                        new File(
                                namespaceDir,
                                "plates"
                        )
                };
            }

            for (File platesDir :
                    plateDirs) {

                if (
                        !platesDir.exists()
                                || !platesDir.isDirectory()
                ) {
                    continue;
                }

                File[] jsonFiles =
                        platesDir.listFiles(
                                (dir, name) ->
                                        name.endsWith(".json")
                        );

                if (jsonFiles == null) {
                    continue;
                }

                for (File jsonFile :
                        jsonFiles) {

                    try (
                            FileReader reader =
                                    new FileReader(
                                            jsonFile
                                    )
                    ) {
                        PlateIndex plate =
                                GSON.fromJson(
                                        reader,
                                        PlateIndex.class
                                );

                        if (plate != null) {
                            String plateName =
                                    jsonFile.getName()
                                            .replace(
                                                    ".json",
                                                    ""
                                            );

                            ResourceLocation id =
                                    new ResourceLocation(
                                            namespace,
                                            plateName
                                    );

                            plate.packNamespace =
                                    namespace;

                            plate.registryName =
                                    id.toString();

                            PLATE_INDEXES.put(
                                    id,
                                    plate
                            );

                            int cmd =
                                    getOrCreatePlateCmd(
                                            id
                                    );

                            registerPlateCmdTexture(
                                    plate,
                                    cmd
                            );

                            JagTaczArmor.LOGGER.info(
                                    "Loaded plate definition: "
                                            + id
                                            + " (CMD="
                                            + cmd
                                            + ")"
                            );
                        }

                    } catch (Exception e) {
                        JagTaczArmor.LOGGER.error(
                                "Failed to load plate definition: "
                                        + jsonFile.getName(),
                                e
                        );
                    }
                }
            }
        }
    }

    private static void registerPlateCmdTexture(
            PlateIndex plate,
            int cmd
    ) {
        if (plate.itemTexture == null) {
            return;
        }

        String tex =
                plate.itemTexture;

        if (tex.contains(":textures/")) {
            tex =
                    tex.replace(
                            ":textures/",
                            ":"
                    );
        }

        if (tex.endsWith(".png")) {
            tex =
                    tex.substring(
                            0,
                            tex.length() - 4
                    );
        }

        PLATE_CMD_TEXTURES.put(
                cmd,
                tex
        );
    }

    private static void loadArmorsFromZip(
            File zipFile,
            String packName
    ) {
        try (ZipFile zip =
                     new ZipFile(zipFile)) {

            Enumeration<? extends ZipEntry> entries =
                    zip.entries();

            List<ZipEntry> entryList =
                    new ArrayList<>();

            while (entries.hasMoreElements()) {
                entryList.add(
                        entries.nextElement()
                );
            }

            entryList.sort(
                    (a, b) ->
                            a.getName().compareTo(
                                    b.getName()
                            )
            );

            for (ZipEntry entry :
                    entryList) {

                String name =
                        entry.getName();

                name =
                        name.replace(
                                "\\",
                                "/"
                        );

                if (name.startsWith("/")) {
                    name =
                            name.substring(1);
                }

                if (
                        entry.isDirectory()
                                || !name.startsWith("data/")
                                || !name.endsWith(".json")
                ) {
                    continue;
                }

                String[] parts =
                        name.split("/");

                String namespace = null;
                String filename = null;

                boolean valid = false;
                boolean isPlate = false;

                /*
                 * data/<namespace>/armors/file.json
                 */
                if (
                        parts.length == 4
                                && (
                                parts[2].equals("armors")
                                        || parts[2].equals("armor")
                        )
                ) {
                    namespace =
                            parts[1];

                    filename =
                            parts[3];

                    valid = true;

                    /*
                     * data/<namespace>/data/armors/file.json
                     */
                } else if (
                        parts.length == 5
                                && parts[2].equals("data")
                                && (
                                parts[3].equals("armors")
                                        || parts[3].equals("armor")
                        )
                ) {
                    namespace =
                            parts[1];

                    filename =
                            parts[4];

                    valid = true;

                    /*
                     * data/<namespace>/plate/file.json
                     */
                } else if (
                        parts.length == 4
                                && (
                                parts[2].equals("plate")
                                        || parts[2].equals("plates")
                        )
                ) {
                    namespace =
                            parts[1];

                    filename =
                            parts[3];

                    valid = true;
                    isPlate = true;

                    /*
                     * data/<namespace>/data/plate/file.json
                     */
                } else if (
                        parts.length == 5
                                && parts[2].equals("data")
                                && (
                                parts[3].equals("plate")
                                        || parts[3].equals("plates")
                        )
                ) {
                    namespace =
                            parts[1];

                    filename =
                            parts[4];

                    valid = true;
                    isPlate = true;
                }

                if (
                        !valid
                                || namespace == null
                                || filename == null
                ) {
                    continue;
                }

                if (isPlate) {
                    loadPlateFromZipEntry(
                            zip,
                            entry,
                            namespace,
                            filename,
                            name
                    );
                } else {
                    loadArmorFromZipEntry(
                            zip,
                            entry,
                            namespace,
                            filename,
                            name,
                            packName
                    );
                }
            }

        } catch (Exception e) {
            JagTaczArmor.LOGGER.error(
                    "Failed to load armors from zip pack: "
                            + zipFile.getName(),
                    e
            );
        }
    }

    private static void loadPlateFromZipEntry(
            ZipFile zip,
            ZipEntry entry,
            String namespace,
            String filename,
            String entryName
    ) {
        String plateName =
                filename.substring(
                        0,
                        filename.length() - 5
                );

        ResourceLocation id =
                new ResourceLocation(
                        namespace,
                        plateName
                );

        try (
                InputStream in =
                        zip.getInputStream(entry);

                InputStreamReader reader =
                        new InputStreamReader(
                                in,
                                StandardCharsets.UTF_8
                        )
        ) {
            PlateIndex plate =
                    GSON.fromJson(
                            reader,
                            PlateIndex.class
                    );

            if (plate == null) {
                return;
            }

            plate.packNamespace =
                    namespace;

            plate.registryName =
                    id.toString();

            PLATE_INDEXES.put(
                    id,
                    plate
            );

            int cmd =
                    getOrCreatePlateCmd(
                            id
                    );

            registerPlateCmdTexture(
                    plate,
                    cmd
            );

            JagTaczArmor.LOGGER.info(
                    "Loaded plate definition from zip: "
                            + id
                            + " (CMD="
                            + cmd
                            + ")"
            );

        } catch (Exception e) {
            JagTaczArmor.LOGGER.error(
                    "Failed to load plate definition from zip: "
                            + entryName,
                    e
            );
        }
    }

    private static void loadArmorFromZipEntry(
            ZipFile zip,
            ZipEntry entry,
            String namespace,
            String filename,
            String entryName,
            String packName
    ) {
        String armorSetName =
                filename.substring(
                        0,
                        filename.length() - 5
                );

        ResourceLocation id =
                new ResourceLocation(
                        namespace,
                        armorSetName
                );

        try (
                InputStream in =
                        zip.getInputStream(entry);

                InputStreamReader reader =
                        new InputStreamReader(
                                in,
                                StandardCharsets.UTF_8
                        )
        ) {
            JsonElement element =
                    JsonParser.parseReader(
                            reader
                    );

            if (!element.isJsonObject()) {
                return;
            }

            JsonObject obj =
                    element.getAsJsonObject();

            ArmorSetIndex index;

            if (obj.has("slot")) {
                ArmorIndex piece =
                        GSON.fromJson(
                                obj,
                                ArmorIndex.class
                        );

                index =
                        new ArmorSetIndex();

                String nameToUse =
                        piece.displayName;

                if (
                        (nameToUse == null
                                || "Custom Armor".equals(
                                nameToUse))
                                && piece.name != null
                ) {
                    nameToUse =
                            piece.name;
                }

                index.name =
                        nameToUse;

                if (piece.slot != null) {
                    switch (
                            piece.slot.toLowerCase()
                    ) {
                        case "helmet":
                        case "head":
                            index.helmet =
                                    piece;
                            break;

                        case "chestplate":
                        case "torso":
                        case "chest":
                            index.chestplate =
                                    piece;
                            break;

                        case "leggings":
                        case "legs":
                            index.leggings =
                                    piece;
                            break;

                        case "boots":
                        case "feet":
                            index.boots =
                                    piece;
                            break;
                    }
                }

            } else {
                index =
                        GSON.fromJson(
                                obj,
                                ArmorSetIndex.class
                        );
            }

            ARMOR_SET_INDEXES.put(
                    id,
                    index
            );

            if (index.helmet != null) {
                index.helmet.plateSlot =
                        false;
            }

            if (index.chestplate != null) {
                index.chestplate.packNamespace =
                        namespace;
            }

            if (index.helmet != null) {
                index.helmet.packNamespace =
                        namespace;
            }

            if (index.leggings != null) {
                index.leggings.plateSlot =
                        false;

                index.leggings.packNamespace =
                        namespace;
            }

            if (index.boots != null) {
                index.boots.plateSlot =
                        false;

                index.boots.packNamespace =
                        namespace;
            }

            registerTaggedPiece(
                    index.helmet,
                    packName
            );

            registerTaggedPiece(
                    index.chestplate,
                    packName
            );

            registerTaggedPiece(
                    index.leggings,
                    packName
            );

            registerTaggedPiece(
                    index.boots,
                    packName
            );

            int cmd =
                    getOrCreateArmorCmd(
                            id
                    );

            collectItemTextures(
                    index,
                    cmd
            );

            /*
             * НИКАКОЙ регистрации Item здесь нет.
             *
             * Предметы создаются из items.json
             * в RegisterEvent.
             */

            JagTaczArmor.LOGGER.info(
                    "Loaded armor definition from zip: "
                            + id
                            + " (CMD="
                            + cmd
                            + ")"
            );

        } catch (Exception e) {
            JagTaczArmor.LOGGER.error(
                    "Failed to load armor definition from zip: "
                            + entryName,
                    e
            );
        }
    }

    private static void registerTaggedPiece(
            ArmorIndex piece,
            String packName
    ) {
        if (piece == null) {
            return;
        }

        piece.packNamespace =
                packName;

        if (
                piece.armorTag != null
                        && !piece.armorTag.trim().isEmpty()
        ) {
            TAGGED_ARMORS.put(
                    piece.armorTag.trim(),
                    piece
            );
        }
    }

    private static void collectItemTextures(
            ArmorSetIndex setIndex,
            int cmd
    ) {
        ArmorIndex[] pieces =
                new ArmorIndex[]{
                        setIndex.helmet,
                        setIndex.chestplate,
                        setIndex.leggings,
                        setIndex.boots
                };

        Map<Integer, String> texMap =
                new HashMap<>();

        for (int i = 0; i < pieces.length; ++i) {

            if (
                    pieces[i] != null
                            && pieces[i].itemTexture != null
            ) {
                String tex =
                        pieces[i].itemTexture;

                if (tex.contains(":textures/")) {
                    tex =
                            tex.replace(
                                    ":textures/",
                                    ":"
                            );
                }

                texMap.put(
                        i,
                        tex
                );
            }
        }

        if (!texMap.isEmpty()) {
            CMD_TEXTURES.put(
                    cmd,
                    texMap
            );
        }
    }

    public void loadPacks(
            Consumer<Pack> pOnLoad
    ) {
        reloadPacks();

        Pack clientPack =
                Pack.create(
                        "jagtaczarmor_resources",
                        Component.literal(
                                "JagTaczArmor Resources"
                        ),
                        true,
                        (id) ->
                                new DelegatingPackResources(
                                        id,
                                        true,
                                        new PackMetadataSection(
                                                Component.literal(
                                                        "JagTaczArmor Resources"
                                                ),
                                                15
                                        ),
                                        ACTIVE_PACKS
                                ),
                        new Pack.Info(
                                Component.literal(
                                        "JagTaczArmor Resources"
                                ),
                                15,
                                net.minecraft.world.flag.FeatureFlagSet.of()
                        ),
                        PackType.CLIENT_RESOURCES,
                        Position.TOP,
                        true,
                        PackSource.BUILT_IN
                );

        pOnLoad.accept(
                clientPack
        );

        Pack serverPack =
                Pack.create(
                        "jagtaczarmor_data",
                        Component.literal(
                                "JagTaczArmor Data"
                        ),
                        true,
                        (id) ->
                                new DelegatingPackResources(
                                        id,
                                        true,
                                        new PackMetadataSection(
                                                Component.literal(
                                                        "JagTaczArmor Data"
                                                ),
                                                15
                                        ),
                                        ACTIVE_PACKS
                                ),
                        new Pack.Info(
                                Component.literal(
                                        "JagTaczArmor Data"
                                ),
                                15,
                                net.minecraft.world.flag.FeatureFlagSet.of()
                        ),
                        PackType.SERVER_DATA,
                        Position.TOP,
                        true,
                        PackSource.BUILT_IN
                );

        pOnLoad.accept(
                serverPack
        );
    }

    @SubscribeEvent
    public static void onAddReloadListener(
            AddReloadListenerEvent event
    ) {
        event.addListener(
                (ResourceManagerReloadListener)
                        manager ->
                                reloadPacks()
        );
    }

    public static class PackMeta {
        public String name;
        public String author;

        public PackMeta() {
        }
    }

    @EventBusSubscriber(
            modid = "jagtaczarmor",
            bus = Bus.MOD
    )
    public static class ModEvents {

        public ModEvents() {
        }

        @SubscribeEvent
        public static void onAddPackFinders(
                AddPackFindersEvent event
        ) {
            event.addRepositorySource(
                    AddonPackLoader.INSTANCE
            );
        }

        @SubscribeEvent
        public static void onRegisterClientReloadListeners(
                RegisterClientReloadListenersEvent event
        ) {
            event.registerReloadListener(
                    (ResourceManagerReloadListener)
                            manager ->
                                    AddonPackLoader.reloadPacks()
            );
        }
    }
}