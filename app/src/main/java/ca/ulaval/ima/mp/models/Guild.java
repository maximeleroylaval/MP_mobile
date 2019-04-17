package ca.ulaval.ima.mp.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.List;

import ca.ulaval.ima.mp.JSONHelper;

public class Guild {
    public static class Member {
        public User user;
        public String nick;
        public List<String> roles;
        @JsonProperty("joined_at")
        public Timestamp joinedAt;
        public Boolean deaf;
        public Boolean mute;

        public Member(JSONObject obj) {
            this.user = new User(JSONHelper.getJSONObject(obj, "user"));
            this.nick = JSONHelper.getString(obj, "nick");
            this.roles = JSONHelper.asArrayWithConstructor(String.class, String.class, JSONHelper.getJSONArray(obj,"roles"));
            this.joinedAt = JSONHelper.getTimestamp(obj, "joined_at");
            this.deaf = JSONHelper.getBoolean(obj, "deaf");
            this.mute = JSONHelper.getBoolean(obj, "mute");
        }
    }

    public static class Unavailable {
        public String id;
        public Boolean unavailable;

        public Unavailable(JSONObject obj) {
            this.id = JSONHelper.getString(obj, "id");
            this.unavailable = JSONHelper.getBoolean(obj, "unavailable");
        }
    }

    public Guild(JSONObject obj) {

    }
}
