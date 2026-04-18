package voicetunnelmod.protocol;

public enum FrameType {
    AUTH((byte) 1),
    AUTH_ACK((byte) 2),
    PING((byte) 3),
    PONG((byte) 4),
    VOICE((byte) 10),
    CLOSE((byte) 127);

    private final byte id;

    FrameType(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

    public static FrameType fromId(byte id) {
        for (FrameType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown frame type id: " + id);
    }
}
