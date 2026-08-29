package com.crimsonmoon.entity;

import com.crimsonmoon.ModEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.joml.Vector3f;

public class BloodBoltEntity extends ThrownItemEntity {

    public static final float DAMAGE = 6.0f;
    private static final int MAX_AGE_TICKS = 40; 

    public BloodBoltEntity(EntityType<? extends BloodBoltEntity> entityType, World world) {
        super(entityType, world);
    }

    public BloodBoltEntity(World world, LivingEntity owner) {
        super(ModEntities.BLOOD_BOLT, owner, world);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.AIR;
    }

    @Override
    protected float getGravity() {
        return 0.0f;
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
                new DustParticleEffect(new Vector3f(0.55f, 0.02f, 0.02f), 0.9f),
                this.getX(), this.getY(), this.getZ(),
                2, 0.02, 0.02, 0.02, 0.0
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
                new DustParticleEffect(new Vector3f(0.55f, 0.02f, 0.02f), 1.6f),
                this.getX(), this.getY(), this.getZ(),
                15, 0.15, 0.15, 0.15, 0.0
            );
        }
    }
}