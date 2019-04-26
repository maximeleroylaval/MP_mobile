package ca.ulaval.ima.mp.gateway.voice.model;

import org.json.JSONObject;

import ca.ulaval.ima.mp.sdk.JSONHelper;

public class Speaking {
    public Boolean speaking;
    public Integer delay;
    public Integer ssrc;

    public Speaking(JSONObject obj) {
        this.speaking = JSONHelper.getBoolean(obj, "speaking");
        this.delay = JSONHelper.getInteger(obj, "delay");
        this.ssrc = JSONHelper.getInteger(obj, "ssrc");
    }

    public Speaking(Boolean speaking, Integer delay, Integer ssrc) {
        this.speaking = speaking;
        this.delay = delay;
        this.ssrc = ssrc;
    }
}
