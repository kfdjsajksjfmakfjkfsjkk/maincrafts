package com.example.tornadomod.client;

import com.example.tornadomod.Storm;
import com.example.tornadomod.TornadoConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import java.util.List;
import java.util.Random;

public class CloudField {

    // Quality-based render distances (in blocks): Low=128ch, Med=256ch, High=512ch, Ultra=3072ch
    private static final float[] RADIUS_BY_QUALITY = { 128f, 256f, 512f, 3072f };
    // Max puffs per quality level
    private static final int[] MAX_PUFFS_BY_QUALITY = { 200, 400, 800, 2000 };

    // Cell size for grid-based placement
    private static final int CELL = 48;

    // Cloud layer heights (Y above sea level)
    private static final float H_CUMULUS    = 180f;
    private static final float H_CUMULONIMBUS = 200f;
    private static final float H_STRATUS    = 140f;
    private static final float H_CIRRUS     = 320f;
    private static final float H_ALTOCUMULUS = 240f;
    private static final float H_ALTOSTRATUS = 260f;
    private static final float H_WALL_CLOUD  = 90f;
    private static final float H_SPIRAL_BAND = 160f;

    // Cloud type identifiers
    private static final int TYPE_CUMULUS    = 0;
    private static final int TYPE_CUMULONIMBUS = 1;
    private static final int TYPE_STRATUS    = 2;
    private static final int TYPE_CIRRUS     = 3;
    private static final int TYPE_ALTOCUMULUS = 4;
    private static final int TYPE_ALTOSTRATUS = 5;
    private static final int TYPE_WALL_CLOUD  = 6;
    private static final int TYPE_SPIRAL_BAND = 7;

    private static final ResourceLocation TEX =
        new ResourceLocation("tornadomod", "dynamic/cloud_puff");

    /** Render all cloud layers for the current frame. */
    public static void render(PoseStack pose, Matrix4f proj,
                              float partialTick, long gameTime) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        TornadoConfig.QualityPreset quality = TornadoConfig.QUALITY_PRESET.get();
        int qIdx = quality.ordinal();
        float configDist = TornadoConfig.CLOUD_DISTANCE.get() * 16f; // chunks→blocks
        float radius = Math.min(RADIUS_BY_QUALITY[qIdx], configDist);
        int maxPuffs = MAX_PUFFS_BY_QUALITY[qIdx];

        List<Storm> storms = ClientStorms.get();
        float timeX = (float)(0.045 * gameTime);
        float timeZ = (float)(0.018 * gameTime);

        double camX = mc.gameRenderer.getMainCamera().getPosition().x;
        double camY = mc.gameRenderer.getMainCamera().getPosition().y;
        double camZ = mc.gameRenderer.getMainCamera().getPosition().z;

        float rain = mc.level.getRainLevel(partialTick);
        float thunder = mc.level.getThunderLevel(partialTick);
        float sunAngle = mc.level.getTimeOfDay(partialTick);
        float brightness = (float) Math.max(0.1, Math.min(1.0,
            0.55 + 0.6 * Math.sin(sunAngle * Math.PI * 2))) * (1f - 0.45f * rain);
        float baseAlpha = 0.4f + 0.28f * rain + 0.12f * thunder;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorTexShader);
        RenderSystem.setShaderTexture(0, TEX);

        BufferBuilder buf = Tesselator.getInstance().getBuilder();

        // --- Layer 1: Cumulus (main mid-level clouds) ---
        if (TornadoConfig.CUMULUS_ENABLED.get()) {
            renderCumulusLayer(buf, pose, proj, camX, camY, camZ,
                radius, maxPuffs / 4, timeX, timeZ, brightness, baseAlpha, storms, gameTime);
        }

        // --- Layer 2: Cumulonimbus (storm towers) ---
        if (TornadoConfig.CUMULONIMBUS_ENABLED.get()) {
            renderCumulonimbusLayer(buf, pose, proj, camX, camY, camZ,
                radius * 0.5f, maxPuffs / 8, timeX, timeZ, brightness, baseAlpha, storms, rain, thunder);
        }

        // --- Layer 3: Stratus (low flat layer) ---
        if (TornadoConfig.STRATUS_ENABLED.get()) {
            renderStratusLayer(buf, pose, proj, camX, camY, camZ,
                radius * 0.7f, maxPuffs / 6, timeX * 0.5f, timeZ * 0.5f, brightness, baseAlpha, rain);
        }

        // --- Layer 4: Cirrus (high wispy) ---
        if (TornadoConfig.CIRRUS_ENABLED.get()) {
            renderCirrusLayer(buf, pose, proj, camX, camY, camZ,
                radius, maxPuffs / 4, timeX * 0.3f, timeZ * 0.1f, brightness, baseAlpha);
        }

        // --- Layer 5: Altocumulus ---
        if (TornadoConfig.ALTOCUMULUS_ENABLED.get()) {
            renderAltocumulusLayer(buf, pose, proj, camX, camY, camZ,
                radius * 0.8f, maxPuffs / 5, timeX * 0.6f, timeZ * 0.6f, brightness, baseAlpha);
        }

        // --- Storm-dependent: Wall clouds + spiral bands ---
        for (Storm s : storms) {
            if (s.kind == Storm.Kind.CYCLONE && TornadoConfig.SPIRAL_BANDS_ENABLED.get()) {
                renderSpiralBands(buf, pose, proj, camX, camY, camZ,
                    s, timeX, timeZ, brightness, baseAlpha, maxPuffs / 3, gameTime);
            }
            if (TornadoConfig.WALL_CLOUDS_ENABLED.get()) {
                renderWallCloud(buf, pose, proj, camX, camY, camZ,
                    s, timeX, timeZ, brightness, baseAlpha, maxPuffs / 6);
            }
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    // ───── Cumulus ──────────────────────────────────────────────────────────
    private static void renderCumulusLayer(BufferBuilder buf, PoseStack pose, Matrix4f proj,
            double camX, double camY, double camZ,
            float radius, int maxPuffs, float tx, float tz,
            float bright, float alpha, List<Storm> storms, long gameTime) {

        float cloudY = (float) Math.max(H_CUMULUS, camY + 55);
        int cells = (int)(radius / CELL);
        int drawn = 0;

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        for (int cx = -cells; cx <= cells && drawn < maxPuffs; cx++) {
            for (int cz = -cells; cz <= cells && drawn < maxPuffs; cz++) {
                float wx = (float)(Math.floor(camX / CELL) + cx) * CELL;
                float wz = (float)(Math.floor(camZ / CELL) + cz) * CELL;

                // LOD: skip detailed puffs far away
                float dist = (float) Math.sqrt((wx - camX) * (wx - camX) + (wz - camZ) * (wz - camZ));
                if (dist > radius) continue;

                float noise = fbm(wx * 0.003f + tx * 0.001f, wz * 0.003f + tz * 0.001f);
                if (noise < 0.35f) continue;

                // Storm influence: darker near tornado/cyclone
                float stormDark = getStormDarkness(wx, cloudY, wz, storms);
                float puffAlpha = alpha * (0.5f + 0.5f * noise) * (1f - 0.4f * stormDark);

                // LOD size scaling: larger puffs at distance
                float lodFactor = 1f + dist / radius * 1.5f;
                float size = (15f + noise * 20f) * lodFactor;

                drawBillboard(buf, pose, (float)(wx - camX), cloudY - (float)camY,
                    (float)(wz - camZ), size, size * 0.6f,
                    bright * (1f - 0.3f * stormDark), puffAlpha);
                drawn++;
            }
        }

        Tesselator.getInstance().end();
    }

    // ───── Cumulonimbus ─────────────────────────────────────────────────────
    private static void renderCumulonimbusLayer(BufferBuilder buf, PoseStack pose, Matrix4f proj,
            double camX, double camY, double camZ,
            float radius, int maxPuffs, float tx, float tz,
            float bright, float alpha, List<Storm> storms, float rain, float thunder) {

        if (rain < 0.3f && thunder < 0.1f && storms.isEmpty()) return;

        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        int cells = (int)(radius / CELL);
        int drawn = 0;

        for (int cx = -cells; cx <= cells && drawn < maxPuffs; cx++) {
            for (int cz = -cells; cz <= cells && drawn < maxPuffs; cz++) {
                float wx = (float)(Math.floor(camX / CELL) + cx) * CELL;
                float wz = (float)(Math.floor(camZ / CELL) + cz) * CELL;
                float dist = (float) Math.sqrt((wx - camX) * (wx - camX) + (wz - camZ) * (wz - camZ));
                if (dist > radius) continue;

                float noise = fbm(wx * 0.004f + tx * 0.0008f, wz * 0.004f + tz * 0.0008f);
                if (noise < 0.5f) continue;

                float stormProx = getStormProximity(wx, wz, storms);
                if (stormProx < 0.15f && rain < 0.3f) continue;

                // Tall anvil towers: multiple stacked puffs
                float baseY = H_CUMULONIMBUS - (float)camY;
                float puffAlpha = alpha * noise * (0.6f + 0.4f * stormProx);
                float size = 25f + noise * 30f;

                // Bottom dark base
                drawBillboard(buf, pose, (float)(wx - camX), baseY - 10f,
                    (float)(wz - camZ), size, 10f, bright * 0.35f, puffAlpha * 0.9f);
                // Middle
                drawBillboard(buf, pose, (float)(wx - camX), baseY,
                    (float)(wz - camZ), size * 0.85f, size * 0.5f, bright * 0.5f, puffAlpha);
                // Anvil top
                drawBillboard(buf, pose, (float)(wx - camX), baseY + size * 0.4f,
                    (float)(wz - camZ), size * 1.3f, size * 0.3f, bright * 0.75f, puffAlpha * 0.7f);
                drawn++;
            }
        }
        Tesselator.getInstance().end();
    }

    // ───── Stratus ──────────────────────────────────────────────────────────
    private static void renderStratusLayer(BufferBuilder buf, PoseStack pose, Matrix4f proj,
            double camX, double camY, double camZ,
            float radius, int maxPuffs, float tx, float tz,
            float bright, float alpha, float rain) {

        if (rain < 0.1f) return;
        float cloudY = (float)(H_STRATUS - camY);
        int cells = (int)(radius / CELL);
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        int drawn = 0;

        for (int cx = -cells; cx <= cells && drawn < maxPuffs; cx++) {
            for (int cz = -cells; cz <= cells && drawn < maxPuffs; cz++) {
                float wx = (float)(Math.floor(camX / CELL) + cx) * CELL * 2;
                float wz = (float)(Math.floor(camZ / CELL) + cz) * CELL * 2;
                float dist = (float) Math.sqrt((wx - camX) * (wx - camX) + (wz - camZ) * (wz - camZ));
                if (dist > radius) continue;

                float noise = fbm(wx * 0.002f + tx * 0.0005f, wz * 0.002f + tz * 0.0005f);
                if (noise < 0.3f) continue;

                float puffAlpha = alpha * rain * noise * 0.7f;
                float size = 50f + noise * 60f;
                drawBillboard(buf, pose, (float)(wx - camX), cloudY,
                    (float)(wz - camZ), size, size * 0.15f, bright * 0.6f, puffAlpha);
                drawn++;
            }
        }
        Tesselator.getInstance().end();
    }

    // ───── Cirrus ───────────────────────────────────────────────────────────
    private static void renderCirrusLayer(BufferBuilder buf, PoseStack pose, Matrix4f proj,
            double camX, double camY, double camZ,
            float radius, int maxPuffs, float tx, float tz,
            float bright, float alpha) {

        float cloudY = (float)(H_CIRRUS - camY);
        int cells = (int)(radius / CELL);
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        int drawn = 0;

        for (int cx = -cells; cx <= cells && drawn < maxPuffs; cx++) {
            for (int cz = -cells; cz <= cells && drawn < maxPuffs; cz++) {
                float wx = (float)(Math.floor(camX / CELL) + cx) * CELL * 3;
                float wz = (float)(Math.floor(camZ / CELL) + cz) * CELL * 3;
                float dist = (float) Math.sqrt((wx - camX) * (wx - camX) + (wz - camZ) * (wz - camZ));
                if (dist > radius) continue;

                float noise = fbm(wx * 0.001f + tx * 0.0003f, wz * 0.001f + tz * 0.0003f);
                if (noise < 0.4f) continue;

                float puffAlpha = alpha * 0.3f * noise;
                float width = 80f + noise * 100f;
                float height = 8f + noise * 8f;
                drawBillboard(buf, pose, (float)(wx - camX), cloudY,
                    (float)(wz - camZ), width, height, bright * 0.95f, puffAlpha);
                drawn++;
            }
        }
        Tesselator.getInstance().end();
    }

    // ───── Altocumulus ──────────────────────────────────────────────────────
    private static void renderAltocumulusLayer(BufferBuilder buf, PoseStack pose, Matrix4f proj,
            double camX, double camY, double camZ,
            float radius, int maxPuffs, float tx, float tz,
            float bright, float alpha) {

        float cloudY = (float)(H_ALTOCUMULUS - camY);
        int cells = (int)(radius / CELL);
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        int drawn = 0;

        for (int cx = -cells; cx <= cells && drawn < maxPuffs; cx++) {
            for (int cz = -cells; cz <= cells && drawn < maxPuffs; cz++) {
                float wx = (float)(Math.floor(camX / CELL) + cx) * CELL;
                float wz = (float)(Math.floor(camZ / CELL) + cz) * CELL;
                float dist = (float) Math.sqrt((wx - camX) * (wx - camX) + (wz - camZ) * (wz - camZ));
                if (dist > radius) continue;

                float noise = fbm(wx * 0.0025f + tx * 0.0006f, wz * 0.0025f + tz * 0.0006f);
                if (noise < 0.45f) continue;

                float puffAlpha = alpha * 0.55f * noise;
                float size = 18f + noise * 22f;
                drawBillboard(buf, pose, (float)(wx - camX), cloudY,
                    (float)(wz - camZ), size, size * 0.35f, bright * 0.85f, puffAlpha);
                drawn++;
            }
        }
        Tesselator.getInstance().end();
    }

    // ───── Spiral Bands (hurricanes) ─────────────────────────────────────
    private static void renderSpiralBands(BufferBuilder buf, PoseStack pose, Matrix4f proj,
            double camX, double camY, double camZ,
            Storm storm, float tx, float tz, float bright, float alpha,
            int maxPuffs, long gameTime) {

        float cloudY = (float)(H_SPIRAL_BAND - camY);
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);
        int drawn = 0;
        float eyeR = storm.eye();
        float maxR = storm.radius();

        for (int i = 0; i < maxPuffs; i++) {
            float t = (float) i / maxPuffs;
            float r = eyeR + t * (maxR - eyeR);
            // Logarithmic spiral: angle increases with radius
            float angle = (float)(t * Math.PI * 4 + gameTime * 0.0002f);
            float bx = (float)(storm.x + r * Math.cos(angle));
            float bz = (float)(storm.z + r * Math.sin(angle));
            float dist = (float) Math.sqrt((bx - camX) * (bx - camX) + (bz - camZ) * (bz - camZ));
            if (dist > 3072f) continue;

            float bandNoise = fbm(bx * 0.003f + tx * 0.001f, bz * 0.003f + tz * 0.001f);
            float puffAlpha = alpha * (0.3f + 0.5f * bandNoise) * (1f - t * 0.4f);
            float size = 20f + bandNoise * 30f;
            drawBillboard(buf, pose, (float)(bx - camX), cloudY,
                (float)(bz - camZ), size, size * 0.4f, bright * 0.6f, puffAlpha);
            drawn++;
        }
        Tesselator.getInstance().end();
    }

    // ───── Wall Clouds (near tornado/cyclone) ────────────────────────────
    private static void renderWallCloud(BufferBuilder buf, PoseStack pose, Matrix4f proj,
            double camX, double camY, double camZ,
            Storm storm, float tx, float tz, float bright, float alpha, int maxPuffs) {

        float wallY = (float)(H_WALL_CLOUD - camY);
        float stormR = storm.radius() * 0.5f;
        buf.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX);

        for (int i = 0; i < maxPuffs; i++) {
            float ang = (float)(i * Math.PI * 2 / maxPuffs);
            float r = stormR * (0.7f + 0.3f * fbm((float)storm.x * 0.01f + ang,
                                                    (float)storm.z * 0.01f + ang));
            float bx = (float)(storm.x + r * Math.cos(ang));
            float bz = (float)(storm.z + r * Math.sin(ang));
            float dist = (float) Math.sqrt((bx - camX) * (bx - camX) + (bz - camZ) * (bz - camZ));
            if (dist > 3072f) continue;

            // Wall cloud is very dark
            float puffAlpha = alpha * (0.7f + 0.3f * storm.f);
            float sz = 12f + storm.f * 15f;
            drawBillboard(buf, pose, (float)(bx - camX), wallY,
                (float)(bz - camZ), sz, sz * 0.6f, bright * 0.2f, puffAlpha);
        }
        Tesselator.getInstance().end();
    }

    // ───── Billboard ────────────────────────────────────────────────────────
    private static void drawBillboard(BufferBuilder buf, PoseStack pose,
            float rx, float ry, float rz,
            float w, float h, float brightness, float alpha) {
        var mat = pose.last().pose();
        float r = brightness, g = brightness, b = brightness;
        int a = (int)(alpha * 255);
        if (a <= 0) return;
        // Simple XZ-aligned billboard quad
        buf.vertex(mat, rx - w/2, ry + h, rz).color(r, g, b, a/255f).uv(0, 0).endVertex();
        buf.vertex(mat, rx - w/2, ry,     rz).color(r, g, b, a/255f).uv(0, 1).endVertex();
        buf.vertex(mat, rx + w/2, ry,     rz).color(r, g, b, a/255f).uv(1, 1).endVertex();
        buf.vertex(mat, rx + w/2, ry + h, rz).color(r, g, b, a/255f).uv(1, 0).endVertex();
    }

    // ───── FBM Noise ────────────────────────────────────────────────────────
    static float fbm(float x, float z) {
        return 0.55f * vnoise(x,     z,     1)
             + 0.30f * vnoise(x*2.1f+7f, z*2.1f+3f, 2)
             + 0.15f * vnoise(x*4.3f, z*4.3f, 3);
    }

    private static float vnoise(float x, float z, int seed) {
        int xi = (int) Math.floor(x); int zi = (int) Math.floor(z);
        float xf = x - xi; float zf = z - zi;
        float u = fade(xf); float v = fade(zf);
        float n00 = grad(xi,     zi,     seed, xf,   zf);
        float n10 = grad(xi + 1, zi,     seed, xf-1, zf);
        float n01 = grad(xi,     zi + 1, seed, xf,   zf-1);
        float n11 = grad(xi + 1, zi + 1, seed, xf-1, zf-1);
        return lerp(v, lerp(u, n00, n10), lerp(u, n01, n11)) * 0.5f + 0.5f;
    }

    private static float fade(float t) { return t * t * t * (t * (t * 6 - 15) + 10); }
    private static float lerp(float t, float a, float b) { return a + t * (b - a); }
    private static float grad(int x, int z, int s, float dx, float dz) {
        int h = (x * 1619 + z * 31337 + s * 1013904223) & 3;
        float u = (h < 2) ? dx : dz, v = (h < 2) ? dz : dx;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    // ───── Storm helpers ────────────────────────────────────────────────────
    private static float getStormDarkness(float wx, float wy, float wz, List<Storm> storms) {
        float dark = 0f;
        for (Storm s : storms) {
            float dx = (float)(wx - s.x), dz = (float)(wz - s.z);
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            float r = s.radius();
            if (dist < r * 2) {
                dark = Math.max(dark, 1f - dist / (r * 2));
            }
        }
        return Math.min(1f, dark);
    }

    private static float getStormProximity(float wx, float wz, List<Storm> storms) {
        float prox = 0f;
        for (Storm s : storms) {
            float dx = (float)(wx - s.x), dz = (float)(wz - s.z);
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            float r = s.radius();
            if (dist < r * 3) prox = Math.max(prox, 1f - dist / (r * 3));
        }
        return prox;
    }
}
