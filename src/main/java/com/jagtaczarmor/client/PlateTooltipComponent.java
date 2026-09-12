//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jagtaczarmor.client;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class PlateTooltipComponent implements TooltipComponent {
    private final ItemStack plateStack;
    private final int curDurability;
    private final int maxDurability;

    public PlateTooltipComponent(ItemStack plateStack, int curDurability, int maxDurability) {
        this.plateStack = plateStack;
        this.curDurability = curDurability;
        this.maxDurability = maxDurability;
    }

    public ItemStack getPlateStack() {
        return this.plateStack;
    }

    public int getCurDurability() {
        return this.curDurability;
    }

    public int getMaxDurability() {
        return this.maxDurability;
    }
}
