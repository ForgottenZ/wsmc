package voicetunnelmod.protocol;

import java.util.Arrays;

public class TunnelCodecSelfTest {

    public static void main(String[] args) {
        byte[] payload = new byte[]{1, 2, 3, 4, 5};
        TunnelFrame input = new TunnelFrame(FrameType.VOICE, Priority.MEDIA, 42L, 1700000000000L, payload);

        byte[] encoded = TunnelCodec.encode(input);
        TunnelFrame decoded = TunnelCodec.decode(encoded);

        if (!input.getType().equals(decoded.getType())) {
            throw new AssertionError("type mismatch");
        }
        if (!input.getPriority().equals(decoded.getPriority())) {
            throw new AssertionError("priority mismatch");
        }
        if (input.getSequence() != decoded.getSequence()) {
            throw new AssertionError("sequence mismatch");
        }
        if (input.getTimestampMs() != decoded.getTimestampMs()) {
            throw new AssertionError("timestamp mismatch");
        }
        if (!Arrays.equals(input.getPayload(), decoded.getPayload())) {
            throw new AssertionError("payload mismatch");
        }

        System.out.println("TunnelCodecSelfTest PASS");
    }
}
