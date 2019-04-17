package ca.ulaval.ima.mp.models.gateway.server;

import org.json.JSONObject;

import java.util.List;

import ca.ulaval.ima.mp.JSONHelper;
import ca.ulaval.ima.mp.models.Guild;
import ca.ulaval.ima.mp.models.User;
import ca.ulaval.ima.mp.models.gateway.Payload;

public class Ready extends Payload {
    public Integer v;
    public User user;
    public List<String> privateChannels;
    public List<Guild.Unavailable> guilds;
    public String sessionId;
    public List<String> _trace;
    public List<Integer> shard;

    public void init() {
        JSONObject jsonObj = (JSONObject)this.d;
        this.v = JSONHelper.getInteger(jsonObj, "v");
        this.user = new User(JSONHelper.getJSONObject(jsonObj, "user"));
        this.privateChannels = JSONHelper.asArrayWithConstructor(String.class, String.class, JSONHelper.getJSONArray(jsonObj, "private_channels"));
        this.guilds = JSONHelper.asArray(Guild.Unavailable.class, JSONHelper.getJSONArray(jsonObj, "guilds"));
        this.sessionId = JSONHelper.getString(jsonObj, "session_id");
        this._trace = JSONHelper.asArrayWithConstructor(String.class, String.class, JSONHelper.getJSONArray(jsonObj, "_trace"));
        this.shard = JSONHelper.asArrayWithConstructor(Integer.class, Integer.class, JSONHelper.getJSONArray(jsonObj, "shard"));
    }

    public Ready(Payload payload) {
        super(payload.op, payload.d, payload.s, payload.t);
        this.init();
    }
}