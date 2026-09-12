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
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InMemoryPackResources implements PackResources {
    private static final String[] PIECE_NAMES = new String[]{"tk_hm", "tk_ch", "tk_lg", "tk_bt"};

    public InMemoryPackResources() {
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... paths) {
        if (paths.length == 1 && "pack.mcmeta".equals(paths[0])) {
            String meta = "{\"pack\":{\"pack_format\":15,\"description\":\"JagTaczArmor Generated Resources\"}}";
            return () -> new ByteArrayInputStream(meta.getBytes(StandardCharsets.UTF_8));
        }
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (!"jagtaczarmor".equals(location.getNamespace())) {
            return null;
        }
        String path = location.getPath();
        if (type == PackType.CLIENT_RESOURCES && path.startsWith("models/item/")) {
            String filename = path.substring("models/item/".length());
            if (filename.endsWith(".json")) {
                String modelName = filename.substring(0, filename.length() - ".json".length());

                // ===== ГЕНЕРАЦИЯ ДЛЯ АДДОННЫХ ПРЕДМЕТОВ =====
                boolean isAddon = false;
                for (ResourceLocation id : AddonPackLoader.ARMOR_SET_INDEXES.keySet()) {
                    if (id.getPath().equals(modelName)) {
                        isAddon = true;
                        break;
                    }
                }
                if (isAddon) {
                    String addonJson = generateAddonItemJson(modelName);
                    return () -> new ByteArrayInputStream(addonJson.getBytes(StandardCharsets.UTF_8));
                }
                // ===== КОНЕЦ ГЕНЕРАЦИИ =====

                if ("plate_armor".equals(modelName)) {
                    return () -> new ByteArrayInputStream(generatePlateBaseJson().getBytes(StandardCharsets.UTF_8));
                }
                if (modelName.startsWith("plate_armor_")) {
                    try {
                        int cmd = Integer.parseInt(modelName.substring("plate_armor_".length()));
                        String tex = AddonPackLoader.PLATE_CMD_TEXTURES.get(cmd);
                        if (tex != null) {
                            String json = "{\n  \"parent\": \"item/generated\",\n  \"textures\": {\n    \"layer0\": \"" + tex + "\"\n  }\n}";
                            return () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                        }
                    } catch (NumberFormatException ignored) {}
                }
                for (int i = 0; i < PIECE_NAMES.length; i++) {
                    if (PIECE_NAMES[i].equals(modelName)) {
                        final int index = i;
                        return () -> new ByteArrayInputStream(generateBaseJson(index).getBytes(StandardCharsets.UTF_8));
                    }
                }
                for (int i = 0; i < PIECE_NAMES.length; i++) {
                    String prefix = PIECE_NAMES[i] + "_";
                    if (modelName.startsWith(prefix)) {
                        try {
                            int cmd = Integer.parseInt(modelName.substring(prefix.length()));
                            String tex = AddonPackLoader.getCmdTexture(cmd, i);
                            if (tex != null) {
                                String json = "{\n  \"parent\": \"item/generated\",\n  \"textures\": {\n    \"layer0\": \"" + tex + "\"\n  }\n}";
                                return () -> new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        String resourcePath = "/assets/" + location.getNamespace() + "/" + location.getPath();
        URL url = InMemoryPackResources.class.getResource(resourcePath);
        return url != null ? () -> url.openStream() : null;
    }

    /**
     * Генерирует модель для аддонного предмета
     */
    private String generateAddonItemJson(String modelName) {
        String texturePath = "jagtaczarmor:item/" + modelName;

        // Ищем текстуру в ARMOR_SET_INDEXES по пути
        for (ResourceLocation id : AddonPackLoader.ARMOR_SET_INDEXES.keySet()) {
            if (id.getPath().equals(modelName)) {
                ArmorSetIndex setIndex = AddonPackLoader.ARMOR_SET_INDEXES.get(id);
                if (setIndex.helmet != null && setIndex.helmet.itemTexture != null) {
                    texturePath = setIndex.helmet.itemTexture;
                } else if (setIndex.chestplate != null && setIndex.chestplate.itemTexture != null) {
                    texturePath = setIndex.chestplate.itemTexture;
                } else if (setIndex.leggings != null && setIndex.leggings.itemTexture != null) {
                    texturePath = setIndex.leggings.itemTexture;
                } else if (setIndex.boots != null && setIndex.boots.itemTexture != null) {
                    texturePath = setIndex.boots.itemTexture;
                }
                break;
            }
        }

        if (texturePath == null || texturePath.isEmpty()) {
            texturePath = "jagtaczarmor:item/tab_icon";
        }
        if (texturePath.endsWith(".png")) {
            texturePath = texturePath.substring(0, texturePath.length() - 4);
        }
        if (texturePath.contains(":textures/")) {
            texturePath = texturePath.replace(":textures/", ":");
        }
        return "{\n" +
                "  \"parent\": \"item/generated\",\n" +
                "  \"textures\": {\n" +
                "    \"layer0\": \"" + texturePath + "\"\n" +
                "  }\n" +
                "}";
    }

    private String generateBaseJson(int pieceIdx) {
        StringBuilder overrides = new StringBuilder();
        boolean first = true;
        List<Map.Entry<Integer, Map<Integer, String>>> entries = new ArrayList<>(AddonPackLoader.getCmdTextures().entrySet());
        entries.sort(Map.Entry.comparingByKey());
        for (Map.Entry<Integer, Map<Integer, String>> entry : entries) {
            int cmd = entry.getKey();
            String tex = entry.getValue().get(pieceIdx);
            if (tex != null) {
                if (!first) overrides.append(",\n    ");
                overrides.append("{\"predicate\":{\"custom_model_data\":").append(cmd).append("},\"model\":\"")
                        .append("jagtaczarmor").append(":item/").append(PIECE_NAMES[pieceIdx]).append("_").append(cmd).append("\"}");
                first = false;
            }
        }
        return "{\n  \"parent\": \"builtin/entity\",\n  \"overrides\": [\n    " + overrides + "\n  ]\n}";
    }

    private String generatePlateBaseJson() {
        StringBuilder overrides = new StringBuilder();
        boolean first = true;
        List<Map.Entry<Integer, String>> entries = new ArrayList<>(AddonPackLoader.PLATE_CMD_TEXTURES.entrySet());
        entries.sort(Map.Entry.comparingByKey());
        for (Map.Entry<Integer, String> entry : entries) {
            int cmd = entry.getKey();
            if (!first) overrides.append(",\n    ");
            overrides.append("{\"predicate\":{\"custom_model_data\":").append(cmd).append("},\"model\":\"")
                    .append("jagtaczarmor").append(":item/plate_armor_").append(cmd).append("\"}");
            first = false;
        }
        return "{\n  \"parent\": \"item/generated\",\n  \"textures\": {\n    \"layer0\": \"jagtaczarmor:item/tab_icon\"\n  },\n  \"overrides\": [\n    " + overrides + "\n  ]\n}";
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type == PackType.CLIENT_RESOURCES && "jagtaczarmor".equals(namespace)) {
            if ("models/item".equals(path) || "models".equals(path) || "".equals(path)) {
                // ===== ГЕНЕРИРУЕМ МОДЕЛИ ДЛЯ ВСЕХ АДДОНОВ =====
                for (ResourceLocation id : AddonPackLoader.ARMOR_SET_INDEXES.keySet()) {
                    String name = id.getPath();
                    ResourceLocation loc = new ResourceLocation("jagtaczarmor", "models/item/" + name + ".json");
                    output.accept(loc, this.getResource(type, loc));
                }
                // ===== КОНЕЦ ГЕНЕРАЦИИ =====

                for (int i = 0; i < PIECE_NAMES.length; i++) {
                    String name = PIECE_NAMES[i];
                    ResourceLocation loc = new ResourceLocation("jagtaczarmor", "models/item/" + name + ".json");
                    output.accept(loc, this.getResource(type, loc));
                    for (Map.Entry<Integer, Map<Integer, String>> entry : AddonPackLoader.getCmdTextures().entrySet()) {
                        int cmd = entry.getKey();
                        if (entry.getValue().containsKey(i)) {
                            String subPath = "models/item/" + name + "_" + cmd + ".json";
                            ResourceLocation subLoc = new ResourceLocation("jagtaczarmor", subPath);
                            output.accept(subLoc, this.getResource(type, subLoc));
                        }
                    }
                }
                ResourceLocation plateLoc = new ResourceLocation("jagtaczarmor", "models/item/plate_armor.json");
                output.accept(plateLoc, this.getResource(type, plateLoc));
                for (int cmd : AddonPackLoader.PLATE_CMD_TEXTURES.keySet()) {
                    String subPath = "models/item/plate_armor_" + cmd + ".json";
                    output.accept(new ResourceLocation("jagtaczarmor", subPath), this.getResource(type, new ResourceLocation("jagtaczarmor", subPath)));
                }
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? Set.of("jagtaczarmor") : Set.of();
    }

    @Override
    public <T> @Nullable T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        return (T) ("pack".equals(serializer.getMetadataSectionName())
                ? new PackMetadataSection(Component.literal("JagTaczArmor Generated Resources"), 15)
                : null);
    }

    @Override
    public String packId() {
        return "jagtaczarmor_generated";
    }

    @Override
    public void close() {
    }
}