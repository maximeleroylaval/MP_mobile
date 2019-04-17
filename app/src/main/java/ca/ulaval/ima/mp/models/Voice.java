package ca.ulaval.ima.mp.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.json.JSONObject;

import ca.ulaval.ima.mp.JSONHelper;

public class Voice {
    public static class State {

        @JsonProperty("guild_id")
        public String guildId;
        @JsonProperty("channel_id")
        public String channelId;
        @JsonProperty("user_id")
        public String userId;
        public Guild.Member member;
        @JsonProperty("session_id")
        public String sessionId;
        public Boolean deaf;
        public Boolean mute;
        @JsonProperty("self_deaf")
        public Boolean selfDeaf;
        @JsonProperty("self_mute")
        public Boolean selfMute;
        public Boolean suppress;

        public State(JSONObject obj) {
            this.guildId = JSONHelper.getString(obj, "guild_id");
            this.channelId = JSONHelper.getString(obj, "channel_id");
            this.userId = JSONHelper.getString(obj, "user_id");
            this.member = new Guild.Member(JSONHelper.getJSONObject(obj, "member"));
            this.sessionId = JSONHelper.getString(obj, "session_id");
            this.deaf = JSONHelper.getBoolean(obj, "deaf");
            this.mute = JSONHelper.getBoolean(obj, "mute");
            this.selfDeaf = JSONHelper.getBoolean(obj, "self_deaf");
            this.selfMute = JSONHelper.getBoolean(obj, "self_mute");
            this.suppress = JSONHelper.getBoolean(obj, "suppress");
        }

        public State(Channel channel) {
            this.channelId = channel.id;
            this.guildId = channel.guildId;
            this.deaf = false;
            this.mute = false;
        }
    }

    public static class Server {
        public String token;
        @JsonProperty("guild_id")
        public String guildId;
        public String endpoint;

        public Server(JSONObject obj) {
            this.token = JSONHelper.getString(obj, "token");
            this.guildId = JSONHelper.getString(obj, "guild_id");
            this.endpoint = JSONHelper.getString(obj, "endpoint");
        }
    }
}
