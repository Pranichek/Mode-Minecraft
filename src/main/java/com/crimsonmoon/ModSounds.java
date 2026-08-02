package com.crimsonmoon;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class ModSounds {
    public static final SoundEvent WATCHER_THEME = register("watcher_theme");

    private static SoundEvent register(String name) {
        Identifier id = CrimsonMoon.id(name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void registerModSounds() {
        // обращение к классу инициализирует static-поля выше и регистрирует звук
    }
}