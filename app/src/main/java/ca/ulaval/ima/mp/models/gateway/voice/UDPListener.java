package ca.ulaval.ima.mp.models.gateway.voice;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Queue;

import ca.ulaval.ima.mp.JSONHelper;
import ca.ulaval.ima.mp.SDK;
import ca.ulaval.ima.mp.lib.TweetNaclFast;
import ca.ulaval.ima.mp.models.User;
import gnu.trove.map.TIntLongMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import okhttp3.Call;
import okhttp3.Response;
import top.oply.opuslib.OpusTool;

public class UDPListener {
    public Ready ready;
    public SessionDescription sessionDescription;
    public String ip;
    public Integer port;
    public Callback discoveryCallback = null;

    public DatagramSocket socket;
    public Thread thread;
    public boolean run = true;

    public AudioSendHandler sendHandler = new AudioSendHandler() {
        File sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        final File file = new File(sdcard,"sao_a_tender_feeling.wav");

        File playingFile = file;
        int currentOffset = 0;

        public File convertFileToOpus(File file) {
            log("LAUNCHING OPUS CONVERSION");
            OpusTool oTool = new OpusTool();
            File outFile = new File(sdcard, "test.opus");
            oTool.encode(file.getAbsolutePath(), outFile.getAbsolutePath(), null);
            return outFile;
        }

        @Override
        public byte[] provide20MsAudio() {
            if (file != null) {
                log(file.getAbsolutePath());
                try {
                    if (file == playingFile) {
                        //convertFileToOpus(file);
                        File outFile = new File(sdcard, "test.opus");
                        byte[] encryptedData = FileUtils.readFileToByteArray(outFile);
                        float millisecondsOfAudio = 20.0f;
                        float frequency = 48000.0f;
                        float channels = 2.0f;
                        int sizeToRead = (int) (millisecondsOfAudio / ((1000.0f / frequency) * channels));
                        encryptedData = new String(encryptedData, currentOffset, sizeToRead).getBytes();
                        currentOffset += sizeToRead;
                        log("PACKET OUT : " + encryptedData.length);
                        return encryptedData;
                    }
                } catch (IOException e) {
                    log("Could not read file" + file.getAbsolutePath());
                    e.printStackTrace();
                }
            } else {
                log("FILE CANNOT BE OPENED");
            }
            return new byte[0];
        }
    };

    public AudioReceiveHandler receiveHandler = new AudioReceiveHandler() {
        @Override
        public boolean canReceiveCombined() {
            return false;
        }

        @Override
        public boolean canReceiveUser() {
            return true;
        }

        @Override
        public void handleCombinedAudio(CombinedAudio combinedAudio) {

        }

        @Override
        public void handleUserAudio(UserAudio userAudio) {
            byte[] data = userAudio.getAudioData(1.0);
            Log.d("AUDIO READER", "READING A BUFFER OF " + data.length);
            OUTPUT_FORMAT.write(data, 0, data.length);
        }
    };

    public interface Callback {
        void onSuccess();
        void onFailure(IOException error);
    }

    private static class AudioData
    {
        private final long time;
        private final short[] data;

        public AudioData(short[] data)
        {
            this.time = System.currentTimeMillis();
            this.data = data;
        }
    }

    public static final int OPUS_SAMPLE_RATE = 48000;
    public static final int OPUS_FRAME_SIZE = 960;
    public static final int OPUS_FRAME_TIME_AMOUNT = 20;
    public static final int OPUS_CHANNEL_COUNT = 2;

    private final TIntLongMap ssrcMap = new TIntLongHashMap();
    private final TIntObjectMap<Decoder> opusDecoders = new TIntObjectHashMap<>();
    private final HashMap<User, Queue<AudioData>> combinedQueue = new HashMap<>();

    private static final byte[] KEEP_ALIVE = { (byte) 0xC9, 0, 0, 0, 0, 0, 0, 0, 0 };
    private static final byte[] silenceBytes = new byte[] {(byte)0xF8, (byte)0xFF, (byte)0xFE};

    private char seq = 0;
    private int timestamp = 0;
    private long nonce = 0;
    private ByteBuffer buffer = ByteBuffer.allocate(512);
    private ByteBuffer encryptionBuffer = ByteBuffer.allocate(512);
    private final byte[] nonceBuffer = new byte[TweetNaclFast.SecretBox.nonceLength];

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

    public void sendDiscovery() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(70);
        buffer.putInt(ready.ssrc);
        send(buffer.array());
    }

    public void discovery(final Callback callback)
    {
        this.discoveryCallback = callback;
        listen();
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

    public void parseAudio(byte[] buffer) {
        final AudioPacket decryptedPacket = AudioPacket.decryptAudioPacket(buffer,
                JSONHelper.toPrimitiveByteArray(sessionDescription.secretKey));
        if (decryptedPacket == null) {
            log("Could not decrypt packet");
            return;
        }

        int ssrc = decryptedPacket.getSSRC();
        final long userId = ssrcMap.get(ssrc);
        if (userId == ssrcMap.getNoEntryValue()) {
            byte[] audio = decryptedPacket.getEncodedAudio();
            if (!Arrays.equals(audio, silenceBytes))
                log("Received audio data with an unknown SSRC id. Ignoring");
            return;
        }
        SDK.getUserById((int)userId, new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log("Failed to check user id");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                User user = new User(JSONHelper.asJSONObject(response));
                if (user.id == null)
                    log("Received audio data with a known SSRC, but the userId associate with the SSRC is unknown");

                //receiveHandler.handleUserAudio(new UserAudio(user, decodedAudio));
            }
        });
    }

   /* private byte[] encodeToOpus(byte[] rawAudio)
    {
        ShortBuffer nonEncodedBuffer = ShortBuffer.allocate(rawAudio.length / 2);
        ByteBuffer encoded = ByteBuffer.allocate(4096);
        for (int i = 0; i < rawAudio.length; i += 2)
        {
            int firstByte =  (0x000000FF & rawAudio[i]);      //Promotes to int and handles the fact that it was unsigned.
            int secondByte = (0x000000FF & rawAudio[i + 1]);  //

            //Combines the 2 bytes into a short. Opus deals with unsigned shorts, not bytes.
            short toShort = (short) ((firstByte << 8) | secondByte);

            nonEncodedBuffer.put(toShort);
        }
        ((Buffer) nonEncodedBuffer).flip();

        int result = Opus.INSTANCE.opus_encode(opusEncoder, nonEncodedBuffer, OPUS_FRAME_SIZE, encoded, encoded.capacity());
        if (result <= 0)
        {
            log("Received error code from opus_encode()");
            return null;
        }

        //ENCODING STOPS HERE

        byte[] audio = new byte[result];
        encoded.get(audio);
        return audio;
    }*/

    /*private byte[] encodeAudio(byte[] rawAudio)
    {
        if (opusEncoder == null)
        {
            if (!AudioNatives.ensureOpus())
            {
                log("Unable to process PCM audio without opus binaries!");
                return null;
            }
            IntBuffer error = IntBuffer.allocate(1);
            opusEncoder = Opus.INSTANCE.opus_encoder_create(OPUS_SAMPLE_RATE, OPUS_CHANNEL_COUNT, Opus.OPUS_APPLICATION_AUDIO, error);
            if (error.get() != Opus.OPUS_OK && opusEncoder == null)
            {
                log("Received error status from opus_encoder_create()");
                return null;
            }
        }
        return encodeToOpus(rawAudio);
    }*/

    private ByteBuffer getPacketData(byte[] rawAudio)
    {
        ensureEncryptionBuffer(rawAudio);
        AudioPacket packet = new AudioPacket(encryptionBuffer, seq, timestamp, ready.ssrc, rawAudio);
        byte[] secretKey = JSONHelper.toPrimitiveByteArray(sessionDescription.secretKey);
        log("secretKey len:" + secretKey);
        return buffer = packet.asEncryptedPacket(buffer, secretKey, nonceBuffer, 0);
    }

    private void ensureEncryptionBuffer(byte[] data)
    {
        ((Buffer) encryptionBuffer).clear();
        int currentCapacity = encryptionBuffer.remaining();
        int requiredCapacity = AudioPacket.RTP_HEADER_BYTE_LENGTH + data.length;
        if (currentCapacity < requiredCapacity)
            encryptionBuffer = ByteBuffer.allocate(requiredCapacity);
    }

    private DatagramPacket getDatagramPacket(ByteBuffer b)
    {
        byte[] data = b.array();
        int offset = b.arrayOffset();
        int position = b.position();
        return new DatagramPacket(data, offset, position - offset, new InetSocketAddress(ready.ip, ready.port));
    }

    public void sendAudioInput() {
        byte[] opusData = sendHandler.provide20MsAudio();
        if (opusData == null || opusData.length == 0) {
            log("Raw data is empty");
            return;
        }
        try {
            VoiceListener.speak(true);
            send(getDatagramPacket(getPacketData(opusData)));
            if (seq + 1 > Character.MAX_VALUE)
                seq = 0;
            else
                seq++;
        } catch (IOException e) {
            e.printStackTrace();
            log("Could not send encrypted audio data");
        }
    }

    public void setEncryption(SessionDescription sessionDescription) {
        this.sessionDescription = sessionDescription;
    }

    public void stop() {
        this.run = false;
    }

    public void keepAlive() {
        try {
            send(KEEP_ALIVE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isReady() {
        return sessionDescription != null && ip != null && !ip.equals("") && port != -1;
    }

    public boolean isDiscovering() {
        return discoveryCallback != null && !isReady();
    }

    public void listen() {
        this.run = true;
        try {
            socket.setSoTimeout(1000);
        } catch (SocketException e) {
            log("Couldn't set SO_TIMEOUT for UDP socket");
        }
        this.thread = new Thread() {
            @Override
            public void run() {
                log("LISTENING");
                while (run) {
                    try {
                        if (isDiscovering()) {
                            log("DISCOVERING");
                            sendDiscovery();
                            byte[] res = receive(70);
                            parseDiscovery(res);
                        } else if (isReady()) {
                            log("TRYING AUDIO INPUT");
                            sendAudioInput();
                            byte[] res = receive(1920);
//                            parseAudio(res);
                            timestamp += OPUS_FRAME_SIZE;
                        }
                    } catch(IOException e) {
                        //log("Failed to receive data");
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
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, new InetSocketAddress(ready.ip, ready.port));
        send(packet);
    }

    public void send(DatagramPacket packet) throws IOException {
        log("Sending : " + packet.getData());
        log("To : " + packet.getSocketAddress() + ":" + packet.getPort());
        socket.send(packet);
    }

    public byte[] receive(Integer size) throws IOException {
        byte[] outData = new byte[size];
        DatagramPacket receivedPacket = new DatagramPacket(outData, size);
        socket.receive(receivedPacket);
        log("Received (" + receivedPacket.getData().length + ") : " + receivedPacket.getData());
        return receivedPacket.getData();
    }
}
