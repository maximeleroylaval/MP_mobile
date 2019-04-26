package ca.ulaval.ima.mp.gateway.server.model;

public class ConnectionProperties {
    public String $os;
    public String $browser;
    public String $device;

    public ConnectionProperties() {
        this.$os = "android";
        this.$browser = "okhttp3";
        this.$device = "okhttp3";
    }
}