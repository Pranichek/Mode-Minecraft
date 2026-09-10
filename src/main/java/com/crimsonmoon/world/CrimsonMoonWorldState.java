package com.crimsonmoon.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;

public class CrimsonMoonWorldState extends PersistentState {
    public boolean isCurseActive = false;
    public boolean nightmareSpawned = false;
    public boolean nightmareDefeated = false;

    // --- Колизей боксёра ---
    public boolean boxerArenaPlaced = false;
    public boolean boxerSpawned = false;
    public boolean boxerDefeated = false;
    public int arenaX = 0;
    public int arenaY = 0;
    public int arenaZ = 0;

    // --- Thorncaller ---
    public boolean thorncallerSpawned = false;

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
        nbt.putBoolean("thorncallerSpawned", thorncallerSpawned);
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
        state.thorncallerSpawned = tag.getBoolean("thorncallerSpawned");
        return state;
    }

    public static CrimsonMoonWorldState getServerState(MinecraftServer server) {
        PersistentStateManager stateManager = server.getWorld(World.OVERWORLD).getPersistentStateManager();
        return stateManager.getOrCreate(
            CrimsonMoonWorldState::createFromNbt,
            CrimsonMoonWorldState::new,
            "crimsonmoon_state"
        );
    }
}