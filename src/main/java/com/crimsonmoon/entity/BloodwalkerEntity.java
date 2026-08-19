package com.crimsonmoon.entity; 

import com.crimsonmoon.item.CrimsonShieldItem;
import net.minecraft.entity.EntityType; 
import net.minecraft.entity.ai.goal.*; 
import net.minecraft.entity.attribute.DefaultAttributeContainer; 
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity; 
import net.minecraft.entity.mob.ZombieEntity; 
import net.minecraft.entity.player.PlayerEntity; 
import net.minecraft.particle.ParticleTypes; 
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory; 
import net.minecraft.sound.SoundEvents; 
import net.minecraft.world.World;

import java.util.List;
public class BloodwalkerEntity extends ZombieEntity {
    private int lastWaveTime = 0;
    private boolean isWaveActive = false;
    private int waveProgress = 0;
    
    
    public BloodwalkerEntity(EntityType<? extends ZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    // Характеристики моба — как у зомби, но чуть быстрее и злее
    public static DefaultAttributeContainer.Builder createBloodwalkerAttributes() {
        return HostileEntity.createHostileAttributes()
            .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS)
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 24.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.28)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
            .add(EntityAttributes.GENERIC_ARMOR, 2.0);
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    // Поведение: атака в ближнем бою + обычное блуждание
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(6, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(7, new LookAtEntityGoal(this, net.minecraft.entity.player.PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, net.minecraft.entity.player.PlayerEntity.class, true));
    }

    
    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient()) {
            
            if (isWaveActive) {
                waveProgress++;
                double currentRadius = waveProgress * 0.5;
                ServerWorld world = (ServerWorld) this.getWorld();

                for (int i = 0; i < 360; i += 10) {
                    double angle = i * Math.PI / 180.0;
                    double x = this.getX() + Math.cos(angle) * currentRadius;
                    double z = this.getZ() + Math.sin(angle) * currentRadius;

                    world.spawnParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, this.getY() + 0.1, z, 1, 0, 0, 0, 0);
                    world.spawnParticles(ParticleTypes.LARGE_SMOKE, x, this.getY() + 0.2, z, 1, 0, 0, 0, 0);
                }

                if(waveProgress >= 10){
                    
                    dealShockWaveDamage();
                    isWaveActive = false;
                    waveProgress = 0;
                }
                
            }
            else {
                if (this.getTarget() != null && this.distanceTo(this.getTarget()) < 6.0) {
                    lastWaveTime++;
                    if (lastWaveTime >= 100) {
                        startShockWawe();
                        lastWaveTime = 0;
                    }
                }
                else {
                    lastWaveTime = Math.max(0, lastWaveTime - 1);
                }
            }
        }
    }

    
    private void startShockWawe(){
        ServerWorld world = (ServerWorld) this.getWorld();
        world.playSound(
            null,
            this.getBlockPos(),
            SoundEvents.ENTITY_GENERIC_EXPLODE,
            SoundCategory.HOSTILE,
            1.0f, 0.5f
        );
        isWaveActive = true;
        waveProgress = 0;
    }

    private void dealShockWaveDamage(){
        ServerWorld world = (ServerWorld) this.getWorld();
        List<PlayerEntity> players = world.getEntitiesByClass(PlayerEntity.class, this.getBoundingBox().expand(5.0), entity -> true);

        for (PlayerEntity player : players){
            boolean isUsingCrimsonShield = player.isBlocking() && player.getActiveItem().getItem() instanceof CrimsonShieldItem;

            if (isUsingCrimsonShield){
                world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.0f, 0.8f);
                player.disableShield(true);
            }else{
                player.damage(this.getDamageSources().mobAttack(this), 10.0f);

                double dX = player.getX() - this.getX();
                double dZ = player.getZ() - this.getZ();
                player.takeKnockback(1.5, dX, dZ);

                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1, false, false, true));
            }
        }
    }
    
    // Убираем зомби-звуки (эмбиент-стон, звук урона, звук смерти), шаги оставляем как есть
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