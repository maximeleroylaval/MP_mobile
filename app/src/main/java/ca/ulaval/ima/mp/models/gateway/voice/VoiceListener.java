package ca.ulaval.ima.mp.models.gateway.voice;

import android.util.Log;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import ca.ulaval.ima.mp.JSONHelper;
import ca.ulaval.ima.mp.models.Voice;
import ca.ulaval.ima.mp.models.gateway.Gateway;
import ca.ulaval.ima.mp.models.gateway.Payload;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class VoiceListener extends WebSocketListener {
    public Heartbeat.Interval heartbeatInterval = null;
    public Voice.State voiceState;
    public Voice.Server voiceServer;
    public static WebSocket socket;
    public static UDPListener udpListener;
    public String audioMode = "xsalsa20_poly1305";

    public void handleMessage(WebSocket webSocket, String text) {
        Payload payload = new Payload(JSONHelper.getJSONObject(text));
        if (payload.op.equals(Gateway.VOICE.OP.HELLO)) {
            heartbeatInterval = new Heartbeat.Interval(payload);
            heartbeatInterval.start(new Heartbeat.Interval.Callback() {
                @Override
                public void onLoop(Heartbeat heartbeat, int interval) {
                    long milliseconds = Double.valueOf(interval * 0.75).longValue();
                    try {
                        TimeUnit.MILLISECONDS.sleep(milliseconds);
                        if (udpListener != null && udpListener.isReady()) {
                            udpListener.keepAlive();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    input(heartbeat.toJSONString());
                }
            });
            Identify identify = new Identify(this.voiceServer.guildId, this.voiceState.userId,
                    this.voiceState.sessionId, this.voiceServer.token);
            Payload identifyPayload = new Payload(Gateway.VOICE.OP.IDENTIFY, identify, null, null);
            input(identifyPayload.toJSONString());
        }
        else if (payload.op.equals(Gateway.VOICE.OP.READY)) {
            Ready ready = new Ready((JSONObject)payload.d);
            try {
                udpListener = new UDPListener(ready);
                udpListener.discovery(new UDPListener.Callback() {
                    @Override
                    public void onSuccess() {
                        output("IP: " + udpListener.getIP());
                        output("PORT: " + udpListener.getPort());
                        SelectProtocol selectProtocol = new SelectProtocol("udp",
                                new SelectProtocol.Data(udpListener.getIP(), udpListener.getPort(), audioMode));
                        Payload selectProtocolPayload = new Payload(Gateway.VOICE.OP.SELECT_PROTOCOL, selectProtocol, null, null);
                        input(selectProtocolPayload.toJSONString());
                    }

                    @Override
                    public void onFailure(IOException error) {
                        output("Failed to discover ip and port");
                        error.printStackTrace();
                    }
                });
            } catch(IOException e) {
                output("Could not connect to voice server through udp");
                e.printStackTrace();
            }
        } else if (payload.op.equals(Gateway.VOICE.OP.SESSION_DESCRIPTION)) {
            SessionDescription sessionDescription = new SessionDescription((JSONObject)payload.d);
            if (udpListener != null) {
                udpListener.setEncryption(sessionDescription);
            }
            speak(false);
        }
        if (heartbeatInterval != null && !payload.op.equals(Gateway.VOICE.OP.HEARTBEAT_ACK)) {
            heartbeatInterval.update(payload);
        }
    }

    public void handleClosing(WebSocket webSocket, int code, String reason) {
        if (heartbeatInterval != null) {
            heartbeatInterval.stop();
        }
    }

    public static void disconnect() {
        Payload payloadSpeaking = new Payload(Gateway.VOICE.OP.CLIENT_DISCONNECT, null, null, null);
        input(payloadSpeaking.toJSONString());
    }

    public static void speak(Boolean isSpeaking) {
        Speaking speaking = new Speaking(isSpeaking, 0, udpListener.ready.ssrc);
        Payload payloadSpeaking = new Payload(Gateway.VOICE.OP.SPEAKING, speaking, null, null);
        input(payloadSpeaking.toJSONString());
    }

    public static void output(final String txt) {
        Log.d("[WS] VOICE RECEIVED", txt);
    }

    public static void input(final String txt) {
        if (txt != null && socket != null) {
            Log.d("[WS] VOICE SENDING", txt);
            socket.send(txt);
        }
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

    public VoiceListener(Voice.State voiceState, Voice.Server voiceServer) {
        this.voiceState = voiceState;
        this.voiceServer = voiceServer;
    }
}
