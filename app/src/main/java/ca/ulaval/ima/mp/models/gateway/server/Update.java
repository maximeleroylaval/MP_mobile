package ca.ulaval.ima.mp.models.gateway.server;

public class Update {
    public static class Status {
        public static class TYPE {
            public static String ONLINE = "online";
            public static String DO_NOT_DISTURB = "dnd";
            public static String AFK = "idle";
            public static String INVISIBLE = "invisible";
            public static String OFFLINE = "offline";
        }

        public Integer since;
        public Activity game;
        public String status;
        public Boolean afk;

        public Status() {
            this.since = null;
            this.status = TYPE.ONLINE;
            this.afk = false;
            this.game = null;
        }
    }
}

