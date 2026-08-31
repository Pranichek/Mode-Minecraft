package com.crimsonmoon.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;

import com.crimsonmoon.entity.BloodBoltEntity;

import java.util.List;

public class BloodStaffItem extends Item {

    private static final int RANGED_COOLDOWN_TICKS = 20; // 1с
    private static final int SLAM_COOLDOWN_TICKS = 13 * 20;    // 13с

    private static final float SLAM_DAMAGE = 24.0f;
    private static final double SLAM_RADIUS = 10.0;
    private static final double SLAM_KNOCKBACK = 2.2;

    private static final String NBT_BOLT_READY_TICK = "BoltReadyTick";
    private static final String NBT_SLAM_READY_TICK = "SlamReadyTick";

    public BloodStaffItem(Settings settings) {
        super(settings);
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.translatable("item.crimsonmoon.blood_staf.tooltip_attack").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.crimsonmoon.blood_staf.tooltip_ability").formatted(Formatting.DARK_GRAY));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        long time = world.getTime();

        if (user.isSneaking()) {
            if (time < getReadyTick(stack, NBT_SLAM_READY_TICK)) {
                return TypedActionResult.pass(stack);
            }
            groundSlam(world, user);
            setReadyTick(stack, NBT_SLAM_READY_TICK, time + SLAM_COOLDOWN_TICKS);
        } else {
            if (time < getReadyTick(stack, NBT_BOLT_READY_TICK)) {
                return TypedActionResult.pass(stack);
            }
            shootBolt(world, user);
            setReadyTick(stack, NBT_BOLT_READY_TICK, time + RANGED_COOLDOWN_TICKS);
        }

        user.swingHand(hand);
        return TypedActionResult.success(stack, world.isClient());
    }

    private static long getReadyTick(ItemStack stack, String key) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null ? nbt.getLong(key) : 0L;
    }

    private static void setReadyTick(ItemStack stack, String key, long readyTick) {
        stack.getOrCreateNbt().putLong(key, readyTick);
    }

    // ==== 1. ПКМ - выстрел====
    private void shootBolt(World world, PlayerEntity user) {
        if (world.isClient) {
            return;
        }

        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_EVOKER_CAST_SPELL,
            SoundCategory.PLAYERS, 1.0f, 0.7f);

        BloodBoltEntity bolt = new BloodBoltEntity(world, user);
        bolt.setPosition(user.getX(), user.getEyeY() - 0.1, user.getZ());
        bolt.setVelocity(user, user.getPitch(), user.getYaw(), 0.0f, 1.6f, 0.0f);
        world.spawnEntity(bolt);

        // короткая яркая вспышка у "дула" — компактная, без дыма
        if (world instanceof ServerWorld serverWorld) {
            Vec3d look = user.getRotationVec(1.0f);
            double mx = user.getX() + look.x * 0.8;
            double my = user.getEyeY() - 0.1 + look.y * 0.8;
            double mz = user.getZ() + look.z * 0.8;
            serverWorld.spawnParticles(
                new DustParticleEffect(new Vector3f(0.95f, 0.1f, 0.1f), 1.4f),
                mx, my, mz, 6, 0.08, 0.08, 0.08, 0.0);
        }
    }

    // ==== 2. Shift + ПКМ — удар посохом об землю ====
    private void groundSlam(World world, PlayerEntity user) {
        if (world.isClient || !(world instanceof ServerWorld serverWorld)) {
            return;
        }

        world.playSound(null, user.getBlockPos(), SoundEvents.BLOCK_ANVIL_LAND,
            SoundCategory.PLAYERS, 0.6f, 0.7f);
        world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_RAVAGER_STUNNED,
            SoundCategory.PLAYERS, 1.0f, 0.6f);

        double cx = user.getX();
        double cy = user.getY() + 0.1;
        double cz = user.getZ();

        // кільця часток, що розходяться від гравця
        for (int ring = 0; ring < 3; ring++) {
            double radius = 1.5 + ring * 2.0;
            int points = (int) (radius * 10);
            for (int i = 0; i < points; i++) {
                double angle = (2 * Math.PI / points) * i;
                double px = cx + Math.cos(angle) * radius;
                double pz = cz + Math.sin(angle) * radius;
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.55f, 0.02f, 0.02f), 1.4f),
                    px, cy, pz,
                    1, 0.0, 0.05, 0.0, 0.0
                );
            }
        }
        serverWorld.spawnParticles(ParticleTypes.EXPLOSION, cx, cy + 0.2, cz, 1, 0.0, 0.0, 0.0, 0.0);

        Box area = new Box(
            cx - SLAM_RADIUS, cy - 2.0, cz - SLAM_RADIUS,
            cx + SLAM_RADIUS, cy + 3.0, cz + SLAM_RADIUS
        );

        List<LivingEntity> targets = world.getEntitiesByClass(
            LivingEntity.class,
            area,
            e -> e != user && e.isAlive() && e.squaredDistanceTo(user) <= SLAM_RADIUS * SLAM_RADIUS
        );

        DamageSource damageSource = world.getDamageSources().playerAttack(user);

        for (LivingEntity target : targets) {
            target.damage(damageSource, SLAM_DAMAGE);

            Vec3d push = target.getPos().subtract(user.getPos());
            double dist = Math.max(push.horizontalLength(), 0.1);
            double dx = (push.x / dist) * SLAM_KNOCKBACK;
            double dz = (push.z / dist) * SLAM_KNOCKBACK;

            target.setVelocity(target.getVelocity().add(dx, 0.5, dz));
            target.velocityModified = true;
        }
    }
}