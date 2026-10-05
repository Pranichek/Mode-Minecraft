package com.crimsonmoon.world;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

/**
 * Персистентный флаг "разового босса": Thorncaller побеждён окончательно.
 * Хранится в данных мира (привязан к Overworld), переживает перезапуск
 * сервера — в отличие от статических полей в памяти.
 *
 * defeated       — финальный флаг: если true, Thorncaller больше НИКОГДА не
 *                   заспавнится (ни на этот мир/сохранение).
 * awaitingLoot   — true в промежутке между убийством и подбором лута: пока
 *                   он true, новый Thorncaller тоже не спавнится, но это ещё
 *                   не финал — если лут так и не подобрали (например, сервер
 *                   перезапустили), при следующем запуске сервера состояние
 *                   всё ещё "ждём подбора", а не "готов спавниться снова".
 */
public class ThorncallerBossState extends PersistentState {

    private static final String STATE_ID = "crimsonmoon_thorncaller";

    private boolean defeated = false;
    private boolean awaitingLoot = false;

    public static ThorncallerBossState get(ServerWorld anyWorld) {
        ServerWorld overworld = anyWorld.getServer().getOverworld();
        return overworld.getPersistentStateManager()
            .getOrCreate(ThorncallerBossState::fromNbt, ThorncallerBossState::new, STATE_ID);
    }

    public boolean isDefeated() {
        return defeated;
    }

    public boolean isAwaitingLoot() {
        return awaitingLoot;
    }

    /** Босса только что убили — блокируем спавн до подбора лута. */
    public void markKilledAwaitingLoot() {
        this.awaitingLoot = true;
        this.markDirty();
    }

    /** Лут подобран — теперь это окончательно и навсегда. */
    public void markLootCollected() {
        this.awaitingLoot = false;
        this.defeated = true;
        this.markDirty();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putBoolean("Defeated", defeated);
        nbt.putBoolean("AwaitingLoot", awaitingLoot);
        return nbt;
    }

    public static ThorncallerBossState fromNbt(NbtCompound nbt) {
        ThorncallerBossState state = new ThorncallerBossState();
        state.defeated = nbt.getBoolean("Defeated");
        state.awaitingLoot = nbt.getBoolean("AwaitingLoot");
        return state;
    }

    public void setDefeated(boolean value) {
        this.defeated = value; // Если твоя переменная называется isDefeated, напиши this.isDefeated = value;
        this.markDirty();      // Сохраняем изменения на сервере
    }
}
