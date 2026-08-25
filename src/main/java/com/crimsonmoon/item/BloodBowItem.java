package com.crimsonmoon.item;

import com.crimsonmoon.ModItems;
import com.crimsonmoon.entity.BloodClotEntity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

/**
 * Кривавий лук. Працює як звичайний лук (натиснути ПКМ, тримати для
 * натягування, відпустити для пострілу), але замість ванільних стріл
 * вимагає та випускає предмет "Сгусток крові" (BLOOD_CLOT), спавнячи
 * кастомну сутність BloodClotEntity.
 */
public class BloodBowItem extends Item {

    public BloodBowItem(Settings settings) {
        super(settings);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BOW;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 72000;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        boolean hasAmmo = user.getAbilities().creativeMode || findClotSlot(user.getInventory()) >= 0;
        if (!hasAmmo) {
            return TypedActionResult.fail(stack);
        }

        user.setCurrentHand(hand);
        return TypedActionResult.consume(stack);
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient || !(user instanceof PlayerEntity player)) {
            return;
        }

        int useTicks = this.getMaxUseTime(stack) - remainingUseTicks;
        float power = getPullProgress(useTicks);
        if (power < 0.1f) {
            return;
        }

        boolean creative = player.getAbilities().creativeMode;
        int clotSlot = findClotSlot(player.getInventory());

        if (!creative && clotSlot < 0) {
            return;
        }

        BloodClotEntity clot = new BloodClotEntity(world, player);
        clot.setPosition(player.getX(), player.getEyeY() - 0.1, player.getZ());
        clot.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, power * 3.0f, 1.0f);
        world.spawnEntity(clot);

        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT,
            SoundCategory.PLAYERS, 1.0f, 0.9f);

        if (!creative) {
            player.getInventory().getStack(clotSlot).decrement(1);
        }

        stack.damage(1, player, p -> p.sendToolBreakStatus(player.getActiveHand()));
    }

    private static int findClotSlot(PlayerInventory inventory) {
        for (int i = 0; i < inventory.size(); i++) {
            if (inventory.getStack(i).isOf(ModItems.BLOOD_CLOT)) {
                return i;
            }
        }
        return -1;
    }

    private static float getPullProgress(int useTicks) {
        float f = (float) useTicks / 20.0f;
        f = (f * f + f * 2.0f) / 3.0f;
        if (f > 1.0f) {
            f = 1.0f;
        }
        return f;
    }
}
