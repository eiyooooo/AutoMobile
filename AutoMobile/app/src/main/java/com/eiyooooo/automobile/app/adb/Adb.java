package com.eiyooooo.automobile.app.adb;

import android.hardware.usb.UsbDevice;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

import com.eiyooooo.automobile.app.buffer.Buffer;
import com.eiyooooo.automobile.app.buffer.BufferStream;

public class Adb {
    private final AdbChannel channel;
    private int localIdPool = 1;
    private int MAX_DATA = AdbProtocol.CONNECT_MAXDATA;
    private final ConcurrentHashMap<Integer, BufferStream> connectionStreams = new ConcurrentHashMap<>(10);
    private final ConcurrentHashMap<Integer, BufferStream> openStreams = new ConcurrentHashMap<>(5);
    private final Buffer sendBuffer = new Buffer();

    private final Thread handleInThread = new Thread(this::handleIn);
    private final Thread handleOutThread = new Thread(this::handleOut);

    public Adb(String host, int port, AdbKeyPair keyPair) throws Exception {
        channel = new TcpChannel(host, port);
        connect(keyPair);
    }

    public Adb(UsbDevice usbDevice, AdbKeyPair keyPair) throws Exception {
        channel = new UsbChannel(usbDevice);
        connect(keyPair);
    }

    private void connect(AdbKeyPair keyPair) throws Exception {
        // connect and auth
        channel.write(AdbProtocol.generateConnect());
        AdbProtocol.AdbMessage message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
        if (message.command == AdbProtocol.CMD_AUTH) {
            channel.write(AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_SIGNATURE, keyPair.signPayload(message.payload)));
            message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
            if (message.command == AdbProtocol.CMD_AUTH) {
                channel.write(AdbProtocol.generateAuth(AdbProtocol.AUTH_TYPE_RSA_PUBLIC, keyPair.publicKeyBytes));
                message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
            }
        }
        if (message.command != AdbProtocol.CMD_CNXN) {
            channel.close();
            throw new Exception("ADB connect error");
        }
        MAX_DATA = message.arg1;
        // start handle thread
        handleInThread.setPriority(Thread.MAX_PRIORITY);
        handleInThread.start();
        handleOutThread.start();
    }

    private BufferStream open(String destination, boolean canMultipleSend) throws InterruptedException {
        int localId = localIdPool++ * (canMultipleSend ? 1 : -1);
        sendBuffer.write(AdbProtocol.generateOpen(localId, destination));
        BufferStream bufferStream;
        do {
            synchronized (this) {
                wait();
            }
            bufferStream = openStreams.get(localId);
        } while (bufferStream == null);
        openStreams.remove(localId);
        return bufferStream;
    }

    public String restartOnTcpip(int port) throws InterruptedException {
        BufferStream bufferStream = open("tcpip:" + port, false);
        do {
            synchronized (this) {
                wait();
            }
        } while (!bufferStream.isClosed());
        return new String(bufferStream.readByteArrayBeforeClose().array());
    }

    public void pushFile(InputStream file, String remotePath) throws Exception {
        // open push channel
        BufferStream bufferStream = open("sync:", false);
        // send file name
        String sendString = remotePath + ",33206";
        byte[] bytes = sendString.getBytes();
        bufferStream.write(AdbProtocol.generateSyncHeader("SEND", sendString.length()));
        bufferStream.write(ByteBuffer.wrap(bytes));
        // push file
        byte[] byteArray = new byte[10240 - 8];
        int len = file.read(byteArray, 0, byteArray.length);
        do {
            bufferStream.write(AdbProtocol.generateSyncHeader("DATA", len));
            bufferStream.write(ByteBuffer.wrap(byteArray, 0, len));
            len = file.read(byteArray, 0, byteArray.length);
        } while (len > 0);
        file.close();
        // done, set time to 2024.1.1 0:0
        bufferStream.write(AdbProtocol.generateSyncHeader("DONE", 1704038400));
        bufferStream.write(AdbProtocol.generateSyncHeader("QUIT", 0));
        do {
            synchronized (this) {
                wait();
            }
        } while (!bufferStream.isClosed());
    }

    public String runAdbCmd(String cmd) throws InterruptedException {
        BufferStream bufferStream = open("shell:" + cmd, true);
        do {
            synchronized (this) {
                wait();
            }
        } while (!bufferStream.isClosed());
        return new String(bufferStream.readByteArrayBeforeClose().array());
    }

    public BufferStream getShell() throws InterruptedException {
        return open("shell:", true);
    }

    public BufferStream tcpForward(int port) throws IOException, InterruptedException {
        BufferStream bufferStream = open("tcp:" + port, true);
        if (bufferStream.isClosed()) throw new IOException("error forward");
        return bufferStream;
    }

    public BufferStream localSocketForward(String socketName) throws IOException, InterruptedException {
        BufferStream bufferStream = open("localabstract:" + socketName, true);
        if (bufferStream.isClosed()) throw new IOException("error forward");
        return bufferStream;
    }

    private void handleIn() {
        try {
            while (!Thread.interrupted()) {
                AdbProtocol.AdbMessage message = AdbProtocol.AdbMessage.parseAdbMessage(channel);
                BufferStream bufferStream = connectionStreams.get(message.arg1);
                boolean isNeedNotify = bufferStream == null;
                // create new stream
                if (isNeedNotify) bufferStream = createNewStream(message.arg1, message.arg0, message.arg1 > 0);
                switch (message.command) {
                    case AdbProtocol.CMD_OKAY:
                        bufferStream.setCanWrite(true);
                        break;
                    case AdbProtocol.CMD_WRTE:
                        bufferStream.pushSource(message.payload);
                        sendBuffer.write(AdbProtocol.generateOkay(message.arg1, message.arg0));
                        break;
                    case AdbProtocol.CMD_CLSE:
                        bufferStream.close();
                        isNeedNotify = true;
                        break;
                }
                if (isNeedNotify) {
                    synchronized (this) {
                        notifyAll();
                    }
                }
            }
        } catch (Exception ignored) {
            close();
        }
    }

    private void handleOut() {
        try {
            while (!Thread.interrupted()) {
                channel.write(sendBuffer.readNext());
                if (!sendBuffer.isEmpty()) channel.write(sendBuffer.read(sendBuffer.getSize()));
                channel.flush();
            }
        } catch (IOException | InterruptedException ignored) {
            close();
        }
    }

    private BufferStream createNewStream(int localId, int remoteId, boolean canMultipleSend) throws Exception {
        return new BufferStream(false, canMultipleSend, new BufferStream.UnderlySocketFunction() {
            @Override
            public void connect(BufferStream bufferStream) {
                connectionStreams.put(localId, bufferStream);
                openStreams.put(localId, bufferStream);
            }

            @Override
            public void write(BufferStream bufferStream, ByteBuffer buffer) {
                while (buffer.hasRemaining()) {
                    byte[] byteArray = new byte[Math.min(MAX_DATA - 128, buffer.remaining())];
                    buffer.get(byteArray);
                    sendBuffer.write(AdbProtocol.generateWrite(localId, remoteId, byteArray));
                }
            }

            @Override
            public void flush(BufferStream bufferStream) {
                sendBuffer.write(AdbProtocol.generateClose(localId, remoteId));
            }

            @Override
            public void close(BufferStream bufferStream) {
                connectionStreams.remove(localId);
                sendBuffer.write(AdbProtocol.generateClose(localId, remoteId));
            }
        });
    }

    public void close() {
        for (Object bufferStream : connectionStreams.values().toArray()) ((BufferStream) bufferStream).close();
        handleInThread.interrupt();
        handleOutThread.interrupt();
        channel.close();
    }

}
