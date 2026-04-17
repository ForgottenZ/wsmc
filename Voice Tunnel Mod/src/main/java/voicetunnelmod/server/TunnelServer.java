package voicetunnelmod.server;

import voicetunnelmod.protocol.TunnelCodec;
import voicetunnelmod.protocol.TunnelFrame;
import voicetunnelmod.svc.VoicePacketSink;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * 逻辑服务端管理器。
 * 这里不绑定具体 HTTP 容器；你可以在 Forge/Fabric 的 Netty WS handler 里调用其 API。
 */
public class TunnelServer {

    private final String expectedToken;
    private final VoicePacketSink inboundVoiceSink;
    private final Map<UUID, TunnelSession> sessions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService flusher = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "vtm-server-flusher");
        t.setDaemon(true);
        return t;
    });

    public TunnelServer(String expectedToken, VoicePacketSink inboundVoiceSink) {
        this.expectedToken = expectedToken;
        this.inboundVoiceSink = inboundVoiceSink;
        flusher.scheduleAtFixedRate(this::flushOnceAll, 0, 5, TimeUnit.MILLISECONDS);
    }

    public UUID attach(WebSocket webSocket) {
        UUID id = UUID.randomUUID();
        sessions.put(id, new TunnelSession(expectedToken, webSocket, inboundVoiceSink));
        return id;
    }

    public void detach(UUID sessionId) {
        sessions.remove(sessionId);
    }

    public void onBinary(UUID sessionId, ByteBuffer data) {
        TunnelSession session = sessions.get(sessionId);
        if (session == null) {
            return;
        }
        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        TunnelFrame frame = TunnelCodec.decode(bytes);
        session.onFrame(frame);
    }

    public void broadcastVoice(byte[] encryptedVoicePayload) {
        for (TunnelSession session : sessions.values()) {
            session.sendVoiceToClient(encryptedVoicePayload);
        }
    }

    private void flushOnceAll() {
        for (TunnelSession session : sessions.values()) {
            session.flushOne();
        }
    }

    public void close() {
        flusher.shutdownNow();
        sessions.clear();
    }
}
