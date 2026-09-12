package com.jagtaczarmor.item;

import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.data.ArmorIndex;
import com.jagtaczarmor.data.PlateIndex;
import com.jagtaczarmor.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CustomPlateItem extends Item {

    /**
     * Настоящий ID плиты.
     *
     * Например:
     *
     *     jag_default_armor:ceramic_plate
     *
     *     lrarmor_pack:steel_plate
     *
     * Теперь он является частью самого Item,
     * а не только NBT ItemStack.
     */
    private final ResourceLocation plateId;

    public CustomPlateItem(
            Properties properties,
            ResourceLocation plateId
    ) {
        super(properties);
        this.plateId = plateId;
    }

    /**
     * Возвращает настоящий ID этой плиты.
     */
    public ResourceLocation getPlateId() {
        return plateId;
    }

    /**
     * Получает PlateIndex этой плиты.
     */
    private PlateIndex getPlateIndex() {
        return AddonPackLoader.PLATE_INDEXES.get(
                plateId
        );
    }

    @Override
    public InteractionResultHolder<ItemStack> use(
            Level level,
            Player player,
            InteractionHand hand
    ) {

        ItemStack plateStack =
                player.getItemInHand(hand);

        ItemStack chestplate =
                player.getItemBySlot(
                        EquipmentSlot.CHEST
                );

        if (!chestplate.isEmpty()) {

            Item item =
                    chestplate.getItem();

            if (item instanceof ArmorItem armorItem) {

                if (
                        armorItem.getType()
                                == ArmorItem.Type.CHESTPLATE
                ) {

                    ArmorIndex vestIndex =
                            CustomGeoArmorItem.getIndex(
                                    chestplate
                            );

                    if (
                            vestIndex != null
                                    && vestIndex.plateSlot
                    ) {

                        player.startUsingItem(
                                hand
                        );

                        return InteractionResultHolder.success(
                                plateStack
                        );
                    }

                    if (level.isClientSide()) {

                        player.displayClientMessage(
                                Component.literal(
                                        "§cThis chestplate vest does not support armor plates!"
                                ),
                                true
                        );
                    }

                    return InteractionResultHolder.fail(
                            plateStack
                    );
                }
            }
        }

        if (level.isClientSide()) {

            player.displayClientMessage(
                    Component.literal(
                            "§cYou must equip a chestplate vest!"
                    ),
                    true
            );
        }

        return InteractionResultHolder.fail(
                plateStack
        );
    }

    @Override
    public int getUseDuration(
            ItemStack stack
    ) {
        return 30;
    }

    @Override
    public UseAnim getUseAnimation(
            ItemStack stack
    ) {
        return UseAnim.BOW;
    }

    @Override
    public ItemStack finishUsingItem(
            ItemStack stack,
            Level level,
            LivingEntity entity
    ) {

        if (
                !level.isClientSide()
                        && entity instanceof Player player
        ) {

            ItemStack chestplate =
                    player.getItemBySlot(
                            EquipmentSlot.CHEST
                    );

            if (!chestplate.isEmpty()) {

                ArmorIndex vestIndex =
                        CustomGeoArmorItem.getIndex(
                                chestplate
                        );

                if (
                        vestIndex != null
                                && vestIndex.plateSlot
                ) {

                    /*
                     * =================================================
                     * НОВЫЙ ID
                     * =================================================
                     *
                     * Приоритет:
                     *
                     * 1. настоящий registry ID Item;
                     * 2. старый plate_id из NBT.
                     *
                     * Таким образом старые предметы тоже
                     * остаются совместимыми.
                     */
                    String targetPlateId =
                            plateId.toString();

                    if (
                            stack.hasTag()
                                    && stack.getTag().contains(
                                    "plate_id"
                            )
                    ) {

                        String legacyPlateId =
                                stack.getTag().getString(
                                        "plate_id"
                                );

                        if (
                                !legacyPlateId.isEmpty()
                                        && !legacyPlateId.equals(
                                        targetPlateId
                                )
                        ) {

                            /*
                             * Старый NBT ID используем только
                             * если он действительно существует.
                             */
                            ResourceLocation legacyId =
                                    ResourceLocation.tryParse(
                                            legacyPlateId
                                    );

                            if (
                                    legacyId != null
                                            && AddonPackLoader.PLATE_INDEXES
                                            .containsKey(legacyId)
                            ) {

                                targetPlateId =
                                        legacyId.toString();
                            }
                        }
                    }

                    ResourceLocation targetId =
                            ResourceLocation.tryParse(
                                    targetPlateId
                            );

                    if (targetId == null) {

                        player.displayClientMessage(
                                Component.literal(
                                        "§cInvalid plate ID: "
                                                + targetPlateId
                                ),
                                true
                        );

                        return stack;
                    }

                    PlateIndex plateIndex =
                            AddonPackLoader.PLATE_INDEXES.get(
                                    targetId
                            );

                    if (plateIndex != null) {

                        CompoundTag tag =
                                chestplate.getOrCreateTag();

                        /*
                         * Если в нагруднике уже стоит плита,
                         * возвращаем её в инвентарь.
                         */
                        if (
                                tag.contains("plate_id")
                                        && tag.contains(
                                        "plate_durability"
                                )
                        ) {

                            String oldPlateId =
                                    tag.getString(
                                            "plate_id"
                                    );

                            int oldPlateDur =
                                    tag.getInt(
                                            "plate_durability"
                                    );

                            ResourceLocation oldRes =
                                    ResourceLocation.tryParse(
                                            oldPlateId
                                    );

                            if (oldRes != null) {

                                PlateIndex oldPlateIndex =
                                        AddonPackLoader.PLATE_INDEXES.get(
                                                oldRes
                                        );

                                /*
                                 * Старую плиту создаём
                                 * по её настоящему registry ID.
                                 */
                                Item oldPlateItem =
                                        ItemRegistry.getPlateItem(
                                                oldRes
                                        );

                                if (oldPlateItem != null) {

                                    ItemStack oldPlateStack =
                                            new ItemStack(
                                                    oldPlateItem
                                            );

                                    /*
                                     * Оставляем NBT plate_id для
                                     * совместимости.
                                     */
                                    oldPlateStack
                                            .getOrCreateTag()
                                            .putString(
                                                    "plate_id",
                                                    oldPlateId
                                            );

                                    oldPlateStack
                                            .getOrCreateTag()
                                            .putInt(
                                                    "plate_durability",
                                                    oldPlateDur
                                            );

                                    if (
                                            tag.contains(
                                                    "plate_enchantments"
                                            )
                                    ) {

                                        oldPlateStack
                                                .getOrCreateTag()
                                                .put(
                                                        "Enchantments",
                                                        tag.get(
                                                                "plate_enchantments"
                                                        )
                                                );
                                    }

                                    if (
                                            oldPlateIndex != null
                                                    && oldPlateIndex.durability > 0
                                    ) {

                                        int damage =
                                                oldPlateIndex.durability
                                                        - oldPlateDur;

                                        oldPlateStack.setDamageValue(
                                                Math.max(
                                                        0,
                                                        damage
                                                )
                                        );
                                    }

                                    if (
                                            !player.getInventory()
                                                    .add(
                                                            oldPlateStack
                                                    )
                                    ) {

                                        player.drop(
                                                oldPlateStack,
                                                false
                                        );
                                    }
                                }
                            }
                        }

                        /*
                         * Устанавливаем новую плиту.
                         */
                        tag.putString(
                                "plate_id",
                                targetPlateId
                        );

                        int targetDurability =
                                plateIndex.durability;

                        if (
                                stack.hasTag()
                                        && stack.getTag().contains(
                                        "plate_durability"
                                )
                        ) {

                            targetDurability =
                                    stack.getTag().getInt(
                                            "plate_durability"
                                    );

                        } else if (
                                stack.isDamageableItem()
                        ) {

                            targetDurability =
                                    Math.max(
                                            0,
                                            plateIndex.durability
                                                    - stack.getDamageValue()
                                    );
                        }

                        tag.putInt(
                                "plate_durability",
                                targetDurability
                        );

                        if (
                                stack.hasTag()
                                        && stack.getTag().contains(
                                        "Enchantments"
                                )
                        ) {

                            tag.put(
                                    "plate_enchantments",
                                    stack.getTag().get(
                                            "Enchantments"
                                    )
                            );

                        } else {

                            tag.remove(
                                    "plate_enchantments"
                            );
                        }

                        if (
                                !player.getAbilities()
                                        .instabuild
                        ) {

                            stack.shrink(1);
                        }

                        level.playSound(
                                null,
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                SoundEvents.ARMOR_EQUIP_CHAIN,
                                SoundSource.PLAYERS,
                                1.0F,
                                1.0F
                        );

                        player.displayClientMessage(
                                Component.literal(
                                        "§aPlate inserted successfully!"
                                ),
                                true
                        );

                        player.setItemSlot(
                                EquipmentSlot.CHEST,
                                chestplate
                        );

                    } else {

                        player.displayClientMessage(
                                Component.literal(
                                        "§cInvalid plate configuration: "
                                                + targetPlateId
                                ),
                                true
                        );
                    }
                }
            }
        }

        return stack;
    }

    @Override
    public Component getName(
            ItemStack stack
    ) {

        CustomGeoArmorItem
                .verifyAndSyncCustomModelData(stack);

        PlateIndex index =
                getPlateIndex();

        if (
                index != null
                        && index.name != null
        ) {

            return Component.literal(
                    index.name
            );
        }

        return super.getName(
                stack
        );
    }

    @Override
    public boolean isFoil(
            ItemStack stack
    ) {

        CustomGeoArmorItem
                .verifyAndSyncCustomModelData(stack);

        return super.isFoil(
                stack
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {

        CustomGeoArmorItem
                .verifyAndSyncCustomModelData(stack);

        PlateIndex index =
                getPlateIndex();

        if (index != null) {

            appendPlateStats(
                    tooltip,
                    index
            );
        }

        super.appendHoverText(
                stack,
                level,
                tooltip,
                flag
        );
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean isSelected
    ) {

        CustomGeoArmorItem
                .verifyAndSyncCustomModelData(stack);

        super.inventoryTick(
                stack,
                level,
                entity,
                slotId,
                isSelected
        );
    }

    public static void appendPlateStats(
            List<Component> tooltip,
            PlateIndex index
    ) {

        tooltip.add(
                Component.literal(
                        "------------------------------------"
                ).withStyle(
                        ChatFormatting.DARK_GRAY,
                        ChatFormatting.STRIKETHROUGH
                )
        );

        tooltip.add(
                Component.literal(
                        "Inserts into compatible vests:"
                ).withStyle(
                        ChatFormatting.GRAY
                )
        );

        if (index.defense > 0) {

            tooltip.add(
                    Component.literal(
                            String.format(
                                    "  Armor: +%d",
                                    index.defense
                            )
                    ).withStyle(
                            ChatFormatting.GREEN
                    )
            );
        }

        if (index.toughness > 0) {

            tooltip.add(
                    Component.literal(
                            String.format(
                                    "  Toughness: +%d",
                                    index.toughness
                            )
                    ).withStyle(
                            ChatFormatting.GREEN
                    )
            );
        }

        if (index.speedModify != 0.0F) {

            ChatFormatting color =
                    index.speedModify > 0.0F
                            ? ChatFormatting.GREEN
                            : ChatFormatting.RED;

            tooltip.add(
                    Component.literal(
                            String.format(
                                    "  Movement Speed: %+.0f%%",
                                    index.speedModify * 100.0F
                            )
                    ).withStyle(
                            color
                    )
            );
        }

        if (index.jumpModify != 0.0F) {

            ChatFormatting color =
                    index.jumpModify > 0.0F
                            ? ChatFormatting.GREEN
                            : ChatFormatting.RED;

            tooltip.add(
                    Component.literal(
                            String.format(
                                    "  Jump Height: %+.0f%%",
                                    index.jumpModify * 100.0F
                            )
                    ).withStyle(
                            color
                    )
            );
        }

        if (index.ammoImmunity > 0.0F) {

            double cappedImmunity =
                    Math.min(
                            1.0F,
                            index.ammoImmunity
                    );

            tooltip.add(
                    Component.literal(
                            String.format(
                                    "  Ammo Immunity: +%.0f%%",
                                    cappedImmunity * 100.0F
                            )
                    ).withStyle(
                            ChatFormatting.GOLD
                    )
            );
        }

        if (index.damageReductionCap > 0.0F) {

            double cappedReduction =
                    Math.min(
                            1.0F,
                            index.damageReductionCap
                    );

            tooltip.add(
                    Component.literal(
                            String.format(
                                    "  Impact Reduction: +%.0f%%",
                                    cappedReduction * 100.0F
                            )
                    ).withStyle(
                            ChatFormatting.GOLD
                    )
            );
        }

        if (index.knockbackResistance > 0.0F) {

            tooltip.add(
                    Component.literal(
                            String.format(
                                    "  Knockback Resistance: +%.1f",
                                    index.knockbackResistance
                            )
                    ).withStyle(
                            ChatFormatting.GREEN
                    )
            );
        }

        if (index.blockVanillaProjectile) {

            tooltip.add(
                    Component.literal(
                            "  Deflects vanilla projectiles"
                    ).withStyle(
                            ChatFormatting.BLUE
                    )
            );
        }

        tooltip.add(
                Component.literal(
                        "------------------------------------"
                ).withStyle(
                        ChatFormatting.DARK_GRAY,
                        ChatFormatting.STRIKETHROUGH
                )
        );
    }

    @Override
    public boolean isDamageable(
            ItemStack stack
    ) {
        return true;
    }

    @Override
    public int getMaxDamage(
            ItemStack stack
    ) {

        PlateIndex index =
                getPlateIndex();

        if (
                index != null
                        && index.durability > 0
        ) {
            return index.durability;
        }

        return 100;
    }

    @Override
    public boolean isEnchantable(
            ItemStack stack
    ) {
        return true;
    }

    @Override
    public boolean isBookEnchantable(
            ItemStack stack,
            ItemStack book
    ) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 15;
    }

    @Override
    public boolean canApplyAtEnchantingTable(
            ItemStack stack,
            Enchantment enchantment
    ) {

        return enchantment == Enchantments.UNBREAKING
                || enchantment == Enchantments.MENDING;
    }
}