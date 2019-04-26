package ca.ulaval.ima.mp.gateway.voice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Identify {

    @JsonProperty("server_id")
    public String serverId;
    @JsonProperty("user_id")
    public String userId;
    @JsonProperty("session_id")
    public String sessionId;
    public String token;

    public Identify(String serverId, String userId, String sessionId, String token) {
        this.serverId = serverId;
        this.userId = userId;
        this.sessionId = sessionId;
        this.token = token;
    }
}