package com.crimsonmoon;

import net.minecraft.network.PacketByteBuf;

public class BossJournalData {
    public boolean nightmareDefeated;
    public boolean boxerDefeated;
    public boolean thorncallerDefeated;
    public boolean monsterikDefeated;
    public boolean isCurseActive;

    public boolean arenaPlaced;
    public int arenaX, arenaY, arenaZ;

    public boolean monolitPlaced;
    public int monolitX, monolitY, monolitZ;

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(nightmareDefeated);
        buf.writeBoolean(boxerDefeated);
        buf.writeBoolean(thorncallerDefeated);
        buf.writeBoolean(monsterikDefeated);
        buf.writeBoolean(isCurseActive);
        buf.writeBoolean(arenaPlaced);
        buf.writeInt(arenaX);
        buf.writeInt(arenaY);
        buf.writeInt(arenaZ);
        buf.writeBoolean(monolitPlaced);
        buf.writeInt(monolitX);
        buf.writeInt(monolitY);
        buf.writeInt(monolitZ);
    }

    public static BossJournalData read(PacketByteBuf buf) {
        BossJournalData d = new BossJournalData();
        d.nightmareDefeated = buf.readBoolean();
        d.boxerDefeated = buf.readBoolean();
        d.thorncallerDefeated = buf.readBoolean();
        d.monsterikDefeated = buf.readBoolean();
        d.isCurseActive = buf.readBoolean();
        d.arenaPlaced = buf.readBoolean();
        d.arenaX = buf.readInt();
        d.arenaY = buf.readInt();
        d.arenaZ = buf.readInt();
        d.monolitPlaced = buf.readBoolean();
        d.monolitX = buf.readInt();
        d.monolitY = buf.readInt();
        d.monolitZ = buf.readInt();
        return d;
    }
}