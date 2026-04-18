package voicetunnelmod.client;

import voicetunnelmod.core.OverflowPolicy;
import voicetunnelmod.core.PriorityFrameQueue;
import voicetunnelmod.protocol.FrameType;
import voicetunnelmod.protocol.Priority;
import voicetunnelmod.protocol.TunnelCodec;
import voicetunnelmod.protocol.TunnelFrame;
import voicetunnelmod.svc.TunnelTransport;
import voicetunnelmod.svc.VoicePacketSink;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class TunnelClient implements WebSocket.Listener, TunnelTransport, AutoCloseable {

    private final String authToken;
    private final VoicePacketSink inboundVoiceSink;
    private final PriorityFrameQueue outboundQueue;
    private final AtomicLong seq = new AtomicLong(1);

    private final ExecutorService senderExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "vtm-client-sender");
        t.setDaemon(true);
        return t;
    });

    private volatile WebSocket webSocket;
    private volatile boolean running;

    public TunnelClient(String authToken, VoicePacketSink inboundVoiceSink) {
        this.authToken = Objects.requireNonNull(authToken, "authToken");
        this.inboundVoiceSink = Objects.requireNonNull(inboundVoiceSink, "inboundVoiceSink");
        this.outboundQueue = new PriorityFrameQueue(64, 256, OverflowPolicy.DROP_OLDEST);
    }

    public CompletableFuture<Void> connect(URI wsUri) {
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        running = true;

        return client.newWebSocketBuilder().buildAsync(wsUri, this).thenAccept(ws -> {
            this.webSocket = ws;
            enqueue(new TunnelFrame(FrameType.AUTH, Priority.CONTROL, nextSeq(), now(), authToken.getBytes()));
            startSenderLoop();
        });
    }

    private void startSenderLoop() {
        senderExecutor.submit(() -> {
            while (running) {
                outboundQueue.poll().ifPresentOrElse(frame -> {
                    WebSocket ws = webSocket;
                    if (ws != null) {
                        ws.sendBinary(ByteBuffer.wrap(TunnelCodec.encode(frame)), true);
                    }
                }, () -> sleep(3));
            }
        });
    }

    private boolean enqueue(TunnelFrame frame) {
        return outboundQueue.offer(frame);
    }

    @Override
    public boolean sendVoice(byte[] encryptedVoicePayload) {
        return enqueue(new TunnelFrame(FrameType.VOICE, Priority.MEDIA, nextSeq(), now(), encryptedVoicePayload));
    }

    @Override
    public boolean sendControl(byte[] controlPayload) {
        return enqueue(new TunnelFrame(FrameType.PING, Priority.CONTROL, nextSeq(), now(), controlPayload));
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        webSocket.request(1);
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        byte[] bytes = new byte[data.remaining()];
        data.get(bytes);
        TunnelFrame frame = TunnelCodec.decode(bytes);
        if (frame.getType() == FrameType.VOICE) {
            inboundVoiceSink.onVoicePacket(frame.getPayload());
        } else if (frame.getType() == FrameType.PING) {
            enqueue(new TunnelFrame(FrameType.PONG, Priority.CONTROL, nextSeq(), now(), frame.getPayload()));
        }
        webSocket.request(1);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        running = false;
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        running = false;
    }

    @Override
    public void close() {
        running = false;
        WebSocket ws = webSocket;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
        senderExecutor.shutdownNow();
    }

    private long nextSeq() {
        return seq.getAndIncrement();
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
