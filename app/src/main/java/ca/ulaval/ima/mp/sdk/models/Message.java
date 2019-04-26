package ca.ulaval.ima.mp.sdk.models;

import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.List;

import ca.ulaval.ima.mp.sdk.JSONHelper;

public class Message {
    public String id;
    public String channelId;
    public String guildId;
    public User author;
    public Guild.Member member;
    public String content;
    public Timestamp timestamp;
    public Timestamp editedTimestamp;
    public Boolean tts;
    public Boolean mentionEveryone;
    public List<Message.Mention> mentions;
    public List<Role> mentionRoles;
    public List<Attachment> attachments;
    public List<Embed> embeds;
    public List<Reaction> reactions;
    public String nonce;
    public Boolean pinned;
    public String webhookId;
    public Integer type;
    public Message.Activity activity;
    public Message.Application application;

    public static class Activity {
        public Activity(JSONObject obj) {

        }
    }

    public static class Application {
        public Application(JSONObject obj) {

        }
    }

    public static class Mention {
        public Mention(JSONObject obj) {

        }
    }

    public Message(JSONObject content) {
        this.id = JSONHelper.getString(content,"id");
        this.channelId = JSONHelper.getString(content,"channel_id");
        this.guildId = JSONHelper.getString(content, "guild_id");
        this.author = new User(JSONHelper.getJSONObject(content, "author"));
        this.member = new Guild.Member(JSONHelper.getJSONObject(content, "member"));
        this.content = JSONHelper.getString(content, "content");
        this.timestamp = JSONHelper.getTimestamp(content, "timestamp");
        this.editedTimestamp = JSONHelper.getTimestamp(content, "edited_timestamp");
        this.tts = JSONHelper.getBoolean(content, "tts");
        this.mentionEveryone = JSONHelper.getBoolean(content,"mention_everyone");
        this.mentions = JSONHelper.asArray(Mention.class, JSONHelper.getJSONArray(content, "mentions"));
        this.mentionRoles = JSONHelper.asArray(Role.class, JSONHelper.getJSONArray(content, "mention_roles"));
        this.attachments = JSONHelper.asArray(Attachment.class, JSONHelper.getJSONArray(content, "attachments"));
        this.embeds = JSONHelper.asArray(Embed.class, JSONHelper.getJSONArray(content, "embeds"));
        this.reactions = JSONHelper.asArray(Reaction.class, JSONHelper.getJSONArray(content, "reactions"));
        this.nonce = JSONHelper.getString(content, "nonce");
        this.pinned = JSONHelper.getBoolean(content, "pinned");
        this.webhookId = JSONHelper.getString(content, "webhook_id");
        this.type = JSONHelper.getInteger(content, "type");
        this.activity = new Message.Activity(JSONHelper.getJSONObject(content, "activity"));
        this.application = new Message.Application(JSONHelper.getJSONObject(content, "application"));
    }
}
