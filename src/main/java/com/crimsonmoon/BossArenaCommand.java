package com.crimsonmoon;

import com.crimsonmoon.world.BossArenaManager;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * /bossarena enter|exit — служебная команда для тестирования измерения
 * боссарены, пока не выбран финальный способ попадания туда (портал, предмет
 * и т.п.). Требует прав оператора (уровень 2).
 */
public class BossArenaCommand {

    private BossArenaCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("bossarena")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("enter").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    BossArenaManager.enter(player);
                    return 1;
                }))
                .then(CommandManager.literal("exit").executes(ctx -> {
                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                    BossArenaManager.exit(player);
                    return 1;
                }))
            )
        );
    }
}
