//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jagtaczarmor;

import com.jagtaczarmor.config.ArmorConfig;
import com.jagtaczarmor.data.AddonPackLoader;
import com.jagtaczarmor.network.NetworkHandler;
import com.jagtaczarmor.registry.CreativeTabRegistry;
import com.jagtaczarmor.registry.ItemRegistry;
import com.jagtaczarmor.registry.ParticleRegistry;
import com.jagtaczarmor.registry.RecipeRegistry;
import com.jagtaczarmor.registry.SoundRegistry;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("jagtaczarmor")
public class JagTaczArmor {
    public static final String MODID = "jagtaczarmor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public JagTaczArmor() {
        ArmorConfig.load();
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        AddonPackLoader.init();
        ItemRegistry.ITEMS.register(modEventBus);
        CreativeTabRegistry.CREATIVE_MODE_TABS.register(modEventBus);
        SoundRegistry.SOUND_EVENTS.register(modEventBus);
        ParticleRegistry.PARTICLES.register(modEventBus);
        RecipeRegistry.RECIPE_SERIALIZERS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        NetworkHandler.register();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("JagTaczArmor Common Setup");
    }
}
