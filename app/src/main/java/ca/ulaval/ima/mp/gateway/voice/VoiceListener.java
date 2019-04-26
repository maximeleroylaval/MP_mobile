package ca.ulaval.ima.mp.gateway.voice;

import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import ca.ulaval.ima.mp.activity.FileManager;
import ca.ulaval.ima.mp.sdk.JSONHelper;
import ca.ulaval.ima.mp.activity.MainActivity;
import ca.ulaval.ima.mp.gateway.crypto.TweetNaclFast;
import ca.ulaval.ima.mp.gateway.voice.model.Disconnect;
import ca.ulaval.ima.mp.gateway.voice.model.Heartbeat;
import ca.ulaval.ima.mp.gateway.voice.model.Identify;
import ca.ulaval.ima.mp.gateway.voice.model.Ready;
import ca.ulaval.ima.mp.gateway.voice.model.SelectProtocol;
import ca.ulaval.ima.mp.gateway.voice.model.SessionDescription;
import ca.ulaval.ima.mp.gateway.voice.model.Speaking;
import ca.ulaval.ima.mp.sdk.models.Voice;
import ca.ulaval.ima.mp.gateway.Gateway;
import ca.ulaval.ima.mp.gateway.server.model.Payload;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class VoiceListener extends WebSocketListener {
    private Heartbeat.Interval heartbeatInterval = null;
    private Voice.State voiceState;
    private Voice.Server voiceServer;
    private Ready ready;
    private WebSocket socket;
    private VoiceSocket voiceSocket;

    private AudioSendHandler provider = new AudioSendHandler();
    private AudioReceiveHandler receiver = new AudioReceiveHandler();

    public VoiceListener(Voice.State voiceState, Voice.Server voiceServer) {
        this.voiceState = voiceState;
        this.voiceServer = voiceServer;
    }

    private void handleMessage(WebSocket webSocket, String text) {
        Payload payload = new Payload(JSONHelper.getJSONObject(text));
        if (payload.op.equals(Gateway.VOICE.OP.HELLO)) {
            heartbeatInterval = new Heartbeat.Interval(payload);
            heartbeatInterval.start(new Heartbeat.Interval.Callback() {
                @Override
                public void onLoop(Heartbeat heartbeat, int interval) {
                    long milliseconds = Double.valueOf(interval * 0.75).longValue();
                    try {
                        TimeUnit.MILLISECONDS.sleep(milliseconds);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    input(heartbeat.toJSONString());
                }
            });
            Identify identify = new Identify(voiceServer.guildId, voiceState.userId,
                    voiceState.sessionId, voiceServer.token);
            Payload identifyPayload = new Payload(Gateway.VOICE.OP.IDENTIFY, identify, null, null);
            input(identifyPayload.toJSONString());
        }
        else if (payload.op.equals(Gateway.VOICE.OP.READY)) {
            ready = new Ready((JSONObject)payload.d);
            try {
                voiceSocket = new VoiceSocket(ready);
                voiceSocket.performIpDiscovery(new VoiceSocket.Callback() {
                    @Override
                    public void onSuccess(String ip, Integer port) {
                        output("IP: " + ip);
                        output("PORT: " + port);
                        SelectProtocol selectProtocol = new SelectProtocol(VoiceSocket.PROTOCOL,
                                new SelectProtocol.Data(ip, port, VoiceSocket.ENCRYPTION_MODE));
                        Payload selectProtocolPayload = new Payload(Gateway.VOICE.OP.SELECT_PROTOCOL, selectProtocol, null, null);
                        input(selectProtocolPayload.toJSONString());
                    }

                    @Override
                    public void onFailure(IOException error) {
                        output("Failed to discover ip and port");
                        error.printStackTrace();
                    }
                });
            } catch (IOException e) {
                output(e.toString());
            }
        } else if (payload.op.equals(Gateway.VOICE.OP.SESSION_DESCRIPTION)) {
            SessionDescription sessionDescription = new SessionDescription((JSONObject)payload.d);

            TweetNaclFast.SecretBox boxer = new TweetNaclFast.SecretBox(sessionDescription.secretKey);
            AudioPacket transformer = new AudioPacket(ready.ssrc, boxer);

            VoiceSendTask sendingTask = new VoiceSendTask(VoiceListener.this, provider, transformer);
            VoiceReceiveTask receivingTask = new VoiceReceiveTask(receiver, transformer);

            voiceSocket.start(sendingTask, receivingTask);
        }
        if (heartbeatInterval != null && !payload.op.equals(Gateway.VOICE.OP.HEARTBEAT_ACK)) {
            heartbeatInterval.update(payload);
        }
    }

    private void handleClosing(WebSocket webSocket, int code, String reason) {
        if (heartbeatInterval != null) {
            heartbeatInterval.stop();
        }
    }

    void speak(Boolean isSpeaking) {
        Speaking speaking = new Speaking(isSpeaking, 0, ready.ssrc);
        Payload payloadSpeaking = new Payload(Gateway.VOICE.OP.SPEAKING, speaking, null, null);
        input(payloadSpeaking.toJSONString());
    }

    public boolean playFile(File file) {
        if (!FileManager.getFileExtension(file).equals("opus")) {
            return false;
        }
        provider.stopPlaying();
        return provider.playOpusFile(file);
    }

    public boolean isPlaying() {
        return provider.isPlaying();
    }

    public void stopPlaying() {
        provider.stopPlaying();
    }

    private void output(final String txt) {
        log("received : " + txt);
    }

    private void input(final String txt) {
        if (txt != null && socket != null) {
            log("sending : " + txt);
            socket.send(txt);
        }
    }

    private static void log(String txt) {
        if (MainActivity.debug)
            Log.d("[WS Voice]", txt);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        output("Open : READY");
        socket = webSocket;
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
        output("Message : " + text);
        this.handleMessage(webSocket, text);
    }
    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        output("Message bytes : " + bytes.hex());
    }
    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
        output("Closing : " + code + " / " + reason);
        this.handleClosing(webSocket, code, reason);
    }
    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        output("Failure : " + t.getMessage());
    }
}
