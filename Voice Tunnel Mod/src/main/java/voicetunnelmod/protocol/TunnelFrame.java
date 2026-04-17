package voicetunnelmod.protocol;

import java.util.Arrays;
import java.util.Objects;

public final class TunnelFrame {
    private final FrameType type;
    private final Priority priority;
    private final long sequence;
    private final long timestampMs;
    private final byte[] payload;

    public TunnelFrame(FrameType type, Priority priority, long sequence, long timestampMs, byte[] payload) {
        this.type = Objects.requireNonNull(type, "type");
        this.priority = Objects.requireNonNull(priority, "priority");
        this.sequence = sequence;
        this.timestampMs = timestampMs;
        this.payload = payload == null ? new byte[0] : payload;
    }

    public FrameType getType() {
        return type;
    }

    public Priority getPriority() {
        return priority;
    }

    public long getSequence() {
        return sequence;
    }

    public long getTimestampMs() {
        return timestampMs;
    }

    public byte[] getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "TunnelFrame{" +
                "type=" + type +
                ", priority=" + priority +
                ", sequence=" + sequence +
                ", timestampMs=" + timestampMs +
                ", payloadLen=" + payload.length +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TunnelFrame that)) return false;
        return sequence == that.sequence
                && timestampMs == that.timestampMs
                && type == that.type
                && priority == that.priority
                && Arrays.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(type, priority, sequence, timestampMs);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }
}
