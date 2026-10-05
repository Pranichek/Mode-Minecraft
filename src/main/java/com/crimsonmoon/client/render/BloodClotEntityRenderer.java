package com.crimsonmoon.client.render;

import com.crimsonmoon.entity.BloodClotEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class BloodClotEntityRenderer extends EntityRenderer<BloodClotEntity> {

    public BloodClotEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(BloodClotEntity entity) {
        return new Identifier("minecraft", "textures/item/snowball.png"); // не використовується, рендер порожній
    }

    // render() навмисно не перевизначаємо — сутність невидима (без текстур),
    // весь візуал іде через частки в BloodClotEntity.tick()
}
