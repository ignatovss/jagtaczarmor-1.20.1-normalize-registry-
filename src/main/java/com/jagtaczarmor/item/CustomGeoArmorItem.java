package com.jagtaczarmor.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.jagtaczarmor.client.ClientTooltipHelper;
import com.jagtaczarmor.client.renderer.CustomGeoArmorItemRenderer;
import com.jagtaczarmor.client.renderer.CustomGeoArmorRenderer;
import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.data.ArmorIndex;
import com.jagtaczarmor.data.ArmorSetIndex;
import com.jagtaczarmor.data.PlateIndex;
import com.jagtaczarmor.event.DamageEventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class CustomGeoArmorItem extends ArmorItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final UUID[] ARMOR_MODIFIER_UUID_PER_SLOT = new UUID[]{
            UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"),
            UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"),
            UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"),
            UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")
    };

    // ===== НАШЕ ДОПОЛНЕНИЕ =====
    public final ArmorIndex armorIndex;
    private final String itemId;

    // Новый конструктор для аддонных предметов
    public CustomGeoArmorItem(ArmorItem.Type type, Item.Properties properties, ArmorIndex index, String itemId) {
        super(CustomArmorMaterial.INSTANCE, type, properties);
        this.armorIndex = index;
        this.itemId = itemId;
    }
    // ===== КОНЕЦ НАШЕГО ДОПОЛНЕНИЯ =====

    // Оригинальный конструктор
    public CustomGeoArmorItem(ArmorItem.Type type, Item.Properties properties) {
        super(CustomArmorMaterial.INSTANCE, type, properties);
        this.armorIndex = null;
        this.itemId = null;
    }

    // ===== ГЕТТЕР ДЛЯ itemId =====
    public String getItemId() {
        return itemId;
    }
    // ===== КОНЕЦ ГЕТТЕРА =====

    public static ArmorSetIndex getSetIndex(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("armor_id")) {
            String armorId = stack.getTag().getString("armor_id");
            return AddonPackLoader.ARMOR_SET_INDEXES.get(new ResourceLocation(armorId));
        } else {
            return stack.getItem() instanceof CustomGeoArmorItem ? AddonPackLoader.ARMOR_SET_INDEXES.get(new ResourceLocation("jag_default_armor", "tactical_armor")) : null;
        }
    }

    public static ArmorIndex getIndex(ItemStack stack) {
        if (stack != null && !stack.isEmpty()) {
            Item item = stack.getItem();
            if (item instanceof CustomGeoArmorItem) {
                CustomGeoArmorItem customItem = (CustomGeoArmorItem) item;

                // ===== НОВАЯ ЛОГИКА =====
                if (customItem.armorIndex != null) {
                    return customItem.armorIndex;
                }
                // ===== КОНЕЦ НОВОЙ ЛОГИКИ =====

                ArmorSetIndex setIndex = getSetIndex(stack);
                return setIndex != null ? setIndex.getPiece(customItem.getEquipmentSlot()) : null;
            } else {
                ResourceLocation itemLoc = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (itemLoc != null) {
                    String registryName = itemLoc.toString();
                    ArmorIndex taggedIndex = AddonPackLoader.TAGGED_ARMORS.get(registryName);
                    if (taggedIndex != null) {
                        return taggedIndex;
                    }
                }
                return null;
            }
        } else {
            return null;
        }
    }

    public static void verifyAndSyncCustomModelData(ItemStack stack) {
        if (stack != null && !stack.isEmpty() && stack.hasTag()) {
            if (stack.getTag().contains("armor_id")) {
                String armorId = stack.getTag().getString("armor_id");
                ResourceLocation id = ResourceLocation.tryParse(armorId);
                if (id != null && AddonPackLoader.ARMOR_SET_CMD.containsKey(id)) {
                    int currentCmd = AddonPackLoader.ARMOR_SET_CMD.get(id);
                    if (!stack.getTag().contains("CustomModelData") || stack.getTag().getInt("CustomModelData") != currentCmd) {
                        stack.getTag().putInt("CustomModelData", currentCmd);
                    }
                }
            } else if (stack.getTag().contains("plate_id")) {
                String plateId = stack.getTag().getString("plate_id");
                ResourceLocation id = ResourceLocation.tryParse(plateId);
                if (id != null && AddonPackLoader.PLATE_CMD.containsKey(id)) {
                    int currentCmd = AddonPackLoader.PLATE_CMD.get(id);
                    if (!stack.getTag().contains("CustomModelData") || stack.getTag().getInt("CustomModelData") != currentCmd) {
                        stack.getTag().putInt("CustomModelData", currentCmd);
                    }
                }
            }
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        verifyAndSyncCustomModelData(stack);
        ArmorIndex index = getIndex(stack);
        if (index != null && stack.getItem() instanceof CustomGeoArmorItem) {
            String nameToUse = index.displayName;
            if ((nameToUse == null || "Custom Armor".equals(nameToUse)) && index.name != null) {
                nameToUse = index.name;
            }
            if (nameToUse != null) {
                return Component.literal(nameToUse);
            }
        }
        return stack.hasTag() && stack.getTag().contains("armor_id") ? Component.translatable("item.jagtaczarmor.removed_armor") : super.getName(stack);
    }

    private static boolean isShiftDown() {
        return FMLEnvironment.dist.isClient() ? ClientTooltipHelper.isShiftDown() : false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        verifyAndSyncCustomModelData(stack);
        ArmorIndex index = getIndex(stack);
        if (index != null) {
            appendStatsAndMeta(stack, tooltip, flag, index);
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public static void appendStatsAndMeta(ItemStack stack, List<Component> tooltip, TooltipFlag flag, ArmorIndex index) {
        double totalSpeedModify = index.speedModify;
        double totalJumpModify = index.jumpModify;
        double totalAmmoImmunity = index.ammoImmunity;
        double totalReductionCap = index.damageReductionCap;
        boolean hasPlate = false;
        String insertedPlateName = null;
        int plateDur = 0;
        int plateMaxDur = 0;
        if (stack.hasTag() && stack.getTag().contains("plate_id")) {
            String plateId = stack.getTag().getString("plate_id");
            plateDur = stack.getTag().getInt("plate_durability");
            PlateIndex plateIndex = AddonPackLoader.PLATE_INDEXES.get(new ResourceLocation(plateId));
            if (plateIndex != null) {
                hasPlate = true;
                insertedPlateName = plateIndex.name;
                plateMaxDur = plateIndex.durability;
                totalSpeedModify += plateIndex.speedModify;
                totalJumpModify += plateIndex.jumpModify;
                totalAmmoImmunity += plateIndex.ammoImmunity;
                totalReductionCap += plateIndex.damageReductionCap;
            }
        }

        boolean hasStats = totalAmmoImmunity > 0.0F || totalReductionCap > 0.0F || totalSpeedModify != 0.0F || totalJumpModify != 0.0F || index.plateSlot;
        if (hasStats) {
            tooltip.add(Component.literal("------------------------------------").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH));
            boolean shiftDown = isShiftDown();
            if (index.plateSlot) {
                if (hasPlate) {
                    tooltip.add(Component.literal("Active Plate: " + insertedPlateName).withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltip.add(Component.translatable("tooltip.jagtaczarmor.plate_slot").withStyle(ChatFormatting.GRAY));
                }
            }

            if (totalAmmoImmunity > 0.0F) {
                double displayImmunity = Math.min(1.0F, totalAmmoImmunity);
                Component val = Component.literal(String.format("%.0f%%", displayImmunity * 100.0F)).withStyle(ChatFormatting.GOLD);
                tooltip.add(Component.translatable("tooltip.jagtaczarmor.ammo_immunity", val).withStyle(ChatFormatting.GOLD));
                if (shiftDown) {
                    tooltip.add(Component.translatable("tooltip.jagtaczarmor.ammo_immunity.desc", val));
                }
            }

            if (totalReductionCap > 0.0F) {
                double displayReduction = Math.min(1.0F, totalReductionCap);
                Component val = Component.literal(String.format("%.0f%%", displayReduction * 100.0F)).withStyle(ChatFormatting.GOLD);
                tooltip.add(Component.translatable("tooltip.jagtaczarmor.impact_reduction", val).withStyle(ChatFormatting.GOLD));
                if (shiftDown) {
                    tooltip.add(Component.translatable("tooltip.jagtaczarmor.impact_reduction.desc", val));
                }
            }

            if (totalSpeedModify != 0.0F) {
                ChatFormatting color = totalSpeedModify > 0.0F ? ChatFormatting.GREEN : ChatFormatting.RED;
                Component val = Component.literal(String.format("%+.0f%%", totalSpeedModify * 100.0F)).withStyle(color);
                tooltip.add(Component.translatable("tooltip.jagtaczarmor.speed_reduction", val).withStyle(color));
            }

            if (totalJumpModify != 0.0F) {
                ChatFormatting color = totalJumpModify > 0.0F ? ChatFormatting.GREEN : ChatFormatting.RED;
                Component val = Component.literal(String.format("%+.0f%%", totalJumpModify * 100.0F)).withStyle(color);
                tooltip.add(Component.translatable("tooltip.jagtaczarmor.jump_reduction", val).withStyle(color));
            }

            if (!shiftDown) {
                tooltip.add(Component.translatable("tooltip.jagtaczarmor.shift_hint"));
            }

            tooltip.add(Component.literal("------------------------------------").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.STRIKETHROUGH));
        }

        if (flag.isAdvanced()) {
            String packNamespace = index.packNamespace;
            if (packNamespace == null) {
                if (stack.hasTag() && stack.getTag().contains("armor_id")) {
                    String armorId = stack.getTag().getString("armor_id");
                    ResourceLocation loc = new ResourceLocation(armorId);
                    packNamespace = loc.getNamespace();
                } else {
                    packNamespace = "jag_default_armor";
                }
            }

            AddonPackLoader.PackMeta meta = AddonPackLoader.PACK_METAS.get(packNamespace);
            if (meta != null) {
                String packDisplayName = meta.name != null ? meta.name : packNamespace;
                if (meta.author != null && !meta.author.isBlank()) {
                    tooltip.add(Component.literal("Pack: ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.literal(packDisplayName).withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(" by ").withStyle(ChatFormatting.DARK_GRAY))
                            .append(Component.literal(meta.author).withStyle(ChatFormatting.GRAY)));
                } else {
                    tooltip.add(Component.literal("Pack: ").withStyle(ChatFormatting.DARK_GRAY)
                            .append(Component.literal(packDisplayName).withStyle(ChatFormatting.GRAY)));
                }
            }
        }
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        ArmorIndex index = getIndex(stack);
        if (index != null) {
            int[] MAX_DAMAGE_ARRAY = new int[]{13, 15, 16, 11};
            return MAX_DAMAGE_ARRAY[this.getEquipmentSlot().getIndex()] * index.durabilityMultiplier;
        } else {
            return super.getMaxDamage(stack);
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repairMaterial) {
        ArmorIndex index = getIndex(toRepair);
        if (index != null && index.repairItem != null) {
            ResourceLocation repairItemLoc = new ResourceLocation(index.repairItem);
            ResourceLocation materialLoc = ForgeRegistries.ITEMS.getKey(repairMaterial.getItem());
            return materialLoc != null && materialLoc.equals(repairItemLoc);
        } else {
            return super.isValidRepairItem(toRepair, repairMaterial);
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == this.getEquipmentSlot()) {
            ArmorIndex index = getIndex(stack);
            if (index != null) {
                ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
                UUID uuid = ARMOR_MODIFIER_UUID_PER_SLOT[slot.getIndex()];
                builder.put(Attributes.ARMOR, new AttributeModifier(uuid, "Armor modifier", index.defense, AttributeModifier.Operation.ADDITION));
                builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(uuid, "Armor toughness", index.toughness, AttributeModifier.Operation.ADDITION));
                if (index.knockbackResistance > 0.0F) {
                    builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uuid, "Armor knockback resistance", index.knockbackResistance, AttributeModifier.Operation.ADDITION));
                }
                return builder.build();
            }
        }
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        return DamageEventHandler.BYPASS_ARMOR_PROTECTION.get() ? amount : 0;
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        ArmorIndex index = getIndex(stack);
        if (index != null && index.texture != null) {
            String tex = index.texture;
            if (!tex.endsWith(".png")) {
                tex = tex + ".png";
            }
            return tex;
        }
        return "jagtaczarmor:textures/models/armor/custom_armor.png";
    }

    @Nullable
    public ResourceLocation getItemTexture(ItemStack stack) {
        ArmorIndex index = getIndex(stack);
        if (index != null && index.itemTexture != null) {
            String tex = index.itemTexture;
            if (!tex.endsWith(".png")) {
                tex = tex + ".png";
            }
            return new ResourceLocation(tex);
        }
        return null;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private CustomGeoArmorRenderer armorRenderer;
            private CustomGeoArmorItemRenderer itemRenderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, HumanoidModel<?> original) {
                if (this.armorRenderer == null) {
                    this.armorRenderer = new CustomGeoArmorRenderer();
                }
                this.armorRenderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.armorRenderer;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.itemRenderer == null) {
                    this.itemRenderer = new CustomGeoArmorItemRenderer();
                }
                return this.itemRenderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 20, this::predicate));
    }

    private PlayState predicate(AnimationState<CustomGeoArmorItem> state) {
        return PlayState.CONTINUE;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        verifyAndSyncCustomModelData(stack);
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        verifyAndSyncCustomModelData(stack);
        return super.isFoil(stack);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}