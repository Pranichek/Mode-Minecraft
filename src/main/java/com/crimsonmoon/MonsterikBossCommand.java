package com.crimsonmoon;

import com.crimsonmoon.world.MonsterikBossManager;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * /monsterikboss spawn — спавнит босса Monsterik на арене
 * (см. {@link MonsterikBossManager#ARENA_SPAWN_POS}), если он ещё не жив
 * (в мире одновременно может быть только один Monsterik).
 *
 * /monsterikboss kill — принудительно убирает активного Monsterik (для теста).
 *
 * Требует прав оператора (уровень 2).
 */
public class MonsterikBossCommand {

    private MonsterikBossCommand() {}

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(CommandManager.literal("monsterikboss")
                .requires(source -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("spawn").executes(ctx -> spawn(ctx.getSource())))
                .then(CommandManager.literal("kill").executes(ctx -> kill(ctx.getSource())))
            )
        );
    }

    private static int spawn(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        MonsterikBossManager.SpawnResult result = MonsterikBossManager.trySpawn(server);

        switch (result) {
            case SPAWNED -> source.sendFeedback(
                () -> Text.literal("Monsterik заспавнен на арене.").formatted(Formatting.DARK_RED),
                true
            );
            case ALREADY_ALIVE -> source.sendError(
                Text.literal("Monsterik уже жив — он существует только в одном экземпляре.")
            );
            case DIMENSION_UNAVAILABLE -> source.sendError(
                Text.literal("Измерение боссарены недоступно (проверь датапак мода).")
            );
        }

        return result == MonsterikBossManager.SpawnResult.SPAWNED ? 1 : 0;
    }

    private static int kill(ServerCommandSource source) {
        MinecraftServer server = source.getServer();
        boolean removed = MonsterikBossManager.killActive(server);

        if (removed) {
            source.sendFeedback(() -> Text.literal("Активный Monsterik убран.").formatted(Formatting.GRAY), true);
            return 1;
        }

        source.sendError(Text.literal("Сейчас нет живого Monsterik."));
        return 0;
    }
}
