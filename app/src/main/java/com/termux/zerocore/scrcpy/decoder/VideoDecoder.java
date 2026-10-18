package com.termux.zerocore.scrcpy.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoDecoder {
    private static final String MIME_TYPE = "video/avc";

    /**
     * The codec currently in use. Written by the network thread in {@link Worker#configure} and by
     * the main thread in {@link #releaseCodec()}, read by {@link Worker#decodeSample} and the worker
     * loop. Marked volatile so those reads never observe a partially published codec.
     */
    private volatile MediaCodec mCodec;
    private Worker mWorker;
    private final AtomicBoolean mIsConfigured = new AtomicBoolean(false);

    public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
        Worker worker = mWorker;
        if (worker != null) {
            worker.decodeSample(data, offset, size, presentationTimeUs, flags);
        }
    }

    public void configure(Surface surface, int width, int height, ByteBuffer csd0, ByteBuffer csd1) {
        Worker worker = mWorker;
        if (worker != null) {
            worker.configure(surface, width, height, csd0, csd1);
        }
    }

    public void start() {
        if (mWorker == null) {
            mWorker = new Worker();
            mWorker.setRunning(true);
            mWorker.start();
        }
    }

    public void stop() {
        Worker worker = mWorker;
        mWorker = null;
        // Mark unconfigured *before* touching the codec so decodeSample() stops submitting frames
        // while the codec is being torn down.
        mIsConfigured.set(false);
        if (worker != null) {
            worker.setRunning(false);
            try {
                // Wait for the worker loop to leave dequeueOutputBuffer() so it can no longer race
                // with the stop/release below. Bounded so a stuck codec cannot hang the main thread.
                worker.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        releaseCodec();
    }

    /**
     * Stops and releases the current codec if one exists. Safe to call when the decoder was never
     * configured (mCodec is null) — the previous implementation called mCodec.stop() unconditionally
     * and crashed with a NullPointerException on stop() before the first configure().
     */
    private synchronized void releaseCodec() {
        MediaCodec codec = mCodec;
        mCodec = null;
        if (codec == null) {
            return;
        }
        try {
            codec.stop();
        } catch (IllegalStateException e) {
            // Codec was never started or already stopped; still try to release it below.
        }
        try {
            codec.release();
        } catch (IllegalStateException e) {
            // Ignore, codec already released.
        }
    }

    private class Worker extends Thread {

        private final AtomicBoolean mIsRunning = new AtomicBoolean(false);

        Worker() {
        }

        private void setRunning(boolean isRunning) {
            mIsRunning.set(isRunning);
        }

        private void configure(Surface surface, int width, int height, ByteBuffer csd0, ByteBuffer csd1) {
            synchronized (VideoDecoder.this) {
                // Release any previous codec before creating a new one.
                MediaCodec oldCodec = mCodec;
                mIsConfigured.set(false);
                mCodec = null;
                if (oldCodec != null) {
                    try {
                        oldCodec.stop();
                    } catch (IllegalStateException e) {
                        // Ignore.
                    }
                    try {
                        oldCodec.release();
                    } catch (IllegalStateException e) {
                        // Ignore.
                    }
                }

                MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
                format.setByteBuffer("csd-0", csd0);
                format.setByteBuffer("csd-1", csd1);
                MediaCodec codec = null;
                try {
                    codec = MediaCodec.createDecoderByType(MIME_TYPE);
                    codec.configure(format, surface, null, 0);
                    codec.start();
                    mCodec = codec;
                    mIsConfigured.set(true);
                } catch (IOException | RuntimeException e) {
                    // Do not take the whole app down if this device cannot decode the stream;
                    // report the failure and leave the decoder unconfigured.
                    mIsConfigured.set(false);
                    mCodec = null;
                    if (codec != null) {
                        try {
                            codec.release();
                        } catch (IllegalStateException ignored) {
                            // Ignore.
                        }
                    }
                    throw new IllegalStateException("Failed to configure video decoder", e);
                }
            }
        }

        public void decodeSample(byte[] data, int offset, int size, long presentationTimeUs, int flags) {
            MediaCodec codec = mCodec;
            if (!mIsConfigured.get() || !mIsRunning.get() || codec == null) {
                return;
            }
            try {
                int index = codec.dequeueInputBuffer(-1);
                if (index >= 0) {
                    ByteBuffer buffer;

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        buffer = codec.getInputBuffers()[index];
                        buffer.clear();
                    } else {
                        buffer = codec.getInputBuffer(index);
                    }
                    if (buffer != null) {
                        buffer.put(data, offset, size);
                        codec.queueInputBuffer(index, 0, size, presentationTimeUs, flags);
                    }
                }
            } catch (IllegalStateException e) {
                // Codec stopped/released concurrently (e.g. videoDecoder.stop() from onPause).
            }
        }

        @Override
        public void run() {
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (mIsRunning.get()) {
                MediaCodec codec = mCodec;
                if (mIsConfigured.get() && codec != null) {
                    try {
                        int index = codec.dequeueOutputBuffer(info, 0);
                        if (index >= 0) {
                            // setting true is telling system to render frame onto Surface
                            codec.releaseOutputBuffer(index, true);
                            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                                break;
                            }
                        }
                    } catch (IllegalStateException e) {
                        // Codec was stopped/released; exit the render loop gracefully.
                        break;
                    }
                } else {
                    // just waiting to be configured, then decode and render
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
    }
}
