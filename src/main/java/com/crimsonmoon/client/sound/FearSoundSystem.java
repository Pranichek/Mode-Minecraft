package com.crimsonmoon.client.sound;

import com.crimsonmoon.ModSounds;
import com.crimsonmoon.entity.BloodwalkerEntity;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Box;

import java.util.List;

public class FearSoundSystem {
    private static final double DETECTION_RADIUS = 15.0;
    
    private static int tickCounter = 0;
    private static int nextBeatDelay = 60;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.isPaused()) return;

            if (client.player == null || client.world == null) return;

            Box searchBox = client.player.getBoundingBox().expand(DETECTION_RADIUS);
            List<BloodwalkerEntity> nearbyMobs = client.world.getEntitiesByClass(BloodwalkerEntity.class, searchBox, entity -> true);

            if (!nearbyMobs.isEmpty()) {
                double closestDistance = DETECTION_RADIUS;
                for (BloodwalkerEntity mob : nearbyMobs) {
                    double dist = client.player.distanceTo(mob);
                    if (dist < closestDistance) {
                        closestDistance = dist;
                    }
                }

                float fearProcentage = (float) (1.0 - (closestDistance / DETECTION_RADIUS));

                tickCounter++;
                
                if (tickCounter >= nextBeatDelay) {
                    tickCounter = 0; 

                    nextBeatDelay = (int) (60 - (fearProcentage * 35));
                    if (nextBeatDelay < 25) nextBeatDelay = 25; 

                    float volume = 0.5f + (fearProcentage * 1.5f); 
                    float pitch = 0.8f + (fearProcentage * 0.4f); 

                    client.player.playSound(ModSounds.HEARTBEAT, SoundCategory.AMBIENT, volume, pitch);
                }
            } else {
                tickCounter = 0;
                nextBeatDelay = 60;
            }
        });
    }
}