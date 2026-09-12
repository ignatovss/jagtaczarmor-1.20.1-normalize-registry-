package com.jagtaczarmor.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BloodParticle extends TextureSheetParticle {
    private final float rotSpeed;

    public BloodParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.quadSize *= 0.25F;
        this.lifetime = 12 + this.random.nextInt(12);
        this.gravity = 1.0F;
        this.hasPhysics = true;
        this.oRoll = (float) (Math.random() * Math.PI * 2.0F);
        this.roll = this.oRoll;
        this.rotSpeed = ((float) Math.random() - 0.5F) * 0.3F;
        this.rCol = 0.45F + (float) Math.random() * 0.08F;
        this.gCol = 0.03F;
        this.bCol = 0.03F;
    }

    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public void tick() {
        super.tick();
        this.roll = this.oRoll;
        this.oRoll += this.rotSpeed;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet spriteSet;

        public Provider(SpriteSet spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
            BloodParticle particle = new BloodParticle(level, x, y, z, vx, vy, vz);
            particle.pickSprite(this.spriteSet);
            return particle;
        }
    }
}