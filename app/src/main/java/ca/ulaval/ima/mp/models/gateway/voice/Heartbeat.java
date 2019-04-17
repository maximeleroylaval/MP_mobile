package ca.ulaval.ima.mp.models.gateway.voice;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import ca.ulaval.ima.mp.JSONHelper;
import ca.ulaval.ima.mp.models.gateway.Gateway;
import ca.ulaval.ima.mp.models.gateway.Payload;

public class Heartbeat extends Payload {
    public static class Interval {
        public Payload count = null;
        public Integer v;
        public Integer heartbeatInterval;

        public Thread thread = null;
        public boolean started = true;

        public Interval(Payload payload) {
            JSONObject jsonObj = (JSONObject)payload.d;
            this.v = JSONHelper.getInteger(jsonObj, "v");
            this.heartbeatInterval = JSONHelper.getInteger(jsonObj, "heartbeat_interval");
            this.update(count);
        }

        public void update(Payload payload) {
            count = payload;
        }

        public void start(final VoiceListener listener) {
            this.thread = new Thread() {
                @Override
                public void run() {
                    try {
                        while(started) {
                            long milliseconds = Double.valueOf(heartbeatInterval * 0.75).longValue();
                            TimeUnit.MILLISECONDS.sleep(milliseconds);
                            listener.input(new Heartbeat(count).toJSONString());
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
        super(Gateway.VOICE.OP.HEARTBEAT, null, null, null);
    }

    public Heartbeat(Payload payload) {
        super(Gateway.VOICE.OP.HEARTBEAT, payload != null ? payload.s : null, null, null);
    }
}
