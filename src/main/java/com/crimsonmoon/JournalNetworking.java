package com.crimsonmoon;

import com.crimsonmoon.world.CrimsonMoonWorldState;
import com.crimsonmoon.world.ThorncallerBossState;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public class JournalNetworking {

    public static final Identifier REQUEST = new Identifier("crimsonmoon", "journal_request");
    public static final Identifier RESPONSE = new Identifier("crimsonmoon", "journal_response");

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST, (server, player, handler, buf, sender) -> {
            server.execute(() -> {
                CrimsonMoonWorldState state = CrimsonMoonWorldState.getServerState(server);
                ThorncallerBossState thorn = ThorncallerBossState.get(server.getOverworld());

                BossJournalData data = new BossJournalData();
                data.nightmareDefeated = state.nightmareDefeated;
                data.boxerDefeated = state.boxerDefeated;
                data.thorncallerDefeated = thorn.isDefeated();
                data.monsterikDefeated = state.monsterikDefeated;
                data.isCurseActive = state.isCurseActive;

                data.arenaPlaced = state.boxerArenaPlaced;
                data.arenaX = state.arenaX;
                data.arenaY = state.arenaY;
                data.arenaZ = state.arenaZ;

                data.monolitPlaced = state.monolitPlaced;
                data.monolitX = state.monolitX;
                data.monolitY = state.monolitY;
                data.monolitZ = state.monolitZ;

                PacketByteBuf out = PacketByteBufs.create();
                data.write(out);
                ServerPlayNetworking.send(player, RESPONSE, out);
            });
        });
    }
}