package voicetunnelmod.server;

import voicetunnelmod.core.OverflowPolicy;
import voicetunnelmod.core.PriorityFrameQueue;
import voicetunnelmod.protocol.FrameType;
import voicetunnelmod.protocol.Priority;
import voicetunnelmod.protocol.TunnelCodec;
import voicetunnelmod.protocol.TunnelFrame;
import voicetunnelmod.svc.VoicePacketSink;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

public class TunnelSession {

    private final String expectedToken;
    private final WebSocket webSocket;
    private final VoicePacketSink inboundVoiceSink;
    private final PriorityFrameQueue outboundQueue;
    private final AtomicLong seq = new AtomicLong(1);

    private volatile boolean authenticated;

    public TunnelSession(String expectedToken, WebSocket webSocket, VoicePacketSink inboundVoiceSink) {
        this.expectedToken = expectedToken;
        this.webSocket = webSocket;
        this.inboundVoiceSink = inboundVoiceSink;
        this.outboundQueue = new PriorityFrameQueue(64, 512, OverflowPolicy.DROP_OLDEST);
    }

    public void onFrame(TunnelFrame frame) {
        switch (frame.getType()) {
            case AUTH -> handleAuth(frame);
            case VOICE -> {
                if (authenticated) {
                    inboundVoiceSink.onVoicePacket(frame.getPayload());
                }
            }
            case PING -> enqueue(new TunnelFrame(FrameType.PONG, Priority.CONTROL, nextSeq(), now(), frame.getPayload()));
            default -> {
            }
        }
    }

    private void handleAuth(TunnelFrame frame) {
        String provided = new String(frame.getPayload());
        if (expectedToken.equals(provided)) {
            authenticated = true;
            enqueue(new TunnelFrame(FrameType.AUTH_ACK, Priority.CONTROL, nextSeq(), now(), new byte[0]));
        } else {
            enqueue(new TunnelFrame(FrameType.CLOSE, Priority.CONTROL, nextSeq(), now(), "auth_failed".getBytes()));
        }
    }

    public boolean sendVoiceToClient(byte[] encryptedVoicePayload) {
        return enqueue(new TunnelFrame(FrameType.VOICE, Priority.MEDIA, nextSeq(), now(), encryptedVoicePayload));
    }

    public void flushOne() {
        outboundQueue.poll().ifPresent(frame -> webSocket.sendBinary(ByteBuffer.wrap(TunnelCodec.encode(frame)), true));
    }

    private boolean enqueue(TunnelFrame frame) {
        return outboundQueue.offer(frame);
    }

    private long nextSeq() {
        return seq.getAndIncrement();
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
