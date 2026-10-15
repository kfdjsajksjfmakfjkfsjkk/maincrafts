package com.example.tornadomod;
import com.example.tornadomod.client.ClientStorms;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Net {
    private static final String PROTOCOL = "1";
    private static SimpleChannel CHANNEL;

    public static void init() {
        CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TornadoMod.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
        );
        CHANNEL.registerMessage(0, SyncPacket.class, SyncPacket::encode, SyncPacket::decode, SyncPacket::handle);
    }

    public static void syncStorms(ServerLevel level, List<Storm> storms) {
        SyncPacket pkt = new SyncPacket(storms);
        for (ServerPlayer p : level.players()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), pkt);
        }
    }

    public static class SyncPacket {
        private final List<Storm> storms;

        SyncPacket(List<Storm> storms) {
            this.storms = new ArrayList<>(storms);
        }

        static void encode(SyncPacket pkt, FriendlyByteBuf buf) {
            buf.writeInt(pkt.storms.size());
            for (Storm s : pkt.storms) {
                buf.writeInt(s.id);
                buf.writeInt(s.kind.ordinal());
                buf.writeInt(s.maxLevel);
                buf.writeInt(s.level);
                buf.writeDouble(s.x);
                buf.writeDouble(s.y);
                buf.writeDouble(s.z);
                buf.writeDouble(s.vx);
                buf.writeDouble(s.vz);
                buf.writeInt(s.age);
                buf.writeInt(s.life);
                buf.writeFloat(s.f);
            }
        }

        static SyncPacket decode(FriendlyByteBuf buf) {
            int count = buf.readInt();
            List<Storm> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                int id       = buf.readInt();
                int kindOrd  = buf.readInt();
                int maxLevel = buf.readInt();
                int level    = buf.readInt();
                double x = buf.readDouble(), y = buf.readDouble(), z = buf.readDouble();
                double vx = buf.readDouble(), vz = buf.readDouble();
                int age = buf.readInt(), life = buf.readInt();
                float f = buf.readFloat();

                Storm s = new Storm(id, Storm.Kind.values()[kindOrd], maxLevel, x, y, z);
                s.level = level;
                s.vx = vx; s.vz = vz;
                s.age = age; s.life = life;
                s.f = f;
                list.add(s);
            }
            return new SyncPacket(list);
        }

        static void handle(SyncPacket pkt, Supplier<net.minecraftforge.network.NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> ClientStorms.set(pkt.storms));
            ctx.get().setPacketHandled(true);
        }
    }
}
