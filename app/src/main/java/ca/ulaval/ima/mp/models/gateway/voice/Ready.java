package ca.ulaval.ima.mp.models.gateway.voice;

import org.json.JSONObject;

import java.util.List;

import ca.ulaval.ima.mp.JSONHelper;

public class Ready {
    public Integer ssrc;
    public String ip;
    public Integer port;
    public List<String> modes;

    public Ready(JSONObject obj) {
        this.ssrc = JSONHelper.getInteger(obj, "ssrc");
        this.ip = JSONHelper.getString(obj, "ip");
        this.port = JSONHelper.getInteger(obj, "port");
        this.modes = JSONHelper.asArrayWithConstructor(String.class, String.class, JSONHelper.getJSONArray(obj, "modes"));
    }
}