package com.crimsonmoon;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.world.World;

public class CrimsonArmorItem extends ArmorItem{
    public CrimsonArmorItem(
        ArmorMaterial material,
        Type type,
        Settings settings
    ){
        super(material, type, settings);
    }

    @Override
    public void inventoryTick(
        ItemStack stack,
        World world,
        Entity entity,
        int slot,
        boolean selected
    ){
        if (!world.isClient()){
            if (entity instanceof PlayerEntity player){
                if(checkArmor(player)){
                    player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.FIRE_RESISTANCE,
                        20,
                        1,
                        false,
                        false,
                        true
                    ));

                    player.addStatusEffect(new StatusEffectInstance(
                        StatusEffects.REGENERATION,
                        20,
                        1,
                        false,
                        false,
                        true
                    ));
                }
            }
        }
    }
    
    private boolean checkArmor(PlayerEntity player){
        ItemStack boots = player.getInventory().getArmorStack(0);
        ItemStack leggings = player.getInventory().getArmorStack(1);
        ItemStack chestplate = player.getInventory().getArmorStack(2);
        ItemStack helmet = player.getInventory().getArmorStack(3);
        
        if (boots.isEmpty() || leggings.isEmpty() || chestplate.isEmpty() || helmet.isEmpty()) {
            return false;
        }
        
        return boots.getItem() == ModItems.BLOOD_BOOTS &&
               leggings.getItem() == ModItems.BLOOD_LEGGINGS &&
               chestplate.getItem() == ModItems.BLOOD_CHESTPLATE &&
               helmet.getItem() == ModItems.BLOOD_HELMET;
    }
}