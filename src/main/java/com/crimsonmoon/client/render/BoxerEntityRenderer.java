package com.crimsonmoon.client.render;

import com.crimsonmoon.entity.BoxerEntity;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.math.RotationAxis;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class BoxerEntityRenderer extends GeoEntityRenderer<BoxerEntity> {

    public BoxerEntityRenderer(EntityRendererFactory.Context context) {
        super(context, new BoxerModel());
        this.shadowRadius = 0.8f;
    }

    @Override
    public void render(BoxerEntity entity, float entityYaw, float partialTick,
                       MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        poseStack.push();
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180));
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.pop();
    }
}