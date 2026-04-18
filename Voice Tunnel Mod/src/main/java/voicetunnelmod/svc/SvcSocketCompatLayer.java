package voicetunnelmod.svc;

import java.util.Objects;

/**
 * 适配层入口：
 * - sendVoicePacket(): 由 SVC "发送 UDP" 改为发送隧道媒体帧
 * - onInboundVoicePacket(): 隧道收到语音数据后回灌给 SVC 解码链
 */
public class SvcSocketCompatLayer {

    private final TunnelTransport transport;
    private final VoicePacketSink inboundSink;

    public SvcSocketCompatLayer(TunnelTransport transport, VoicePacketSink inboundSink) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.inboundSink = Objects.requireNonNull(inboundSink, "inboundSink");
    }

    public boolean sendVoicePacket(byte[] encryptedVoicePayload) {
        return transport.sendVoice(encryptedVoicePayload);
    }

    public void onInboundVoicePacket(byte[] encryptedVoicePayload) {
        inboundSink.onVoicePacket(encryptedVoicePayload);
    }
}
