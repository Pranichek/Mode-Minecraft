package com.crimsonmoon.item;

import com.crimsonmoon.world.BossArenaManager;
import com.crimsonmoon.world.CrimsonMoonWorldState;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class PortalKeyItem extends Item {

    public PortalKeyItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            CrimsonMoonWorldState state = CrimsonMoonWorldState.getServerState(serverWorld.getServer());

            if (!state.portalPlaced) {
                user.sendMessage(Text.literal("Портал ще не з'явився. Здолай трьох стражів!")
                    .formatted(Formatting.GRAY), true);
                user.getItemCooldownManager().set(this, 40);
                return TypedActionResult.fail(user.getStackInHand(hand));
            }

            Vec3d playerPos = user.getPos();
            Vec3d portalPos = new Vec3d(state.portalX, state.portalY, state.portalZ);
            double dist = playerPos.distanceTo(portalPos);

            if (dist <= 6.0) {
                // рядом с порталом — телепорт к финальному боссу
                if (user instanceof ServerPlayerEntity sp) {
                    world.playSound(null, user.getBlockPos(),
                        SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1f, 1f);
                    BossArenaManager.enter(sp);
                }
            } else {
                // далеко — рывок в сторону портала (навигация)
                Vec3d dir = portalPos.subtract(playerPos).normalize();
                user.setVelocity(dir.x * 2.5, 0.4, dir.z * 2.5);
                user.velocityModified = true;
                world.playSound(null, user.getBlockPos(),
                    SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1f);
                user.sendMessage(Text.literal("Ключ тягне тебе до порталу...")
                    .formatted(Formatting.DARK_PURPLE), true);
            }
        }

        user.getItemCooldownManager().set(this, 40);
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public void appendTooltip(ItemStack stack, World world,
                              java.util.List<Text> tooltip,
                              net.minecraft.client.item.TooltipContext context) {
        tooltip.add(Text.literal("Ключ від Кривавого Порталу")
            .formatted(Formatting.DARK_RED, Formatting.BOLD));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("ПКМ вдалині — тягне до порталу")
            .formatted(Formatting.GRAY));
        tooltip.add(Text.literal("ПКМ біля порталу — переносить до боса")
            .formatted(Formatting.GRAY));
        tooltip.add(Text.empty());
        tooltip.add(Text.literal("Не зникає після смерті")
            .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
    }
}