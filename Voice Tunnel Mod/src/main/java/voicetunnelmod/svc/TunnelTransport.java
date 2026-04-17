package voicetunnelmod.svc;

public interface TunnelTransport {
    boolean sendVoice(byte[] encryptedVoicePayload);
    boolean sendControl(byte[] controlPayload);
}
