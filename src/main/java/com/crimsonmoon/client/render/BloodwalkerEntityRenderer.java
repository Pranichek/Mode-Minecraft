package com.crimsonmoon.client.render;

import com.crimsonmoon.CrimsonMoon;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.util.Identifier;

/**
 * Рендерер Bloodwalker: переиспользует модель и анимации ZombieEntityRenderer,
 * но подставляет собственную текстуру.
 *
 * Текстуру нужно положить в:
 * src/main/resources/assets/crimsonmoon/textures/entity/bloodwalker/bloodwalker.png
 * (размер как у обычной зомби-текстуры — 64x64)
 */
public class BloodwalkerEntityRenderer extends ZombieEntityRenderer {

    private static final Identifier TEXTURE =
        Identifier.of(CrimsonMoon.MOD_ID, "textures/entity/bloodwalker/bloodwalker.png");

    public BloodwalkerEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(ZombieEntity entity) {
        return TEXTURE;
    }
}
