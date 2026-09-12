//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jagtaczarmor.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.entity.EquipmentSlot;

public class ArmorSetIndex {
    @SerializedName("name")
    public String name = "Custom Armor Set";
    @SerializedName("helmet")
    public ArmorIndex helmet;
    @SerializedName("chestplate")
    public ArmorIndex chestplate;
    @SerializedName("leggings")
    public ArmorIndex leggings;
    @SerializedName("boots")
    public ArmorIndex boots;

    public ArmorSetIndex() {
    }

    public ArmorIndex getPiece(EquipmentSlot slot) {
        ArmorIndex var10000;
        switch (slot) {
            case HEAD -> var10000 = this.helmet;
            case CHEST -> var10000 = this.chestplate;
            case LEGS -> var10000 = this.leggings;
            case FEET -> var10000 = this.boots;
            default -> var10000 = null;
        }

        return var10000;
    }
}
