//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.jagtaczarmor.network.packet;

import com.jagtaczarmor.event.ClientEventHandler.ClientForgeEvents;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class S2CDamageTiltPacket {
    private final float multiplier;
    private final int duration;
    private final float frequency;
    private final float decayExponent;
    private final float vignetteIntensity;
    private final int vignetteDuration;
    private final float initialPitchDir;
    private final float initialRollDir;
    private final float initialYawDir;

    public S2CDamageTiltPacket(float multiplier, int duration, float frequency, float decayExponent, float vignetteIntensity, int vignetteDuration, float initialPitchDir, float initialRollDir, float initialYawDir) {
        this.multiplier = multiplier;
        this.duration = duration;
        this.frequency = frequency;
        this.decayExponent = decayExponent;
        this.vignetteIntensity = vignetteIntensity;
        this.vignetteDuration = vignetteDuration;
        this.initialPitchDir = initialPitchDir;
        this.initialRollDir = initialRollDir;
        this.initialYawDir = initialYawDir;
    }

    public S2CDamageTiltPacket(float multiplier, int duration, float frequency, float decayExponent, float vignetteIntensity, int vignetteDuration, float initialPitchDir, float initialRollDir) {
        this(multiplier, duration, frequency, decayExponent, vignetteIntensity, vignetteDuration, initialPitchDir, initialRollDir, 0.0F);
    }

    public S2CDamageTiltPacket(float multiplier, int duration, float frequency, float decayExponent, float vignetteIntensity, int vignetteDuration) {
        this(multiplier, duration, frequency, decayExponent, vignetteIntensity, vignetteDuration, 0.0F, 0.0F, 0.0F);
    }

    public S2CDamageTiltPacket(FriendlyByteBuf buffer) {
        this.multiplier = buffer.readFloat();
        this.duration = buffer.readInt();
        this.frequency = buffer.readFloat();
        this.decayExponent = buffer.readFloat();
        this.vignetteIntensity = buffer.readFloat();
        this.vignetteDuration = buffer.readInt();
        this.initialPitchDir = buffer.readFloat();
        this.initialRollDir = buffer.readFloat();
        this.initialYawDir = buffer.readFloat();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeFloat(this.multiplier);
        buffer.writeInt(this.duration);
        buffer.writeFloat(this.frequency);
        buffer.writeFloat(this.decayExponent);
        buffer.writeFloat(this.vignetteIntensity);
        buffer.writeInt(this.vignetteDuration);
        buffer.writeFloat(this.initialPitchDir);
        buffer.writeFloat(this.initialRollDir);
        buffer.writeFloat(this.initialYawDir);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = (NetworkEvent.Context)contextSupplier.get();
        context.enqueueWork(() -> {
            ClientForgeEvents.triggerCustomTilt(this.multiplier, this.duration, this.frequency, this.decayExponent, this.initialPitchDir, this.initialRollDir, this.initialYawDir);
            if (this.vignetteIntensity > 0.0F) {
                ClientForgeEvents.triggerVignette(this.vignetteIntensity, this.vignetteDuration);
            }

        });
        context.setPacketHandled(true);
    }
}
