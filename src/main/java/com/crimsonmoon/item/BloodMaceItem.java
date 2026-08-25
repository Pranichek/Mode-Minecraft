package com.crimsonmoon.item;

import com.crimsonmoon.ModToolMaterials;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.List;

/**
 * Кривава булава.
 *
 * ЛКМ — звичайна ближня атака (важча за меч, але повільніша), із додатковими
 * частками/звуком при влучанні, щоб удар було добре видно.
 *
 * Shift + ПКМ — ударна хвиля навколо гравця, яка відкидає та трохи шкодить
 * ворогам поруч. Має перезарядку не менше 7 секунд.
 */
public class BloodMaceItem extends SwordItem {

    private static final int SHOCKWAVE_COOLDOWN_TICKS = 7 * 20; // 7с — мінімальна перезарядка

    private static final float SHOCKWAVE_DAMAGE = 8.0f;
    private static final double SHOCKWAVE_RADIUS = 4.5;
    private static final double SHOCKWAVE_KNOCKBACK = 2.2;
    private static final double SHOCKWAVE_UP_KNOCKBACK = 0.6;

    public BloodMaceItem(Settings settings) {
        // важка й повільна зброя: більше шкоди за удар, менша швидкість атаки за меч
        super(ModToolMaterials.BLOOD, 7, -3.2f, settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!user.isSneaking()) {
            return TypedActionResult.pass(stack);
        }

        if (user.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.pass(stack);
        }

        shockwave(world, user);
        user.getItemCooldownManager().set(this, SHOCKWAVE_COOLDOWN_TICKS);
        user.swingHand(hand);

        return TypedActionResult.success(stack, world.isClient());
    }

    // ==== Ефект при звичайному ударі (ЛКМ) ====
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.getWorld().isClient && target.getWorld() instanceof ServerWorld serverWorld) {
            double midY = target.getY() + target.getHeight() * 0.5;

            serverWorld.spawnParticles(
                new DustParticleEffect(new Vector3f(0.55f, 0.02f, 0.02f), 1.7f),
                target.getX(), midY, target.getZ(),
                16, 0.3, 0.3, 0.3, 0.01
            );
            serverWorld.spawnParticles(
                ParticleTypes.CRIT,
                target.getX(), midY, target.getZ(),
                6, 0.25, 0.25, 0.25, 0.05
            );

            serverWorld.playSound(null, target.getBlockPos(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG,
                SoundCategory.PLAYERS, 1.0f, 0.8f);
        }

        return super.postHit(stack, target, attacker);
    }

    // ==== Shift + ПКМ — ударна хвиля ====
    private void shockwave(World world, PlayerEntity user) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
            return;
        }

        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND,
            SoundCategory.PLAYERS, 0.7f, 0.6f);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_RAVAGER_STUNNED,
            SoundCategory.PLAYERS, 1.0f, 0.5f);

        double cx = user.getX();
        double cy = user.getY() + 0.1;
        double cz = user.getZ();

        // кільця часток, що розходяться від гравця — власне "видима" хвиля
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.0 + ring * 1.6;
            int points = (int) (radius * 12);
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i;
                double px = cx + Math.cos(angle) * radius;
                double pz = cz + Math.sin(angle) * radius;
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.55f, 0.02f, 0.02f), 1.5f),
                    px, cy, pz,
                    1, 0.0, 0.05, 0.0, 0.0
                );
            }
        }
        serverWorld.spawnParticles(ParticleTypes.SONIC_BOOM, cx, cy + 1.0, cz, 1, 0.0, 0.0, 0.0, 0.0);
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION, cx, cy + 0.2, cz, 1, 0.0, 0.0, 0.0, 0.0);

        Box area = new Box(
            cx - SHOCKWAVE_RADIUS, cy - 2.0, cz - SHOCKWAVE_RADIUS,
            cx + SHOCKWAVE_RADIUS, cy + 3.0, cz + SHOCKWAVE_RADIUS
        );

        List<LivingEntity> targets = world.getEntitiesByClass(
            LivingEntity.class,
            area,
            e -> e != user && e.isAlive() && e.squaredDistanceTo(user) <= SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS
        );

        DamageSource damageSource = world.getDamageSources().playerAttack(user);

        for (LivingEntity target : targets) {
            target.damage(damageSource, SHOCKWAVE_DAMAGE);

            Vec3d push = target.getPos().subtract(user.getPos());
            double dist = Math.max(push.horizontalLength(), 0.1);
            double dx = (push.x / dist) * SHOCKWAVE_KNOCKBACK;
            double dz = (push.z / dist) * SHOCKWAVE_KNOCKBACK;

            target.setVelocity(target.getVelocity().add(dx, SHOCKWAVE_UP_KNOCKBACK, dz));
            target.velocityModified = true;
        }
    }
}
