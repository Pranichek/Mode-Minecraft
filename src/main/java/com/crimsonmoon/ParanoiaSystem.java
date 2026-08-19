package com.crimsonmoon;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ParanoiaSystem {
    private static int tickCounter = 0;
    
    public static void register(){
        ServerTickEvents.END_SERVER_TICK.register(server ->{
            tickCounter++;
            if (tickCounter >= 100){
                tickCounter = 0;
                
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()){
                    System.out.println("Зайшло в цикл");

                    int targetCount = countTrigerItems(player);
                    System.out.println(targetCount + "dirty block");
                    
                    if (targetCount >= 3 && targetCount <= 5){
                        //параноя 1 рівня
                        System.out.println("Параноя 1 рівня");
                        firstLevelParanoia(player);

                    }
                    else if (targetCount >= 6 && targetCount <= 9){
                        //параноя 2 рівня
                        System.out.println("Параноя 2 рівня");
                        secondLevelParanoia(player);
                    }
                    else if (targetCount >= 10){
                        //параноя 3 рівня
                        thirdLevelParanoia(player);
                    }
                }
            }
        });
    }

    // підрахунок потрібних блоків
    private static int countTrigerItems(ServerPlayerEntity player){
        int count = 0;
        
        for (int i = 0; i < player.getInventory().size(); i++){
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(Items.DIRT)){
                count += stack.getCount();  
            }
        }
        return count;
    }

    private static void firstLevelParanoia(ServerPlayerEntity player){
        if (player.getRandom().nextFloat() <= 0.2f){
            player.playSound(SoundEvents.AMBIENT_CAVE.value(), SoundCategory.AMBIENT, 1.0f, 1.0f );
        }
    }

    private static void secondLevelParanoia(ServerPlayerEntity player){
        if (player.getRandom().nextFloat() <= 0.25f){
            player.playSound(SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.HOSTILE, 1.0f, 1.0f );
            Text message = Text.literal("Ви почуваете що за вами хтось спостерігає?...").formatted(Formatting.DARK_RED);
            player.sendMessage(message, true);
        }
    }

    private static void thirdLevelParanoia(ServerPlayerEntity player){
        float randomEvent = player.getRandom().nextFloat();
        if (randomEvent <= 0.15f){
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 80, 0, false, false, true)); 
            player.sendMessage(Text.literal("Світло тебе не врятує...").formatted(Formatting.DARK_RED), true);
            
        }
        else if (randomEvent <= 0.3f){
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 1, false, false, true)); 
            player.playSound(SoundEvents.ENTITY_WARDEN_ANGRY, SoundCategory.HOSTILE, 1.0f, 1.0f);
        }
    }
}

