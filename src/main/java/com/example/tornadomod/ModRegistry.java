package com.example.tornadomod;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;

@Mod.EventBusSubscriber(modid = TornadoMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModRegistry {

    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, TornadoMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, TornadoMod.MODID);

    public static final RegistryObject<Block> ANEMOMETER_BLOCK = BLOCKS.register("anemometer",
        () -> new AnemometerBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(2.0f)
            .sound(SoundType.METAL)));

    public static final RegistryObject<Block> WEATHER_STATION = BLOCKS.register("weather_station",
        () -> new StationBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .strength(3.0f)
            .sound(SoundType.METAL)));

    public static final RegistryObject<Item> ANEMOMETER_ITEM = ITEMS.register("anemometer",
        () -> new BlockItem(ANEMOMETER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<Item> WEATHER_STATION_ITEM = ITEMS.register("weather_station",
        () -> new BlockItem(WEATHER_STATION.get(), new Item.Properties()));

    public static final RegistryObject<Item> RADAR_ITEM = ITEMS.register("radar",
        () -> new RadarItem(new Item.Properties().stacksTo(1)));

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
    }

    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ANEMOMETER_ITEM);
            event.accept(WEATHER_STATION_ITEM);
            event.accept(RADAR_ITEM);
        }
    }
}
