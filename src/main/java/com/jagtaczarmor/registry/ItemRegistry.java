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
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
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

    private static final Set<ResourceLocation> REGISTERED_DYNAMIC_ITEMS =
            new HashSet<>();

    private static final Map<ResourceLocation, RegistryObject<Item>> ADDON_ITEMS =
            new java.util.HashMap<>();

    private ItemRegistry() {
    }

    @SubscribeEvent
    public static void onItemRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(
                ForgeRegistries.ITEMS.getRegistryKey()
        )) {
            return;
        }

        registerAddonItems(event);
    }

    private static void registerAddonItems(RegisterEvent event) {
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
            if (pack.isDirectory()) {
                registerDirectoryPack(event, pack.toPath());
            } else if (
                    pack.isFile()
                            && pack.getName().toLowerCase().endsWith(".zip")
            ) {
                registerZipPack(event, pack.toPath());
            }
        }
    }

    private static void registerDirectoryPack(
            RegisterEvent event,
            Path packPath
    ) {
        Path itemsPath = packPath.resolve("items.json");

        if (!Files.isRegularFile(itemsPath)) {
            return;
        }

        String namespace = packPath.getFileName().toString();

        try (
                InputStream stream = Files.newInputStream(itemsPath);
                InputStreamReader reader =
                        new InputStreamReader(
                                stream,
                                StandardCharsets.UTF_8
                        )
        ) {
            JsonObject root = GSON.fromJson(
                    reader,
                    JsonObject.class
            );

            if (root == null) {
                return;
            }

            registerArmorGroup(
                    event,
                    root,
                    namespace
            );

        } catch (IOException | RuntimeException exception) {
            JagTaczArmor.LOGGER.error(
                    "Failed to load item manifest '{}'.",
                    itemsPath,
                    exception
            );
        }
    }

    private static void registerZipPack(
            RegisterEvent event,
            Path packPath
    ) {
        String fileName = packPath.getFileName().toString();

        String namespace = fileName.substring(
                0,
                fileName.length() - 4
        );

        try (ZipFile zipFile = new ZipFile(packPath.toFile())) {

            ZipEntry manifestEntry =
                    zipFile.getEntry("items.json");

            if (manifestEntry == null) {
                return;
            }

            try (
                    InputStream stream =
                            zipFile.getInputStream(manifestEntry);

                    InputStreamReader reader =
                            new InputStreamReader(
                                    stream,
                                    StandardCharsets.UTF_8
                            )
            ) {
                JsonObject root = GSON.fromJson(
                        reader,
                        JsonObject.class
                );

                if (root == null) {
                    return;
                }

                registerArmorGroup(
                        event,
                        root,
                        namespace
                );
            }

        } catch (IOException | RuntimeException exception) {
            JagTaczArmor.LOGGER.error(
                    "Failed to load item manifest from '{}'.",
                    packPath,
                    exception
            );
        }
    }

    private static void registerArmorGroup(
            RegisterEvent event,
            JsonObject root,
            String namespace
    ) {
        if (!root.has("armor")
                || !root.get("armor").isJsonObject()) {
            return;
        }

        JsonObject armorGroup =
                root.getAsJsonObject("armor");

        for (Map.Entry<String, JsonElement> entry :
                armorGroup.entrySet()) {

            String itemName = entry.getKey();

            if (!entry.getValue().isJsonObject()) {
                JagTaczArmor.LOGGER.warn(
                        "Invalid armor item definition '{}' in pack '{}'.",
                        itemName,
                        namespace
                );
                continue;
            }

            JsonObject itemObject =
                    entry.getValue().getAsJsonObject();

            String itemType =
                    itemObject.has("item_type")
                            ? itemObject
                            .get("item_type")
                            .getAsString()
                            : "armor";

            if (!"armor".equals(itemType)) {
                JagTaczArmor.LOGGER.warn(
                        "Unknown armor item type '{}' for '{}:{}'.",
                        itemType,
                        namespace,
                        itemName
                );
                continue;
            }

            ResourceLocation itemId =
                    ResourceLocation.tryBuild(
                            namespace,
                            itemName
                    );

            if (itemId == null) {
                JagTaczArmor.LOGGER.warn(
                        "Invalid armor item id '{}:{}'",
                        namespace,
                        itemName
                );
                continue;
            }

            if (!REGISTERED_DYNAMIC_ITEMS.add(itemId)) {
                JagTaczArmor.LOGGER.warn(
                        "Duplicate dynamic armor item '{}'. Skipping.",
                        itemId
                );
                continue;
            }

            ArmorDefinition definition =
                    findArmorDefinition(itemId);

            if (definition == null) {
                REGISTERED_DYNAMIC_ITEMS.remove(itemId);

                JagTaczArmor.LOGGER.warn(
                        "No armor definition found for item '{}'.",
                        itemId
                );

                continue;
            }

            CustomGeoArmorItem item =
                    new CustomGeoArmorItem(
                            definition.slot,
                            new Item.Properties(),
                            definition.index,
                            itemId.toString()
                    );

            event.register(
                    ForgeRegistries.ITEMS.getRegistryKey(),
                    helper -> helper.register(
                            itemId,
                            item
                    )
            );

            ADDON_ITEMS.put(
                    itemId,
                    RegistryObject.create(
                            itemId,
                            ForgeRegistries.ITEMS
                    )
            );

            JagTaczArmor.LOGGER.info(
                    "Registered armor item '{}' -> {}",
                    itemId,
                    definition.slot
            );
        }
    }

    private static ArmorDefinition findArmorDefinition(
            ResourceLocation itemId
    ) {
        ArmorSetIndex set =
                AddonPackLoader.ARMOR_SET_INDEXES.get(itemId);

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

    public static RegistryObject<Item> getAddonItem(
            ResourceLocation id
    ) {
        return ADDON_ITEMS.get(id);
    }

    public static RegistryObject<Item> getAddonItem(
            String id
    ) {
        for (Map.Entry<ResourceLocation, RegistryObject<Item>> entry :
                ADDON_ITEMS.entrySet()) {

            if (entry.getKey().getPath().equals(id)) {
                return entry.getValue();
            }
        }

        return null;
    }

    public static boolean isAddonArmor(
            ResourceLocation id
    ) {
        return ADDON_ITEMS.containsKey(id);
    }

    public static boolean isAddonArmor(
            String id
    ) {
        return getAddonItem(id) != null;
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