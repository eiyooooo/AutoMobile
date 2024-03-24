package com.eiyooooo.automobile.app.buffer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class BufferStream {
    private boolean isClosed = false;
    private boolean canWrite;
    private final boolean canMultipleSend;

    private final Buffer source = new Buffer();
    private final Buffer sink = new Buffer();
    private final UnderlySocketFunction underlySocketFunction;

    // canWrite is set to be compatible with some underlying connections that cannot be sent at any time
    // canMultipleSend is set to be compatible with some upper-layer applications that need to send one by one
    public BufferStream(boolean canWrite, boolean canMultipleSend, UnderlySocketFunction underlySocketFunction) throws Exception {
        this.canWrite = canWrite;
        this.canMultipleSend = canMultipleSend;
        this.underlySocketFunction = underlySocketFunction;
        underlySocketFunction.connect(this);
    }

    public BufferStream(boolean canWrite, UnderlySocketFunction underlySocketFunction) throws Exception {
        this(canWrite, true, underlySocketFunction);
    }

    public BufferStream(UnderlySocketFunction underlySocketFunction) throws Exception {
        this(true, true, underlySocketFunction);
    }

    public void pushSource(ByteBuffer byteBuffer) {
        if (byteBuffer != null) source.write(byteBuffer);
    }

    public byte readByte() throws InterruptedException, IOException {
        return readByteArray(1).get();
    }

    public short readShort() throws InterruptedException, IOException {
        return readByteArray(2).getShort();
    }

    public int readInt() throws InterruptedException, IOException {
        return readByteArray(4).getInt();
    }

    public long readLong() throws InterruptedException, IOException {
        return readByteArray(8).getLong();
    }

    public ByteBuffer readAllBytes() throws InterruptedException, IOException {
        return readByteArray(getSize());
    }

    public ByteBuffer readByteArray(int size) throws InterruptedException, IOException {
        if (isClosed) throw new IOException("connection is closed");
        return source.read(size);
    }

    public ByteBuffer readByteArrayBeforeClose() {
        return source.readByteArrayBeforeClose();
    }

    public void write(ByteBuffer byteBuffer) throws Exception {
        if (isClosed) throw new IOException("connection is closed");
        sink.write(byteBuffer);
        pollSink();
    }

    public void setCanWrite(boolean canWrite) throws Exception {
        if (isClosed) return;
        this.canWrite = canWrite;
        if (canWrite) pollSink();
    }

    private synchronized void pollSink() throws Exception {
        if (canWrite && !sink.isEmpty()) underlySocketFunction.write(this, canMultipleSend ? sink.read(sink.getSize()) : sink.readNext());
    }

    public boolean isEmpty() {
        return source.isEmpty();
    }

    public int getSize() {
        return source.getSize();
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void flush() throws Exception {
        underlySocketFunction.flush(this);
    }

    public void close() {
        if (isClosed) return;
        isClosed = true;
        source.close();
        sink.close();
        try {
            underlySocketFunction.close(this);
        } catch (Exception ignored) {
        }
    }

    public interface UnderlySocketFunction {
        void connect(BufferStream bufferStream) throws Exception;

        void write(BufferStream bufferStream, ByteBuffer buffer) throws Exception;

        void flush(BufferStream bufferStream) throws Exception;

        void close(BufferStream bufferStream) throws Exception;
    }
}
