package voicetunnelmod.svc;

public interface VoicePacketSink {
    void onVoicePacket(byte[] encryptedVoicePayload);
}
