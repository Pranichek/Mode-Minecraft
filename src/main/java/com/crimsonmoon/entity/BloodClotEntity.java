package com.crimsonmoon.entity;

import com.crimsonmoon.ModEntities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * Кастомна сутність "сгустка" крові, яку випускає кривавий лук замість
 * ванільної стріли. Без текстур/моделі — весь візуал реалізовано частками,
 * так само як BloodBoltEntity, який використовує посох.
 */
public class BloodClotEntity extends ThrownItemEntity {

    public static final float DAMAGE = 5.0f;
    private static final int MAX_AGE_TICKS = 100;

    public BloodClotEntity(EntityType<? extends BloodClotEntity> entityType, World world) {
        super(entityType, world);
    }

    public BloodClotEntity(World world, LivingEntity owner) {
        super(ModEntities.BLOOD_CLOT, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    @Override
    protected float getGravity() {
        return 0.03f; // легка дуга польоту, як у звичайної стріли
    }

    @Override
    public void tick() {
        super.tick();

        if (this.age > MAX_AGE_TICKS) {
            this.discard();
            return;
        }

        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                new DustParticleEffect(new Vector3f(0.5f, 0.0f, 0.0f), 0.8f),
                this.getX(), this.getY(), this.getZ(),
                1, 0.01, 0.01, 0.01, 0.0
            );
        }
    }

    @Override
    protected void onEntityHit(EntityHitResult hitResult) {
        super.onEntityHit(hitResult);

        if (this.getWorld().isClient) {
            return;
        }

        Entity target = hitResult.getEntity();
        Entity owner = this.getOwner();

        if (target == owner) {
            return;
        }

        DamageSource damageSource = this.getWorld().getDamageSources().thrown(this, owner != null ? owner : this);
        target.damage(damageSource, DAMAGE);

        if (target instanceof LivingEntity livingTarget) {
            livingTarget.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 0));
        }

        spawnImpactParticles();
        this.discard();
    }

    @Override
    protected void onBlockHit(BlockHitResult hitResult) {
        super.onBlockHit(hitResult);

        if (!this.getWorld().isClient) {
            spawnImpactParticles();
            this.discard();
        }
    }

    private void spawnImpactParticles() {
        if (this.getWorld() instanceof ServerWorld serverWorld) {
            serverWorld.spawnParticles(
                new DustParticleEffect(new Vector3f(0.5f, 0.0f, 0.0f), 1.4f),
                this.getX(), this.getY(), this.getZ(),
                12, 0.15, 0.15, 0.15, 0.0
            );
        }
    }
}
