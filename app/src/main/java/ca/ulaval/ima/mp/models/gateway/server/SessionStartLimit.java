package ca.ulaval.ima.mp.models.gateway.server;

import org.json.JSONObject;

import ca.ulaval.ima.mp.JSONHelper;

public class SessionStartLimit {
    public Integer total;
    public Integer remaining;
    public Integer resetAfter;

    public SessionStartLimit(JSONObject obj) {
        this.total = JSONHelper.getInteger(obj, "total");
        this.remaining = JSONHelper.getInteger(obj, "remaining");
        this.resetAfter = JSONHelper.getInteger(obj, "reset_after");
    }
}