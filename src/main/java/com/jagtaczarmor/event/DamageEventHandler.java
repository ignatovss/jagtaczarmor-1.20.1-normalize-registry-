package com.jagtaczarmor.event;

import com.jagtaczarmor.config.ArmorConfig;
import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.data.ArmorIndex;
import com.jagtaczarmor.data.ArmorSetIndex;
import com.jagtaczarmor.data.PlateIndex;
import com.jagtaczarmor.item.CustomGeoArmorItem;
import com.jagtaczarmor.network.NetworkHandler;
import com.jagtaczarmor.network.packet.S2CDamageTiltPacket;
import com.jagtaczarmor.network.packet.S2CStopBulletPacket;
import com.jagtaczarmor.registry.ItemRegistry;
import com.jagtaczarmor.registry.ParticleRegistry;
import com.jagtaczarmor.registry.SoundRegistry;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.entity.KnockBackModifier;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.tacz.guns.api.event.common.GunDamageSourcePart;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

@EventBusSubscriber(modid = "jagtaczarmor")
public class DamageEventHandler {

    public static final ThreadLocal<Boolean> BYPASS_ARMOR_PROTECTION =
            ThreadLocal.withInitial(() -> false);

    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private static Field bulletPierceField = null;
    private static Field bulletArmorIgnoreField = null;
    private static Field bulletKnockbackField = null;

    private static boolean debugMode = false;

    private static final UUID SPEED_MODIFIER_UUID;
    private static final UUID PLATE_ARMOR_UUID;
    private static final UUID PLATE_TOUGHNESS_UUID;
    private static final UUID PLATE_KNOCKBACK_UUID;

    private static int lastSoundIndex;
    private static int lastHurtSoundIndex;

    private static Method firstAidGetSlotMethod;
    private static boolean firstAidChecked;

    public DamageEventHandler() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("jagtaczarmor")
                        .then(Commands.literal("reload")
                                .executes((context) -> {
                                    ArmorConfig.load();
                                    AddonPackLoader.reloadPacks();

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "§aJagTaczArmor packs reloaded!"
                                            ),
                                            true
                                    );

                                    return 1;
                                }))
                        .then(Commands.literal("debug")
                                .executes((context) -> {
                                    debugMode = !debugMode;

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "§eJagTaczArmor Debug Mode: "
                                                            + (debugMode
                                                            ? "§aON"
                                                            : "§cOFF")
                                            ),
                                            true
                                    );

                                    return 1;
                                }))
                        .then(Commands.literal("overwrite")
                                .executes((context) -> {
                                    boolean value =
                                            ArmorConfig.DATA.overwrite_default_pack;

                                    context.getSource().sendSuccess(
                                            () -> Component.literal(
                                                    "§eJagTaczArmor overwrite default pack: "
                                                            + (value
                                                            ? "§aTRUE"
                                                            : "§cFALSE")
                                            ),
                                            true
                                    );

                                    return 1;
                                })
                                .then(Commands.argument(
                                                "value",
                                                BoolArgumentType.bool()
                                        )
                                        .executes((context) -> {
                                            boolean value =
                                                    BoolArgumentType.getBool(
                                                            context,
                                                            "value"
                                                    );

                                            ArmorConfig.DATA.overwrite_default_pack =
                                                    value;

                                            ArmorConfig.save();

                                            context.getSource().sendSuccess(
                                                    () -> Component.literal(
                                                            "§aJagTaczArmor overwrite default pack set to: "
                                                                    + (value
                                                                    ? "§aTRUE"
                                                                    : "§cFALSE")
                                                    ),
                                                    true
                                            );

                                            return 1;
                                        }))
                        )
        );
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();

        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
            float maxCap = 0.0F;

            for (ItemStack stack : entity.getArmorSlots()) {
                ArmorIndex index = CustomGeoArmorItem.getIndex(stack);

                if (index != null) {
                    maxCap = Math.max(
                            maxCap,
                            index.damageReductionCap
                    );
                }
            }

            if (maxCap > 0.0F) {
                float reduction = maxCap * 0.25F;
                float originalAmount = event.getAmount();
                float newAmount =
                        originalAmount * (1.0F - reduction);

                event.setAmount(newAmount);

                if (debugMode && entity instanceof Player) {
                    Player player = (Player) entity;

                    player.displayClientMessage(
                            Component.literal(
                                    String.format(
                                            "§e[DEBUG] §fExplosion Reduced: §a%.1f%% §f(%.2f -> §c%.2f§f)",
                                            reduction * 100.0F,
                                            originalAmount,
                                            newAmount
                                    )
                            ),
                            false
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityHurtByGun(EntityHurtByGunEvent.Pre event) {
        if (!event.isCanceled()) {
            Entity bullet = event.getHurtEntity();

            if (bullet instanceof LivingEntity) {
                LivingEntity entity = (LivingEntity) bullet;
                bullet = event.getBullet();

                Vec3 hitPos = null;

                if (bullet != null) {
                    Vec3 dir =
                            bullet.getDeltaMovement().normalize();

                    if (dir.length() < 0.01
                            && event.getAttacker() != null) {

                        dir =
                                entity.position()
                                        .add(
                                                0.0D,
                                                entity.getBbHeight() / 2.0F,
                                                0.0D
                                        )
                                        .subtract(
                                                event.getAttacker().position()
                                        )
                                        .normalize();
                    }

                    Vec3 start = bullet.position();
                    Vec3 end = start.add(dir.scale(200.0F));

                    hitPos =
                            entity.getBoundingBox()
                                    .inflate(0.05)
                                    .clip(start, end)
                                    .orElse(null);
                }

                if (hitPos == null) {
                    double heightMul =
                            event.isHeadShot()
                                    ? 0.9
                                    : 0.6;

                    hitPos =
                            new Vec3(
                                    entity.getX(),
                                    entity.getY()
                                            + entity.getBbHeight()
                                            * heightMul,
                                    entity.getZ()
                            );
                }

                EquipmentSlot hitSlot = null;
                boolean firstAidProvidedSlot = false;

                if (entity instanceof Player) {
                    Player player = (Player) entity;

                    if (bullet != null) {
                        hitSlot =
                                getFirstAidHitSlot(
                                        bullet,
                                        player
                                );

                        if (hitSlot != null) {
                            firstAidProvidedSlot = true;
                        }
                    }
                }

                if (hitSlot == null) {
                    hitSlot =
                            getHitSlot(
                                    entity,
                                    hitPos,
                                    event.isHeadShot()
                            );
                }

                boolean effectiveHeadShot =
                        firstAidProvidedSlot
                                ? hitSlot == EquipmentSlot.HEAD
                                : event.isHeadShot();

                ItemStack hitStack =
                        entity.getItemBySlot(hitSlot);

                ArmorIndex hitIndex =
                        CustomGeoArmorItem.getIndex(hitStack);

                Vec3 victimLook =
                        entity.getLookAngle();

                Vec3 forward =
                        new Vec3(
                                victimLook.x,
                                0.0D,
                                victimLook.z
                        ).normalize();

                Vec3 right =
                        new Vec3(
                                -forward.z,
                                0.0D,
                                forward.x
                        ).normalize();

                Vec3 incomingDir = null;

                if (bullet != null) {
                    Vec3 bulletDir =
                            bullet.getDeltaMovement().normalize();

                    if (bulletDir.length() < 0.01
                            && event.getAttacker() != null) {

                        bulletDir =
                                entity.position()
                                        .add(
                                                0.0D,
                                                entity.getBbHeight() / 2.0F,
                                                0.0D
                                        )
                                        .subtract(
                                                event.getAttacker().position()
                                        )
                                        .normalize();
                    }

                    incomingDir =
                            new Vec3(
                                    -bulletDir.x,
                                    0.0D,
                                    -bulletDir.z
                            ).normalize();

                } else if (event.getAttacker() != null) {

                    incomingDir =
                            new Vec3(
                                    event.getAttacker().getX()
                                            - entity.getX(),
                                    0.0D,
                                    event.getAttacker().getZ()
                                            - entity.getZ()
                            ).normalize();
                }

                if (incomingDir == null
                        || incomingDir.length() < 0.01) {

                    incomingDir =
                            forward.scale(-1.0F);
                }

                double dotForward =
                        incomingDir.dot(forward);

                double dotRight =
                        incomingDir.dot(right);

                boolean isFrontShot =
                        dotForward >= Math.abs(dotRight);

                boolean isBackShot =
                        -dotForward >= Math.abs(dotRight);

                boolean isRightShot =
                        dotRight >= Math.abs(dotForward);

                boolean isLeftShot =
                        -dotRight >= Math.abs(dotForward);

                float initialPitchDir = 0.0F;
                float initialRollDir = 0.0F;
                float initialYawDir = 0.0F;

                if (hitSlot == EquipmentSlot.HEAD) {

                    initialPitchDir =
                            (float) (-dotForward);

                    initialRollDir =
                            (float) (-dotRight);

                    initialYawDir =
                            (float) (-dotRight);

                } else if (hitSlot == EquipmentSlot.CHEST) {

                    initialPitchDir =
                            (float) dotForward;

                    initialRollDir =
                            (float) dotRight;

                    initialYawDir =
                            (float) dotRight;

                } else {

                    boolean isLeftLeg =
                            hitPos != null
                                    && hitPos.subtract(
                                    entity.position()
                            ).dot(right) < 0.0F;

                    initialPitchDir =
                            (float) dotForward;

                    if (isLeftLeg) {
                        initialRollDir =
                                (float) Mth.clamp(
                                        -0.7 * dotForward
                                                + 0.7 * dotRight,
                                        -1.0F,
                                        1.0F
                                );
                    } else {
                        initialRollDir =
                                (float) Mth.clamp(
                                        0.7 * dotForward
                                                + 0.7 * dotRight,
                                        -1.0F,
                                        1.0F
                                );
                    }

                    initialYawDir =
                            (float) dotRight;
                }

                if (debugMode) {
                    double hitAngleDeg =
                            Math.toDegrees(
                                    Math.atan2(
                                            -dotRight,
                                            dotForward
                                    )
                            );

                    if (hitAngleDeg < 0.0F) {
                        hitAngleDeg += 360.0F;
                    }

                    String sectorStr =
                            isFrontShot
                                    ? "FRONT"
                                    : (isBackShot
                                    ? "BACK"
                                    : (isLeftShot
                                    ? "LEFT"
                                    : "RIGHT"));

                    String msg =
                            String.format(
                                    "§e[DEBUG] §fHit Sector: §b%s (%.0f°) §f| Slot: §d%s §f| Impulse: Pitch=§c%.2f§f, Roll=§c%.2f§f, Yaw=§c%.2f",
                                    sectorStr,
                                    hitAngleDeg,
                                    hitSlot.name(),
                                    initialPitchDir,
                                    initialRollDir,
                                    initialYawDir
                            );

                    sendDebugMessage(
                            entity,
                            event.getAttacker(),
                            msg
                    );
                }

                float ammoImmunity = -1.0F;

                if (hitIndex != null) {
                    float rawImmunity =
                            isBackShot
                                    ? hitIndex.backAmmoImmunity
                                    : hitIndex.ammoImmunity;

                    if (rawImmunity > 0.0F) {
                        ammoImmunity = rawImmunity;
                    }
                }

                float reductionCap =
                        hitIndex != null
                                ? hitIndex.damageReductionCap
                                : 0.75F;

                if (hitSlot == EquipmentSlot.CHEST
                        && hitStack.hasTag()
                        && hitStack.getTag().contains("plate_id")
                        && isFrontShot) {

                    String plateId =
                            hitStack.getTag().getString("plate_id");

                    PlateIndex plateIndex =
                            AddonPackLoader.PLATE_INDEXES.get(
                                    new ResourceLocation(plateId)
                            );

                    if (plateIndex != null) {

                        if (ammoImmunity < 0.0F) {
                            ammoImmunity = 0.0F;
                        }

                        ammoImmunity +=
                                (float) plateIndex.ammoImmunity;

                        reductionCap +=
                                (float) plateIndex.damageReductionCap;
                    }
                }

                if (ammoImmunity > 1.0F) {
                    ammoImmunity = 1.0F;
                }

                if (reductionCap > 1.0F) {
                    reductionCap = 1.0F;
                }

                if (ammoImmunity >= 0.0F) {

                    float armorIgnore = 0.0F;

                    if (bulletArmorIgnoreField != null
                            && bullet instanceof EntityKineticBullet) {

                        try {
                            armorIgnore =
                                    bulletArmorIgnoreField.getFloat(
                                            bullet
                                    );
                        } catch (Exception ignored) {
                        }

                    } else {

                        Optional<CommonGunIndex> gunIndex =
                                TimelessAPI.getCommonGunIndex(
                                        event.getGunId()
                                );

                        if (gunIndex.isPresent()) {

                            ExtraDamage extraDamage =
                                    gunIndex.get()
                                            .getGunData()
                                            .getBulletData()
                                            .getExtraDamage();

                            if (extraDamage != null) {
                                armorIgnore =
                                        extraDamage.getArmorIgnore();
                            }
                        }
                    }

                    if (bullet != null) {

                        if (ammoImmunity >= armorIgnore) {

                            event.setBaseAmount(0.0F);

                            if (debugMode) {
                                String msg =
                                        String.format(
                                                "§e[DEBUG] §fShot %s at §d%s §cDEFLECTED §fby §b%.2f §fimmunity (Ignore: §b%.2f§f)",
                                                isBackShot
                                                        ? "§c[BACK]§f"
                                                        : "§a[FRONT/SIDE]§f",
                                                hitSlot.name(),
                                                ammoImmunity,
                                                armorIgnore
                                        );

                                sendDebugMessage(
                                        entity,
                                        event.getAttacker(),
                                        msg
                                );
                            }

                            if (bulletPierceField != null
                                    && bullet instanceof EntityKineticBullet) {

                                try {
                                    bulletPierceField.set(
                                            bullet,
                                            0
                                    );
                                } catch (IllegalAccessException ignored) {
                                }
                            }

                            final Entity finalBullet =
                                    bullet;

                            NetworkHandler.CHANNEL.send(
                                    PacketDistributor.TRACKING_ENTITY.with(
                                            () -> finalBullet
                                    ),
                                    new S2CStopBulletPacket(
                                            finalBullet.getId(),
                                            hitPos.x,
                                            hitPos.y,
                                            hitPos.z
                                    )
                            );

                        } else {

                            float reduction = 0.0F;

                            if (armorIgnore > 0.0F) {
                                reduction =
                                        Math.min(
                                                1.0F,
                                                Math.max(
                                                        0.0F,
                                                        reductionCap
                                                                * (
                                                                ammoImmunity
                                                                        / armorIgnore
                                                        )
                                                )
                                        );
                            }

                            float multiplier =
                                    effectiveHeadShot
                                            ? event.getHeadshotMultiplier()
                                            : 1.0F;

                            float modReducedDamage =
                                    event.getBaseAmount()
                                            * multiplier
                                            * (1.0F - reduction);

                            float armorValue =
                                    entity.getArmorValue();

                            float toughness =
                                    entity.getAttribute(
                                            Attributes.ARMOR_TOUGHNESS
                                    ) != null
                                            ? (float) entity.getAttributeValue(
                                            Attributes.ARMOR_TOUGHNESS
                                    )
                                            : 0.0F;

                            float finalDamage =
                                    CombatRules.getDamageAfterAbsorb(
                                            modReducedDamage,
                                            armorValue,
                                            toughness
                                    );

                            if (debugMode) {

                                String type =
                                        effectiveHeadShot
                                                ? "§cHEADSHOT§f"
                                                : "§bBODY§f";

                                String side =
                                        isBackShot
                                                ? "§c[BACK]§f"
                                                : "§a[FRONT/SIDE]§f";

                                String msg =
                                        String.format(
                                                "§e[DEBUG] §f%s %s at §d%s | Hit: §b%.2f §fbase (x§b%.1f§f) | §b%.2f §fignore vs §b%.2f §fimmunity | Mod Reduc: §a%.1f%% §f| Vanilla Arm: §d%.0f §f| Final Dmg (Pre-Ench): §c%.2f",
                                                type,
                                                side,
                                                hitSlot.name(),
                                                event.getBaseAmount(),
                                                multiplier,
                                                armorIgnore,
                                                ammoImmunity,
                                                reduction * 100.0F,
                                                armorValue,
                                                finalDamage
                                        );

                                sendDebugMessage(
                                        entity,
                                        event.getAttacker(),
                                        msg
                                );
                            }

                            event.setBaseAmount(0.0F);

                            if (modReducedDamage > 0.0F) {

                                boolean entityInvulnerable =
                                        entity.isInvulnerable();

                                boolean playerAbilityInvulnerable =
                                        entity instanceof Player
                                                && ((Player) entity)
                                                .getAbilities()
                                                .invulnerable;

                                boolean spawnProtection =
                                        false;

                                if (!entityInvulnerable
                                        && !playerAbilityInvulnerable
                                        && entity instanceof Player) {

                                    Player p =
                                            (Player) entity;

                                    Level level =
                                            entity.level();

                                    if (level instanceof ServerLevel serverLevel) {

                                        int spawnRadius =
                                                serverLevel
                                                        .getServer()
                                                        .getSpawnRadius(
                                                                serverLevel
                                                        );

                                        if (spawnRadius > 0) {

                                            BlockPos spawnPos =
                                                    serverLevel
                                                            .getSharedSpawnPos();

                                            BlockPos playerPos =
                                                    p.blockPosition();

                                            int dx =
                                                    Math.abs(
                                                            playerPos.getX()
                                                                    - spawnPos.getX()
                                                    );

                                            int dz =
                                                    Math.abs(
                                                            playerPos.getZ()
                                                                    - spawnPos.getZ()
                                                    );

                                            if (Math.max(dx, dz)
                                                    <= spawnRadius) {

                                                spawnProtection = true;
                                            }
                                        }
                                    }
                                }

                                boolean isInvulnerable =
                                        entityInvulnerable
                                                || playerAbilityInvulnerable
                                                || spawnProtection;

                                if (debugMode) {

                                    LOGGER.info(
                                            "[JAG-TACZ-DIAG] MANUAL HURT CHECK | victim={} | entityInvulnerable={} | playerAbilityInvulnerable={} | spawnProtection={} | invulnerableTime={} | damage={} | ammoImmunity={} | armorIgnore={}",
                                            entity.getName().getString(),
                                            entityInvulnerable,
                                            playerAbilityInvulnerable,
                                            spawnProtection,
                                            entity.invulnerableTime,
                                            modReducedDamage,
                                            ammoImmunity,
                                            armorIgnore
                                    );
                                }

                                if (!isInvulnerable) {

                                    DamageSource source =
                                            event.getDamageSource(
                                                    GunDamageSourcePart.NON_ARMOR_PIERCING
                                            );

                                    if (source == null) {
                                        source =
                                                entity.damageSources()
                                                        .generic();
                                    }

                                    boolean tempDisabled =
                                            false;

                                    ItemStack chest =
                                            entity.getItemBySlot(
                                                    EquipmentSlot.CHEST
                                            );

                                    if (!isFrontShot
                                            && !chest.isEmpty()
                                            && chest.hasTag()
                                            && chest.getTag().contains(
                                            "plate_id"
                                    )) {

                                        chest.getTag().putBoolean(
                                                "temp_disable_plate",
                                                true
                                        );

                                        entity.setItemSlot(
                                                EquipmentSlot.CHEST,
                                                chest
                                        );

                                        tempDisabled = true;
                                    }

                                    try {

                                        entity.invulnerableTime = 0;

                                        if (debugMode) {
                                            LOGGER.info(
                                                    "[JAG-TACZ-DIAG] BEFORE MANUAL HURT | victim={} | invulnerableTime={} | damage={}",
                                                    entity.getName().getString(),
                                                    entity.invulnerableTime,
                                                    modReducedDamage
                                            );
                                        }

                                        hurtWithBulletKnockback(
                                                entity,
                                                source,
                                                modReducedDamage,
                                                bullet
                                        );

                                        if (debugMode) {
                                            LOGGER.info(
                                                    "[JAG-TACZ-DIAG] AFTER MANUAL HURT | victim={} | invulnerableTime={} | health={}",
                                                    entity.getName().getString(),
                                                    entity.invulnerableTime,
                                                    entity.getHealth()
                                            );
                                        }

                                    } finally {

                                        if (tempDisabled) {

                                            chest.getTag().remove(
                                                    "temp_disable_plate"
                                            );

                                            entity.setItemSlot(
                                                    EquipmentSlot.CHEST,
                                                    chest
                                            );
                                        }
                                    }

                                } else if (debugMode) {

                                    LOGGER.info(
                                            "[JAG-TACZ-DIAG] MANUAL HURT SKIPPED | victim={} | reason={} | entityInvulnerable={} | playerAbilityInvulnerable={} | spawnProtection={} | invulnerableTime={}",
                                            entity.getName().getString(),
                                            entityInvulnerable
                                                    ? "ENTITY_INVULNERABLE"
                                                    : playerAbilityInvulnerable
                                                    ? "PLAYER_ABILITY"
                                                    : "SPAWN_PROTECTION",
                                            entityInvulnerable,
                                            playerAbilityInvulnerable,
                                            spawnProtection,
                                            entity.invulnerableTime
                                    );
                                }
                            }

                            if (bulletPierceField != null
                                    && bullet instanceof EntityKineticBullet) {

                                try {
                                    bulletPierceField.set(
                                            bullet,
                                            0
                                    );
                                } catch (IllegalAccessException ignored) {
                                }
                            }

                            final Entity finalBullet2 =
                                    bullet;

                            NetworkHandler.CHANNEL.send(
                                    PacketDistributor.TRACKING_ENTITY.with(
                                            () -> finalBullet2
                                    ),
                                    new S2CStopBulletPacket(
                                            finalBullet2.getId(),
                                            hitPos.x,
                                            hitPos.y,
                                            hitPos.z
                                    )
                            );
                        }

                        damageArmor(
                                entity,
                                1,
                                hitSlot,
                                isFrontShot
                        );
                    }
                }

                float armorIgnoreForShake = 0.0F;

                if (ammoImmunity >= 0.0F
                        && bulletArmorIgnoreField != null
                        && bullet instanceof EntityKineticBullet) {

                    try {
                        armorIgnoreForShake =
                                bulletArmorIgnoreField.getFloat(
                                        bullet
                                );
                    } catch (Exception ignored) {
                    }
                }

                float ignoreScale =
                        0.5F
                                + 0.5F
                                * Math.min(
                                1.0F,
                                armorIgnoreForShake
                        );

                float vignetteIntensity = 0.0F;

                int vignetteDuration =
                        ArmorConfig.DATA.vignette_duration;

                float shakeMultiplier;
                int shakeDuration;
                float shakeFrequency;
                float shakeDecay;

                if (hitIndex != null) {

                    float totalCameraShake =
                            hitIndex.cameraShakeMultiplier;

                    if (hitSlot == EquipmentSlot.CHEST
                            && hitStack.hasTag()
                            && hitStack.getTag().contains(
                            "plate_id"
                    )) {

                        String plateId =
                                hitStack.getTag().getString(
                                        "plate_id"
                                );

                        PlateIndex plateIndex =
                                AddonPackLoader.PLATE_INDEXES.get(
                                        new ResourceLocation(
                                                plateId
                                        )
                                );

                        if (plateIndex != null) {
                            totalCameraShake =
                                    (float) (
                                            totalCameraShake
                                                    * plateIndex.cameraShakeMultiplier
                                    );
                        }
                    }

                    if (armorIgnoreForShake > ammoImmunity) {

                        if (ArmorConfig.DATA.enable_penetrant_shaking) {

                            shakeMultiplier =
                                    (float) (
                                            ArmorConfig.DATA.penetrant_tilt_multiplier
                                                    * ignoreScale
                                                    * totalCameraShake
                                    );

                            shakeDuration =
                                    ArmorConfig.DATA.penetrant_tilt_duration;

                            shakeFrequency =
                                    (float)
                                            ArmorConfig.DATA.penetrant_tilt_frequency;

                            shakeDecay =
                                    (float)
                                            ArmorConfig.DATA.penetrant_tilt_decay_exponent;

                        } else {

                            shakeMultiplier = 0.0F;
                            shakeDuration = 0;
                            shakeFrequency = 0.0F;
                            shakeDecay = 0.0F;
                        }

                        if (ArmorConfig.DATA.enable_vignette) {
                            vignetteIntensity =
                                    (float)
                                            ArmorConfig.DATA.vignette_intensity
                                            * 0.7F;
                        }

                    } else if (ArmorConfig.DATA.enable_impact_shaking) {

                        shakeMultiplier =
                                (float) (
                                        ArmorConfig.DATA.impact_tilt_multiplier
                                                * ignoreScale
                                                * totalCameraShake
                                );

                        shakeDuration =
                                ArmorConfig.DATA.impact_tilt_duration;

                        shakeFrequency =
                                (float)
                                        ArmorConfig.DATA.impact_tilt_frequency;

                        shakeDecay =
                                (float)
                                        ArmorConfig.DATA.impact_tilt_decay_exponent;

                    } else {

                        shakeMultiplier = 0.0F;
                        shakeDuration = 0;
                        shakeFrequency = 0.0F;
                        shakeDecay = 0.0F;
                    }

                } else if (
                        ArmorConfig.DATA.enable_no_armor_hit_shaking
                ) {

                    float defaultSlotShake =
                            switch (hitSlot) {
                                case HEAD ->
                                        (float)
                                                ArmorConfig.DATA.default_shake_helmet;

                                case CHEST ->
                                        (float)
                                                ArmorConfig.DATA.default_shake_chestplate;

                                case LEGS ->
                                        (float)
                                                ArmorConfig.DATA.default_shake_leggings;

                                case FEET ->
                                        (float)
                                                ArmorConfig.DATA.default_shake_boots;

                                default ->
                                        1.0F;
                            };

                    shakeMultiplier =
                            (float) (
                                    ArmorConfig.DATA.hurt_tilt_multiplier
                                            * ignoreScale
                                            * defaultSlotShake
                            );

                    shakeDuration =
                            ArmorConfig.DATA.hurt_tilt_duration;

                    shakeFrequency =
                            (float)
                                    ArmorConfig.DATA.hurt_tilt_frequency;

                    shakeDecay =
                            (float)
                                    ArmorConfig.DATA.hurt_tilt_decay_exponent;

                    if (ArmorConfig.DATA.enable_vignette) {
                        vignetteIntensity =
                                (float)
                                        ArmorConfig.DATA.vignette_intensity;
                    }

                } else {

                    shakeMultiplier = 0.0F;
                    shakeDuration = 0;
                    shakeFrequency = 0.0F;
                    shakeDecay = 0.0F;
                }

                if (entity instanceof Player) {

                    Player player =
                            (Player) entity;

                    if (shakeMultiplier > 0.0F
                            || vignetteIntensity > 0.0F) {

                        NetworkHandler.CHANNEL.send(
                                PacketDistributor.PLAYER.with(
                                        () -> (ServerPlayer) player
                                ),
                                new S2CDamageTiltPacket(
                                        shakeMultiplier,
                                        shakeDuration,
                                        shakeFrequency,
                                        shakeDecay,
                                        vignetteIntensity,
                                        vignetteDuration,
                                        initialPitchDir,
                                        initialRollDir,
                                        initialYawDir
                                )
                        );
                    }
                }

                if (hitIndex != null) {

                    if (ArmorConfig.DATA.enable_impact_sound) {

                        int soundIndex;

                        do {
                            soundIndex =
                                    1
                                            + (int) (
                                            Math.random()
                                                    * 5.0F
                                    );
                        } while (
                                soundIndex == lastSoundIndex
                        );

                        lastSoundIndex = soundIndex;

                        SoundEvent hitSound =
                                switch (soundIndex) {
                                    case 1 ->
                                            SoundRegistry.ARMOR_HIT_1.get();
                                    case 2 ->
                                            SoundRegistry.ARMOR_HIT_2.get();
                                    case 3 ->
                                            SoundRegistry.ARMOR_HIT_3.get();
                                    case 4 ->
                                            SoundRegistry.ARMOR_HIT_4.get();
                                    default ->
                                            SoundRegistry.ARMOR_HIT_5.get();
                                };

                        float randomPitch =
                                0.9F
                                        + (float)
                                        Math.random()
                                        * 0.2F;

                        entity.level().playSound(
                                null,
                                hitPos.x,
                                hitPos.y,
                                hitPos.z,
                                hitSound,
                                SoundSource.NEUTRAL,
                                1.0F,
                                randomPitch
                        );

                        if (event.getAttacker()
                                instanceof ServerPlayer shooter) {

                            double dist =
                                    shooter.distanceTo(
                                            entity
                                    );

                            if (dist > 16.0F) {

                                float shooterVolume =
                                        (float)
                                                Math.max(
                                                        0.0F,
                                                        1.0F
                                                                - dist
                                                                / 48.0F
                                                );

                                if (shooterVolume > 0.01F) {
                                    shooter.playSound(
                                            hitSound,
                                            shooterVolume,
                                            randomPitch
                                    );
                                }
                            }
                        }
                    }

                    if (armorIgnoreForShake > ammoImmunity
                            && ArmorConfig.DATA.enable_hurt_sound) {

                        int hurtIndex;

                        do {
                            hurtIndex =
                                    1
                                            + (int) (
                                            Math.random()
                                                    * 6.0F
                                    );
                        } while (
                                hurtIndex == lastHurtSoundIndex
                        );

                        lastHurtSoundIndex = hurtIndex;

                        SoundEvent hurtSound =
                                switch (hurtIndex) {
                                    case 1 ->
                                            SoundRegistry.HURT_1.get();
                                    case 2 ->
                                            SoundRegistry.HURT_2.get();
                                    case 3 ->
                                            SoundRegistry.HURT_3.get();
                                    case 4 ->
                                            SoundRegistry.HURT_4.get();
                                    case 5 ->
                                            SoundRegistry.HURT_5.get();
                                    default ->
                                            SoundRegistry.HURT_6.get();
                                };

                        float randomPitch =
                                0.9F
                                        + (float)
                                        Math.random()
                                        * 0.2F;

                        entity.level().playSound(
                                null,
                                hitPos.x,
                                hitPos.y,
                                hitPos.z,
                                hurtSound,
                                SoundSource.NEUTRAL,
                                0.375F,
                                randomPitch
                        );

                        if (event.getAttacker()
                                instanceof ServerPlayer shooter) {

                            double dist =
                                    shooter.distanceTo(
                                            entity
                                    );

                            if (dist > 12.0F) {

                                float shooterVolume =
                                        0.3F
                                                * (float)
                                                Math.max(
                                                        0.0F,
                                                        1.0F
                                                                - dist
                                                                / 32.0F
                                                );

                                if (shooterVolume > 0.01F) {
                                    shooter.playSound(
                                            hurtSound,
                                            shooterVolume,
                                            randomPitch
                                    );
                                }
                            }
                        }
                    }

                    if (entity.level()
                            instanceof ServerLevel serverLevel) {

                        Entity attacker =
                                event.getAttacker();

                        double dirX;
                        double dirZ;

                        if (attacker != null) {

                            double dx_shot =
                                    attacker.getX()
                                            - entity.getX();

                            double dz_shot =
                                    attacker.getZ()
                                            - entity.getZ();

                            double dist =
                                    Math.sqrt(
                                            dx_shot
                                                    * dx_shot
                                                    + dz_shot
                                                    * dz_shot
                                    );

                            dirX =
                                    dx_shot
                                            / (
                                            dist > 0.0F
                                                    ? dist
                                                    : 1.0F
                                    );

                            dirZ =
                                    dz_shot
                                            / (
                                            dist > 0.0F
                                                    ? dist
                                                    : 1.0F
                                    );

                        } else {

                            double angle =
                                    Math.random()
                                            * Math.PI
                                            * 2.0F;

                            dirX =
                                    Math.cos(angle);

                            dirZ =
                                    Math.sin(angle);
                        }

                        double randomAngle =
                                (Math.random() - 0.5F)
                                        * (Math.PI / 2D);

                        double finalDirX =
                                dirX
                                        * Math.cos(randomAngle)
                                        - dirZ
                                        * Math.sin(randomAngle);

                        double finalDirZ =
                                dirX
                                        * Math.sin(randomAngle)
                                        + dirZ
                                        * Math.cos(randomAngle);

                        double dx =
                                finalDirX * 0.4;

                        double dz =
                                finalDirZ * 0.4;

                        double horizontalSpeed =
                                0.2
                                        + Math.random()
                                        * 0.4;

                        double vx =
                                finalDirX
                                        * horizontalSpeed;

                        double vy =
                                0.2
                                        + Math.random()
                                        * 0.2;

                        double vz =
                                finalDirZ
                                        * horizontalSpeed;

                        serverLevel.sendParticles(
                                ParticleRegistry.IMPACT_SPARK.get(),
                                hitPos.x + dx,
                                hitPos.y,
                                hitPos.z + dz,
                                0,
                                vx,
                                vy,
                                vz,
                                1.0F
                        );

                        if (armorIgnoreForShake > ammoImmunity) {
                            spawnBloodParticles(
                                    serverLevel,
                                    hitPos,
                                    vx,
                                    vy,
                                    vz
                            );
                        }
                    }

                } else {

                    if (ArmorConfig.DATA.enable_no_armor_hit_shaking
                            && ArmorConfig.DATA.enable_hurt_sound) {

                        int soundIndex;

                        do {
                            soundIndex =
                                    1
                                            + (int) (
                                            Math.random()
                                                    * 6.0F
                                    );
                        } while (
                                soundIndex == lastHurtSoundIndex
                        );

                        lastHurtSoundIndex =
                                soundIndex;

                        SoundEvent hurtSound =
                                switch (soundIndex) {
                                    case 1 ->
                                            SoundRegistry.HURT_1.get();
                                    case 2 ->
                                            SoundRegistry.HURT_2.get();
                                    case 3 ->
                                            SoundRegistry.HURT_3.get();
                                    case 4 ->
                                            SoundRegistry.HURT_4.get();
                                    case 5 ->
                                            SoundRegistry.HURT_5.get();
                                    default ->
                                            SoundRegistry.HURT_6.get();
                                };

                        float randomPitch =
                                0.9F
                                        + (float)
                                        Math.random()
                                        * 0.2F;

                        entity.level().playSound(
                                null,
                                hitPos.x,
                                hitPos.y,
                                hitPos.z,
                                hurtSound,
                                SoundSource.NEUTRAL,
                                0.375F,
                                randomPitch
                        );

                        if (event.getAttacker()
                                instanceof ServerPlayer shooter) {

                            double dist =
                                    shooter.distanceTo(
                                            entity
                                    );

                            if (dist > 12.0F) {

                                float shooterVolume =
                                        0.3F
                                                * (float)
                                                Math.max(
                                                        0.0F,
                                                        1.0F
                                                                - dist
                                                                / 32.0F
                                                );

                                if (shooterVolume > 0.01F) {
                                    shooter.playSound(
                                            hurtSound,
                                            shooterVolume,
                                            randomPitch
                                    );
                                }
                            }
                        }
                    }

                    if (entity.level()
                            instanceof ServerLevel serverLevel) {

                        Entity attacker =
                                event.getAttacker();

                        double dirX;
                        double dirZ;

                        if (attacker != null) {

                            double dx_shot =
                                    attacker.getX()
                                            - entity.getX();

                            double dz_shot =
                                    attacker.getZ()
                                            - entity.getZ();

                            double dist =
                                    Math.sqrt(
                                            dx_shot
                                                    * dx_shot
                                                    + dz_shot
                                                    * dz_shot
                                    );

                            dirX =
                                    dx_shot
                                            / (
                                            dist > 0.0F
                                                    ? dist
                                                    : 1.0F
                                    );

                            dirZ =
                                    dz_shot
                                            / (
                                            dist > 0.0F
                                                    ? dist
                                                    : 1.0F
                                    );

                        } else {

                            double angle =
                                    Math.random()
                                            * Math.PI
                                            * 2.0F;

                            dirX =
                                    Math.cos(angle);

                            dirZ =
                                    Math.sin(angle);
                        }

                        double randomAngle =
                                (Math.random() - 0.5F)
                                        * (Math.PI / 2D);

                        double finalDirX =
                                dirX
                                        * Math.cos(randomAngle)
                                        - dirZ
                                        * Math.sin(randomAngle);

                        double finalDirZ =
                                dirX
                                        * Math.sin(randomAngle)
                                        + dirZ
                                        * Math.cos(randomAngle);

                        double dx =
                                finalDirX * 0.4;

                        double dz =
                                finalDirZ * 0.4;

                        double horizontalSpeed =
                                0.2
                                        + Math.random()
                                        * 0.4;

                        double vx =
                                finalDirX
                                        * horizontalSpeed;

                        double vy =
                                0.2
                                        + Math.random()
                                        * 0.2;

                        double vz =
                                finalDirZ
                                        * horizontalSpeed;

                        spawnBloodParticles(
                                serverLevel,
                                hitPos,
                                vx,
                                vy,
                                vz
                        );
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        LivingEntity entity =
                event.getEntity();

        if (entity != null) {

            if (event.getSource().is(
                    DamageTypeTags.IS_PROJECTILE
            )) {

                Entity directEntity =
                        event.getSource()
                                .getDirectEntity();

                if (directEntity
                        instanceof EntityKineticBullet) {
                    return;
                }

                EquipmentSlot hitSlot;

                if (directEntity != null) {
                    hitSlot =
                            getProjectileHitSlot(
                                    entity,
                                    directEntity
                            );
                } else {
                    hitSlot =
                            EquipmentSlot.CHEST;
                }

                boolean isFrontShot = false;

                if (directEntity != null) {

                    Vec3 dir =
                            directEntity
                                    .getDeltaMovement()
                                    .normalize();

                    if (dir.length() < 0.01
                            && event.getSource().getEntity() != null) {

                        dir =
                                entity.position()
                                        .add(
                                                0.0D,
                                                entity.getBbHeight() / 2.0F,
                                                0.0D
                                        )
                                        .subtract(
                                                event.getSource()
                                                        .getEntity()
                                                        .position()
                                        )
                                        .normalize();
                    }

                    if (dir.length() > 0.01) {

                        Vec3 victimLook =
                                entity.getLookAngle();

                        Vec3 victimLook2D =
                                new Vec3(
                                        victimLook.x,
                                        0.0D,
                                        victimLook.z
                                ).normalize();

                        Vec3 bulletDir2D =
                                new Vec3(
                                        dir.x,
                                        0.0D,
                                        dir.z
                                ).normalize();

                        isFrontShot =
                                victimLook2D.dot(
                                        bulletDir2D
                                ) < -0.707;
                    }

                } else if (
                        event.getSource().getEntity() != null
                ) {

                    Vec3 victimLook =
                            entity.getLookAngle();

                    Vec3 victimLook2D =
                            new Vec3(
                                    victimLook.x,
                                    0.0D,
                                    victimLook.z
                            ).normalize();

                    Vec3 attackDir =
                            entity.position()
                                    .subtract(
                                            event.getSource()
                                                    .getEntity()
                                                    .position()
                                    )
                                    .normalize();

                    Vec3 attackDir2D =
                            new Vec3(
                                    attackDir.x,
                                    0.0D,
                                    attackDir.z
                            ).normalize();

                    isFrontShot =
                            victimLook2D.dot(
                                    attackDir2D
                            ) < -0.707;
                }

                ItemStack armorStack =
                        entity.getItemBySlot(
                                hitSlot
                        );

                ArmorIndex index =
                        CustomGeoArmorItem.getIndex(
                                armorStack
                        );

                boolean blockVanilla =
                        index != null
                                && index.blockVanillaProjectile;

                if (hitSlot == EquipmentSlot.CHEST
                        && armorStack.hasTag()
                        && armorStack.getTag().contains(
                        "plate_id"
                )) {

                    String plateId =
                            armorStack.getTag().getString(
                                    "plate_id"
                            );

                    PlateIndex plateIndex =
                            AddonPackLoader.PLATE_INDEXES.get(
                                    new ResourceLocation(
                                            plateId
                                    )
                            );

                    if (plateIndex != null
                            && plateIndex.blockVanillaProjectile
                            && isFrontShot) {

                        blockVanilla = true;
                    }
                }

                if (blockVanilla) {

                    boolean isProtected =
                            entity.isInvulnerableTo(
                                    event.getSource()
                            );

                    if (!isProtected
                            && entity instanceof Player) {

                        Player p =
                                (Player) entity;

                        if (entity.level()
                                instanceof ServerLevel serverLevel) {

                            int spawnRadius =
                                    serverLevel
                                            .getServer()
                                            .getSpawnRadius(
                                                    serverLevel
                                            );

                            if (spawnRadius > 0) {

                                BlockPos spawnPos =
                                        serverLevel
                                                .getSharedSpawnPos();

                                BlockPos playerPos =
                                        p.blockPosition();

                                int dx =
                                        Math.abs(
                                                playerPos.getX()
                                                        - spawnPos.getX()
                                        );

                                int dz =
                                        Math.abs(
                                                playerPos.getZ()
                                                        - spawnPos.getZ()
                                        );

                                if (Math.max(dx, dz)
                                        <= spawnRadius) {

                                    isProtected = true;
                                }
                            }
                        }
                    }

                    if (isProtected) {
                        return;
                    }

                    event.setCanceled(true);

                    Vec3 soundPos =
                            directEntity != null
                                    ? directEntity.position()
                                    : entity.position()
                                    .add(
                                            0.0D,
                                            entity.getBbHeight() / 2.0D,
                                            0.0D
                                    );

                    entity.level().playSound(
                            null,
                            soundPos.x,
                            soundPos.y,
                            soundPos.z,
                            SoundEvents.ARROW_HIT,
                            entity instanceof Player
                                    ? SoundSource.PLAYERS
                                    : SoundSource.NEUTRAL,
                            1.0F,
                            0.8F
                                    + entity.getRandom()
                                    .nextFloat()
                                    * 0.4F
                    );

                    damageArmor(
                            entity,
                            1,
                            hitSlot,
                            isFrontShot
                    );
                }
            }
        }
    }

    private static EquipmentSlot getProjectileHitSlot(
            LivingEntity entity,
            Entity projectile
    ) {

        Vec3 dir =
                projectile.getDeltaMovement()
                        .normalize();

        if (dir.length() < 0.01
                && projectile instanceof Projectile proj) {

            if (proj.getOwner() != null) {

                dir =
                        entity.position()
                                .add(
                                        0.0D,
                                        entity.getBbHeight() / 2.0F,
                                        0.0D
                                )
                                .subtract(
                                        proj.getOwner()
                                                .position()
                                )
                                .normalize();
            }
        }

        if (dir.length() < 0.01) {

            Vec3 center =
                    entity.getBoundingBox()
                            .getCenter();

            Vec3 diff =
                    center.subtract(
                            projectile.position()
                    );

            if (diff.length() > 0.001) {
                dir = diff.normalize();
            } else {
                dir =
                        new Vec3(
                                0.0F,
                                -1.0F,
                                0.0F
                        );
            }
        }

        Vec3 start =
                projectile.position()
                        .subtract(
                                dir.scale(10.0F)
                        );

        Vec3 end =
                projectile.position()
                        .add(
                                dir.scale(2.0F)
                        );

        Vec3 hitPos =
                entity.getBoundingBox()
                        .clip(start, end)
                        .orElse(null);

        if (hitPos == null) {

            hitPos =
                    new Vec3(
                            entity.getX(),
                            entity.getY()
                                    + entity.getBbHeight()
                                    * 0.6,
                            entity.getZ()
                    );
        }

        EquipmentSlot hitSlot = null;

        if (entity instanceof Player player) {
            hitSlot =
                    getFirstAidHitSlot(
                            projectile,
                            player
                    );
        }

        if (hitSlot == null) {
            hitSlot =
                    getHitSlot(
                            entity,
                            hitPos,
                            false
                    );
        }

        return hitSlot;
    }

    @SubscribeEvent
    public static void onEquipmentChange(
            LivingEquipmentChangeEvent event
    ) {

        LivingEntity entity =
                event.getEntity();

        if (entity != null) {

            AttributeInstance speedAttr =
                    entity.getAttribute(
                            Attributes.MOVEMENT_SPEED
                    );

            if (speedAttr != null) {

                double totalSpeedModify = 0.0F;

                ItemStack head =
                        entity.getItemBySlot(
                                EquipmentSlot.HEAD
                        );

                ItemStack chest =
                        entity.getItemBySlot(
                                EquipmentSlot.CHEST
                        );

                ItemStack legs =
                        entity.getItemBySlot(
                                EquipmentSlot.LEGS
                        );

                ItemStack feet =
                        entity.getItemBySlot(
                                EquipmentSlot.FEET
                        );

                ArmorIndex headIndex =
                        CustomGeoArmorItem.getIndex(
                                head
                        );

                if (headIndex != null) {
                    totalSpeedModify +=
                            headIndex.speedModify;
                }

                ArmorIndex chestIndex =
                        CustomGeoArmorItem.getIndex(
                                chest
                        );

                if (chestIndex != null) {
                    totalSpeedModify +=
                            chestIndex.speedModify;
                }

                if (chest.hasTag()
                        && chest.getTag().contains(
                        "plate_id"
                )) {

                    String plateId =
                            chest.getTag().getString(
                                    "plate_id"
                            );

                    PlateIndex plateIndex =
                            AddonPackLoader.PLATE_INDEXES.get(
                                    new ResourceLocation(
                                            plateId
                                    )
                            );

                    if (plateIndex != null) {
                        totalSpeedModify +=
                                plateIndex.speedModify;
                    }
                }

                ArmorIndex legsIndex =
                        CustomGeoArmorItem.getIndex(
                                legs
                        );

                if (legsIndex != null) {
                    totalSpeedModify +=
                            legsIndex.speedModify;
                }

                ArmorIndex feetIndex =
                        CustomGeoArmorItem.getIndex(
                                feet
                        );

                if (feetIndex != null) {
                    totalSpeedModify +=
                            feetIndex.speedModify;
                }

                speedAttr.removeModifier(
                        SPEED_MODIFIER_UUID
                );

                if (Math.abs(totalSpeedModify)
                        > 0.001) {

                    speedAttr.addTransientModifier(
                            new AttributeModifier(
                                    SPEED_MODIFIER_UUID,
                                    "Armor speed penalty",
                                    totalSpeedModify,
                                    Operation.MULTIPLY_TOTAL
                            )
                    );
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingJump(
            LivingEvent.LivingJumpEvent event
    ) {

        LivingEntity entity =
                event.getEntity();

        if (entity != null) {

            double totalJumpModify = 0.0F;

            ItemStack head =
                    entity.getItemBySlot(
                            EquipmentSlot.HEAD
                    );

            ItemStack chest =
                    entity.getItemBySlot(
                            EquipmentSlot.CHEST
                    );

            ItemStack legs =
                    entity.getItemBySlot(
                            EquipmentSlot.LEGS
                    );

            ItemStack feet =
                    entity.getItemBySlot(
                            EquipmentSlot.FEET
                    );

            ArmorIndex headIndex =
                    CustomGeoArmorItem.getIndex(
                            head
                    );

            if (headIndex != null) {
                totalJumpModify +=
                        headIndex.jumpModify;
            }

            ArmorIndex chestIndex =
                    CustomGeoArmorItem.getIndex(
                            chest
                    );

            if (chestIndex != null) {
                totalJumpModify +=
                        chestIndex.jumpModify;
            }

            if (chest.hasTag()
                    && chest.getTag().contains(
                    "plate_id"
            )) {

                String plateId =
                        chest.getTag().getString(
                                "plate_id"
                        );

                PlateIndex plateIndex =
                        AddonPackLoader.PLATE_INDEXES.get(
                                new ResourceLocation(
                                        plateId
                                )
                        );

                if (plateIndex != null) {
                    totalJumpModify +=
                            plateIndex.jumpModify;
                }
            }

            ArmorIndex legsIndex =
                    CustomGeoArmorItem.getIndex(
                            legs
                    );

            if (legsIndex != null) {
                totalJumpModify +=
                        legsIndex.jumpModify;
            }

            ArmorIndex feetIndex =
                    CustomGeoArmorItem.getIndex(
                            feet
                    );

            if (feetIndex != null) {
                totalJumpModify +=
                        feetIndex.jumpModify;
            }

            if (Math.abs(totalJumpModify)
                    > 0.001) {

                Vec3 delta =
                        entity.getDeltaMovement();

                entity.setDeltaMovement(
                        delta.x,
                        delta.y
                                * (1.0F + totalJumpModify),
                        delta.z
                );
            }
        }
    }

    public static ArmorSetIndex getMatchingFullSet(
            LivingEntity entity
    ) {

        ItemStack head =
                entity.getItemBySlot(
                        EquipmentSlot.HEAD
                );

        ItemStack chest =
                entity.getItemBySlot(
                        EquipmentSlot.CHEST
                );

        ItemStack legs =
                entity.getItemBySlot(
                        EquipmentSlot.LEGS
                );

        ItemStack feet =
                entity.getItemBySlot(
                        EquipmentSlot.FEET
                );

        String id1 =
                getArmorId(head);

        return id1 != null
                && id1.equals(getArmorId(chest))
                && id1.equals(getArmorId(legs))
                && id1.equals(getArmorId(feet))
                ? CustomGeoArmorItem.getSetIndex(head)
                : null;
    }

    private static float getAmmoImmunity(
            LivingEntity entity,
            EquipmentSlot slot
    ) {

        ItemStack item =
                entity.getItemBySlot(slot);

        ArmorIndex index =
                CustomGeoArmorItem.getIndex(item);

        return index != null
                ? index.ammoImmunity
                : -1.0F;
    }

    public static boolean hasAmmoImmunityEquipped(
            LivingEntity entity
    ) {

        return getAmmoImmunity(
                entity,
                EquipmentSlot.HEAD
        ) >= 0.0F
                || getAmmoImmunity(
                entity,
                EquipmentSlot.CHEST
        ) >= 0.0F
                || getAmmoImmunity(
                entity,
                EquipmentSlot.LEGS
        ) >= 0.0F
                || getAmmoImmunity(
                entity,
                EquipmentSlot.FEET
        ) >= 0.0F;
    }

    private static String getArmorId(
            ItemStack stack
    ) {

        if (stack.getItem()
                instanceof CustomGeoArmorItem) {

            return stack.hasTag()
                    && stack.getTag().contains(
                    "armor_id"
            )
                    ? stack.getTag().getString(
                    "armor_id"
            )
                    : "jag_default_armor:tactical_armor";

        } else {
            return null;
        }
    }

    private static EquipmentSlot getHitSlot(
            LivingEntity entity,
            Vec3 hitPos,
            boolean isHeadShot
    ) {

        if (isHeadShot) {
            return EquipmentSlot.HEAD;

        } else if (
                entity.getPose()
                        == Pose.SWIMMING
        ) {

            return EquipmentSlot.CHEST;

        } else if (hitPos == null) {

            return EquipmentSlot.CHEST;

        } else {

            double hitY =
                    hitPos.y;

            double entityY =
                    entity.getY();

            double height =
                    entity.getBbHeight();

            if (height <= 0.0F) {
                return EquipmentSlot.CHEST;
            }

            double ratio =
                    (hitY - entityY)
                            / height;

            if (ratio >= 0.83) {

                return EquipmentSlot.HEAD;

            } else if (ratio >= 0.42) {

                return EquipmentSlot.CHEST;

            } else {

                return ratio >= 0.22
                        ? EquipmentSlot.LEGS
                        : EquipmentSlot.FEET;
            }
        }
    }

    public static void damagePlate(
            LivingEntity entity,
            ItemStack chestplate,
            int amount
    ) {

        if (!chestplate.isEmpty()
                && chestplate.hasTag()
                && chestplate.getTag().contains(
                "plate_durability"
        )) {

            int plateDur =
                    chestplate.getTag().getInt(
                            "plate_durability"
                    );

            if (chestplate.getTag().contains(
                    "plate_enchantments"
            )) {

                ResourceLocation plateResourceId =
                        chestplate.hasTag()
                                && chestplate.getTag().contains("plate_id")
                                ? new ResourceLocation(
                                chestplate.getTag().getString("plate_id")
                        )
                                : null;

                if (plateResourceId == null) {
                    return;
                }

                Item plateItem =
                        ItemRegistry.getPlateItem(
                                plateResourceId
                        );

                if (plateItem == null) {
                    return;
                }

                ItemStack tempPlateStack =
                        new ItemStack(plateItem);

                tempPlateStack.getOrCreateTag().put(
                        "Enchantments",
                        chestplate.getTag()
                                .getCompound(
                                        "plate_enchantments"
                                )
                );

                int unbreakingLvl =
                        EnchantmentHelper.getTagEnchantmentLevel(
                                Enchantments.UNBREAKING,
                                tempPlateStack
                        );

                if (unbreakingLvl > 0) {

                    double chanceToTakeDamage =
                            0.6
                                    + 0.4
                                    / (unbreakingLvl + 1);

                    if (entity.getRandom().nextDouble()
                            > chanceToTakeDamage) {

                        return;
                    }
                }
            }

            plateDur -= amount;

            if (plateDur <= 0) {

                chestplate.getTag().remove(
                        "plate_id"
                );

                chestplate.getTag().remove(
                        "plate_durability"
                );

                chestplate.getTag().remove(
                        "plate_enchantments"
                );

                entity.level().playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.ITEM_BREAK,
                        SoundSource.PLAYERS,
                        1.0F,
                        0.8F
                                + entity.getRandom()
                                .nextFloat()
                                * 0.4F
                );

                if (entity instanceof Player player) {

                    player.displayClientMessage(
                            Component.literal(
                                    "§cYour armor plate was destroyed!"
                            ),
                            true
                    );
                }

            } else {

                chestplate.getTag().putInt(
                        "plate_durability",
                        plateDur
                );
            }

            entity.setItemSlot(
                    EquipmentSlot.CHEST,
                    chestplate
            );
        }
    }

    @SubscribeEvent
    public static void onItemAttributeModifiers(
            ItemAttributeModifierEvent event
    ) {

        if (event.getSlotType()
                == EquipmentSlot.CHEST) {

            ItemStack stack =
                    event.getItemStack();

            if (stack.getItem()
                    instanceof CustomGeoArmorItem
                    && stack.hasTag()
                    && stack.getTag().contains(
                    "plate_id"
            )
                    && !stack.getTag().getBoolean(
                    "temp_disable_plate"
            )) {

                String plateId =
                        stack.getTag().getString(
                                "plate_id"
                        );

                PlateIndex plateIndex =
                        AddonPackLoader.PLATE_INDEXES.get(
                                new ResourceLocation(
                                        plateId
                                )
                        );

                if (plateIndex != null) {

                    if (plateIndex.defense > 0) {

                        event.addModifier(
                                Attributes.ARMOR,
                                new AttributeModifier(
                                        PLATE_ARMOR_UUID,
                                        "Plate armor modifier",
                                        plateIndex.defense,
                                        Operation.ADDITION
                                )
                        );
                    }

                    if (plateIndex.toughness > 0) {

                        event.addModifier(
                                Attributes.ARMOR_TOUGHNESS,
                                new AttributeModifier(
                                        PLATE_TOUGHNESS_UUID,
                                        "Plate toughness modifier",
                                        plateIndex.toughness,
                                        Operation.ADDITION
                                )
                        );
                    }

                    if (plateIndex.knockbackResistance > 0.0F) {

                        event.addModifier(
                                Attributes.KNOCKBACK_RESISTANCE,
                                new AttributeModifier(
                                        PLATE_KNOCKBACK_UUID,
                                        "Plate knockback modifier",
                                        plateIndex.knockbackResistance,
                                        Operation.ADDITION
                                )
                        );
                    }
                }
            }
        }
    }

    private static void damageArmor(
            LivingEntity entity,
            int amount,
            EquipmentSlot slot,
            boolean isFrontShot
    ) {

        if (entity instanceof Player player) {

            if (player.getAbilities().invulnerable) {
                return;
            }
        }

        if (entity.getPose() == Pose.SWIMMING
                && slot != EquipmentSlot.HEAD) {

            int rand =
                    entity.getRandom().nextInt(3);

            slot =
                    switch (rand) {
                        case 0 ->
                                EquipmentSlot.CHEST;
                        case 1 ->
                                EquipmentSlot.LEGS;
                        default ->
                                EquipmentSlot.FEET;
                    };
        }

        EquipmentSlot finalSlot =
                slot;

        ItemStack item =
                entity.getItemBySlot(slot);

        if (!item.isEmpty()) {

            if (slot == EquipmentSlot.CHEST
                    && isFrontShot
                    && item.hasTag()
                    && item.getTag().contains(
                    "plate_durability"
            )) {

                damagePlate(
                        entity,
                        item,
                        amount
                );
            }

            try {

                BYPASS_ARMOR_PROTECTION.set(
                        true
                );

                item.hurtAndBreak(
                        amount,
                        entity,
                        (e) ->
                                e.setItemSlot(
                                        finalSlot,
                                        item
                                )
                );

            } finally {

                BYPASS_ARMOR_PROTECTION.set(
                        false
                );
            }
        }
    }

    private static void damageArmor(
            LivingEntity entity,
            int amount,
            EquipmentSlot slot
    ) {

        damageArmor(
                entity,
                amount,
                slot,
                false
        );
    }

    private static void hurtWithBulletKnockback(
            LivingEntity entity,
            DamageSource source,
            float amount,
            Entity bullet
    ) {

        float bulletKnockback = 0.0F;

        if (bulletKnockbackField != null
                && bullet instanceof EntityKineticBullet) {

            try {

                bulletKnockback =
                        bulletKnockbackField.getFloat(
                                bullet
                        );

            } catch (Exception ignored) {
            }
        }

        KnockBackModifier modifier =
                KnockBackModifier.fromLivingEntity(
                        entity
                );

        double originalKnockback =
                modifier.getKnockBackStrength();

        try {

            modifier.setKnockBackStrength(
                    bulletKnockback
            );

            entity.invulnerableTime = 0;

            if (debugMode) {
                LOGGER.info(
                        "[JAG-TACZ-DIAG] hurtWithBulletKnockback | victim={} | invulnerableTime={} | amount={}",
                        entity.getName().getString(),
                        entity.invulnerableTime,
                        amount
                );
            }

            boolean hurtResult =
                    entity.hurt(
                            source,
                            amount
                    );

            if (debugMode) {
                LOGGER.info(
                        "[JAG-TACZ-DIAG] hurt() RESULT | victim={} | result={} | health={} | invulnerableTime={}",
                        entity.getName().getString(),
                        hurtResult,
                        entity.getHealth(),
                        entity.invulnerableTime
                );
            }

        } finally {

            modifier.setKnockBackStrength(
                    originalKnockback
            );
        }
    }

    private static EquipmentSlot getFirstAidHitSlot(
            Entity bullet,
            Player player
    ) {

        if (!firstAidChecked) {

            try {

                if (ModList.get()
                        .isLoaded("firstaid")) {

                    Class<?> clazz =
                            Class.forName(
                                    "ichttt.mods.firstaid.common.util.PlayerSizeHelper"
                            );

                    firstAidGetSlotMethod =
                            clazz.getMethod(
                                    "getSlotTypeForProjectileHit",
                                    Entity.class,
                                    Player.class
                            );
                }

            } catch (Throwable t) {

                LogUtils.getLogger().error(
                        "JagTaczArmor: Failed to bind First Aid getSlotTypeForProjectileHit method",
                        t
                );
            }

            firstAidChecked = true;
        }

        if (firstAidGetSlotMethod != null) {

            try {

                return (EquipmentSlot)
                        firstAidGetSlotMethod.invoke(
                                null,
                                bullet,
                                player
                        );

            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static void sendDebugMessage(
            LivingEntity victim,
            LivingEntity attacker,
            String message
    ) {

        if (debugMode) {

            Component component =
                    Component.literal(
                            message
                    );

            if (attacker instanceof Player playerAttacker) {

                playerAttacker.displayClientMessage(
                        component,
                        false
                );
            }

            if (victim instanceof Player playerVictim) {

                if (playerVictim != attacker) {

                    playerVictim.displayClientMessage(
                            component,
                            false
                    );
                }
            }
        }
    }

    private static void spawnBloodParticles(
            ServerLevel serverLevel,
            Vec3 pos,
            double vx,
            double vy,
            double vz
    ) {

        if (ArmorConfig.DATA.enable_blood_particles) {

            int particleCount =
                    4
                            + (int) (
                            Math.random()
                                    * 3.0F
                    );

            for (int i = 0;
                 i < particleCount;
                 ++i) {

                double spreadX =
                        (Math.random() - 0.5F)
                                * 0.15;

                double spreadY =
                        (Math.random() - 0.5F)
                                * 0.15;

                double spreadZ =
                        (Math.random() - 0.5F)
                                * 0.15;

                double pvx =
                        vx * 0.5F
                                + (Math.random() - 0.5F)
                                * 0.2;

                double pvy =
                        vy * 0.5F
                                + Math.random()
                                * 0.25F;

                double pvz =
                        vz * 0.5F
                                + (Math.random() - 0.5F)
                                * 0.2;

                serverLevel.sendParticles(
                        ParticleRegistry.BLOOD.get(),
                        pos.x + spreadX,
                        pos.y + spreadY,
                        pos.z + spreadZ,
                        0,
                        pvx,
                        pvy,
                        pvz,
                        1.0F
                );
            }
        }
    }

    @SubscribeEvent
    public static void onAnvilUpdate(
            AnvilUpdateEvent event
    ) {

        ItemStack left =
                event.getLeft();

        ItemStack right =
                event.getRight();

        if (left.getItem()
                instanceof CustomGeoArmorItem
                && right.getItem()
                instanceof CustomGeoArmorItem) {

            String id1 =
                    left.hasTag()
                            ? left.getTag().getString(
                            "armor_id"
                    )
                            : "";

            String id2 =
                    right.hasTag()
                            ? right.getTag().getString(
                            "armor_id"
                    )
                            : "";

            if (!id1.equals(id2)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onPickupXp(
            PlayerXpEvent.PickupXp event
    ) {

        Player player =
                event.getEntity();

        if (player != null
                && !player.level().isClientSide) {

            ItemStack chest =
                    player.getItemBySlot(
                            EquipmentSlot.CHEST
                    );

            if (!chest.isEmpty()
                    && chest.hasTag()
                    && chest.getTag().contains(
                    "plate_id"
            )
                    && chest.getTag().contains(
                    "plate_durability"
            )) {

                String plateId =
                        chest.getTag().getString(
                                "plate_id"
                        );

                int curDur =
                        chest.getTag().getInt(
                                "plate_durability"
                        );

                PlateIndex plateIndex =
                        AddonPackLoader.PLATE_INDEXES.get(
                                new ResourceLocation(
                                        plateId
                                )
                        );

                if (plateIndex != null
                        && curDur < plateIndex.durability) {

                    ResourceLocation plateResourceId =
                            new ResourceLocation(plateId);

                    Item plateItem =
                            ItemRegistry.getPlateItem(
                                    plateResourceId
                            );

                    if (plateItem == null) {
                        return;
                    }

                    ItemStack plateStack =
                            new ItemStack(plateItem);

                    plateStack.getOrCreateTag()
                            .putString(
                                    "plate_id",
                                    plateId
                            );

                    if (chest.getTag().contains(
                            "plate_enchantments"
                    )) {

                        plateStack.getTag().put(
                                "Enchantments",
                                chest.getTag().getCompound(
                                        "plate_enchantments"
                                )
                        );
                    }

                    int mendingLvl =
                            EnchantmentHelper.getTagEnchantmentLevel(
                                    Enchantments.MENDING,
                                    plateStack
                            );

                    if (mendingLvl > 0) {

                        ExperienceOrb orb =
                                event.getOrb();

                        int xpValue =
                                orb.value;

                        if (xpValue > 0) {

                            int neededDur =
                                    plateIndex.durability
                                            - curDur;

                            int neededXp =
                                    (neededDur + 1) / 2;

                            int repairXp =
                                    Math.min(
                                            xpValue,
                                            neededXp
                                    );

                            if (repairXp > 0) {

                                curDur +=
                                        repairXp * 2;

                                if (curDur
                                        > plateIndex.durability) {

                                    curDur =
                                            plateIndex.durability;
                                }

                                chest.getTag().putInt(
                                        "plate_durability",
                                        curDur
                                );

                                orb.value -=
                                        repairXp;

                                if (orb.value <= 0) {

                                    orb.discard();

                                    event.setCanceled(
                                            true
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static {
        try {

            bulletPierceField =
                    EntityKineticBullet.class
                            .getDeclaredField(
                                    "pierce"
                            );

            bulletPierceField.setAccessible(
                    true
            );

            bulletArmorIgnoreField =
                    EntityKineticBullet.class
                            .getDeclaredField(
                                    "armorIgnore"
                            );

            bulletArmorIgnoreField.setAccessible(
                    true
            );

            bulletKnockbackField =
                    EntityKineticBullet.class
                            .getDeclaredField(
                                    "knockback"
                            );

            bulletKnockbackField.setAccessible(
                    true
            );

        } catch (NoSuchFieldException e) {

            try {

                LogUtils.getLogger().error(
                        "JagTaczArmor: Could not find pierce field in EntityKineticBullet",
                        e
                );

            } catch (Exception ignored) {
            }
        }

        SPEED_MODIFIER_UUID =
                UUID.fromString(
                        "c07b6118-2081-4b13-911d-6169528f8f2b"
                );

        PLATE_ARMOR_UUID =
                UUID.fromString(
                        "e57b6118-2081-4b13-911d-6169528f8f2b"
                );

        PLATE_TOUGHNESS_UUID =
                UUID.fromString(
                        "e57b6118-2081-4b13-911d-6169528f8f2c"
                );

        PLATE_KNOCKBACK_UUID =
                UUID.fromString(
                        "e57b6118-2081-4b13-911d-6169528f8f2d"
                );

        lastSoundIndex = 0;
        lastHurtSoundIndex = 0;

        firstAidGetSlotMethod = null;
        firstAidChecked = false;
    }
}