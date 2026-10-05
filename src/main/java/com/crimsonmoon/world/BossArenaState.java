package com.crimsonmoon.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.PersistentState;

/**
 * Флаг "структура арены уже размещена" + закэшированный размер структуры для
 * измерения боссарены. Персистентный (переживает перезапуск сервера) — чтобы
 * не пытаться разместить структуру заново при каждом заходе игрока и не
 * перечитывать сам файл структуры (он большой) ради одного только размера.
 */
public class BossArenaState extends PersistentState {

    private static final String STATE_ID = "crimsonmoon_boss_arena";

    private boolean placed = false;
    private int sizeX;
    private int sizeY;
    private int sizeZ;

    public static BossArenaState get(ServerWorld arenaWorld) {
        return arenaWorld.getPersistentStateManager()
            .getOrCreate(BossArenaState::fromNbt, BossArenaState::new, STATE_ID);
    }

    public boolean isPlaced() {
        return placed;
    }

    public Vec3i getSize() {
        return new Vec3i(sizeX, sizeY, sizeZ);
    }

    public void markPlaced(Vec3i size) {
        this.placed = true;
        this.sizeX = size.getX();
        this.sizeY = size.getY();
        this.sizeZ = size.getZ();
        this.markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("Placed", placed);
        nbt.putInt("SizeX", sizeX);
        nbt.putInt("SizeY", sizeY);
        nbt.putInt("SizeZ", sizeZ);
        return nbt;
    }

    public static BossArenaState fromNbt(NbtCompound nbt) {
        BossArenaState state = new BossArenaState();
        state.placed = nbt.getBoolean("Placed");
        state.sizeX = nbt.getInt("SizeX");
        state.sizeY = nbt.getInt("SizeY");
        state.sizeZ = nbt.getInt("SizeZ");
        return state;
    }
}
