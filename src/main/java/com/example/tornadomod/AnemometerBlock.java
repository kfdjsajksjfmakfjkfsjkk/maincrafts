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

public class AnemometerBlock extends Block {
    public AnemometerBlock(Properties props) { super(props); }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) return InteractionResult.SUCCESS;
        var storms = com.example.tornadomod.client.ClientStorms.get();
        if (storms.isEmpty()) {
            player.displayClientMessage(Component.literal("Wind: calm"), true);
        } else {
            var s = storms.get(0);
            float wx = (float)Wind.getWindX(player.getX(), player.getZ(), storms);
            float wz = (float)Wind.getWindZ(player.getX(), player.getZ(), storms);
            float spd = (float) Math.sqrt(wx * wx + wz * wz) * 3.6f;
            player.displayClientMessage(Component.literal(
                String.format("Wind: %.0f km/h | Nearest: %s", spd, s.name())), true);
        }
        return InteractionResult.SUCCESS;
    }
}
