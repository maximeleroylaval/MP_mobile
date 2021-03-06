package ca.ulaval.ima.mp.gateway.voice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.json.JSONObject;

import ca.ulaval.ima.mp.sdk.JSONHelper;

public class Disconnect {
    @JsonProperty("user_id")
    public String userId;

    public Disconnect(JSONObject obj) {
        this.userId = JSONHelper.getString(obj, "user_id");
    }

    public Disconnect(String userId) {
        this.userId = userId;
    }
}
