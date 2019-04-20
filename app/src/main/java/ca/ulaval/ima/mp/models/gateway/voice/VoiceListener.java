package ca.ulaval.ima.mp.models.gateway.voice;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import ca.ulaval.ima.mp.JSONHelper;
import ca.ulaval.ima.mp.MainActivity;
import ca.ulaval.ima.mp.lib.TweetNaclFast;
import ca.ulaval.ima.mp.models.Voice;
import ca.ulaval.ima.mp.models.gateway.Gateway;
import ca.ulaval.ima.mp.models.gateway.Payload;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class VoiceListener extends WebSocketListener {
    private Heartbeat.Interval heartbeatInterval = null;
    private static Voice.State voiceState;
    private static Voice.Server voiceServer;
    private static Ready ready;
    private static WebSocket socket;
    static VoiceSocket voiceSocket;

    private static IAudioSendHandler provider = new AudioSendHandler();
    private static IAudioReceiveHandler receiver = new AudioReceiveHandler();

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

    public static void disconnect() {
        Payload payloadSpeaking = new Payload(Gateway.VOICE.OP.CLIENT_DISCONNECT, new Disconnect(voiceState.userId), null, null);
        input(payloadSpeaking.toJSONString());
    }

    public static void speak(Boolean isSpeaking) {
        Speaking speaking = new Speaking(isSpeaking, 0, ready.ssrc);
        Payload payloadSpeaking = new Payload(Gateway.VOICE.OP.SPEAKING, speaking, null, null);
        input(payloadSpeaking.toJSONString());
    }

    private static void output(final String txt) {
        log("Voice received : " + txt);
    }

    private static void input(final String txt) {
        if (txt != null && socket != null) {
            log("Voice sending : " + txt);
            socket.send(txt);
        }
    }

    private static void log(String txt) {
        if (MainActivity.debug)
            Log.d("[WS]", txt);
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
