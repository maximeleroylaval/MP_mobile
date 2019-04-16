package ca.ulaval.ima.mp.models;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ca.ulaval.ima.mp.SDK;

public class Channel {
    public String id;
    public String parentId;
    public String lastMessageId;
    public String guildId;
    public String name;
    public String topic;
    public Boolean nsfw;
    public Integer position;
    public Integer rateLimitPerUser;
    public Integer type;
    public Integer bitrate;
    public Integer userLimit;
    public JSONArray permissionOverwrites;

    public static class TYPES {
        public static final int GUILD_TEXT = 0;
        public static final int DM = 1;
        public static final int GUILD_VOICE = 2;
        public static final int GROUP_DM = 3;
        public static final int CATEGORY = 4;
        public static final int NEWS = 5;
        public static final int STORE = 6;
    }

    public Channel(JSONObject content) {
        this.id = JSONHelper.getString(content, "id");
        this.guildId = JSONHelper.getString(content, "guild_id");
        this.name = JSONHelper.getString(content, "name");
        this.type = JSONHelper.getInteger(content, "type");
        this.position = JSONHelper.getInteger(content, "position");
        this.nsfw = JSONHelper.getBoolean(content, "nsfw");
        this.parentId = JSONHelper.getString(content, "parent_id");
        this.permissionOverwrites = JSONHelper.getJSONArray(content,"permission_overwrites");
        this.topic = JSONHelper.getString(content, "topic");
        this.rateLimitPerUser = JSONHelper.getInteger(content, "rate_limit_per_user");
        this.lastMessageId = JSONHelper.getString(content, "last_message_id");
        this.bitrate = JSONHelper.getInteger(content, "bitrate");
        this.userLimit = JSONHelper.getInteger(content, "user_limit");
    }

    public static List<Channel> sort(List<Channel> channels) {
        Collections.sort(channels, new Comparator<Channel>() {
            @Override
            public int compare(Channel o1, Channel o2) {
                return o1.position - o2.position;
            }
        });
        return channels;
    }
}
