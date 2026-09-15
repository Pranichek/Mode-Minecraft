package com.crimsonmoon.world;

import com.crimsonmoon.ModItems;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Выдаёт ключ от портала, когда убиты все три пред-босса, и возвращает ключ
 * игроку после смерти (чтобы не терялся).
 */
public class PortalKeyDrops {

    private static final String GOT_KEY_TAG = "crimsonmoon_got_portal_key";

    public static void register() {
        // выдаём ключ, когда все три босса повержены (один раз на игрока)
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % 40 != 0) return;

            CrimsonMoonWorldState state = CrimsonMoonWorldState.getServerState(server);
            ThorncallerBossState thorn = ThorncallerBossState.get(server.getOverworld());
            boolean allThree = state.nightmareDefeated && state.boxerDefeated && thorn.isDefeated();
            if (!allThree) return;

            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (p.getCommandTags().contains(GOT_KEY_TAG)) continue;
                if (!p.getInventory().contains(new ItemStack(ModItems.PORTAL_KEY))) {
                    p.getInventory().insertStack(new ItemStack(ModItems.PORTAL_KEY));
                }
                p.addCommandTag(GOT_KEY_TAG);
            }
        });

        // после смерти возвращаем ключ (если он был), чтобы не потерялся
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            if (oldPlayer.getCommandTags().contains(GOT_KEY_TAG)) {
                boolean hasKey = newPlayer.getInventory().contains(new ItemStack(ModItems.PORTAL_KEY));
                if (!hasKey) {
                    newPlayer.getInventory().insertStack(new ItemStack(ModItems.PORTAL_KEY));
                }
                newPlayer.addCommandTag(GOT_KEY_TAG);
            }
        });
    }

    private PortalKeyDrops() {}
}