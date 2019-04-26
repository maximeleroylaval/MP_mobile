package ca.ulaval.ima.mp.gateway.server.model;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import ca.ulaval.ima.mp.sdk.JSONHelper;
import ca.ulaval.ima.mp.gateway.Gateway;
import ca.ulaval.ima.mp.gateway.server.ServerListener;

public class Heartbeat extends Payload {
    public static class Interval {
        Payload count = null;
        Integer heartbeatInterval;
        List<String> _trace;

        Thread thread = null;
        boolean started = true;

        public Interval(Payload payload) {
            JSONObject jsonObj = (JSONObject)payload.d;
            this.heartbeatInterval = JSONHelper.getInteger(jsonObj, "heartbeat_interval");
            this._trace = JSONHelper.asArrayWithConstructor(String.class, String.class, JSONHelper.getJSONArray(jsonObj, "_trace"));
            this.update(count);
        }

        public void update(Payload payload) {
            count = payload;
        }

        public void start(final ServerListener listener) {
            this.thread = new Thread() {
                @Override
                public void run() {
                    try {
                        while(started) {
                            TimeUnit.MILLISECONDS.sleep(heartbeatInterval);
                            listener.sendHeartbeat(count);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            this.thread.start();
        }

        public void stop() {
            this.started = false;
        }
    }

    public Heartbeat() {
        super(Gateway.SERVER.OP.HEARTBEAT, null, null, null);
    }

    public Heartbeat(Payload payload) {
        super(Gateway.SERVER.OP.HEARTBEAT, payload != null ? payload.s : null, null, null);
    }
}
