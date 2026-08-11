package com.crimsonmoon.client.gui;

import com.crimsonmoon.entity.BloodwalkerEntity;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.List;


public class FearSoundSystem {
    private static final double DETECTION_RADIUS = 15.0;

    private static int tickCounter = 0;
    private static int tickDelay = 40;
    
}
