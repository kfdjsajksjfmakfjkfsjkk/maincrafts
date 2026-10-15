package com.example.tornadomod;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import net.minecraft.util.RandomSource;

public class StormEffects {

    public static void destroyBlocks(Storm s, ServerLevel level, RandomSource rng) {
        if (!TornadoConfig.DESTROY_BLOCKS.get()) return;

        int radius = (int) s.radius();
        int cx = (int) s.x;
        int cz = (int) s.z;
        int attempts = s.level + 1;

        for (int i = 0; i < attempts; i++) {
            int dx = rng.nextInt(radius * 2) - radius;
            int dz = rng.nextInt(radius * 2) - radius;
            if (dx * dx + dz * dz > radius * radius) continue;

            int bx = cx + dx;
            int bz = cz + dz;
            int by = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, bx, bz);

            for (int dy = 0; dy <= s.level * 3 + 3; dy++) {
                BlockPos pos = new BlockPos(bx, by - dy, bz);
                BlockState state = level.getBlockState(pos);
                if (state.isAir() || state.is(Blocks.BEDROCK)) continue;

                float hardness = state.getDestroySpeed(level, pos);
                if (hardness < 0 || hardness > 3 + s.level * 1.5f) continue;

                if (TornadoConfig.PROTECT_BUILDINGS.get()) {
                    // skip blocks that have other blocks on all 4 horizontal sides (protected structure)
                    boolean enclosed = !level.getBlockState(pos.north()).isAir()
                            && !level.getBlockState(pos.south()).isAir()
                            && !level.getBlockState(pos.east()).isAir()
                            && !level.getBlockState(pos.west()).isAir();
                    if (enclosed) continue;
                }

                // Spawn debris item
                if (rng.nextInt(3) == 0 && level.getEntities(null, new AABB(pos)).size() < TornadoConfig.MAX_DEBRIS.get()) {
                    ItemEntity item = new ItemEntity(level, bx + 0.5, by + 0.5, bz + 0.5,
                            new net.minecraft.world.item.ItemStack(state.getBlock().asItem()));
                    double vx = (rng.nextDouble() - 0.5) * 0.8;
                    double vy = 0.4 + rng.nextDouble() * 0.4;
                    double vz = (rng.nextDouble() - 0.5) * 0.8;
                    item.setDeltaMovement(vx, vy, vz);
                    level.addFreshEntity(item);
                }

                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                break;
            }
        }

        // Affect entities
        double erad = s.radius() * 1.5;
        List<Entity> nearby = level.getEntities(null, new AABB(s.x - erad, s.y - 10, s.z - erad,
                s.x + erad, s.y + 100, s.z + erad));
        for (Entity e : nearby) {
            double dx = e.getX() - s.x;
            double dz = e.getZ() - s.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist > erad) continue;
            double force = (1.0 - dist / erad) * (s.level + 1) * 0.15;
            // Tangential + upward
            double tx = -dz / dist * force;
            double tz =  dx / dist * force;
            double ty = force * 0.5;
            e.setDeltaMovement(e.getDeltaMovement().add(tx, ty, tz));
            if (e instanceof LivingEntity living) {
                living.hurt(level.damageSources().inWall(), s.level * 0.5f);
            }
        }
    }
}
