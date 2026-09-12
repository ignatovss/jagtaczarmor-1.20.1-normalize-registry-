package com.jagtaczarmor.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientTooltipHelper {
    public ClientTooltipHelper() {
    }

    public static boolean isShiftDown() {
        return Screen.hasShiftDown();
    }
}