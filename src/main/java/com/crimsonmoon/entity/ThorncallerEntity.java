package com.crimsonmoon.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
    Тип должен быть MobEntity (подходят Bloodwalker, Nightmare и т.д.),
 */
public class ThorncallerEntity extends ZombieEntity {

    // === Настройка призыва подкрепления ===
    public static EntityType<? extends MobEntity> MINION_TYPE = EntityType.ZOMBIE;

    private static final String MINION_TAG = "thorncaller_minion";
    private static final int SUMMON_COOLDOWN_TICKS = 15 * 20; // раз в 15 секунд
    private static final int MINIONS_PER_CAST = 2;
    private static final int MAX_ALIVE_MINIONS = 6;   // сколько живых миньонов допускается рядом
    private static final double SUMMON_SEARCH_RADIUS = 3.0;
    private static final double MINION_TRACK_RADIUS = 24.0;

    private int summonCooldown = 60; // небольшая пауза перед первым призывом после спавна

    public ThorncallerEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    public static DefaultAttributeContainer.Builder createThorncallerAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS)
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
            .add(EntityAttributes.GENERIC_ARMOR, 1.0)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0);
    }

    @Override
    protected boolean isAffectedByDaylight() {
        return false;
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient) {
            return;
        }

        if (this.summonCooldown > 0) {
            this.summonCooldown--;
            return;
        }

        if (this.getTarget() == null) {
            return;
        }

        summonMinions();
        this.summonCooldown = SUMMON_COOLDOWN_TICKS;
    }

    private void summonMinions() {
        ServerWorld world = (ServerWorld) this.getWorld();

        long aliveMinionsNearby = world.getEntitiesByClass(
            MobEntity.class,
            this.getBoundingBox().expand(MINION_TRACK_RADIUS),
            e -> e.isAlive() && e.getCommandTags().contains(MINION_TAG)
        ).size();

        if (aliveMinionsNearby >= MAX_ALIVE_MINIONS) {
            return;
        }

        int toSpawn = (int) Math.min(MINIONS_PER_CAST, MAX_ALIVE_MINIONS - aliveMinionsNearby);

        for (int i = 0; i < toSpawn; i++) {
            spawnOneMinion(world);
        }
    }

    private void spawnOneMinion(ServerWorld world) {
        MobEntity minion = MINION_TYPE.create(world);
        if (minion == null) {
            return;
        }

        BlockPos pos = findMinionSpawnPos(world);

        minion.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, this.getYaw(), 0.0f);
        minion.getCommandTags().add(MINION_TAG);

        world.spawnEntity(minion);

        if (minion instanceof HostileEntity hostile && this.getTarget() != null) {
            hostile.setTarget(this.getTarget());
        }
    }

    private BlockPos findMinionSpawnPos(ServerWorld world) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = this.random.nextDouble() * Math.PI * 2;
            double dist = 1.5 + this.random.nextDouble() * SUMMON_SEARCH_RADIUS;

            int x = MathHelper.floor(this.getX() + Math.cos(angle) * dist);
            int z = MathHelper.floor(this.getZ() + Math.sin(angle) * dist);
            int y = MathHelper.floor(this.getY());

            BlockPos candidate = new BlockPos(x, y, z);
            if (world.getBlockState(candidate.down()).isSolidBlock(world, candidate.down())
                && world.isSpaceEmpty(minionBox(candidate))) {
                return candidate;
            }
        }
        // не нашли идеальное место — призываем прямо рядом с собой
        return this.getBlockPos();
    }

    private net.minecraft.util.math.Box minionBox(BlockPos pos) {
        return new net.minecraft.util.math.Box(
            pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1,
            pos.getX() + 0.9, pos.getY() + 1.9, pos.getZ() + 0.9
        );
    }

    @Override
    public net.minecraft.sound.SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    public net.minecraft.sound.SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) {
        return null;
    }

    @Override
    public net.minecraft.sound.SoundEvent getDeathSound() {
        return null;
    }
}