package com.crimsonmoon.client.render;

import com.crimsonmoon.CrimsonMoon;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;


// src/main/resources/assets/crimsonmoon/textures/entity/thorncaller/thorncaller.png (64x64)

public class ThorncallerEntityRenderer extends ZombieEntityRenderer {

    private static final Identifier TEXTURE =
        Identifier.of(CrimsonMoon.MOD_ID, "textures/entity/thorncaller/thorncaller.png");

    public ThorncallerEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(ZombieEntity entity) {
        return TEXTURE;
    }
}