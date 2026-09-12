//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jagtaczarmor.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class ArmorIndex {
    @SerializedName("geo_model")
    public String geoModel;
    @SerializedName("slot")
    public String slot;
    @SerializedName("texture")
    public String texture;
    @SerializedName("defense")
    public int defense = 3;
    @SerializedName("toughness")
    public float toughness = 0.0F;
    @SerializedName("knockback_resistance")
    public float knockbackResistance = 0.0F;
    @SerializedName("camera_shake_multiplier")
    public float cameraShakeMultiplier = 1.0F;
    @SerializedName("has_vignette")
    public boolean hasVignette = false;
    @SerializedName("damage_reduction_multiplier")
    public float damageReductionMultiplier = 1.0F;
    @SerializedName("durability_multiplier")
    public int durabilityMultiplier = 33;
    @SerializedName("ammo_immunity")
    public float ammoImmunity = 0.0F;
    @SerializedName("back_ammo_immunity")
    public float backAmmoImmunity = 0.0F;
    @SerializedName("speed_modify")
    public double speedModify = (double)0.0F;
    @SerializedName("jump_modify")
    public double jumpModify = (double)0.0F;
    @SerializedName("display_name")
    public String displayName = "Custom Armor";
    @SerializedName("name")
    public String name;
    @SerializedName("damage_reduction_cap")
    public float damageReductionCap = 0.75F;
    @SerializedName("item_texture")
    public String itemTexture;
    @SerializedName("overlay_texture")
    public String overlayTexture;
    @SerializedName("repair_item")
    public String repairItem;
    @SerializedName("armor_tag")
    public String armorTag;
    public String packNamespace;
    @SerializedName("block_vanila_projectile")
    public boolean blockVanillaProjectile = false;
    @SerializedName("plate_slot")
    public boolean plateSlot = false;

    public ArmorIndex() {
    }

    public ResourceLocation getModelLocation() {
        return this.geoModel != null ? new ResourceLocation(this.geoModel) : null;
    }

    public ResourceLocation getTextureLocation() {
        return this.texture != null ? new ResourceLocation(this.texture) : null;
    }

    public ResourceLocation getOverlayLocation() {
        if (this.overlayTexture != null && !this.overlayTexture.trim().isEmpty()) {
            String tex = this.overlayTexture.trim();
            String namespace;
            String path;
            if (tex.contains(":")) {
                String[] parts = tex.split(":", 2);
                namespace = parts[0];
                path = parts[1];
            } else {
                namespace = this.packNamespace != null && !this.packNamespace.trim().isEmpty() ? this.packNamespace.trim() : "minecraft";
                path = tex;
            }

            if (!path.startsWith("textures/")) {
                path = "textures/" + path;
            }

            if (!path.endsWith(".png")) {
                path = path + ".png";
            }

            return new ResourceLocation(namespace, path);
        } else {
            return null;
        }
    }
}
