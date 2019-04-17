package ca.ulaval.ima.mp.models.gateway.voice;

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

public class UDPListener {
    public Ready ready;
    public SessionDescription sessionDescription;
    public String ip;
    public Integer port;
    public Callback discoveryCallback = null;

    public DatagramSocket socket;
    public Thread thread;
    public boolean run = true;

    public interface Callback {
        void onSuccess();
        void onFailure(IOException error);
    }

    public UDPListener(Ready ready) throws IOException {
        this.ready = ready;
        try {
            this.socket = new DatagramSocket();
        } catch (SocketException e) {
            log("Failed to open socket connection");
            e.printStackTrace();
            throw e;
        }
    }

    public String getIP() {
        return this.ip;
    }

    public Integer getPort() {
        return this.port;
    }

    public void discovery(final Callback callback)
    {
        try
        {
            this.discoveryCallback = callback;
            listen();
            ByteBuffer buffer = ByteBuffer.allocate(70);
            buffer.putInt(ready.ssrc);
            send(buffer.array());
        } catch (IOException e) {
            callback.onFailure(e);
        }
    }

    public void parseDiscovery(byte[] received) {
        String resIP = new String(received);
        resIP = resIP.substring(4, resIP.length() - 2);
        ip = resIP.trim();

        byte[] portBytes = new byte[2];
        portBytes[0] = received[received.length - 1];
        portBytes[1] = received[received.length - 2];

        // Tricky part to convert 2 last bytes of an unsigned short to integer
        int firstByte = (0x000000FF & ((int) portBytes[0]));
        int secondByte = (0x000000FF & ((int) portBytes[1]));

        // Combines the two bytes
        port = (firstByte << 8) | secondByte;

        discoveryCallback.onSuccess();
        discoveryCallback = null;
    }

    public void setEncryption(SessionDescription sessionDescription) {
        this.sessionDescription = sessionDescription;
    }

    public void stop() {
        this.run = false;
    }

    public void listen() {
        this.run = true;
        this.thread = new Thread() {
            @Override
            public void run() {
                log("LISTENING");
                while (run) {
                    try {
                        if (discoveryCallback != null) {
                            parseDiscovery(receive(70));
                        } else {
                            receive(15000);
                        }
                    } catch(IOException e) {
                        log("Failed to receive data");
                    }
                }
            }
        };
        thread.start();
    }

    public void log(String txt) {
        Log.d("[UDP] VOICE", txt);
    }

    public void send(byte[] buffer) throws IOException {
        log("Sending : " + buffer);
        log("To : " + ready.ip + ":" + ready.port);
        DatagramPacket discoveryPacket = new DatagramPacket(buffer, buffer.length, new InetSocketAddress(ready.ip, ready.port));
        socket.send(discoveryPacket);
    }

    public byte[] receive(Integer size) throws IOException {
        byte[] outData = new byte[size];
        DatagramPacket receivedPacket = new DatagramPacket(outData, size);
        socket.receive(receivedPacket);
        log("Receiving : " + receivedPacket.getData());
        return receivedPacket.getData();
    }
}
