package com.example.tornadomod.client;

import com.example.tornadomod.Storm;
import com.example.tornadomod.TornadoConfig;
import com.example.tornadomod.Wind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

@Mod.EventBusSubscriber(modid = "tornadomod", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        if (TornadoConfig.CLOUD_MODE.get() == TornadoConfig.CloudMode.NONE) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        long gameTime = mc.level.getGameTime();
        PoseStack pose = event.getPoseStack();
        var proj = event.getProjectionMatrix();

        CloudField.render(pose, proj, event.getPartialTick(), gameTime);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        LocalPlayer player = mc.player;
        List<Storm> storms = ClientStorms.get();
        if (storms.isEmpty()) return;

        // Apply wind push to player
        float wx = (float)Wind.getWindX(player.getX(), player.getZ(), storms);
        float wz = (float)Wind.getWindZ(player.getX(), player.getZ(), storms);

        if (Math.abs(wx) > 0.01f || Math.abs(wz) > 0.01f) {
            player.push(wx * 0.05f, 0, wz * 0.05f);
        }
    }
}
