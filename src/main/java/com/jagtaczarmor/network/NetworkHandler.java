//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jagtaczarmor.network;

import com.jagtaczarmor.network.packet.S2CDamageTiltPacket;
import com.jagtaczarmor.network.packet.S2CParticlePacket;
import com.jagtaczarmor.network.packet.S2CStopBulletPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("jagtaczarmor", "main"), () -> "1", "1"::equals, "1"::equals);

    public NetworkHandler() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, S2CDamageTiltPacket.class, S2CDamageTiltPacket::encode, S2CDamageTiltPacket::new, S2CDamageTiltPacket::handle);
        CHANNEL.registerMessage(id++, S2CParticlePacket.class, S2CParticlePacket::encode, S2CParticlePacket::new, S2CParticlePacket::handle);
        CHANNEL.registerMessage(id++, S2CStopBulletPacket.class, S2CStopBulletPacket::encode, S2CStopBulletPacket::new, S2CStopBulletPacket::handle);
    }
}
