package com.example.tornadomod;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class StationBlock extends Block {
    public StationBlock(Properties props) { super(props); }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) return InteractionResult.SUCCESS;
        var storms = com.example.tornadomod.client.ClientStorms.get();
        if (storms.isEmpty()) {
            player.displayClientMessage(Component.literal("Weather Station: No active storms"), true);
        } else {
            for (var s : storms) {
                double dx = s.x - player.getX();
                double dz = s.z - player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                player.displayClientMessage(Component.literal(
                    String.format("%s | %.0f m | %.0f km/h", s.name(), dist, s.kmh())), true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
