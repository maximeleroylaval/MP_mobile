package ca.ulaval.ima.mp.models.gateway.voice;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

import ca.ulaval.ima.mp.MainActivity;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

class VoiceSocket {

    private static final int BYTE_SIZE = 8;
    private static final int SHORT_SIZE = 16;
    private static final int SHORT_BYTES = SHORT_SIZE / BYTE_SIZE;
    private static final int INTEGER_SIZE = 32;
    private static final int INTEGER_BYTES = INTEGER_SIZE / BYTE_SIZE;

    static final String PROTOCOL = "udp";
    static final String ENCRYPTION_MODE = "xsalsa20_poly1305";
    private static final int DISCOVERY_PACKET_LENGTH = 70;

    private Ready ready;
    private DatagramSocket socket;
    private Callback discoveryCallback = null;
    private VoiceSendTask sendTask;
    private VoiceReceiveTask receiveTask;

    public interface Callback {
        void onSuccess(String ip, Integer port);
        void onFailure(IOException e);
    }

    VoiceSocket(Ready ready) throws IOException {
        this.ready = ready;
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            log("Failed to open socket connection");
            e.printStackTrace();
            throw e;
        }
    }

    void performIpDiscovery(Callback discoveryCallback) {
        this.discoveryCallback = discoveryCallback;
        this.listen();
    }

    private void parseDiscovery(ByteBuf buf) {
        String ip = getNullTerminatedString(buf, INTEGER_BYTES);
        int port = buf.getUnsignedShortLE(DISCOVERY_PACKET_LENGTH - SHORT_BYTES);
        discoveryCallback.onSuccess(ip, port);
        discoveryCallback = null;
    }

    private static String getNullTerminatedString(ByteBuf buffer, int offset) {
        buffer.skipBytes(offset);
        ByteArrayOutputStream os = new ByteArrayOutputStream(15);
        byte c;
        while ((c = buffer.readByte()) != 0) {
            os.write(c);
        }
        return new String(os.toByteArray());
    }

    private void sendDiscovery() {
        ByteBuf discoveryPacket = Unpooled.buffer(DISCOVERY_PACKET_LENGTH)
                .writeInt(ready.ssrc)
                .writeZero(DISCOVERY_PACKET_LENGTH - INTEGER_BYTES);
        send(discoveryPacket);
    }

    private boolean isReady() {
        return sendTask != null;
    }

    private boolean isDiscovering() {
        return discoveryCallback != null && !isReady();
    }

    private void listen() {
        Thread mainSocketThread = new Thread() {
            @Override
            public void run() {
                log("LISTENING");
                while (true) {
                    try {
                        if (isDiscovering()) {
                            log("DISCOVERING");
                            sendDiscovery();
                            ByteBuf buffer = receive(DISCOVERY_PACKET_LENGTH);
                            parseDiscovery(buffer);
                        } else if (isReady()) {
                            log("TRYING AUDIO INPUT");
                            ByteBuf buf = sendTask.run();
                            if (buf != null) {
                                send(buf);
                                try {
                                    Thread.sleep(Opus.CONFIG.FRAME_TIME - 1);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                            /*
                            log("TRYING TO RECEIVE AUDIO");
                            ByteBuf buffer = receive(1920);
                            receiveTask.run(buffer);
                            */
                        }
                    } catch(IOException e) {
                        log("Failed to receive data");
                    }
                }
            }
        };
        mainSocketThread.start();
    }

    void start(VoiceSendTask sendTask, VoiceReceiveTask receiveTask) {
        this.sendTask = sendTask;
        this.receiveTask = receiveTask;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void send(ByteBuf data) {
        byte[] buffer = new byte[data.readableBytes()];
        data.getBytes(data.readerIndex(), buffer);
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, new InetSocketAddress(ready.ip, ready.port));
        try {
            log("Sending (" + buffer.length + ") : " + bytesToHex(buffer));
            socket.send(packet);
        } catch (IOException e) {
            log(e.toString());
        }
    }

    private ByteBuf receive(Integer size) throws IOException {
        byte[] buffer = new byte[size];
        DatagramPacket receivedPacket = new DatagramPacket(buffer, size);
        socket.receive(receivedPacket);
        log("Received (" + buffer.length + ") : " + bytesToHex(buffer));
        return Unpooled.buffer(size)
                .writeBytes(buffer);
    }

    private void log(String txt) {
        if (MainActivity.debug)
            Log.d("[UDP] VOICE", txt);
    }
}
