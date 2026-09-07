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

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("isCurseActive", isCurseActive);
        nbt.putBoolean("nightmareSpawned", nightmareSpawned);
        nbt.putBoolean("nightmareDefeated", nightmareDefeated);
        return nbt;
    }

    public static CrimsonMoonWorldState createFromNbt(NbtCompound tag) {
        CrimsonMoonWorldState state = new CrimsonMoonWorldState();
        state.isCurseActive = tag.getBoolean("isCurseActive");
        state.nightmareSpawned = tag.getBoolean("nightmareSpawned");
        state.nightmareDefeated = tag.getBoolean("nightmareDefeated");
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