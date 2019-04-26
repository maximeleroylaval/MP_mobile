package ca.ulaval.ima.mp.gateway.voice.model;

import org.json.JSONObject;

import ca.ulaval.ima.mp.sdk.JSONHelper;

public class SelectProtocol {
    public static class Data {
        public String address;
        public Integer port;
        public String mode;

        public Data(JSONObject obj) {
            this.address = JSONHelper.getString(obj, "address");
            this.port = JSONHelper.getInteger(obj, "port");
            this.mode = JSONHelper.getString(obj, "mode");
        }

        public Data(String address, Integer port, String mode) {
            this.address = address;
            this.port = port;
            this.mode = mode;
        }
    }

    public String protocol;
    public Data data;

    public SelectProtocol(JSONObject obj) {
        this.protocol = JSONHelper.getString(obj, "protocol");
        this.data = new Data(JSONHelper.getJSONObject(obj, "data"));
    }

    public SelectProtocol(String protocol, Data data) {
        this.protocol = protocol;
        this.data = data;
    }
}
