package voicetunnelmod.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class TunnelCodec {

    private static final int MAGIC = 0x56544D31; // VTM1
    private static final byte VERSION = 1;

    private TunnelCodec() {
    }

    public static byte[] encode(TunnelFrame frame) {
        byte[] payload = frame.getPayload();
        ByteBuffer buf = ByteBuffer.allocate(4 + 1 + 1 + 1 + 8 + 8 + 4 + payload.length)
                .order(ByteOrder.BIG_ENDIAN);

        buf.putInt(MAGIC);
        buf.put(VERSION);
        buf.put(frame.getType().getId());
        buf.put((byte) (frame.getPriority() == Priority.CONTROL ? 0 : 1));
        buf.putLong(frame.getSequence());
        buf.putLong(frame.getTimestampMs());
        buf.putInt(payload.length);
        buf.put(payload);
        return buf.array();
    }

    public static TunnelFrame decode(byte[] data) {
        ByteBuffer buf = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        if (buf.remaining() < 27) {
            throw new IllegalArgumentException("Frame too short");
        }

        int magic = buf.getInt();
        if (magic != MAGIC) {
            throw new IllegalArgumentException("Invalid magic: " + Integer.toHexString(magic));
        }

        byte version = buf.get();
        if (version != VERSION) {
            throw new IllegalArgumentException("Unsupported version: " + version);
        }

        FrameType type = FrameType.fromId(buf.get());
        byte priorityRaw = buf.get();
        Priority priority = priorityRaw == 0 ? Priority.CONTROL : Priority.MEDIA;
        long seq = buf.getLong();
        long ts = buf.getLong();
        int len = buf.getInt();

        if (len < 0 || len > buf.remaining()) {
            throw new IllegalArgumentException("Invalid payload length: " + len);
        }

        byte[] payload = new byte[len];
        buf.get(payload);
        return new TunnelFrame(type, priority, seq, ts, payload);
    }
}
