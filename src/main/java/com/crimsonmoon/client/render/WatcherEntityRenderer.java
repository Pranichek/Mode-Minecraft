package com.crimsonmoon.client.render;

import com.crimsonmoon.CrimsonMoon;
import com.crimsonmoon.entity.WatcherEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class WatcherEntityRenderer extends MobEntityRenderer<WatcherEntity, BipedEntityModel<WatcherEntity>> {

    private static final Identifier TEXTURE =
        Identifier.of(CrimsonMoon.MOD_ID, "textures/entity/watcher/watcher.png");

    public WatcherEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BipedEntityModel<>(context.getPart(EntityModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public Identifier getTexture(WatcherEntity entity) {
        return TEXTURE;
    }
}