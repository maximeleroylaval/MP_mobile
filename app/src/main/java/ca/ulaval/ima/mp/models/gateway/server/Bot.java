package ca.ulaval.ima.mp.models.gateway.server;

import org.json.JSONObject;

import ca.ulaval.ima.mp.JSONHelper;

public class Bot {
    public String url;
    public Integer shards;
    public SessionStartLimit sessionStartLimit;

    public Bot(JSONObject obj) {
        this.url = JSONHelper.getString(obj, "url");
        this.shards = JSONHelper.getInteger(obj,"shards");
        this.sessionStartLimit = new SessionStartLimit(JSONHelper.getJSONObject(obj, "session_start_limit"));
    }
}