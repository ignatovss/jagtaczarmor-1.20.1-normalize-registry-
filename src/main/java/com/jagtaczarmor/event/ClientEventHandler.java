package com.jagtaczarmor.event;

import com.jagtaczarmor.JagTaczArmor;
import com.jagtaczarmor.client.PlateClientTooltipComponent;
import com.jagtaczarmor.client.PlateTooltipComponent;
import com.jagtaczarmor.client.particle.BloodParticle;
import com.jagtaczarmor.client.particle.ImpactSparkParticle;
import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.data.ArmorIndex;
import com.jagtaczarmor.data.PlateIndex;
import com.jagtaczarmor.item.CustomGeoArmorItem;
import com.jagtaczarmor.item.CustomPlateItem;
import com.jagtaczarmor.registry.ItemRegistry;
import com.jagtaczarmor.registry.ParticleRegistry;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Either;
import com.tacz.guns.crafting.GunSmithTableRecipe;
import com.tacz.guns.crafting.result.GunSmithTableResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorItem.Type;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.IItemDecorator;
import net.minecraftforge.client.event.RecipesUpdatedEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.List;

@EventBusSubscriber(modid = "jagtaczarmor", value = {Dist.CLIENT}, bus = Bus.MOD)
public class ClientEventHandler {
    private static final ResourceLocation VIGNETTE_LOCATION = new ResourceLocation("jagtaczarmor", "textures/misc/helmet_blur.png");

    public static final IGuiOverlay HELMET_OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player != null && mc.options.getCameraType().isFirstPerson()) {
            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            if (!helmet.isEmpty()) {
                ArmorIndex index = CustomGeoArmorItem.getIndex(helmet);
                if (index != null && index.hasVignette) {
                    RenderSystem.disableDepthTest();
                    RenderSystem.depthMask(false);
                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

                    ResourceLocation mask = index.getOverlayLocation();
                    if (mask == null || mc.getTextureManager().getTexture(mask) == null) {
                        ResourceLocation foundFallback = null;
                        if (mask != null) {
                            String path = mask.getPath();
                            if (index.packNamespace != null && !index.packNamespace.isEmpty()) {
                                ResourceLocation packFallback = new ResourceLocation(index.packNamespace, path);
                                if (mc.getTextureManager().getTexture(packFallback) != null) {
                                    foundFallback = packFallback;
                                }
                            }
                            if (foundFallback == null) {
                                ResourceLocation defaultPackFallback = new ResourceLocation("jag_default_armor", path);
                                if (mc.getTextureManager().getTexture(defaultPackFallback) != null) {
                                    foundFallback = defaultPackFallback;
                                }
                            }
                        }
                        if (foundFallback != null) {
                            mask = foundFallback;
                        } else {
                            mask = VIGNETTE_LOCATION;
                        }
                    }

                    guiGraphics.blit(mask, 0, 0, -90, 0.0F, 0.0F, width, height, width, height);

                    RenderSystem.disableBlend();
                    RenderSystem.depthMask(true);
                    RenderSystem.enableDepthTest();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        }
    };

    public ClientEventHandler() {
    }

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerBelow(VanillaGuiOverlay.HOTBAR.id(), "helmet_overlay", HELMET_OVERLAY);
        event.registerAboveAll("vignette_overlay", ClientForgeEvents.VIGNETTE_OVERLAY);
    }

    @SubscribeEvent
    public static void onRegisterParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.IMPACT_SPARK.get(), ImpactSparkParticle.Provider::new);
        event.registerSpriteSet(ParticleRegistry.BLOOD.get(), BloodParticle.Provider::new);
    }

    @SubscribeEvent
    public static void onRegisterTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(PlateTooltipComponent.class, PlateClientTooltipComponent::new);
    }

    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof ArmorItem armorItem) {
                if (armorItem.getType() == Type.CHESTPLATE) {
                    event.register(item, new IItemDecorator() {
                        @Override
                        public boolean render(GuiGraphics guiGraphics, Font font, ItemStack stack, int x, int y) {
                            if (stack.hasTag() && stack.getTag().contains("plate_id")) {
                                String plateId = stack.getTag().getString("plate_id");
                                int plateDur = stack.getTag().getInt("plate_durability");
                                PlateIndex plateIndex = AddonPackLoader.PLATE_INDEXES.get(new ResourceLocation(plateId));
                                if (plateIndex != null && plateIndex.durability > 0) {
                                    int barWidth = Math.round((float) plateDur * 13.0F / (float) plateIndex.durability);
                                    barWidth = Math.max(0, Math.min(13, barWidth));
                                    guiGraphics.pose().pushPose();
                                    guiGraphics.pose().translate(0.0F, 0.0F, 200.0F);
                                    RenderSystem.disableDepthTest();
                                    guiGraphics.fill(x + 2, y + 11, x + 15, y + 13, -16777216);
                                    guiGraphics.fill(x + 2, y + 11, x + 2 + barWidth, y + 12, -16733441);
                                    RenderSystem.enableDepthTest();
                                    guiGraphics.pose().popPose();
                                }
                            }
                            return false;
                        }
                    });
                }
            }
        }
    }

    @EventBusSubscriber(modid = "jagtaczarmor", value = {Dist.CLIENT}, bus = Bus.FORGE)
    public static class ClientForgeEvents {
        public static int customTiltTime = 0;
        public static float customTiltMultiplier = 1.0F;
        public static int maxTiltTime = 10;
        public static float customTiltFrequency = 3.0F;
        public static float customTiltDecay = 1.0F;
        public static float randomRollDir = 1.0F;
        public static float randomPitchDir = 1.0F;
        public static float randomYawDir = 1.0F;
        private static float prevTickRoll = 0.0F;
        private static float currentTickRoll = 0.0F;
        private static float prevTickPitch = 0.0F;
        private static float currentTickPitch = 0.0F;
        private static float prevTickYaw = 0.0F;
        private static float currentTickYaw = 0.0F;
        public static float prevTickFov = 0.0F;
        public static float currentTickFov = 0.0F;
        public static float tickPhase = 0.0F;
        private static double originalDamageTilt = -1.0F;
        public static float vignetteAlpha = 0.0F;
        public static float prevVignetteAlpha = 0.0F;
        public static int vignetteTime = 0;
        public static int maxVignetteTime = 10;
        public static float vignetteTargetIntensity = 1.0F;
        public static float biasPitchDir = 0.0F;
        public static float biasRollDir = 0.0F;
        public static float biasYawDir = 0.0F;
        public static float pitchPhaseOffset = 0.0F;
        public static float rollPhaseOffset = 0.0F;
        public static float yawPhaseOffset = 0.0F;

        public static final IGuiOverlay VIGNETTE_OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
            float alpha = Mth.lerp(partialTick, prevVignetteAlpha, vignetteAlpha);
            if (!(alpha <= 0.01F)) {
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
                RenderSystem.setShaderColor(0.0F, alpha, alpha, 1.0F);
                guiGraphics.blit(new ResourceLocation("minecraft", "textures/misc/vignette.png"), 0, 0, -90, 0.0F, 0.0F, width, height, width, height);
                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            }
        };

        public ClientForgeEvents() {
        }

        public static void triggerCustomTilt(float multiplier, int duration, float frequency, float decayExponent) {
            triggerCustomTilt(multiplier, duration, frequency, decayExponent, 0.0F, 0.0F, 0.0F);
        }

        public static void triggerCustomTilt(float multiplier, int duration, float frequency, float decayExponent, float initialPitchDir, float initialRollDir) {
            triggerCustomTilt(multiplier, duration, frequency, decayExponent, initialPitchDir, initialRollDir, 0.0F);
        }

        public static void triggerCustomTilt(float multiplier, int duration, float frequency, float decayExponent, float initialPitchDir, float initialRollDir, float initialYawDir) {
            customTiltTime = duration;
            maxTiltTime = duration;
            customTiltFrequency = frequency;
            customTiltDecay = decayExponent;
            float randomStrength = 0.8F + (float) Math.random() * 0.4F;
            customTiltMultiplier = multiplier * randomStrength;
            biasPitchDir = initialPitchDir;
            biasRollDir = initialRollDir;
            biasYawDir = initialYawDir;
            pitchPhaseOffset = (float) (Math.random() * Math.PI * 2.0F);
            rollPhaseOffset = (float) (Math.random() * Math.PI * 2.0F);
            yawPhaseOffset = (float) (Math.random() * Math.PI * 2.0F);
            tickPhase = 0.0F;

            float rollWave = Mth.sin(rollPhaseOffset);
            float pitchWave = Mth.cos(pitchPhaseOffset);
            float yawWave = Mth.cos(yawPhaseOffset);

            float rollLimit = 14.0F * multiplier * 0.4F;
            if (Math.abs(currentTickRoll) > rollLimit) {
                randomRollDir = currentTickRoll > 0.0F ? (rollWave >= 0.0F ? -1.0F : 1.0F) : (rollWave >= 0.0F ? 1.0F : -1.0F);
            } else {
                randomRollDir = Math.random() > 0.5F ? 1.0F : -1.0F;
            }

            float pitchLimit = 10.0F * multiplier * 0.4F;
            if (Math.abs(currentTickPitch) > pitchLimit) {
                randomPitchDir = currentTickPitch > 0.0F ? (pitchWave >= 0.0F ? -1.0F : 1.0F) : (pitchWave >= 0.0F ? 1.0F : -1.0F);
            } else {
                randomPitchDir = Math.random() > 0.5F ? 1.0F : -1.0F;
            }

            float yawLimit = 8.0F * multiplier * 0.4F;
            if (Math.abs(currentTickYaw) > yawLimit) {
                randomYawDir = currentTickYaw > 0.0F ? (yawWave >= 0.0F ? -1.0F : 1.0F) : (yawWave >= 0.0F ? 1.0F : -1.0F);
            } else {
                randomYawDir = Math.random() > 0.5F ? 1.0F : -1.0F;
            }
        }

        public static void triggerVignette(float intensity, int duration) {
            vignetteTime = duration;
            maxVignetteTime = duration;
            vignetteTargetIntensity = intensity;
            vignetteAlpha = intensity;
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == Phase.END) {
                if (customTiltTime > 0) {
                    if (originalDamageTilt < 0.0F) {
                        originalDamageTilt = Minecraft.getInstance().options.sensitivity().get();
                    }
                    Minecraft.getInstance().options.sensitivity().set(0.0D);
                } else if (originalDamageTilt >= 0.0F) {
                    Minecraft.getInstance().options.sensitivity().set(originalDamageTilt);
                    originalDamageTilt = -1.0F;
                }

                prevTickRoll = currentTickRoll;
                prevTickPitch = currentTickPitch;
                prevTickYaw = currentTickYaw;
                prevTickFov = currentTickFov;
                prevVignetteAlpha = vignetteAlpha;

                if (customTiltTime > 0) {
                    --customTiltTime;
                    ++tickPhase;
                    float fade = Math.max(0.0F, (float) customTiltTime / (float) maxTiltTime);
                    fade = (float) Math.pow(fade, customTiltDecay);

                    float pitchJitter = Mth.sin(tickPhase * customTiltFrequency * 1.3F + pitchPhaseOffset) * 4.0F * randomPitchDir;
                    float rollJitter = Mth.cos(tickPhase * customTiltFrequency * 1.1F + rollPhaseOffset) * 5.0F * randomRollDir;
                    float yawJitter = Mth.sin(tickPhase * customTiltFrequency * 1.5F + yawPhaseOffset) * 6.0F * randomYawDir;

                    float targetPitch = (biasPitchDir * 12.0F + pitchJitter) * customTiltMultiplier * fade;
                    float targetRoll = (biasRollDir * 14.0F + rollJitter) * customTiltMultiplier * fade;
                    float targetYaw = (biasYawDir * 10.0F + yawJitter) * customTiltMultiplier * fade;
                    float targetFovDrop = 10.0F * customTiltMultiplier * fade;

                    currentTickRoll = Mth.lerp(0.5F, currentTickRoll, targetRoll);
                    currentTickPitch = Mth.lerp(0.5F, currentTickPitch, targetPitch);
                    currentTickYaw = Mth.lerp(0.5F, currentTickYaw, targetYaw);
                    currentTickFov = Mth.lerp(0.5F, currentTickFov, targetFovDrop);
                } else {
                    currentTickRoll = Mth.lerp(0.5F, currentTickRoll, 0.0F);
                    currentTickPitch = Mth.lerp(0.5F, currentTickPitch, 0.0F);
                    currentTickYaw = Mth.lerp(0.5F, currentTickYaw, 0.0F);
                    currentTickFov = Mth.lerp(0.5F, currentTickFov, 0.0F);
                }

                if (vignetteTime > 0) {
                    --vignetteTime;
                    float fade = Math.max(0.0F, (float) vignetteTime / (float) maxVignetteTime);
                    vignetteAlpha = vignetteTargetIntensity * fade;
                } else {
                    vignetteAlpha = Mth.lerp(0.3F, vignetteAlpha, 0.0F);
                }
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
            ItemStack stack = event.getItemStack();
            CustomGeoArmorItem.verifyAndSyncCustomModelData(stack);
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId != null && itemId.getNamespace().equals("jagtaczarmor")) {
                event.getTooltipElements().removeIf((either) -> either.map((formattedText) -> {
                    String text = formattedText.getString().trim();
                    return text.equalsIgnoreCase("JagTaczArmor") || text.equalsIgnoreCase("Jag Tacz Armor");
                }, (tooltipComponent) -> false));
            }

            if (stack.getItem() instanceof ArmorItem armorItem) {
                if (armorItem.getType() == Type.CHESTPLATE && stack.hasTag() && stack.getTag().contains("plate_id")) {
                    String plateId = stack.getTag().getString("plate_id");
                    int plateDur = stack.getTag().getInt("plate_durability");
                    PlateIndex plateIndex = AddonPackLoader.PLATE_INDEXES.get(new ResourceLocation(plateId));
                    if (plateIndex != null) {
                        ItemStack plateStack = new ItemStack(ItemRegistry.PLATE_ARMOR.get());
                        plateStack.getOrCreateTag().putString("plate_id", plateId);
                        plateStack.getOrCreateTag().putInt("plate_durability", plateDur);
                        ResourceLocation id = new ResourceLocation(plateId);
                        if (AddonPackLoader.PLATE_CMD.containsKey(id)) {
                            plateStack.getOrCreateTag().putInt("CustomModelData", AddonPackLoader.PLATE_CMD.get(id));
                        }
                        if (stack.hasTag() && stack.getTag().contains("plate_enchantments")) {
                            plateStack.getTag().put("Enchantments", stack.getTag().getCompound("plate_enchantments"));
                        }

                        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();
                        int insertIdx = -1;
                        for (int i = 0; i < elements.size(); ++i) {
                            Either<FormattedText, TooltipComponent> elem = elements.get(i);
                            if (elem.left().isPresent()) {
                                String text = elem.left().get().getString();
                                if (text.contains("Active Plate:") || text.contains("Inserted Plate:")) {
                                    insertIdx = i + 1;
                                    break;
                                }
                            }
                        }

                        if (insertIdx != -1) {
                            elements.add(insertIdx, Either.right(new PlateTooltipComponent(plateStack, plateDur, plateIndex.durability)));
                            ++insertIdx;
                            if (stack.hasTag() && stack.getTag().contains("plate_enchantments")) {
                                ListTag enchants = stack.getTag().getList("plate_enchantments", 10);
                                for (int i = 0; i < enchants.size(); ++i) {
                                    CompoundTag enchantTag = enchants.getCompound(i);
                                    ResourceLocation enchantId = ResourceLocation.tryParse(enchantTag.getString("id"));
                                    if (enchantId != null) {
                                        Enchantment enchant = ForgeRegistries.ENCHANTMENTS.getValue(enchantId);
                                        if (enchant != null) {
                                            int lvl = enchantTag.getInt("lvl");
                                            Component enchantName = enchant.getFullname(lvl);
                                            elements.add(insertIdx++, Either.left(Component.literal("   ").append(enchantName)));
                                        }
                                    }
                                }
                            }
                        } else {
                            elements.add(Either.right(new PlateTooltipComponent(plateStack, plateDur, plateIndex.durability)));
                        }
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onItemTooltip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack != null && !stack.isEmpty()) {
                if (!(stack.getItem() instanceof CustomGeoArmorItem)) {
                    ArmorIndex index = CustomGeoArmorItem.getIndex(stack);
                    if (index != null) {
                        CustomGeoArmorItem.appendStatsAndMeta(stack, event.getToolTip(), event.getFlags(), index);
                    }
                }
            }
        }

        @SubscribeEvent
        public static void onComputeFov(ViewportEvent.ComputeFov event) {
            float partialTicks = (float) event.getPartialTick();
            float renderFovDrop = Mth.lerp(partialTicks, prevTickFov, currentTickFov);
            if (renderFovDrop > 0.01F) {
                event.setFOV(event.getFOV() - renderFovDrop);
            }
        }

        @SubscribeEvent
        public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
            float partialTicks = (float) event.getPartialTick();
            float renderRoll = Mth.lerp(partialTicks, prevTickRoll, currentTickRoll);
            float renderPitch = Mth.lerp(partialTicks, prevTickPitch, currentTickPitch);
            float renderYaw = Mth.lerp(partialTicks, prevTickYaw, currentTickYaw);
            if (Math.abs(renderRoll) > 0.01F || Math.abs(renderPitch) > 0.01F || Math.abs(renderYaw) > 0.01F) {
                event.setRoll(event.getRoll() + renderRoll);
                event.setPitch(event.getPitch() + renderPitch);
                event.setYaw(event.getYaw() + renderYaw);
            }
        }

        @SubscribeEvent
        public static void onRecipesUpdated(RecipesUpdatedEvent event) {
            RecipeManager recipeManager = event.getRecipeManager();
            for (Recipe<?> recipe : recipeManager.getRecipes()) {
                if (recipe instanceof GunSmithTableRecipe tableRecipe) {
                    ItemStack output = tableRecipe.getOutput();
                    if (output != null) {
                        if (output.getItem() instanceof CustomGeoArmorItem) {
                            ArmorIndex index = CustomGeoArmorItem.getIndex(output);
                            if (index != null && index.slot != null) {
                                String slotGroup = null;
                                switch (index.slot.toLowerCase()) {
                                    case "helmet":
                                    case "head":
                                        slotGroup = "jagtaczarmor:helmet";
                                        break;
                                    case "chestplate":
                                    case "torso":
                                    case "chest":
                                        slotGroup = "jagtaczarmor:chestplate";
                                        break;
                                    case "leggings":
                                    case "legs":
                                        slotGroup = "jagtaczarmor:leggings";
                                        break;
                                    case "boots":
                                    case "feet":
                                        slotGroup = "jagtaczarmor:boots";
                                        break;
                                }
                                if (slotGroup != null) {
                                    setRecipeGroup(tableRecipe, slotGroup);
                                }
                            }
                        } else if (output.getItem() instanceof CustomPlateItem) {
                            setRecipeGroup(tableRecipe, "jagtaczarmor:plate");
                        }
                    }
                }
            }
        }

        private static void setRecipeGroup(GunSmithTableRecipe recipe, String group) {
            try {
                GunSmithTableResult result = recipe.getResult();
                if (result != null) {
                    Field groupField = GunSmithTableResult.class.getDeclaredField("group");
                    groupField.setAccessible(true);
                    groupField.set(result, new ResourceLocation(group));
                }
            } catch (Exception e) {
                JagTaczArmor.LOGGER.error("Failed to dynamically override recipe group", e);
            }
        }
    }
}