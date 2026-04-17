package voicetunnelmod.fabric.runtime;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.lang.reflect.Constructor;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class VoiceTunnelRuntime {
    public static final Identifier C2S_VOICE_ID = new Identifier("voice_tunnel_mod", "svc_voice_c2s");
    public static final Identifier S2C_VOICE_ID = new Identifier("voice_tunnel_mod", "svc_voice_s2c");

    private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("vtm.enable", "true"));

    private static final ConcurrentLinkedQueue<Object> CLIENT_INBOUND = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Object> SERVER_INBOUND = new ConcurrentLinkedQueue<>();

    private static final Map<UUID, SocketAddress> PLAYER_TO_VIRTUAL = new ConcurrentHashMap<>();
    private static final Map<String, ServerPlayerEntity> VIRTUAL_TO_PLAYER = new ConcurrentHashMap<>();

    private static volatile Constructor<?> rawUdpPacketCtor;

    private VoiceTunnelRuntime() {
    }

    public static boolean enabled() {
        return ENABLED;
    }

    public static void onClientInbound(byte[] data) {
        Object packet = newRawUdpPacket(data, new InetSocketAddress("127.0.0.1", 65500));
        if (packet != null) {
            CLIENT_INBOUND.offer(packet);
        }
    }

    public static void onServerInbound(ServerPlayerEntity player, byte[] data) {
        SocketAddress address = PLAYER_TO_VIRTUAL.computeIfAbsent(player.getUuid(), uuid -> {
            int port = 10000 + Math.floorMod(uuid.hashCode(), 50000);
            InetSocketAddress virtual = new InetSocketAddress("127.0.0.1", port);
            VIRTUAL_TO_PLAYER.put(virtual.toString(), player);
            return virtual;
        });

        Object packet = newRawUdpPacket(data, address);
        if (packet != null) {
            SERVER_INBOUND.offer(packet);
        }
    }

    public static Object pollClientInboundBlocking() throws InterruptedException {
        while (ENABLED) {
            Object packet = CLIENT_INBOUND.poll();
            if (packet != null) {
                return packet;
            }
            Thread.sleep(2L);
        }
        return null;
    }

    public static Object pollServerInboundBlocking() throws InterruptedException {
        while (ENABLED) {
            Object packet = SERVER_INBOUND.poll();
            if (packet != null) {
                return packet;
            }
            Thread.sleep(2L);
        }
        return null;
    }

    public static void sendVoiceFromClient(byte[] data) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByteArray(data);
        ClientPlayNetworking.send(C2S_VOICE_ID, buf);
    }

    public static void sendVoiceFromServer(byte[] data, SocketAddress address) {
        ServerPlayerEntity player = VIRTUAL_TO_PLAYER.get(address.toString());
        if (player == null) {
            return;
        }

        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeByteArray(data);
        ServerPlayNetworking.send(player, S2C_VOICE_ID, buf);
    }

    public static void onPlayerDisconnect(ServerPlayerEntity player) {
        SocketAddress address = PLAYER_TO_VIRTUAL.remove(player.getUuid());
        if (address != null) {
            VIRTUAL_TO_PLAYER.remove(address.toString());
        }
    }

    private static Object newRawUdpPacket(byte[] data, SocketAddress address) {
        try {
            Constructor<?> ctor = rawUdpPacketCtor;
            if (ctor == null) {
                Class<?> clazz = Class.forName("de.maxhenkel.voicechat.plugins.impl.RawUdpPacketImpl");
                ctor = clazz.getConstructor(byte[].class, SocketAddress.class, long.class);
                rawUdpPacketCtor = ctor;
            }
            return ctor.newInstance(data, address, System.currentTimeMillis());
        } catch (Throwable t) {
            return null;
        }
    }
}
