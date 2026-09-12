package com.jagtaczarmor.data;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InMemoryPackResources implements PackResources {

    private static final String[] PIECE_NAMES =
            new String[]{"tk_hm", "tk_ch", "tk_lg", "tk_bt"};

    public InMemoryPackResources() {
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(
            String... paths
    ) {

        if (paths.length == 1
                && "pack.mcmeta".equals(paths[0])) {

            String meta =
                    "{\"pack\":{\"pack_format\":15,"
                            + "\"description\":\"JagTaczArmor Generated Resources\"}}";

            return () ->
                    new ByteArrayInputStream(
                            meta.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );
        }

        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(
            PackType type,
            ResourceLocation location
    ) {

        if (type == PackType.CLIENT_RESOURCES) {

            String path =
                    location.getPath();

            /*
             * =========================================================
             * ADDON ARMOR MODELS
             * =========================================================
             *
             * Например:
             *
             * lrarmor_pack:models/item/atf_chestplate.json
             */
            if (path.startsWith("models/item/")) {

                String filename =
                        path.substring(
                                "models/item/".length()
                        );

                if (filename.endsWith(".json")) {

                    String modelName =
                            filename.substring(
                                    0,
                                    filename.length() - ".json".length()
                            );

                    /*
                     * -------------------------------------------------
                     * ADDON ARMOR
                     * -------------------------------------------------
                     */
                    ResourceLocation addonArmorId =
                            findAddonArmorId(
                                    location.getNamespace(),
                                    modelName
                            );

                    if (addonArmorId != null) {

                        String addonJson =
                                generateAddonItemJson(
                                        addonArmorId
                                );

                        return () ->
                                new ByteArrayInputStream(
                                        addonJson.getBytes(
                                                StandardCharsets.UTF_8
                                        )
                                );
                    }

                    /*
                     * -------------------------------------------------
                     * ADDON PLATE
                     * -------------------------------------------------
                     *
                     * Теперь плиты тоже имеют настоящий registry ID.
                     *
                     * Например:
                     *
                     * lrarmor_pack:steel_plate
                     *
                     * Minecraft запрашивает:
                     *
                     * lrarmor_pack:models/item/steel_plate.json
                     */
                    ResourceLocation addonPlateId =
                            findAddonPlateId(
                                    location.getNamespace(),
                                    modelName
                            );

                    if (addonPlateId != null) {

                        String plateJson =
                                generateAddonPlateItemJson(
                                        addonPlateId
                                );

                        if (plateJson != null) {

                            return () ->
                                    new ByteArrayInputStream(
                                            plateJson.getBytes(
                                                    StandardCharsets.UTF_8
                                            )
                                    );
                        }
                    }
                }
            }

            /*
             * =========================================================
             * LEGACY / PLATE RESOURCES
             * =========================================================
             *
             * Старые ресурсы JagTaczArmor продолжают жить
             * в namespace jagtaczarmor.
             */
            if ("jagtaczarmor".equals(
                    location.getNamespace()
            )) {

                if (path.startsWith("models/item/")) {

                    String filename =
                            path.substring(
                                    "models/item/".length()
                            );

                    if (filename.endsWith(".json")) {

                        String modelName =
                                filename.substring(
                                        0,
                                        filename.length() - ".json".length()
                                );

                        if ("plate_armor".equals(
                                modelName
                        )) {

                            return () ->
                                    new ByteArrayInputStream(
                                            generatePlateBaseJson()
                                                    .getBytes(
                                                            StandardCharsets.UTF_8
                                                    )
                                    );
                        }

                        if (modelName.startsWith(
                                "plate_armor_"
                        )) {

                            try {

                                int cmd =
                                        Integer.parseInt(
                                                modelName.substring(
                                                        "plate_armor_".length()
                                                )
                                        );

                                String tex =
                                        AddonPackLoader
                                                .PLATE_CMD_TEXTURES
                                                .get(cmd);

                                if (tex != null) {

                                    String json =
                                            "{\n"
                                                    + "  \"parent\": \"item/generated\",\n"
                                                    + "  \"textures\": {\n"
                                                    + "    \"layer0\": \""
                                                    + tex
                                                    + "\"\n"
                                                    + "  }\n"
                                                    + "}";

                                    return () ->
                                            new ByteArrayInputStream(
                                                    json.getBytes(
                                                            StandardCharsets.UTF_8
                                                    )
                                            );
                                }

                            } catch (
                                    NumberFormatException ignored
                            ) {
                            }
                        }

                        /*
                         * Старые tk_hm/tk_ch/tk_lg/tk_bt.
                         */
                        for (
                                int i = 0;
                                i < PIECE_NAMES.length;
                                i++
                        ) {

                            if (PIECE_NAMES[i].equals(
                                    modelName
                            )) {

                                final int index = i;

                                return () ->
                                        new ByteArrayInputStream(
                                                generateBaseJson(
                                                        index
                                                ).getBytes(
                                                        StandardCharsets.UTF_8
                                                )
                                        );
                            }
                        }

                        for (
                                int i = 0;
                                i < PIECE_NAMES.length;
                                i++
                        ) {

                            String prefix =
                                    PIECE_NAMES[i] + "_";

                            if (modelName.startsWith(
                                    prefix
                            )) {

                                try {

                                    int cmd =
                                            Integer.parseInt(
                                                    modelName.substring(
                                                            prefix.length()
                                                    )
                                            );

                                    String tex =
                                            AddonPackLoader
                                                    .getCmdTexture(
                                                            cmd,
                                                            i
                                                    );

                                    if (tex != null) {

                                        String json =
                                                "{\n"
                                                        + "  \"parent\": \"item/generated\",\n"
                                                        + "  \"textures\": {\n"
                                                        + "    \"layer0\": \""
                                                        + tex
                                                        + "\"\n"
                                                        + "  }\n"
                                                        + "}";

                                        return () ->
                                                new ByteArrayInputStream(
                                                        json.getBytes(
                                                                StandardCharsets.UTF_8
                                                        )
                                                );
                                    }

                                } catch (
                                        NumberFormatException ignored
                                ) {
                                }
                            }
                        }
                    }
                }
            }
        }

        /*
         * Обычные встроенные ресурсы JagTaczArmor.
         */
        String resourcePath =
                "/assets/"
                        + location.getNamespace()
                        + "/"
                        + location.getPath();

        URL url =
                InMemoryPackResources.class.getResource(
                        resourcePath
                );

        return url != null
                ? () -> url.openStream()
                : null;
    }

    /**
     * Ищет armor definition по полному namespace + path.
     *
     * Например:
     *
     * lrarmor_pack + atf_chestplate
     *
     * -> lrarmor_pack:atf_chestplate
     */
    private ResourceLocation findAddonArmorId(
            String namespace,
            String modelName
    ) {

        ResourceLocation id =
                new ResourceLocation(
                        namespace,
                        modelName
                );

        if (
                AddonPackLoader.ARMOR_SET_INDEXES.containsKey(
                        id
                )
        ) {
            return id;
        }

        return null;
    }

    /**
     * Ищет plate definition по настоящему registry ID.
     *
     * Например:
     *
     * lrarmor_pack:steel_plate
     *
     * -> lrarmor_pack:steel_plate
     */
    private ResourceLocation findAddonPlateId(
            String namespace,
            String modelName
    ) {

        ResourceLocation id =
                new ResourceLocation(
                        namespace,
                        modelName
                );

        if (
                AddonPackLoader.PLATE_INDEXES.containsKey(
                        id
                )
        ) {
            return id;
        }

        return null;
    }

    /**
     * Генерирует item model для конкретного addon armor.
     */
    private String generateAddonItemJson(
            ResourceLocation armorId
    ) {

        String texturePath =
                armorId.getNamespace()
                        + ":item/"
                        + armorId.getPath();

        ArmorSetIndex setIndex =
                AddonPackLoader.ARMOR_SET_INDEXES.get(
                        armorId
                );

        if (setIndex != null) {

            if (setIndex.helmet != null
                    && setIndex.helmet.itemTexture != null) {

                texturePath =
                        setIndex.helmet.itemTexture;

            } else if (
                    setIndex.chestplate != null
                            && setIndex.chestplate.itemTexture != null
            ) {

                texturePath =
                        setIndex.chestplate.itemTexture;

            } else if (
                    setIndex.leggings != null
                            && setIndex.leggings.itemTexture != null
            ) {

                texturePath =
                        setIndex.leggings.itemTexture;

            } else if (
                    setIndex.boots != null
                            && setIndex.boots.itemTexture != null
            ) {

                texturePath =
                        setIndex.boots.itemTexture;
            }
        }

        if (texturePath == null
                || texturePath.isEmpty()) {

            texturePath =
                    "jagtaczarmor:item/tab_icon";
        }

        if (texturePath.endsWith(".png")) {

            texturePath =
                    texturePath.substring(
                            0,
                            texturePath.length() - 4
                    );
        }

        if (texturePath.contains(":textures/")) {

            texturePath =
                    texturePath.replace(
                            ":textures/",
                            ":"
                    );
        }

        return "{\n"
                + "  \"parent\": \"item/generated\",\n"
                + "  \"textures\": {\n"
                + "    \"layer0\": \""
                + texturePath
                + "\"\n"
                + "  }\n"
                + "}";
    }

    /**
     * Генерирует item model для конкретной addon-плиты.
     *
     * Registry ID плиты используется напрямую:
     *
     * lrarmor_pack:steel_plate
     *
     * Модель:
     *
     * lrarmor_pack:models/item/steel_plate.json
     *
     * Текстура берётся через PLATE_CMD.
     */
    private String generateAddonPlateItemJson(
            ResourceLocation plateId
    ) {

        Integer cmd =
                AddonPackLoader.PLATE_CMD.get(
                        plateId
                );

        if (cmd == null) {
            return null;
        }

        String texturePath =
                AddonPackLoader.PLATE_CMD_TEXTURES.get(
                        cmd
                );

        if (texturePath == null
                || texturePath.isEmpty()) {

            return null;
        }

        if (texturePath.endsWith(".png")) {

            texturePath =
                    texturePath.substring(
                            0,
                            texturePath.length() - 4
                    );
        }

        if (texturePath.contains(":textures/")) {

            texturePath =
                    texturePath.replace(
                            ":textures/",
                            ":"
                    );
        }

        return "{\n"
                + "  \"parent\": \"item/generated\",\n"
                + "  \"textures\": {\n"
                + "    \"layer0\": \""
                + texturePath
                + "\"\n"
                + "  }\n"
                + "}";
    }

    private String generateBaseJson(
            int pieceIdx
    ) {

        StringBuilder overrides =
                new StringBuilder();

        boolean first = true;

        List<
                Map.Entry<
                        Integer,
                        Map<Integer, String>
                        >
                > entries =
                new ArrayList<>(
                        AddonPackLoader
                                .getCmdTextures()
                                .entrySet()
                );

        entries.sort(
                Map.Entry.comparingByKey()
        );

        for (
                Map.Entry<
                        Integer,
                        Map<Integer, String>
                        > entry :
                entries
        ) {

            int cmd =
                    entry.getKey();

            String tex =
                    entry.getValue()
                            .get(pieceIdx);

            if (tex != null) {

                if (!first) {
                    overrides.append(",\n    ");
                }

                overrides
                        .append(
                                "{\"predicate\":{\"custom_model_data\":"
                        )
                        .append(cmd)
                        .append(
                                "},\"model\":\"jagtaczarmor:item/"
                        )
                        .append(
                                PIECE_NAMES[pieceIdx]
                        )
                        .append("_")
                        .append(cmd)
                        .append("\"}");

                first = false;
            }
        }

        return "{\n"
                + "  \"parent\": \"builtin/entity\",\n"
                + "  \"overrides\": [\n"
                + "    "
                + overrides
                + "\n"
                + "  ]\n"
                + "}";
    }

    private String generatePlateBaseJson() {

        StringBuilder overrides =
                new StringBuilder();

        boolean first = true;

        List<
                Map.Entry<
                        Integer,
                        String
                        >
                > entries =
                new ArrayList<>(
                        AddonPackLoader
                                .PLATE_CMD_TEXTURES
                                .entrySet()
                );

        entries.sort(
                Map.Entry.comparingByKey()
        );

        for (
                Map.Entry<
                        Integer,
                        String
                        > entry :
                entries
        ) {

            int cmd =
                    entry.getKey();

            if (!first) {
                overrides.append(",\n    ");
            }

            overrides
                    .append(
                            "{\"predicate\":{\"custom_model_data\":"
                    )
                    .append(cmd)
                    .append(
                            "},\"model\":\"jagtaczarmor:item/plate_armor_"
                    )
                    .append(cmd)
                    .append("\"}");

            first = false;
        }

        return "{\n"
                + "  \"parent\": \"item/generated\",\n"
                + "  \"textures\": {\n"
                + "    \"layer0\": \"jagtaczarmor:item/tab_icon\"\n"
                + "  },\n"
                + "  \"overrides\": [\n"
                + "    "
                + overrides
                + "\n"
                + "  ]\n"
                + "}";
    }

    @Override
    public void listResources(
            PackType type,
            String namespace,
            String path,
            ResourceOutput output
    ) {

        if (type != PackType.CLIENT_RESOURCES) {
            return;
        }

        /*
         * =============================================================
         * ADDON ARMOR
         * =============================================================
         */
        if (
                "models/item".equals(path)
                        || "models".equals(path)
                        || "".equals(path)
        ) {

            for (
                    ResourceLocation id :
                    AddonPackLoader.ARMOR_SET_INDEXES.keySet()
            ) {

                if (!id.getNamespace().equals(
                        namespace
                )) {
                    continue;
                }

                String name =
                        id.getPath();

                ResourceLocation loc =
                        new ResourceLocation(
                                id.getNamespace(),
                                "models/item/"
                                        + name
                                        + ".json"
                        );

                IoSupplier<InputStream> resource =
                        getResource(
                                type,
                                loc
                        );

                if (resource != null) {
                    output.accept(
                            loc,
                            resource
                    );
                }
            }

            /*
             * =========================================================
             * ADDON PLATES
             * =========================================================
             *
             * Каждая плита теперь получает собственную модель
             * по своему registry ID.
             *
             * Например:
             *
             * lrarmor_pack:steel_plate
             *
             * ->
             *
             * lrarmor_pack:models/item/steel_plate.json
             */
            for (
                    ResourceLocation id :
                    AddonPackLoader.PLATE_INDEXES.keySet()
            ) {

                if (!id.getNamespace().equals(
                        namespace
                )) {
                    continue;
                }

                String name =
                        id.getPath();

                ResourceLocation loc =
                        new ResourceLocation(
                                id.getNamespace(),
                                "models/item/"
                                        + name
                                        + ".json"
                        );

                IoSupplier<InputStream> resource =
                        getResource(
                                type,
                                loc
                        );

                if (resource != null) {
                    output.accept(
                            loc,
                            resource
                    );
                }
            }
        }

        /*
         * =============================================================
         * JAGTACZARMOR LEGACY RESOURCES
         * =============================================================
         */
        if ("jagtaczarmor".equals(
                namespace
        )) {

            if (
                    "models/item".equals(path)
                            || "models".equals(path)
                            || "".equals(path)
            ) {

                for (
                        int i = 0;
                        i < PIECE_NAMES.length;
                        i++
                ) {

                    String name =
                            PIECE_NAMES[i];

                    ResourceLocation loc =
                            new ResourceLocation(
                                    "jagtaczarmor",
                                    "models/item/"
                                            + name
                                            + ".json"
                            );

                    IoSupplier<InputStream> resource =
                            getResource(
                                    type,
                                    loc
                            );

                    if (resource != null) {
                        output.accept(
                                loc,
                                resource
                        );
                    }

                    for (
                            Map.Entry<
                                    Integer,
                                    Map<Integer, String>
                                    > entry :
                            AddonPackLoader
                                    .getCmdTextures()
                                    .entrySet()
                    ) {

                        int cmd =
                                entry.getKey();

                        if (entry.getValue()
                                .containsKey(i)) {

                            String subPath =
                                    "models/item/"
                                            + name
                                            + "_"
                                            + cmd
                                            + ".json";

                            ResourceLocation subLoc =
                                    new ResourceLocation(
                                            "jagtaczarmor",
                                            subPath
                                    );

                            IoSupplier<InputStream> subResource =
                                    getResource(
                                            type,
                                            subLoc
                                    );

                            if (subResource != null) {
                                output.accept(
                                        subLoc,
                                        subResource
                                );
                            }
                        }
                    }
                }

                /*
                 * Старый общий plate_armor оставляем для
                 * обратной совместимости.
                 */
                ResourceLocation plateLoc =
                        new ResourceLocation(
                                "jagtaczarmor",
                                "models/item/plate_armor.json"
                        );

                IoSupplier<InputStream> plateResource =
                        getResource(
                                type,
                                plateLoc
                        );

                if (plateResource != null) {
                    output.accept(
                            plateLoc,
                            plateResource
                    );
                }

                for (
                        int cmd :
                        AddonPackLoader
                                .PLATE_CMD_TEXTURES
                                .keySet()
                ) {

                    String subPath =
                            "models/item/plate_armor_"
                                    + cmd
                                    + ".json";

                    ResourceLocation subLoc =
                            new ResourceLocation(
                                    "jagtaczarmor",
                                    subPath
                            );

                    IoSupplier<InputStream> subResource =
                            getResource(
                                    type,
                                    subLoc
                            );

                    if (subResource != null) {
                        output.accept(
                                subLoc,
                                subResource
                        );
                    }
                }
            }
        }
    }

    @Override
    public Set<String> getNamespaces(
            PackType type
    ) {

        if (type != PackType.CLIENT_RESOURCES) {
            return Set.of();
        }

        /*
         * Обязательно добавляем jagtaczarmor
         * для собственных ресурсов мода.
         */
        Set<String> namespaces =
                new HashSet<>();

        namespaces.add(
                "jagtaczarmor"
        );

        /*
         * Namespace armor-паков.
         */
        for (
                ResourceLocation id :
                AddonPackLoader.ARMOR_SET_INDEXES.keySet()
        ) {

            namespaces.add(
                    id.getNamespace()
            );
        }

        /*
         * Namespace plate-паков.
         *
         * Это важно, потому что теперь registry ID
         * плиты находится в namespace addon-пака.
         */
        for (
                ResourceLocation id :
                AddonPackLoader.PLATE_INDEXES.keySet()
        ) {

            namespaces.add(
                    id.getNamespace()
            );
        }

        return namespaces;
    }

    @Override
    public <T> @Nullable T getMetadataSection(
            MetadataSectionSerializer<T> serializer
    ) {

        return (T) (
                "pack".equals(
                        serializer.getMetadataSectionName()
                )
                        ? new PackMetadataSection(
                        Component.literal(
                                "JagTaczArmor Generated Resources"
                        ),
                        15
                )
                        : null
        );
    }

    @Override
    public String packId() {
        return "jagtaczarmor_generated";
    }

    @Override
    public void close() {
    }
}