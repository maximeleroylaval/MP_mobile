package ca.ulaval.ima.mp.models.gateway.server;

import android.util.Log;

import org.json.JSONObject;

import ca.ulaval.ima.mp.JSONHelper;
import ca.ulaval.ima.mp.models.Channel;
import ca.ulaval.ima.mp.models.Voice;
import ca.ulaval.ima.mp.models.gateway.Gateway;
import ca.ulaval.ima.mp.models.gateway.Payload;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ServerListener extends WebSocketListener {
    public Heartbeat.Interval heartbeatInterval = null;
    public Ready ready = null;
    public Voice.State voiceState = null;
    public Voice.Server voiceServer = null;
    public static WebSocket socket;

    public void joinVoiceChannel(Channel channel) {
        Voice.State voiceState = new Voice.State(channel);
        Payload payload = new Payload(Gateway.SERVER.OP.VOICE_STATE_UPDATE, voiceState, null, null);
        input(payload.toJSONString());
    }

    public void handleMessage(WebSocket webSocket, String text) {
        Payload payload = new Payload(JSONHelper.getJSONObject(text));
        if (payload.op.equals(Gateway.SERVER.OP.HELLO)) {
            heartbeatInterval = new Heartbeat.Interval(payload);
            heartbeatInterval.start(this);
            Payload identifyPayload = new Payload(Gateway.SERVER.OP.IDENTIFY, new Identify(), null, null);
            input(identifyPayload.toJSONString());
        } else if (payload.op.equals(Gateway.SERVER.OP.HEARTBEAT)) {

        } else if (payload.op.equals(Gateway.SERVER.OP.INVALID_SESSION)) {

        } else if (payload.op.equals(Gateway.SERVER.OP.DISPATCH)) {
            if (payload.t.equals(Gateway.SERVER.EVENT.READY)) {
                ready = new Ready(payload);
            } else if (payload.t.equals(Gateway.SERVER.EVENT.VOICE_STATE_UPDATE)) {
                voiceState = new Voice.State((JSONObject)payload.d);
            } else if (payload.t.equals(Gateway.SERVER.EVENT.VOICE_SERVER_UPDATE)) {
                voiceServer = new Voice.Server((JSONObject) payload.d);
                Gateway.establishVoiceConnection(voiceState, voiceServer);
            }
        } else if (payload.op.equals(Gateway.SERVER.OP.HEARTBEAT_ACK)) {
            //check if ack is between last heartbeat or else reconnect
        } else if (payload.op.equals(Gateway.SERVER.OP.VOICE_STATE_UPDATE)) {

        }
        if (heartbeatInterval != null && !payload.op.equals(Gateway.SERVER.OP.HEARTBEAT_ACK)) {
            heartbeatInterval.update(payload);
        }
    }

    public void handleClosing(WebSocket webSocket, int code, String reason) {
        if (heartbeatInterval != null) {
            heartbeatInterval.stop();
        }
    }

    public void output(final String txt) {
        Log.d("[WS] SERVER RECEIVED", txt);
    }

    public void input(final String txt) {
        if (txt != null && socket != null) {
            Log.d("[WS] SERVER SENDING", txt);
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
}
