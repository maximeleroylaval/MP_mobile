package ca.ulaval.ima.mp.gateway.server.model;

import org.json.JSONObject;

import ca.ulaval.ima.mp.sdk.JSONHelper;

public class Payload {
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