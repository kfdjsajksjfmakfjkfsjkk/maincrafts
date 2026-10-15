package com.example.tornadomod.client;

import com.example.tornadomod.Storm;
import com.example.tornadomod.TornadoConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

@Mod.EventBusSubscriber(modid = "tornadomod", bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = net.minecraftforge.api.distmarker.Dist.CLIENT)
public class CloudRenderer {

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        List<Storm> storms = ClientStorms.get();
        float partialTick = (float)event.getPartialTick();
        float rain = mc.level.getRainLevel(partialTick);
        float thunder = mc.level.getThunderLevel(partialTick);

        // Storm fog: darken fog color near active storms
        float stormProx = getStormProximity(mc, storms);
        if (stormProx > 0.01f) {
            float factor = 1f - stormProx * 0.7f * Math.max(rain, 0.3f);
            event.setRed(event.getRed() * factor);
            event.setGreen(event.getGreen() * factor);
            event.setBlue(event.getBlue() * factor + stormProx * 0.03f); // slight blue tint
        }

        // Atmospheric haze: warm sunrise/sunset tint
        if (TornadoConfig.ATMOSPHERIC_HAZE_ENABLED.get()) {
            float sunAngle = mc.level.getTimeOfDay(partialTick);
            float dawnDusk = getDawnDuskFactor(sunAngle);
            if (dawnDusk > 0.01f) {
                event.setRed(event.getRed() + dawnDusk * 0.15f);
                event.setGreen(event.getGreen() + dawnDusk * 0.07f);
                event.setBlue(event.getBlue() - dawnDusk * 0.05f);
            }
        }
    }

    @SubscribeEvent
    public static void onFogDensity(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (!TornadoConfig.GROUND_FOG_ENABLED.get() &&
            !TornadoConfig.STORM_FOG_ENABLED.get() &&
            !TornadoConfig.MOUNTAIN_FOG_ENABLED.get()) return;

        List<Storm> storms = ClientStorms.get();
        float partialTick = (float)event.getPartialTick();
        float rain = mc.level.getRainLevel(partialTick);
        float thunder = mc.level.getThunderLevel(partialTick);
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;

        float fogDensityMult = 1f;
        float fogIntensity = (float) TornadoConfig.FOG_INTENSITY.get().doubleValue();
        float fogHeight = (float) TornadoConfig.FOG_HEIGHT.get().doubleValue();
        float fogDensityCfg = (float) TornadoConfig.FOG_DENSITY.get().doubleValue();

        // Ground fog: only below configured height
        if (TornadoConfig.GROUND_FOG_ENABLED.get() && camY < fogHeight) {
            float groundFogStrength = (float)(1.0 - camY / fogHeight);
            fogDensityMult += groundFogStrength * fogDensityCfg * fogIntensity;
        }

        // Storm fog: near active storms
        if (TornadoConfig.STORM_FOG_ENABLED.get()) {
            float stormProx = getStormProximity(mc, storms);
            fogDensityMult += stormProx * 2.5f * Math.max(rain, 0.2f) * fogIntensity;
        }

        // Mountain fog: based on terrain height variance (approximate via Y oscillation)
        if (TornadoConfig.MOUNTAIN_FOG_ENABLED.get()) {
            double camX = mc.gameRenderer.getMainCamera().getPosition().x;
            double camZ = mc.gameRenderer.getMainCamera().getPosition().z;
            float terrainNoise = CloudField.fbm((float)camX * 0.002f, (float)camZ * 0.002f);
            if (terrainNoise > 0.55f && camY < 100) {
                fogDensityMult += (terrainNoise - 0.55f) * 3f * fogIntensity * 0.5f;
            }
        }

        if (fogDensityMult > 1.01f) {
            float near = event.getNearPlaneDistance();
            float far = event.getFarPlaneDistance();
            // Compress fog range to simulate denser fog
            float compression = 1f / fogDensityMult;
            event.setNearPlaneDistance(near * compression);
            event.setFarPlaneDistance(far * compression);
            event.setCanceled(true);
        }
    }

    private static float getStormProximity(Minecraft mc, List<Storm> storms) {
        if (storms.isEmpty()) return 0f;
        Vec3 pos = mc.gameRenderer.getMainCamera().getPosition();
        float prox = 0f;
        for (Storm s : storms) {
            float dx = (float)(pos.x - s.x), dz = (float)(pos.z - s.z);
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            float r = s.radius() * 2f;
            if (dist < r) prox = Math.max(prox, 1f - dist / r);
        }
        return prox;
    }

    private static float getDawnDuskFactor(float timeOfDay) {
        // Dawn ~0.23, Dusk ~0.76 (normalized 0-1 game time)
        float dawn = Math.max(0f, 1f - Math.abs(timeOfDay - 0.235f) / 0.04f);
        float dusk = Math.max(0f, 1f - Math.abs(timeOfDay - 0.760f) / 0.04f);
        return Math.max(dawn, dusk);
    }
}
