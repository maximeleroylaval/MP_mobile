package ca.ulaval.ima.mp.models.gateway.server;

import com.fasterxml.jackson.annotation.JsonProperty;

import ca.ulaval.ima.mp.SDK;

public class Identify {
    public String token;
    public ConnectionProperties properties;
    public Boolean compress;
    @JsonProperty("large_threshold")
    public Integer largeThreshold;
    public Update.Status presence;
    //public List<Integer> shard;

    public Identify() {
        this.token = SDK.botToken;
        this.properties = new ConnectionProperties();
        this.compress = false;
        this.largeThreshold = 250;
        this.presence = new Update.Status();
        //this.shard = new ArrayList<>();
    }
}
