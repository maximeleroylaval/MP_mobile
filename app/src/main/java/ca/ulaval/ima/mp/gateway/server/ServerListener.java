package ca.ulaval.ima.mp.gateway.server;

import android.util.Log;

import org.json.JSONObject;

import ca.ulaval.ima.mp.sdk.JSONHelper;
import ca.ulaval.ima.mp.activity.MainActivity;
import ca.ulaval.ima.mp.gateway.server.model.Heartbeat;
import ca.ulaval.ima.mp.gateway.server.model.Identify;
import ca.ulaval.ima.mp.gateway.server.model.Ready;
import ca.ulaval.ima.mp.sdk.models.Channel;
import ca.ulaval.ima.mp.sdk.models.Voice;
import ca.ulaval.ima.mp.gateway.Gateway;
import ca.ulaval.ima.mp.gateway.server.model.Payload;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class ServerListener extends WebSocketListener {
    private Heartbeat.Interval heartbeatInterval = null;
    private Ready ready = null;
    private Voice.State voiceState = null;
    private Voice.Server voiceServer = null;
    private WebSocket socket;

    public void joinVoiceChannel(Channel channel) {
        Voice.State voiceState = new Voice.State(channel);
        Payload payload = new Payload(Gateway.SERVER.OP.VOICE_STATE_UPDATE, voiceState, null, null);
        input(payload.toJSONString());
    }

    public void leaveVoiceChannel() {
        Voice.State voiceState = new Voice.State(voiceServer.guildId);
        Payload payload = new Payload(Gateway.SERVER.OP.VOICE_STATE_UPDATE, voiceState, null, null);
        input(payload.toJSONString());
    }

    public void sendHeartbeat(Payload heartbeat) {
        input(new Heartbeat(heartbeat).toJSONString());
    }

    private void handleMessage(WebSocket webSocket, String text) {
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

    private void handleClosing(WebSocket webSocket, int code, String reason) {
        if (heartbeatInterval != null) {
            heartbeatInterval.stop();
        }
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

    private void log(String txt) {
        if (MainActivity.debug)
            Log.d("[WS Server]", txt);
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
