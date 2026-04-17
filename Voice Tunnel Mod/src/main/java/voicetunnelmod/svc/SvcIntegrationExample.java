package voicetunnelmod.svc;

import voicetunnelmod.client.TunnelClient;

import java.net.URI;
import java.util.Objects;

/**
 * 示例：如何把兼容层挂到 SVC。
 *
 * 真实接入时，将 onInboundVoice 回调替换为 SVC 的实际入站处理函数即可。
 */
public class SvcIntegrationExample {

    private final SvcSocketCompatLayer compatLayer;
    private final TunnelClient client;

    public SvcIntegrationExample(String authToken, VoicePacketSink inboundVoiceSink) {
        Objects.requireNonNull(inboundVoiceSink, "inboundVoiceSink");
        this.client = new TunnelClient(authToken, inboundVoiceSink);
        this.compatLayer = new SvcSocketCompatLayer(client, inboundVoiceSink);
    }

    public void connect(String wsUrl) {
        client.connect(URI.create(wsUrl));
    }

    // 用这个替代 SVC 原来的 UDP send
    public boolean sendVoice(byte[] encryptedVoicePayload) {
        return compatLayer.sendVoicePacket(encryptedVoicePayload);
    }

    public void close() {
        client.close();
    }
}
