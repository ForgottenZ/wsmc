package voicetunnelmod.transport;

import voicetunnelmod.svc.TunnelTransport;
import voicetunnelmod.svc.VoicePacketSink;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用于本地演示/联调的内存隧道，不依赖网络。
 */
public class InMemoryTunnelPipe {

    private final Endpoint client;
    private final Endpoint server;

    public InMemoryTunnelPipe(VoicePacketSink clientSink, VoicePacketSink serverSink) {
        Endpoint clientEndpoint = new Endpoint("client", clientSink);
        Endpoint serverEndpoint = new Endpoint("server", serverSink);

        clientEndpoint.peer = serverEndpoint;
        serverEndpoint.peer = clientEndpoint;

        this.client = clientEndpoint;
        this.server = serverEndpoint;
    }

    public TunnelTransport clientTransport() {
        return client;
    }

    public TunnelTransport serverTransport() {
        return server;
    }

    public void close() {
        client.close();
        server.close();
    }

    private static class Endpoint implements TunnelTransport {
        private final String name;
        private final VoicePacketSink sink;
        private final ExecutorService deliverExecutor;
        private volatile Endpoint peer;
        private volatile boolean closed;

        private Endpoint(String name, VoicePacketSink sink) {
            this.name = Objects.requireNonNull(name, "name");
            this.sink = Objects.requireNonNull(sink, "sink");
            this.deliverExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "vtm-inmem-" + name);
                t.setDaemon(true);
                return t;
            });
        }

        @Override
        public boolean sendVoice(byte[] encryptedVoicePayload) {
            if (closed || peer == null || peer.closed) {
                return false;
            }
            byte[] copy = encryptedVoicePayload == null ? new byte[0] : encryptedVoicePayload.clone();
            peer.deliverExecutor.submit(() -> peer.sink.onVoicePacket(copy));
            return true;
        }

        @Override
        public boolean sendControl(byte[] controlPayload) {
            // 演示里控制包与语音包复用同一交付通路
            return sendVoice(controlPayload);
        }

        private void close() {
            closed = true;
            deliverExecutor.shutdownNow();
        }
    }
}
