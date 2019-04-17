package ca.ulaval.ima.mp.models.gateway.voice;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.json.JSONObject;

import ca.ulaval.ima.mp.JSONHelper;

public class SessionDescription {
    public String mode;
    @JsonProperty("secret_key")
    public Byte[] secretKey;
    @JsonProperty("video_codec")
    public String videoCodec;
    @JsonProperty("media_session_id")
    public String mediaSessionId;
    @JsonProperty("audio_codec")
    public String audioCodec;

    public SessionDescription(JSONObject obj) {
        this.mode = JSONHelper.getString(obj, "mode");
        this.secretKey = JSONHelper.getByteArray(obj, "secret_key");
        this.videoCodec = JSONHelper.getString(obj, "video_codec");
        this.mediaSessionId = JSONHelper.getString(obj, "media_session_id");
        this.audioCodec = JSONHelper.getString(obj, "audio_codec");
    }
}
