package com.eiyooooo.automobile.app.adb;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

import com.eiyooooo.automobile.app.buffer.Buffer;
import com.eiyooooo.automobile.app.entity.AppData;

public class UsbChannel implements AdbChannel {
    private final UsbDeviceConnection usbConnection;
    private UsbInterface usbInterface = null;
    private UsbEndpoint endpointIn = null;
    private UsbEndpoint endpointOut = null;
    private final Buffer sourceBuffer = new Buffer();
    private final Thread readBackgroundThread = new Thread(this::readBackground);
    private final LinkedList<UsbRequest> mInRequestPool = new LinkedList<>();

    public UsbChannel(UsbDevice usbDevice) throws IOException {
        // connect USB device
        if (AppData.usbManager == null) throw new IOException("not have usbManager");
        usbConnection = AppData.usbManager.openDevice(usbDevice);
        if (usbConnection == null) return;
        // loop to find the adb interface
        for (int i = 0; i < usbDevice.getInterfaceCount(); i++) {
            UsbInterface tmpUsbInterface = usbDevice.getInterface(i);
            if ((tmpUsbInterface.getInterfaceClass() == UsbConstants.USB_CLASS_VENDOR_SPEC) && (tmpUsbInterface.getInterfaceSubclass() == 66) && (tmpUsbInterface.getInterfaceProtocol() == 1)) {
                usbInterface = tmpUsbInterface;
                break;
            }
        }
        if (usbInterface == null) return;
        // claim the interface
        if (usbConnection.claimInterface(usbInterface, true)) {
            // loop to find the bulk endpoints
            for (int i = 0; i < usbInterface.getEndpointCount(); i++) {
                UsbEndpoint endpoint = usbInterface.getEndpoint(i);
                if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) endpointOut = endpoint;
                    else if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) endpointIn = endpoint;
                    if (endpointIn != null && endpointOut != null) {
                        readBackgroundThread.start();
                        return;
                    }
                }
            }
        }
        throw new IOException("USB connection error");
    }

    @Override
    public void write(ByteBuffer data) throws IOException {
        while (data.remaining() > 0) {
            // read header
            byte[] header = new byte[AdbProtocol.ADB_HEADER_LENGTH];
            data.get(header);
            usbConnection.bulkTransfer(endpointOut, header, header.length, 1000);
            // read payload
            int payloadLength = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).getInt(12);
            if (payloadLength > 0) {
                byte[] payload = new byte[payloadLength];
                data.get(payload);
                usbConnection.bulkTransfer(endpointOut, payload, payload.length, 1000);
            }
        }
    }

    @Override
    public ByteBuffer read(int size) throws InterruptedException, IOException {
        return sourceBuffer.read(size);
    }

    private void readBackground() {
        try {
            while (!Thread.interrupted()) {
                // read header
                ByteBuffer header = readRequest(AdbProtocol.ADB_HEADER_LENGTH).order(ByteOrder.LITTLE_ENDIAN);
                if (header.remaining() < AdbProtocol.ADB_HEADER_LENGTH) throw new IOException("read error");
                sourceBuffer.write(header);
                // read payload
                int payloadLength = header.getInt(12);
                if (payloadLength > 0) {
                    ByteBuffer payload = readRequest(payloadLength);
                    sourceBuffer.write(payload);
                }
            }
        } catch (IOException ignored) {
            sourceBuffer.close();
        }
    }

    private ByteBuffer readRequest(int len) throws IOException {
        // get a UsbRequest from pool
        UsbRequest request;
        if (mInRequestPool.isEmpty()) {
            request = new UsbRequest();
            request.initialize(usbConnection, endpointIn);
        } else request = mInRequestPool.removeFirst();
        ByteBuffer data = ByteBuffer.allocate(len);
        request.setClientData(data);
        // queue the request
        if (!request.queue(data, len)) throw new IOException("fail to queue read UsbRequest");
        // wait for the request
        while (true) {
            UsbRequest wait = usbConnection.requestWait();
            if (wait == null) throw new IOException("Connection.requestWait return null");
            if (wait.getEndpoint() == endpointIn) {
                ByteBuffer clientData = (ByteBuffer) wait.getClientData();
                mInRequestPool.add(request);
                if (clientData == data) {
                    data.flip();
                    return data;
                }
            }
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() {
        try {
            readBackgroundThread.interrupt();
            usbConnection.releaseInterface(usbInterface);
            // send a broken packet to make USB disconnect and reconnect
            usbConnection.bulkTransfer(endpointOut, new byte[40], 40, 2000);
            sourceBuffer.close();
            usbConnection.close();
        } catch (Exception ignored) {
        }
    }

}