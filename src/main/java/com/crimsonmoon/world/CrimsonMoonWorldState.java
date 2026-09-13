package com.crimsonmoon.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.LivingEntity;
import com.crimsonmoon.ModItems;

public class CrimsonMoonWorldState extends PersistentState {

    public boolean isCurseActive = false;
    public boolean nightmareSpawned = false;
    public boolean nightmareDefeated = false;
    
    public boolean boxerArenaPlaced = false;
    public boolean boxerSpawned = false;
    public boolean boxerDefeated = false;
    public int arenaX = 0;
    public int arenaY = 0;
    public int arenaZ = 0;

    public boolean monolitPlaced = false;
    public int monolitX = 0;
    public int monolitY = 0;
    public int monolitZ = 0;

    public boolean monsterikDefeated = false;
    public boolean portalKeyDropped = false;

    // Змінні для порталу
    public boolean portalPlaced = false;
    public int portalX = 0;
    public int portalY = 0;
    public int portalZ = 0;

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("isCurseActive", isCurseActive);
        nbt.putBoolean("nightmareSpawned", nightmareSpawned);
        nbt.putBoolean("nightmareDefeated", nightmareDefeated);
        nbt.putBoolean("boxerArenaPlaced", boxerArenaPlaced);
        nbt.putBoolean("boxerSpawned", boxerSpawned);
        nbt.putBoolean("boxerDefeated", boxerDefeated);
        nbt.putInt("arenaX", arenaX);
        nbt.putInt("arenaY", arenaY);
        nbt.putInt("arenaZ", arenaZ);
        nbt.putBoolean("monolitPlaced", monolitPlaced);
        nbt.putInt("monolitX", monolitX);
        nbt.putInt("monolitY", monolitY);
        nbt.putInt("monolitZ", monolitZ);
        nbt.putBoolean("monsterikDefeated", monsterikDefeated);
        nbt.putBoolean("portalKeyDropped", portalKeyDropped);
        
        nbt.putBoolean("portalPlaced", portalPlaced);
        nbt.putInt("portalX", portalX);
        nbt.putInt("portalY", portalY);
        nbt.putInt("portalZ", portalZ);
        return nbt;
    }

    public static CrimsonMoonWorldState createFromNbt(NbtCompound tag) {
        CrimsonMoonWorldState state = new CrimsonMoonWorldState();
        state.isCurseActive = tag.getBoolean("isCurseActive");
        state.nightmareSpawned = tag.getBoolean("nightmareSpawned");
        state.nightmareDefeated = tag.getBoolean("nightmareDefeated");
        state.boxerArenaPlaced = tag.getBoolean("boxerArenaPlaced");
        state.boxerSpawned = tag.getBoolean("boxerSpawned");
        state.boxerDefeated = tag.getBoolean("boxerDefeated");
        state.arenaX = tag.getInt("arenaX");
        state.arenaY = tag.getInt("arenaY");
        state.arenaZ = tag.getInt("arenaZ");
        state.monolitPlaced = tag.getBoolean("monolitPlaced");
        state.monolitX = tag.getInt("monolitX");
        state.monolitY = tag.getInt("monolitY");
        state.monolitZ = tag.getInt("monolitZ");
        state.monsterikDefeated = tag.getBoolean("monsterikDefeated");
        state.portalKeyDropped = tag.getBoolean("portalKeyDropped");
        
        state.portalPlaced = tag.getBoolean("portalPlaced");
        state.portalX = tag.getInt("portalX");
        state.portalY = tag.getInt("portalY");
        state.portalZ = tag.getInt("portalZ");
        return state;
    }

    public static CrimsonMoonWorldState getServerState(MinecraftServer server) {
        PersistentStateManager manager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return manager.getOrCreate(
                CrimsonMoonWorldState::createFromNbt,
                CrimsonMoonWorldState::new,
                "crimsonmoon_world_state"
        );
    }

    public static void checkAndDropKey(ServerWorld world, LivingEntity boss) {
        CrimsonMoonWorldState state = getServerState(world.getServer());
        ThorncallerBossState thornState = ThorncallerBossState.get(world.getServer().getOverworld());

        if (state.boxerDefeated && state.nightmareDefeated && thornState.isDefeated()) {
            if (!state.portalKeyDropped) {
                boss.dropItem(ModItems.PORTAL_KEY);
                state.portalKeyDropped = true;
                state.markDirty();
            }
        }
    }
}