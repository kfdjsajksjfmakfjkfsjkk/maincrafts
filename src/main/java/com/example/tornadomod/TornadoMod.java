package com.example.tornadomod;

import com.example.tornadomod.client.ClientEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TornadoMod.MODID)
public class TornadoMod {
    public static final String MODID = "tornadomod";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public TornadoMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TornadoConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TornadoConfig.CLIENT_SPEC);

        ModRegistry.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ServerEvents.class);
        MinecraftForge.EVENT_BUS.register(Net.class);

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            MinecraftForge.EVENT_BUS.register(ClientEvents.class);
        });
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        Net.init();
    }

    private void clientSetup(final FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        TornadoCommands.register(event.getServer().getCommands().getDispatcher());
    }
}
