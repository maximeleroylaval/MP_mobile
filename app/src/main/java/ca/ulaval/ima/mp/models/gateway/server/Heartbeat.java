package ca.ulaval.ima.mp.models.gateway.server;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import ca.ulaval.ima.mp.JSONHelper;
import ca.ulaval.ima.mp.models.gateway.Gateway;
import ca.ulaval.ima.mp.models.gateway.Payload;

public class Heartbeat extends Payload {
    public static class Interval {
        Payload count = null;
        Integer heartbeatInterval;
        List<String> _trace;

        Thread thread = null;
        boolean started = true;

        Interval(Payload payload) {
            JSONObject jsonObj = (JSONObject)payload.d;
            this.heartbeatInterval = JSONHelper.getInteger(jsonObj, "heartbeat_interval");
            this._trace = JSONHelper.asArrayWithConstructor(String.class, String.class, JSONHelper.getJSONArray(jsonObj, "_trace"));
            this.update(count);
        }

        void update(Payload payload) {
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

        void stop() {
            this.started = false;
        }
    }

    public Heartbeat() {
        super(Gateway.SERVER.OP.HEARTBEAT, null, null, null);
    }

    Heartbeat(Payload payload) {
        super(Gateway.SERVER.OP.HEARTBEAT, payload != null ? payload.s : null, null, null);
    }
}
