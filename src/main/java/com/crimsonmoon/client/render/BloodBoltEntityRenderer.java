package com.crimsonmoon.client.render;

import com.crimsonmoon.entity.BloodBoltEntity;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;

public class BloodBoltEntityRenderer extends EntityRenderer<BloodBoltEntity> {

    public BloodBoltEntityRenderer(EntityRendererFactory.Context context) {
        super(context);
    }

    @Override
    public Identifier getTexture(BloodBoltEntity entity) {
        return new Identifier("minecraft", "textures/item/snowball.png"); // не використовується, рендер порожній
    }

    // render() навмисно не перевизначаємо — сутність невидима,
    // весь візуал іде через частки в BloodBoltEntity.tick()
}