package com.example.tornadomod;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ServerEvents {
    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel sl)) return;
        if (!sl.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) return;
        StormManager.tick(sl);
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        TornadoCommands.register(event.getDispatcher());
    }
}
