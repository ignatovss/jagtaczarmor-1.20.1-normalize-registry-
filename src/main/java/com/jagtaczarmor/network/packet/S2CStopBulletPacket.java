package com.jagtaczarmor.network.packet;

import com.tacz.guns.entity.EntityKineticBullet;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

public class S2CStopBulletPacket {
    private final int bulletId;
    private final double x;
    private final double y;
    private final double z;

    public S2CStopBulletPacket(int bulletId, double x, double y, double z) {
        this.bulletId = bulletId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public S2CStopBulletPacket(FriendlyByteBuf buf) {
        this.bulletId = buf.readInt();
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.bulletId);
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                Entity bullet = level.getEntity(this.bulletId);
                if (bullet != null) {
                    if (bullet instanceof EntityKineticBullet) {
                        bullet.getPersistentData().putFloat("tacz:tracer_size", 0.0F);
                    }
                    bullet.setPos(this.x, -1000.0F, this.z);
                    bullet.xOld = this.x;
                    bullet.yOld = -1000.0F;
                    bullet.zOld = this.z;
                    bullet.setDeltaMovement(Vec3.ZERO);
                    bullet.discard();
                }
            }
        });
        context.setPacketHandled(true);
    }
}