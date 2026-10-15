package com.example.tornadomod;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import java.util.concurrent.atomic.AtomicInteger;

public class StormManager {
    private static final AtomicInteger ID_SEQ = new AtomicInteger(0);
    private static final List<Storm> storms = new ArrayList<>();
    private static int tickCounter = 0;

    public static List<Storm> getStorms() {
        return List.copyOf(storms);
    }

    public static void tick(ServerLevel level) {
        tickCounter++;
        if (tickCounter % 20 != 0) return;

        RandomSource rng = level.random;

        // Spawn new storms
        if (TornadoConfig.NATURAL_SPAWN.get() && storms.size() < TornadoConfig.MAX_STORMS.get()) {
            if (rng.nextDouble() < 0.0005 * TornadoConfig.TORNADO_FREQ.get()) {
                spawnTornado(level, rng);
            }
            if (rng.nextDouble() < 0.0002 * TornadoConfig.HURRICANE_FREQ.get()) {
                spawnCyclone(level, rng);
            }
        }

        // Update existing storms
        storms.removeIf(s -> !updateStorm(s, level, rng));

        // Sync to clients
        Net.syncStorms(level, storms);
    }

    private static void spawnTornado(ServerLevel level, RandomSource rng) {
        double x = level.players().isEmpty() ? 0 :
            level.players().get(rng.nextInt(level.players().size())).getX() + (rng.nextDouble() - 0.5) * 2000;
        double z = level.players().isEmpty() ? 0 :
            level.players().get(rng.nextInt(level.players().size())).getZ() + (rng.nextDouble() - 0.5) * 2000;
        int maxLevel = rng.nextInt(6);
        Storm s = new Storm(ID_SEQ.incrementAndGet(), Storm.Kind.TORNADO, maxLevel, x, 64, z);
        s.vx = (rng.nextDouble() - 0.5) * 0.3;
        s.vz = (rng.nextDouble() - 0.3) * 0.3;
        storms.add(s);
        TornadoMod.LOGGER.info("Spawned tornado EF{} at {},{}", maxLevel, (int)x, (int)z);
    }

    private static void spawnCyclone(ServerLevel level, RandomSource rng) {
        double x = level.players().isEmpty() ? 0 :
            level.players().get(rng.nextInt(level.players().size())).getX() + (rng.nextDouble() - 0.5) * 4000;
        double z = level.players().isEmpty() ? 0 :
            level.players().get(rng.nextInt(level.players().size())).getZ() + (rng.nextDouble() - 0.5) * 4000;
        int maxLevel = rng.nextInt(5);
        Storm s = new Storm(ID_SEQ.incrementAndGet(), Storm.Kind.CYCLONE, maxLevel, x, 64, z);
        s.vx = (rng.nextDouble() - 0.5) * 0.15;
        s.vz = (rng.nextDouble() - 0.5) * 0.15;
        storms.add(s);
        TornadoMod.LOGGER.info("Spawned cyclone CAT{} at {},{}", maxLevel + 1, (int)x, (int)z);
    }

    private static boolean updateStorm(Storm s, ServerLevel level, RandomSource rng) {
        s.age++;
        s.x += s.vx;
        s.z += s.vz;

        // Gentle steering
        s.vx += (rng.nextDouble() - 0.5) * 0.02;
        s.vz += (rng.nextDouble() - 0.5) * 0.02;
        s.vx = Math.max(-0.5, Math.min(0.5, s.vx));
        s.vz = Math.max(-0.5, Math.min(0.5, s.vz));

        // Level progression
        float prog = s.progress();
        if (prog < 0.3f) {
            s.level = (int)(s.maxLevel * (prog / 0.3f));
        } else if (prog > 0.75f) {
            s.level = (int)(s.maxLevel * (1f - (prog - 0.75f) / 0.25f));
        } else {
            s.level = s.maxLevel;
        }
        s.f = Math.min(1f, prog / 0.1f);

        // Block destruction
        if (TornadoConfig.DESTROY_BLOCKS.get() && s.kind == Storm.Kind.TORNADO && s.level >= 1) {
            StormEffects.destroyBlocks(s, level, rng);
        }

        return s.age < s.life;
    }

    public static void addStorm(Storm s) {
        storms.add(s);
    }

    public static void clearStorms() {
        storms.clear();
    }
}
