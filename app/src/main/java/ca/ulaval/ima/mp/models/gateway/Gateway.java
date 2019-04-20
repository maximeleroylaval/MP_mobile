package ca.ulaval.ima.mp.models.gateway;

import android.util.Log;

import java.io.IOException;

import ca.ulaval.ima.mp.JSONHelper;
import ca.ulaval.ima.mp.MainActivity;
import ca.ulaval.ima.mp.SDK;
import ca.ulaval.ima.mp.models.Voice;
import ca.ulaval.ima.mp.models.gateway.server.Bot;
import ca.ulaval.ima.mp.models.gateway.server.ServerListener;
import ca.ulaval.ima.mp.models.gateway.voice.VoiceListener;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class Gateway {
    public static class SERVER {
        public static class OP {
            public static Integer DISPATCH = 0;
            public static Integer HEARTBEAT = 1;
            public static Integer IDENTIFY = 2;
            public static Integer STATUS_UPDATE = 3;
            public static Integer VOICE_STATE_UPDATE = 4;
            public static Integer RESUME = 6;
            public static Integer RECONNECT = 7;
            public static Integer REQUEST_GUILD_MEMBERS = 8;
            public static Integer INVALID_SESSION = 9;
            public static Integer HELLO = 10;
            public static Integer HEARTBEAT_ACK = 11;
        }

        public static class EVENT {
            public static String READY = "READY";
            public static String VOICE_STATE_UPDATE = "VOICE_STATE_UPDATE";
            public static String VOICE_SERVER_UPDATE = "VOICE_SERVER_UPDATE";
        }
    }

    public static class VOICE {
        public static class OP {
            public static Integer IDENTIFY = 0;
            public static Integer SELECT_PROTOCOL = 1;
            public static Integer READY = 2;
            public static Integer HEARTBEAT = 3;
            public static Integer SESSION_DESCRIPTION = 4;
            public static Integer SPEAKING = 5;
            public static Integer HEARTBEAT_ACK = 6;
            public static Integer RESUME = 7;
            public static Integer HELLO = 8;
            public static Integer RESUMED = 9;
            public static Integer CLIENT_DISCONNECT = 13;
        }

        public static class EVENT {
            public static String READY = "READY";
            public static String VOICE_STATE_UPDATE = "VOICE_STATE_UPDATE";
            public static String VOICE_SERVER_UPDATE = "VOICE_SERVER_UPDATE";
        }
    }

    public static Integer version = 6;
    public static String encoding = "json";
    public static ServerListener server = null;
    public static VoiceListener voice = null;

    public Gateway() {
        SDK.getGatewayBot(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                log("Failed to retrieve wss url");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Bot botGateway = new Bot(JSONHelper.getJSONObject(response));
                establishServerConnection(botGateway);
            }
        });
    }

    private static HttpUrl getServerGateway(String host) {
        Request req = new Request.Builder().url(host).build();
        return new HttpUrl.Builder()
                .scheme(req.url().scheme())
                .host(req.url().host())
                .addQueryParameter("v", Gateway.version.toString())
                .addQueryParameter("encoding", Gateway.encoding)
                .build();
    }

    private static HttpUrl getVoiceGateway(String host) {
        return getServerGateway("wss://" + (host != null ? host : ""));
    }

    private static WebSocket createWebSocket(HttpUrl url, WebSocketListener listener) {
        Request request = new Request.Builder().url(url).build();
        return SDK.client.newWebSocket(request, listener);
    }

    private static void establishServerConnection(Bot botGateway) {
        server = new ServerListener();
        createWebSocket(getServerGateway(botGateway.url), server);
    }

    public static void establishVoiceConnection(Voice.State voiceState, Voice.Server voiceServer) {
        log("INSTANTIATE VOICE CONNECTION TO : " + getVoiceGateway(voiceServer.endpoint));
        voice = new VoiceListener(voiceState, voiceServer);
        createWebSocket(getVoiceGateway(voiceServer.endpoint), voice);
    }

    private static void log(String txt) {
        if (MainActivity.debug)
            Log.d("[GATEWAY]", txt);
    }
}
