package com.jagtaczarmor.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jagtaczarmor.JagTaczArmor;
import com.jagtaczarmor.config.ArmorConfig;
import com.jagtaczarmor.registry.ItemRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.Pack.Position;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.loading.FMLPaths;
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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@EventBusSubscriber(modid = "jagtaczarmor", bus = Bus.FORGE)
public class AddonPackLoader implements RepositorySource {
    public static final AddonPackLoader INSTANCE = new AddonPackLoader();
    private static final Gson GSON = (new GsonBuilder()).create();
    private static final List<Path> LOADED_PACK_PATHS = new ArrayList();
    public static final Map<ResourceLocation, ArmorSetIndex> ARMOR_SET_INDEXES = new HashMap();
    public static final Map<ResourceLocation, Integer> ARMOR_SET_CMD = new HashMap();
    public static final Map<ResourceLocation, PlateIndex> PLATE_INDEXES = new HashMap();
    public static final Map<ResourceLocation, Integer> PLATE_CMD = new HashMap();
    public static final Map<Integer, String> PLATE_CMD_TEXTURES = new HashMap();
    private static final Map<Integer, ResourceLocation> CMD_TO_ARMOR_ID = new HashMap();
    private static final Map<Integer, ResourceLocation> CMD_TO_PLATE_ID = new HashMap();
    private static final List<PackResources> ACTIVE_PACKS = new ArrayList();
    public static final Map<String, PackMeta> PACK_METAS = new HashMap();
    public static final Map<String, ArmorIndex> TAGGED_ARMORS = new HashMap();
    private static final String[] PIECE_NAMES = new String[]{"tk_hm", "tk_ch", "tk_lg", "tk_bt"};
    private static final Map<Integer, Map<Integer, String>> CMD_TEXTURES = new HashMap();

    public AddonPackLoader() {
    }

    public static int getOrCreateArmorCmd(ResourceLocation id) {
        if (ARMOR_SET_CMD.containsKey(id)) {
            return ARMOR_SET_CMD.get(id);
        } else {
            int cmd = Math.abs(id.toString().hashCode());
            if (cmd == 0) {
                cmd = 1;
            }
            while (CMD_TO_ARMOR_ID.containsKey(cmd) && !CMD_TO_ARMOR_ID.get(cmd).equals(id)) {
                ++cmd;
            }
            CMD_TO_ARMOR_ID.put(cmd, id);
            ARMOR_SET_CMD.put(id, cmd);
            return cmd;
        }
    }

    public static int getOrCreatePlateCmd(ResourceLocation id) {
        if (PLATE_CMD.containsKey(id)) {
            return PLATE_CMD.get(id);
        } else {
            int cmd = Math.abs(id.toString().hashCode());
            if (cmd == 0) {
                cmd = 1;
            }
            while (CMD_TO_PLATE_ID.containsKey(cmd) && !CMD_TO_PLATE_ID.get(cmd).equals(id)) {
                ++cmd;
            }
            CMD_TO_PLATE_ID.put(cmd, id);
            PLATE_CMD.put(id, cmd);
            return cmd;
        }
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
            JagTaczArmor.LOGGER.error("Failed to initialize directories", e);
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
                    JagTaczArmor.LOGGER.error("Failed to clean up old default pack", e);
                }
            }
        }

        if (shouldExtract) {
            extractDefaultPack(defaultPackDir);
        }

        reloadPacks();
    }

    public static void reloadPacks() {
        JagTaczArmor.LOGGER.info("Reloading JagTaczArmor packs...");

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

        Path taczDir = FMLPaths.GAMEDIR.get().resolve("tacz");

        ACTIVE_PACKS.add(new InMemoryPackResources());

        File[] packs = taczDir.toFile().listFiles();

        if (packs != null) {
            Arrays.sort(packs, (a, b) -> a.getName().compareTo(b.getName()));

            for (File pack : packs) {
                if (pack.isDirectory() && !pack.getName().equals("jag_generated_resources")) {
                    File metaFile = new File(pack, "armorpack.meta.json");

                    if (metaFile.exists()) {
                        PackMeta packMeta = null;

                        try (FileReader reader = new FileReader(metaFile)) {
                            packMeta = GSON.fromJson(reader, PackMeta.class);
                        } catch (Exception e) {
                            JagTaczArmor.LOGGER.error(
                                    "Failed to parse armorpack.meta.json for " + pack.getName(),
                                    e
                            );
                        }

                        if (packMeta == null) {
                            packMeta = new PackMeta();
                        }

                        if (packMeta.name == null) {
                            packMeta.name = pack.getName();
                        }

                        PACK_METAS.put(pack.getName(), packMeta);

                        File packMcMeta = new File(pack, "pack.mcmeta");

                        if (!packMcMeta.exists()) {
                            try {
                                Files.writeString(
                                        packMcMeta.toPath(),
                                        "{\"pack\":{\"pack_format\":15,\"description\":\"JagTaczArmor Addon\"}}"
                                );
                            } catch (Exception e) {
                                JagTaczArmor.LOGGER.error(
                                        "Failed to generate pack.mcmeta for " + pack.getName(),
                                        e
                                );
                            }
                        }

                        LOADED_PACK_PATHS.add(pack.toPath());
                        loadArmorsFromPack(pack.toPath());

                        ACTIVE_PACKS.add(
                                new PathPackResources(
                                        "jagtaczarmor_addon_" + pack.getName(),
                                        true,
                                        pack.toPath()
                                )
                        );
                    }

                } else if (pack.isFile() && pack.getName().endsWith(".zip")) {
                    String packName = pack.getName().substring(
                            0,
                            pack.getName().length() - 4
                    );

                    try (ZipFile zip = new ZipFile(pack)) {
                        ZipEntry metaEntry = zip.getEntry("armorpack.meta.json");

                        if (metaEntry != null) {
                            PackMeta packMeta = null;

                            try (
                                    InputStream in = zip.getInputStream(metaEntry);
                                    InputStreamReader reader =
                                            new InputStreamReader(in, StandardCharsets.UTF_8)
                            ) {
                                packMeta = GSON.fromJson(reader, PackMeta.class);
                            } catch (Exception e) {
                                JagTaczArmor.LOGGER.error(
                                        "Failed to parse armorpack.meta.json inside " + pack.getName(),
                                        e
                                );
                            }

                            if (packMeta == null) {
                                packMeta = new PackMeta();
                            }

                            if (packMeta.name == null) {
                                packMeta.name = packName;
                            }

                            PACK_METAS.put(packName, packMeta);

                            loadArmorsFromZip(pack, packName);

                            ACTIVE_PACKS.add(
                                    new FilePackResources(
                                            "jagtaczarmor_addon_" + packName,
                                            pack,
                                            true
                                    )
                            );
                        }

                    } catch (Exception e) {
                        JagTaczArmor.LOGGER.error(
                                "Failed to process zip pack: " + pack.getName(),
                                e
                        );
                    }
                }
            }
        }
    }

    private static void extractDefaultPack(Path defaultPackDir) {
        try {
            URL resource = AddonPackLoader.class.getResource("/default_pack.zip");

            if (resource != null) {
                File zipFile = File.createTempFile("default_pack", ".zip");

                try (InputStream in = resource.openStream()) {
                    Files.copy(
                            in,
                            zipFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }

                ZipFile zip = new ZipFile(zipFile);
                Enumeration<? extends ZipEntry> entries = zip.entries();

                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();

                    String name = entry.getName();
                    name = name.replace("\\", "/");

                    if (name.startsWith("/")) {
                        name = name.substring(1);
                    }

                    File destFile = new File(defaultPackDir.toFile(), name);

                    if (!entry.isDirectory() && !name.endsWith("/")) {
                        File parent = destFile.getParentFile();

                        if (parent != null && !parent.exists()) {
                            parent.mkdirs();
                        }

                        try (InputStream in = zip.getInputStream(entry)) {
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
                        "Successfully extracted default_pack.zip to " + defaultPackDir
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

    private static void loadArmorsFromPack(Path packPath) {
        Path dataPath = packPath.resolve("data");

        if (Files.exists(dataPath, new LinkOption[0])
                && Files.isDirectory(dataPath, new LinkOption[0])) {

            File[] namespaceDirs =
                    dataPath.toFile().listFiles(File::isDirectory);

            if (namespaceDirs != null) {
                String packName =
                        packPath.getFileName().toString();

                for (File namespaceDir : namespaceDirs) {
                    String namespace = namespaceDir.getName();

                    File dataSubDir =
                            new File(namespaceDir, "data");

                    File[] armorDirs;

                    if (dataSubDir.exists()
                            && dataSubDir.isDirectory()) {

                        armorDirs = new File[]{
                                new File(dataSubDir, "armors"),
                                new File(dataSubDir, "armor")
                        };

                    } else {
                        armorDirs = new File[]{
                                new File(namespaceDir, "armors"),
                                new File(namespaceDir, "armor")
                        };
                    }

                    for (File armorsDir : armorDirs) {
                        if (armorsDir.exists()
                                && armorsDir.isDirectory()) {

                            File[] jsonFiles =
                                    armorsDir.listFiles(
                                            (dir, name) ->
                                                    name.endsWith(".json")
                                    );

                            if (jsonFiles != null) {
                                Arrays.sort(
                                        jsonFiles,
                                        (a, b) ->
                                                a.getName().compareTo(b.getName())
                                );

                                for (File jsonFile : jsonFiles) {
                                    try (FileReader reader =
                                                 new FileReader(jsonFile)) {

                                        JsonElement element =
                                                JsonParser.parseReader(reader);

                                        if (element.isJsonObject()) {
                                            JsonObject obj =
                                                    element.getAsJsonObject();

                                            ArmorSetIndex index;

                                            String armorSetName =
                                                    jsonFile.getName()
                                                            .replace(".json", "");

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

                                                index = new ArmorSetIndex();

                                                String nameToUse =
                                                        piece.displayName;

                                                if ((nameToUse == null
                                                        || "Custom Armor".equals(nameToUse))
                                                        && piece.name != null) {

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
                                                            index.helmet = piece;
                                                            break;

                                                        case "chestplate":
                                                        case "torso":
                                                        case "chest":
                                                            index.chestplate = piece;
                                                            break;

                                                        case "leggings":
                                                        case "legs":
                                                            index.leggings = piece;
                                                            break;

                                                        case "boots":
                                                        case "feet":
                                                            index.boots = piece;
                                                            break;
                                                    }
                                                }
                                            }

                                            ARMOR_SET_INDEXES.put(
                                                    id,
                                                    index
                                            );

                                            if (index.helmet != null) {
                                                index.helmet.plateSlot = false;
                                                index.helmet.packNamespace =
                                                        namespace;
                                            }

                                            if (index.chestplate != null) {
                                                index.chestplate.packNamespace =
                                                        namespace;
                                            }

                                            if (index.leggings != null) {
                                                index.leggings.plateSlot = false;
                                                index.leggings.packNamespace =
                                                        namespace;
                                            }

                                            if (index.boots != null) {
                                                index.boots.plateSlot = false;
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
                                                    getOrCreateArmorCmd(id);

                                            collectItemTextures(
                                                    index,
                                                    cmd
                                            );

                                            if (index.helmet != null
                                                    && (index.helmet.armorTag == null
                                                    || index.helmet.armorTag.trim().isEmpty())) {

                                                String itemId =
                                                        armorSetName;

                                                ItemRegistry.registerAddonArmor(
                                                        itemId,
                                                        index.helmet,
                                                        Type.HELMET
                                                );
                                            }

                                            if (index.chestplate != null
                                                    && (index.chestplate.armorTag == null
                                                    || index.chestplate.armorTag.trim().isEmpty())) {

                                                String itemId =
                                                        armorSetName;

                                                ItemRegistry.registerAddonArmor(
                                                        itemId,
                                                        index.chestplate,
                                                        Type.CHESTPLATE
                                                );
                                            }

                                            if (index.leggings != null
                                                    && (index.leggings.armorTag == null
                                                    || index.leggings.armorTag.trim().isEmpty())) {

                                                String itemId =
                                                        armorSetName;

                                                ItemRegistry.registerAddonArmor(
                                                        itemId,
                                                        index.leggings,
                                                        Type.LEGGINGS
                                                );
                                            }

                                            if (index.boots != null
                                                    && (index.boots.armorTag == null
                                                    || index.boots.armorTag.trim().isEmpty())) {

                                                String itemId =
                                                        armorSetName;

                                                ItemRegistry.registerAddonArmor(
                                                        itemId,
                                                        index.boots,
                                                        Type.BOOTS
                                                );
                                            }

                                            JagTaczArmor.LOGGER.info(
                                                    "Loaded armor definition: "
                                                            + id
                                                            + " (CMD="
                                                            + cmd
                                                            + ")"
                                            );
                                        }

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
                    }
                }

                loadPlatesFromPack(packPath);
            }
        }
    }

    private static void loadPlatesFromPack(Path packPath) {
        Path dataPath =
                packPath.resolve("data");

        if (Files.exists(dataPath, new LinkOption[0])
                && Files.isDirectory(dataPath, new LinkOption[0])) {

            File[] namespaceDirs =
                    dataPath.toFile().listFiles(File::isDirectory);

            if (namespaceDirs != null) {
                for (File namespaceDir : namespaceDirs) {
                    String namespace =
                            namespaceDir.getName();

                    File dataSubDir =
                            new File(namespaceDir, "data");

                    File[] plateDirs;

                    if (dataSubDir.exists()
                            && dataSubDir.isDirectory()) {

                        plateDirs = new File[]{
                                new File(dataSubDir, "plate"),
                                new File(dataSubDir, "plates")
                        };

                    } else {
                        plateDirs = new File[]{
                                new File(namespaceDir, "plate"),
                                new File(namespaceDir, "plates")
                        };
                    }

                    for (File platesDir : plateDirs) {
                        if (platesDir.exists()
                                && platesDir.isDirectory()) {

                            File[] jsonFiles =
                                    platesDir.listFiles(
                                            (dir, name) ->
                                                    name.endsWith(".json")
                                    );

                            if (jsonFiles != null) {
                                for (File jsonFile : jsonFiles) {
                                    try (FileReader reader =
                                                 new FileReader(jsonFile)) {

                                        PlateIndex plate =
                                                GSON.fromJson(
                                                        reader,
                                                        PlateIndex.class
                                                );

                                        if (plate != null) {
                                            String plateName =
                                                    jsonFile.getName()
                                                            .replace(".json", "");

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
                                                    getOrCreatePlateCmd(id);

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
                }
            }
        }
    }

    private static void registerPlateCmdTexture(
            PlateIndex plate,
            int cmd
    ) {
        if (plate.itemTexture != null) {
            String tex =
                    plate.itemTexture;

            if (tex.contains(":textures/")) {
                tex = tex.replace(
                        ":textures/",
                        ":"
                );
            }

            if (tex.endsWith(".png")) {
                tex = tex.substring(
                        0,
                        tex.length() - 4
                );
            }

            PLATE_CMD_TEXTURES.put(
                    cmd,
                    tex
            );
        }
    }

    private static void loadArmorsFromZip(
            File zipFile,
            String packName
    ) {
        try (ZipFile zip = new ZipFile(zipFile)) {
            Enumeration<? extends ZipEntry> entries =
                    zip.entries();

            List<ZipEntry> entryList =
                    new ArrayList();

            while (entries.hasMoreElements()) {
                entryList.add(
                        entries.nextElement()
                );
            }

            entryList.sort(
                    (a, b) ->
                            a.getName().compareTo(b.getName())
            );

            for (ZipEntry entry : entryList) {
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

                if (!entry.isDirectory()
                        && name.startsWith("data/")
                        && name.endsWith(".json")) {

                    String[] parts =
                            name.split("/");

                    String namespace = null;
                    String filename = null;
                    boolean valid = false;
                    boolean isPlate = false;

                    if (parts.length != 4
                            || !parts[2].equals("armors")
                            && !parts[2].equals("armor")) {

                        if (parts.length != 5
                                || !parts[2].equals("data")
                                || !parts[3].equals("armors")
                                && !parts[3].equals("armor")) {

                            if (parts.length != 4
                                    || !parts[2].equals("plate")
                                    && !parts[2].equals("plates")) {

                                if (parts.length == 5
                                        && parts[2].equals("data")
                                        && (parts[3].equals("plate")
                                        || parts[3].equals("plates"))) {

                                    namespace =
                                            parts[1];

                                    filename =
                                            parts[4];

                                    valid = true;
                                    isPlate = true;

                                }

                            } else {
                                namespace =
                                        parts[1];

                                filename =
                                        parts[3];

                                valid = true;
                                isPlate = true;
                            }

                        } else {
                            namespace =
                                    parts[1];

                            filename =
                                    parts[4];

                            valid = true;
                        }

                    } else {
                        namespace =
                                parts[1];

                        filename =
                                parts[3];

                        valid = true;
                    }

                    if (valid
                            && namespace != null
                            && filename != null) {

                        if (isPlate) {
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

                                if (plate != null) {
                                    plate.packNamespace =
                                            namespace;

                                    plate.registryName =
                                            id.toString();

                                    PLATE_INDEXES.put(
                                            id,
                                            plate
                                    );

                                    int cmd =
                                            getOrCreatePlateCmd(id);

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
                                }

                            } catch (Exception e) {
                                JagTaczArmor.LOGGER.error(
                                        "Failed to load plate definition from zip: "
                                                + name,
                                        e
                                );
                            }

                        } else {
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
                                        JsonParser.parseReader(reader);

                                if (element.isJsonObject()) {
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

                                        if ((nameToUse == null
                                                || "Custom Armor".equals(nameToUse))
                                                && piece.name != null) {

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

                                    if (index.leggings != null) {
                                        index.leggings.plateSlot =
                                                false;
                                    }

                                    if (index.boots != null) {
                                        index.boots.plateSlot =
                                                false;
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
                                            getOrCreateArmorCmd(id);

                                    collectItemTextures(
                                            index,
                                            cmd
                                    );

                                    if (index.helmet != null
                                            && (index.helmet.armorTag == null
                                            || index.helmet.armorTag.trim().isEmpty())) {

                                        String itemId =
                                                armorSetName;

                                        ItemRegistry.registerAddonArmor(
                                                itemId,
                                                index.helmet,
                                                Type.HELMET
                                        );
                                    }

                                    if (index.chestplate != null
                                            && (index.chestplate.armorTag == null
                                            || index.chestplate.armorTag.trim().isEmpty())) {

                                        String itemId =
                                                armorSetName;

                                        ItemRegistry.registerAddonArmor(
                                                itemId,
                                                index.chestplate,
                                                Type.CHESTPLATE
                                        );
                                    }

                                    if (index.leggings != null
                                            && (index.leggings.armorTag == null
                                            || index.leggings.armorTag.trim().isEmpty())) {

                                        String itemId =
                                                armorSetName;

                                        ItemRegistry.registerAddonArmor(
                                                itemId,
                                                index.leggings,
                                                Type.LEGGINGS
                                        );
                                    }

                                    if (index.boots != null
                                            && (index.boots.armorTag == null
                                            || index.boots.armorTag.trim().isEmpty())) {

                                        String itemId =
                                                armorSetName;

                                        ItemRegistry.registerAddonArmor(
                                                itemId,
                                                index.boots,
                                                Type.BOOTS
                                        );
                                    }

                                    JagTaczArmor.LOGGER.info(
                                            "Loaded armor definition from zip: "
                                                    + id
                                                    + " (CMD="
                                                    + cmd
                                                    + ")"
                                    );
                                }

                            } catch (Exception e) {
                                JagTaczArmor.LOGGER.error(
                                        "Failed to load armor definition from zip: "
                                                + name,
                                        e
                                );
                            }
                        }
                    }
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

    private static void registerTaggedPiece(
            ArmorIndex piece,
            String packName
    ) {
        if (piece != null) {
            piece.packNamespace =
                    packName;

            if (piece.armorTag != null
                    && !piece.armorTag.trim().isEmpty()) {

                TAGGED_ARMORS.put(
                        piece.armorTag.trim(),
                        piece
                );
            }
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
                new HashMap();

        for (int i = 0; i < pieces.length; ++i) {
            if (pieces[i] != null
                    && pieces[i].itemTexture != null) {

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

    public void loadPacks(Consumer<Pack> pOnLoad) {
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

        pOnLoad.accept(clientPack);

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

        pOnLoad.accept(serverPack);
    }

    @SubscribeEvent
    public static void onAddReloadListener(
            AddReloadListenerEvent event
    ) {
        event.addListener(
                (ResourceManagerReloadListener)
                        (manager) ->
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
                            (manager) ->
                                    AddonPackLoader.reloadPacks()
            );
        }
    }
}