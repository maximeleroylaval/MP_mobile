package ca.ulaval.ima.mp.models;

import android.util.Log;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ca.ulaval.ima.mp.SDK;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class Gateway {
    public static class Payload {
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

        public Integer op;
        public Object d;
        public Integer s;
        public String t;

        public Payload(JSONObject obj) {
            this.op = JSONHelper.getInteger(obj, "op");
            this.d = JSONHelper.getJSONObject(obj, "d");
            this.s = JSONHelper.getInteger(obj, "s");
            this.t = JSONHelper.getString(obj, "t");
        }

        public Payload(Integer op, Object d, Integer s, String t) {
            this.op = op;
            this.d = d;
            this.s = s;
            this.t = t;
        }

        public String toJSONString() {
            return JSONHelper.asJSONString(this);
        }
    }

    public static class Heartbeat extends Payload {
        public static class Interval {
            public Payload count = null;
            public Integer heartbeatInterval;
            public List<String> _trace;

            public Thread thread = null;
            public boolean started = true;

            public Interval(Payload payload) {
                JSONObject jsonObj = (JSONObject)payload.d;
                this.heartbeatInterval = JSONHelper.getInteger(jsonObj, "heartbeat_interval");
                this._trace = JSONHelper.asArrayWithConstructor(String.class, String.class, JSONHelper.getJSONArray(jsonObj, "_trace"));
                this.update(count);
            }

            public void update(Payload payload) {
                count = payload;
            }

            public void start(final WebSocket webSocket) {
                this.thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            while(started) {
                                ServerListener.input(new Heartbeat(count).toJSONString());
                                TimeUnit.MILLISECONDS.sleep(heartbeatInterval);
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                };
                this.thread.start();
            }

            public void stop() {
                this.started = false;
            }
        }

        public Heartbeat() {
            super(OP.HEARTBEAT, null, null, null);
        }

        public Heartbeat(Payload payload) {
            super(OP.HEARTBEAT, payload != null ? payload.s : null, null, null);


        }
    }

    public static class Activity {
        public Activity() {

        }
    }

    public static class ConnectionProperties {
        public String $os;
        public String $browser;
        public String $device;

        public ConnectionProperties() {
            this.$os = "android";
            this.$browser = "okhttp3";
            this.$device = "okhttp3";
        }
    }

    public static class Update {
        public static class Status {
            public static class TYPE {
                public static String ONLINE = "online";
                public static String DO_NOT_DISTURB = "dnd";
                public static String AFK = "idle";
                public static String INVISIBLE = "invisible";
                public static String OFFLINE = "offline";
            }

            public Integer since;
            public Activity game;
            public String status;
            public Boolean afk;

            public Status() {
                this.since = null;
                this.status = TYPE.ONLINE;
                this.afk = false;
                this.game = null;
            }
        }
    }

    public static class SessionStartLimit {
        public Integer total;
        public Integer remaining;
        public Integer resetAfter;

        public SessionStartLimit(JSONObject obj) {
            this.total = JSONHelper.getInteger(obj, "total");
            this.remaining = JSONHelper.getInteger(obj, "remaining");
            this.resetAfter = JSONHelper.getInteger(obj, "reset_after");
        }
    }

    public static class Bot {
        public String url;
        public Integer shards;
        public SessionStartLimit sessionStartLimit;

        public Bot(JSONObject obj) {
            this.url = JSONHelper.getString(obj, "url");
            this.shards = JSONHelper.getInteger(obj,"shards");
            this.sessionStartLimit = new SessionStartLimit(JSONHelper.getJSONObject(obj, "session_start_limit"));
        }
    }

    public static class Ready extends Payload {
        public Integer v;
        public User user;
        public List<String> privateChannels;
        public List<Guild.Unavailable> guilds;
        public String sessionId;
        public List<String> _trace;
        public List<Integer> shard;

        public void init() {
            JSONObject jsonObj = (JSONObject)this.d;
            this.v = JSONHelper.getInteger(jsonObj, "v");
            this.user = new User(JSONHelper.getJSONObject(jsonObj, "user"));
            this.privateChannels = JSONHelper.asArrayWithConstructor(String.class, String.class, JSONHelper.getJSONArray(jsonObj, "private_channels"));
            this.guilds = JSONHelper.asArray(Guild.Unavailable.class, JSONHelper.getJSONArray(jsonObj, "guilds"));
            this.sessionId = JSONHelper.getString(jsonObj, "session_id");
            this._trace = JSONHelper.asArrayWithConstructor(String.class, String.class, JSONHelper.getJSONArray(jsonObj, "_trace"));
            this.shard = JSONHelper.asArrayWithConstructor(Integer.class, Integer.class, JSONHelper.getJSONArray(jsonObj, "shard"));
        }

        public Ready(Payload payload) {
            super(payload.op, payload.d, payload.s, payload.t);
            this.init();
        }
    }

    private final static class ServerListener extends WebSocketListener {
        public static class Identify {
            public String token;
            public ConnectionProperties properties;
            public Boolean compress;
            @JsonProperty("large_threshold")
            public Integer largeThreshold;
            public Update.Status presence;
            //public List<Integer> shard;

            public static Integer op = Payload.OP.IDENTIFY;

            public Identify() {
                this.token = SDK.botToken;
                this.properties = new ConnectionProperties();
                this.compress = false;
                this.largeThreshold = 250;
                this.presence = new Update.Status();
                //this.shard = new ArrayList<>();
            }
        }

        public Heartbeat.Interval heartbeatInterval = null;
        public Ready ready = null;
        public Voice.State voiceState = null;
        public Voice.Server voiceServer = null;

        public void handleMessage(WebSocket webSocket, String text) {
            Payload payload = new Payload(JSONHelper.getJSONObject(text));
            if (payload.op.equals(Payload.OP.HELLO)) {
                heartbeatInterval = new Heartbeat.Interval(payload);
                heartbeatInterval.start(webSocket);
                Payload identifyPayload = new Payload(Identify.op, new Identify(), null, null);
                ServerListener.input(identifyPayload.toJSONString());
            } else if (payload.op.equals(Payload.OP.HEARTBEAT)) {

            } else if (payload.op.equals(Payload.OP.INVALID_SESSION)) {

            } else if (payload.op.equals(Payload.OP.DISPATCH)) {
                if (payload.t.equals(Payload.EVENT.READY)) {
                    ready = new Ready(payload);
                } else if (payload.t.equals(Payload.EVENT.VOICE_STATE_UPDATE)) {
                    voiceState = new Voice.State((JSONObject)payload.d);
                } else if (payload.t.equals(Payload.EVENT.VOICE_SERVER_UPDATE)) {
                    voiceServer = new Voice.Server((JSONObject) payload.d);
                    establishVoiceConnection(voiceState, voiceServer);
                }
            } else if (payload.op.equals(Payload.OP.HEARTBEAT_ACK)) {
                //check if ack is between last heartbeat or else reconnect
            } else if (payload.op.equals(Payload.OP.VOICE_STATE_UPDATE)) {

            }
            if (heartbeatInterval != null && !payload.op.equals(Payload.OP.HEARTBEAT_ACK)) {
                heartbeatInterval.update(payload);
            }
        }

        public void handleClosing(WebSocket webSocket, int code, String reason) {
            if (heartbeatInterval != null) {
                heartbeatInterval.stop();
            }
        }

        private static void output(final String txt) {
            Log.d("[WS] SERVER RECEIVED", txt);
        }

        public static void input(final String txt) {
            if (txt != null && webSocketServer != null) {
                Log.d("[WS] SERVER SENDING", txt);
                webSocketServer.send(txt);
            }
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            ServerListener.output("Open : READY");
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            ServerListener.output("Message : " + text);
            this.handleMessage(webSocket, text);
        }
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            ServerListener.output("Message bytes : " + bytes.hex());
        }
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            ServerListener.output("Closing : " + code + " / " + reason);
            this.handleClosing(webSocket, code, reason);
        }
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            ServerListener.output("Failure : " + t.getMessage());
        }
    }

    private final static class VoiceListener extends WebSocketListener {
        public Heartbeat.Interval heartbeatInterval = null;
        public Voice.State voiceState;
        public Voice.Server voiceServer;

        public static class Identify {

            @JsonProperty("server_id")
            public String serverId;
            @JsonProperty("user_id")
            public String userId;
            @JsonProperty("session_id")
            public String sessionId;
            public String token;

            public static Integer op = 2;

            public Identify(String serverId, String userId, String sessionId, String token) {
                this.serverId = serverId;
                this.userId = userId;
                this.sessionId = sessionId;
                this.token = token;
            }
        }

        public void handleMessage(WebSocket webSocket, String text) {
            Payload payload = new Payload(JSONHelper.getJSONObject(text));
            if (payload.op.equals(2)) {
                //READY
            }
            if (heartbeatInterval != null && !payload.op.equals(Payload.OP.HEARTBEAT_ACK)) {
                heartbeatInterval.update(payload);
            }
        }

        public void handleClosing(WebSocket webSocket, int code, String reason) {
            if (heartbeatInterval != null) {
                heartbeatInterval.stop();
            }
        }

        public void handleOpen(WebSocket webSocket, Response response) {
            Identify identify = new Identify(this.voiceServer.guildId, this.voiceState.userId,
                    this.voiceState.sessionId, this.voiceServer.token);
            Payload payload = new Payload(Identify.op, identify, null, null);
            VoiceListener.input(payload.toJSONString());
        }

        private static void output(final String txt) {
            Log.d("[WS] VOICE RECEIVED", txt);
        }

        public static void input(final String txt) {
            if (txt != null && webSocketVoice != null) {
                Log.d("[WS] VOICE SENDING", txt);
                webSocketVoice.send(txt);
            }
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            VoiceListener.output("Open : READY");
            this.handleOpen(webSocket, response);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            VoiceListener.output("Message : " + text);
            this.handleMessage(webSocket, text);
        }
        @Override
        public void onMessage(WebSocket webSocket, ByteString bytes) {
            VoiceListener.output("Message bytes : " + bytes.hex());
        }
        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            VoiceListener.output("Closing : " + code + " / " + reason);
            this.handleClosing(webSocket, code, reason);
        }
        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            VoiceListener.output("Failure : " + t.getMessage());
        }

        public VoiceListener(Voice.State voiceState, Voice.Server voiceServer) {
            this.voiceState = voiceState;
            this.voiceServer = voiceServer;
        }
    }

    public static int version = 6;

    public static String encoding = "json";

    public static WebSocket webSocketServer;

    public static WebSocket webSocketVoice;

    public static String getServerGateway(String host) {
        return host == null ? "" : host + "/?v=" + Gateway.version + "&encoding=" + Gateway.encoding;
    }

    public static String getVoiceGateway(String host) {
        return getServerGateway("wss://" + (host == null ? "" : host));
    }

    public static WebSocket createWebSocket(String url, WebSocketListener listener) {
        Request request = new Request.Builder().url(url).build();
        WebSocket ws = SDK.client.newWebSocket(request, listener);
        return ws;
    }

    public static void establishVoiceConnection(Voice.State voiceState, Voice.Server voiceServer) {
        Log.d("INSTANTIATE", "VOICE CONNECTION TO : " + getVoiceGateway(voiceServer.endpoint));
        webSocketVoice = createWebSocket(getVoiceGateway(voiceServer.endpoint), new VoiceListener(voiceState, voiceServer));
    }

    public static void establishServerConnection(Bot botGateway) {
        webSocketServer = createWebSocket(getServerGateway(botGateway.url), new ServerListener());
    }

    public static void joinVoiceChannel(Channel channel) {
        Voice.State voiceState = new Voice.State(channel);
        Payload payload = new Payload(Payload.OP.VOICE_STATE_UPDATE, voiceState, null, null);
        ServerListener.input(payload.toJSONString());
    }

    public static void Initialize() {
        SDK.getGatewayBot(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d("FAIL", "Gateway wss url retrieving");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Bot botGateway = new Bot(JSONHelper.getJSONObject(response));
                establishServerConnection(botGateway);
            }
        });
    }
}
