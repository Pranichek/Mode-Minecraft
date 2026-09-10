package com.crimsonmoon.entity;

import java.util.UUID;

import com.crimsonmoon.ModSounds;
import com.crimsonmoon.world.CrimsonMoonWorldState; // ДОБАВЛЕН ИМПОРТ СОСТОЯНИЯ

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.world.World;

public class WatcherEntity extends PathAwareEntity {

    private static final int LIFETIME_TICKS = 600;       // 30 секунд
    private static final double DESPAWN_DISTANCE = 6.5;  // блоков до игрока

    private int ageTicks = 0;
    private UUID ownerUuid;

    public WatcherEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
    }

    public void setOwner(ServerPlayerEntity player) {
        this.ownerUuid = player.getUuid();
    }

    public static DefaultAttributeContainer.Builder createWatcherAttributes() {
        return PathAwareEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 1.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new LookAtEntityGoal(this, PlayerEntity.class, 24.0f, 1.0f));
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true; 
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getWorld().isClient()) {
            return;
        }

        // --- ПРЕДОХРАНИТЕЛЬ ПРОКЛЯТЬЯ ---
        if (this.getServer() != null) {
            CrimsonMoonWorldState state = CrimsonMoonWorldState.getServerState(this.getServer());
            if (!state.isCurseActive) {
                this.discard(); // Мгновенно удаляем, если монолит не сломан
                return;
            }
        }
        // --------------------------------

        this.ageTicks++;

        boolean playerTooClose = this.getWorld().getClosestPlayer(this, DESPAWN_DISTANCE) != null;

        if (playerTooClose || this.ageTicks >= LIFETIME_TICKS) {
            stopMusicForOwner();
            this.discard(); 
        }
    }

    private void stopMusicForOwner() {
        if (ownerUuid == null || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return;
        }

        ServerPlayerEntity owner = serverWorld.getServer().getPlayerManager().getPlayer(ownerUuid);
        if (owner != null) {
            owner.networkHandler.sendPacket(
                new StopSoundS2CPacket(ModSounds.WATCHER_THEME.getId(), SoundCategory.AMBIENT)
            );
        }
    }
}