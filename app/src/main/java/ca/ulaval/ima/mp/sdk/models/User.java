package ca.ulaval.ima.mp.sdk.models;

import android.net.Uri;

import org.json.JSONObject;

import ca.ulaval.ima.mp.sdk.JSONHelper;

public class User {
    public String id;
    public String username;
    public String discriminator;
    public String avatar;
    public Boolean bot;
    public Boolean mfaEnabled;
    public String locale;
    public Boolean verified;
    public String email;
    public Integer flags;
    public Integer premiumType;

    public User(JSONObject object) {
        this.id = JSONHelper.getString(object, "id");
        this.username = JSONHelper.getString(object, "username");
        this.discriminator = JSONHelper.getString(object, "discriminator");
        this.avatar = JSONHelper.getString(object, "avatar");
        this.bot = JSONHelper.getBoolean(object, "bot");
        this.mfaEnabled = JSONHelper.getBoolean(object, "mfa_enabled");
        this.locale = JSONHelper.getString(object, "locale");
        this.verified = JSONHelper.getBoolean(object, "verified");
        this.email = JSONHelper.getString(object, "email");
        this.flags = JSONHelper.getInteger(object, "flags");
        this.premiumType = JSONHelper.getInteger(object, "premium_type");
    }

    public Uri getAvatarURI() {
        if (this.id != null && !this.id.equals("") && this.avatar != null && !this.avatar.equals(""))
            return Uri.parse("https://cdn.discordapp.com/avatars/" + this.id + "/" + this.avatar + ".jpg");
        return null;
    }
}
