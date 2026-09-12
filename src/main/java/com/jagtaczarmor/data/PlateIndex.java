//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jagtaczarmor.data;

import com.google.gson.annotations.SerializedName;

public class PlateIndex {
    @SerializedName("name")
    public String name = "Custom Plate";
    @SerializedName("defense")
    public int defense = 0;
    @SerializedName("toughness")
    public int toughness = 0;
    @SerializedName("item_texture")
    public String itemTexture;
    @SerializedName("block_vanila_projectile")
    public boolean blockVanillaProjectile = false;
    @SerializedName("speed_modify")
    public double speedModify = (double)0.0F;
    @SerializedName("jump_modify")
    public double jumpModify = (double)0.0F;
    @SerializedName("camera_shake_multiplier")
    public double cameraShakeMultiplier = (double)1.0F;
    @SerializedName("durability")
    public int durability = 10;
    @SerializedName("ammo_immunity")
    public double ammoImmunity = (double)0.0F;
    @SerializedName("knockback_resistance")
    public double knockbackResistance = (double)0.0F;
    @SerializedName("damage_reduction_cap")
    public double damageReductionCap = (double)0.0F;
    public String packNamespace;
    public String registryName;

    public PlateIndex() {
    }
}
