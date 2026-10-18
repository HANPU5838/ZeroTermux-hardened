package com.termux.zerocore.control;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal server-sent-events stream for {@code /control/events}.
 *
 * <p>Every audit event and every terminal output event published through
 * {@link ControlRegistry} is forwarded as one SSE frame. When idle a comment
 * heartbeat keeps the connection alive; the stream unsubscribes on close.</p>
 */
public final class ControlEventStream extends InputStream implements ControlEventListener {

    private static final long HEARTBEAT_MILLIS = 15_000L;

    private final LinkedBlockingQueue<String> frames = new LinkedBlockingQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private byte[] pending = new byte[0];
    private int pendingOffset = 0;

    public ControlEventStream() {
        ControlRegistry.get().subscribe(this);
        frames.offer(": ZeroTermux control event stream\n\n");
        frames.offer("event: ready\ndata: {\"ok\":true}\n\n");
    }

    @Override
    public void onEvent(ControlEvent event) {
        if (closed.get() || event == null) return;
        frames.offer(event.toSse());
    }

    @Override
    public int read() throws IOException {
        byte[] one = new byte[1];
        int read = read(one, 0, 1);
        return read <= 0 ? -1 : one[0] & 0xFF;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (closed.get()) return -1;
        if (pendingOffset >= pending.length) {
            try {
                String frame = frames.poll(HEARTBEAT_MILLIS, TimeUnit.MILLISECONDS);
                if (frame == null) {
                    frame = ": ping " + System.currentTimeMillis() + "\n\n";
                }
                pending = frame.getBytes(StandardCharsets.UTF_8);
                pendingOffset = 0;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                InterruptedIOException ioException = new InterruptedIOException("event stream interrupted");
                ioException.initCause(e);
                throw ioException;
            }
        }
        int available = pending.length - pendingOffset;
        int count = Math.min(available, length);
        System.arraycopy(pending, pendingOffset, buffer, offset, count);
        pendingOffset += count;
        return count;
    }

    @Override
    public int available() {
        return pending.length - pendingOffset;
    }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            ControlRegistry.get().unsubscribe(this);
        }
        super.close();
    }
}
