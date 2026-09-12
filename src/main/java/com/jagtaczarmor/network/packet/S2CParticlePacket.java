package com.jagtaczarmor.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class S2CParticlePacket {
    private final double x;
    private final double y;
    private final double z;
    private final double vx;
    private final double vy;
    private final double vz;
    private final float scale;
    private final int lifetime;

    public S2CParticlePacket(double x, double y, double z, double vx, double vy, double vz, float scale, int lifetime) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.vx = vx;
        this.vy = vy;
        this.vz = vz;
        this.scale = scale;
        this.lifetime = lifetime;
    }

    public S2CParticlePacket(FriendlyByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.vx = buf.readDouble();
        this.vy = buf.readDouble();
        this.vz = buf.readDouble();
        this.scale = buf.readFloat();
        this.lifetime = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeDouble(this.vx);
        buf.writeDouble(this.vy);
        buf.writeDouble(this.vz);
        buf.writeFloat(this.scale);
        buf.writeInt(this.lifetime);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            handleClient();
        });
        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            // Используем разные типы частиц для эффектов
            // Для крови используем CRIMSON_SPORE
            // Для искр используем FLAME
            // Можно добавить логику выбора типа частицы

            // Создаем несколько частиц для эффекта
            for (int i = 0; i < 5; i++) {
                double offsetX = (Math.random() - 0.5) * 0.1;
                double offsetY = (Math.random() - 0.5) * 0.1;
                double offsetZ = (Math.random() - 0.5) * 0.1;

                mc.level.addParticle(
                        ParticleTypes.CRIMSON_SPORE,
                        this.x + offsetX,
                        this.y + offsetY,
                        this.z + offsetZ,
                        this.vx * 0.5 + (Math.random() - 0.5) * 0.1,
                        this.vy * 0.5 + (Math.random() - 0.5) * 0.1,
                        this.vz * 0.5 + (Math.random() - 0.5) * 0.1
                );

                // Добавляем искры
                if (i % 2 == 0) {
                    mc.level.addParticle(
                            ParticleTypes.FLAME,
                            this.x + offsetX,
                            this.y + offsetY,
                            this.z + offsetZ,
                            this.vx * 0.3 + (Math.random() - 0.5) * 0.05,
                            this.vy * 0.3 + (Math.random() - 0.5) * 0.05,
                            this.vz * 0.3 + (Math.random() - 0.5) * 0.05
                    );
                }
            }
        }
    }
}