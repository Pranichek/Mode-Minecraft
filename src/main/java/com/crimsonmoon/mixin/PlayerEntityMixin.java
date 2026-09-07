package com.crimsonmoon.mixin;

import com.crimsonmoon.ModItems;
import net.minecraft.entity.ItemEntity; 
import net.minecraft.entity.player.PlayerEntity; 
import net.minecraft.item.ItemStack; 
import org.spongepowered.asm.mixin.Mixin; 
import org.spongepowered.asm.mixin.injection.At; 
import org.spongepowered.asm.mixin.injection.Inject; 
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {
    
    @Inject(method = "dropItem(Lnet/minecraft/item/ItemStack;ZZ)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    
    private void preventCurseDrop(ItemStack stack, boolean throwRandomly, boolean retainOwnership, CallbackInfoReturnable<ItemEntity> cir) {
        if (stack.isOf(ModItems.BLOOD_MONOLIT)) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            if(!player.getWorld().isClient()){
                player.getInventory().insertStack(stack.copy());
            }

            cir.setReturnValue(null);
        }
    }
}
