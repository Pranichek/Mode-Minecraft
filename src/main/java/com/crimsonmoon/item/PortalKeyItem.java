package com.crimsonmoon.item;

import com.crimsonmoon.CrimsonMoon;
import com.crimsonmoon.world.CrimsonMoonWorldState;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;

public class PortalKeyItem extends Item {

    public PortalKeyItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient() && world instanceof ServerWorld serverWorld) {
            CrimsonMoonWorldState state = CrimsonMoonWorldState.getServerState(serverWorld.getServer());
            
            // Если портал еще не был создан в мире — создаем его прямо сейчас рядом с игроком
            if (!state.portalPlaced) {
                boolean spawned = trySpawnPortalNearPlayer(serverWorld, user.getBlockPos());
                if (!spawned) {
                    user.sendMessage(Text.literal("Не вдалося знайти місце для порталу навколо!").formatted(Formatting.RED), true);
                    return TypedActionResult.fail(user.getStackInHand(hand));
                }
            }

            // Логика рывка к порталу
            Vec3d playerPos = user.getPos();
            Vec3d portalPos = new Vec3d(state.portalX, state.portalY, state.portalZ);
            double dist = playerPos.distanceTo(portalPos);
            
            if (dist > 15) {
                Vec3d dir = portalPos.subtract(playerPos).normalize();
                user.setVelocity(dir.x * 2.5, 0.4, dir.z * 2.5);
                user.velocityModified = true;
                
                world.playSound(null, user.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1f, 1f);
                user.sendMessage(Text.literal("Ключ тягне тебе до порталу...").formatted(Formatting.DARK_PURPLE), true);
            } else {
                user.sendMessage(Text.literal("Портал вже зовсім поруч!").formatted(Formatting.GREEN), true);
            }
        }
        
        user.getItemCooldownManager().set(this, 60); 
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    // Метод генерации портала на суше неподалеку от игрока при первом использовании/подборе
    private boolean trySpawnPortalNearPlayer(ServerWorld world, BlockPos center) {
        CrimsonMoonWorldState state = CrimsonMoonWorldState.getServerState(world.getServer());
        if (state.portalPlaced) return true;

        for (int i = 0; i < 30; i++) {
            int rx = center.getX() + world.random.nextInt(100) - 50;
            int rz = center.getZ() + world.random.nextInt(100) - 50;
            int ry = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, rx, rz);

            BlockPos pPos = new BlockPos(rx, ry, rz);
            BlockState ground = world.getBlockState(pPos.down());

            // Проверяем, что это твердая земля, а не вода/лед
            if (ground.isSolidBlock(world, pPos.down()) && !ground.isOf(Blocks.WATER) && !ground.isOf(Blocks.ICE)) {
                Identifier portalId = new Identifier(CrimsonMoon.MOD_ID, "portal");
                var templateOpt = world.getStructureTemplateManager().getTemplate(portalId);

                if (templateOpt.isPresent()) {
                    templateOpt.get().place(world, pPos, pPos, new StructurePlacementData(), world.random, 2);
                    state.portalPlaced = true;
                    state.portalX = rx;
                    state.portalY = ry;
                    state.portalZ = rz;
                    state.markDirty();
                    return true;
                }
            }
        }
        return false;
    }
}