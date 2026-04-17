package voicetunnelmod.core;

import voicetunnelmod.protocol.Priority;
import voicetunnelmod.protocol.TunnelFrame;

import java.util.ArrayDeque;
import java.util.Optional;

/**
 * Control 帧总是优先于媒体帧，媒体帧队列溢出时默认丢旧包。
 */
public class PriorityFrameQueue {

    private final int controlCapacity;
    private final int mediaCapacity;
    private final OverflowPolicy mediaOverflowPolicy;

    private final ArrayDeque<TunnelFrame> controlQueue;
    private final ArrayDeque<TunnelFrame> mediaQueue;

    public PriorityFrameQueue(int controlCapacity, int mediaCapacity, OverflowPolicy mediaOverflowPolicy) {
        if (controlCapacity <= 0 || mediaCapacity <= 0) {
            throw new IllegalArgumentException("capacities must be > 0");
        }
        this.controlCapacity = controlCapacity;
        this.mediaCapacity = mediaCapacity;
        this.mediaOverflowPolicy = mediaOverflowPolicy;
        this.controlQueue = new ArrayDeque<>(controlCapacity);
        this.mediaQueue = new ArrayDeque<>(mediaCapacity);
    }

    public synchronized boolean offer(TunnelFrame frame) {
        if (frame.getPriority() == Priority.CONTROL) {
            if (controlQueue.size() >= controlCapacity) {
                return false;
            }
            return controlQueue.offer(frame);
        }

        if (mediaQueue.size() < mediaCapacity) {
            return mediaQueue.offer(frame);
        }

        if (mediaOverflowPolicy == OverflowPolicy.DROP_OLDEST) {
            mediaQueue.poll();
            return mediaQueue.offer(frame);
        }

        return false;
    }

    public synchronized Optional<TunnelFrame> poll() {
        TunnelFrame control = controlQueue.poll();
        if (control != null) {
            return Optional.of(control);
        }
        return Optional.ofNullable(mediaQueue.poll());
    }

    public synchronized int size() {
        return controlQueue.size() + mediaQueue.size();
    }
}
