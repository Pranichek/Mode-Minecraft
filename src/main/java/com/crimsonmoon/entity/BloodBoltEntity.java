package com.crimsonmoon.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import com.crimsonmoon.ModEntities;

public class BloodBoltEntity extends PersistentProjectileEntity {

    private static final float DAMAGE = 12.0f;
    private static final int MAX_LIFE_TICKS = 60; // 3с, потім зникає

    // кольори (RGB 0..1)
    private static final Vector3f TRAIL_COLOR = new Vector3f(0.5f, 0.02f, 0.02f);  // темно-червоний слід
    private static final Vector3f CORE_COLOR  = new Vector3f(0.95f, 0.15f, 0.15f); // яскраве ядро

    private int ticksAlive = 0;

    public BloodBoltEntity(EntityType<? extends BloodBoltEntity> type, World world) {
        super(type, world);
        init();
    }

    public BloodBoltEntity(World world, LivingEntity owner) {
        super(ModEntities.BLOOD_BOLT, world);
        this.setOwner(owner);
        this.setPosition(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
        init();
    }

    private void init() {
        this.setNoGravity(true); // летить прямо, без падіння
        this.pickupType = PickupPermission.DISALLOWED;
        this.setDamage(DAMAGE);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld() instanceof ServerWorld serverWorld) {
            spawnTrail(serverWorld);
        }

        if (++ticksAlive > MAX_LIFE_TICKS) {
            this.discard();
        }
    }

    private void spawnTrail(ServerWorld world) {
        Vec3d pos  = this.getPos();
        Vec3d prev = new Vec3d(this.prevX, this.prevY, this.prevZ);
        Vec3d step = pos.subtract(prev);

        // щільна червона лінія вздовж траєкторії — чітко видно шлях
        int count = Math.max(1, (int) Math.ceil(step.length() * 5));
        for (int i = 0; i < count; i++) {
            double t = i / (double) count;
            world.spawnParticles(
                new DustParticleEffect(TRAIL_COLOR, 1.1f),
                prev.x + step.x * t,
                prev.y + step.y * t,
                prev.z + step.z * t,
                1, 0.0, 0.0, 0.0, 0.0
            );
        }

        // яскраве пульсуюче "ядро" (без диму)
        world.spawnParticles(
            new DustColorTransitionParticleEffect(CORE_COLOR, TRAIL_COLOR, 1.6f),
            pos.x, pos.y, pos.z, 2, 0.03, 0.03, 0.03, 0.0);
    }

    @Override
    protected void onEntityHit(EntityHitResult result) {
        if (!this.getWorld().isClient && result.getEntity() instanceof LivingEntity living) {
            living.damage(
                this.getWorld().getDamageSources().indirectMagic(this, this.getOwner()),
                DAMAGE
            );
        }
        if (!this.getWorld().isClient) {
            impactBurst();
            this.discard();
        }
    }

    @Override
    protected void onBlockHit(BlockHitResult result) {
        super.onBlockHit(result);
        if (!this.getWorld().isClient) {
            impactBurst();
            this.discard();
        }
    }

    private void impactBurst() {
        if (!(this.getWorld() instanceof ServerWorld world)) {
            return;
        }
        Vec3d pos = this.getPos();

        world.playSound(null, this.getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE,
            SoundCategory.PLAYERS, 0.5f, 1.4f);

        // яскравий сплеск при попаданні, без диму
        world.spawnParticles(
            new DustParticleEffect(CORE_COLOR, 1.8f),
            pos.x, pos.y, pos.z, 20, 0.2, 0.2, 0.2, 0.0);
        world.spawnParticles(
            ParticleTypes.EXPLOSION, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    @Override
    protected ItemStack asItemStack() {
        return ItemStack.EMPTY; // снаряд не підбирається
    }
}