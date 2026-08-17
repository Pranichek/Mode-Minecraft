package com.crimsonmoon;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    
    public static final SoundEvent HEARTBEAT = registerSoundEvent("heartbeat");

    private static SoundEvent registerSoundEvent(String name) {
        Identifier id = new Identifier("crimsonmoon", name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerSounds() {
        System.out.println("Реєстрація звуків для наашого моду");
    }
}