package com.example.tornadomod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

public class TornadoCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("tornado")
            .requires(s -> s.hasPermission(2))
            .then(Commands.literal("spawn")
                .then(Commands.argument("type", StringArgumentType.word())
                    .then(Commands.argument("level", IntegerArgumentType.integer(0, 5))
                        .executes(ctx -> {
                            String type = StringArgumentType.getString(ctx, "type");
                            int lvl = IntegerArgumentType.getInteger(ctx, "level");
                            ServerLevel level = (ServerLevel) ctx.getSource().getLevel();
                            net.minecraft.world.phys.Vec3 pos = ctx.getSource().getPosition();
                            Storm.Kind kind = type.equalsIgnoreCase("cyclone") ? Storm.Kind.CYCLONE : Storm.Kind.TORNADO;
                            Storm s = new Storm(99, kind, lvl, pos.x, pos.y, pos.z);
                            s.level = lvl;
                            s.f = 1f;
                            StormManager.addStorm(s);
                            ctx.getSource().sendSuccess(() -> Component.literal("Spawned " + s.name()), false);
                            return 1;
                        }))))
            .then(Commands.literal("clear")
                .executes(ctx -> {
                    StormManager.clearStorms();
                    ctx.getSource().sendSuccess(() -> Component.literal("All storms cleared"), false);
                    return 1;
                }))
            .then(Commands.literal("list")
                .executes(ctx -> {
                    var storms = StormManager.getStorms();
                    ctx.getSource().sendSuccess(() -> Component.literal("Active storms: " + storms.size()), false);
                    for (Storm s : storms) {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            s.name() + " at " + (int)s.x + "," + (int)s.z + " (" + s.kmh() + " km/h)"), false);
                    }
                    return storms.size();
                }))
        );
    }
}
