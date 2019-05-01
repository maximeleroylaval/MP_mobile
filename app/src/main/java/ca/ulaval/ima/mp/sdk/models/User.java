package ca.ulaval.ima.mp.sdk.models;

import android.net.Uri;
import android.util.Log;

import org.json.JSONObject;

import ca.ulaval.ima.mp.sdk.JSONHelper;
import ca.ulaval.ima.mp.sdk.SDK;

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

    private String getAssetId() {
        switch (Integer.parseInt(this.discriminator) % 5)
        {
            case 0: return "6debd47ed13483642cf09e832ed0bc1b";
            case 1: return "322c936a8c8be1b803cd94861bdfa868";
            case 2: return "dd4dbc0016779df1378e7812eabaa04d";
            case 3: return "0e291f67c9274a1abdddeb3fd919cbaa";
            case 4: return "1cbd08c76f8af6dddce02c5138971129";
        }
        return null;
    }

    public Uri getAvatarURI() {
        if (this.id != null && !this.id.equals("") && this.avatar != null && !this.avatar.equals("")) {
            return Uri.parse(SDK.cdn + "avatars/" + this.id + "/" + this.avatar + ".jpg");
        }
        else if (this.discriminator != null && !this.discriminator.equals("")) {
            String assetId = getAssetId();
            return Uri.parse(SDK.main + "assets/" + assetId + ".png");
        }
        return null;
    }
}
