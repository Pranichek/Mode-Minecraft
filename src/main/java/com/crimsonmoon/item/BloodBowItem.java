package com.crimsonmoon.item;

import com.crimsonmoon.entity.BloodClotEntity;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Кривавий лук. Працює як звичайний лук (натиснути ПКМ, тримати для
 * натягування, відпустити для пострілу).
 * Тепер НЕ ВИМАГАЄ стріл в інвентарі, створює їх з власної міцності.
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
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);
        tooltip.add(Text.literal("Стріляє власною кров'ю (Не потребує стріл)").formatted(Formatting.DARK_RED));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        
        // Дозволяємо натягувати лук завжди, без перевірки інвентарю
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

        // Спавнимо кастомний сгусток крові
        BloodClotEntity clot = new BloodClotEntity(world, player);
        clot.setPosition(player.getX(), player.getEyeY() - 0.1, player.getZ());
        clot.setVelocity(player, player.getPitch(), player.getYaw(), 0.0f, power * 3.0f, 1.0f);
        world.spawnEntity(clot);

        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ARROW_SHOOT,
            SoundCategory.PLAYERS, 1.0f, 0.9f);

        // Витрачаємо міцність самого лука
        stack.damage(1, player, p -> p.sendToolBreakStatus(player.getActiveHand()));
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