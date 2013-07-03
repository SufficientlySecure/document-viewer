package org.emdev.ui;

public interface ActivityEvents {

    int BEFORE_CREATE = 1 << 0;
    int BEFORE_RECREATE = 1 << 1;
    int ON_CREATE = 1 << 2;
    int AFTER_CREATE = 1 << 3;
    int ON_RESTART = 1 << 5;
    int ON_START = 1 << 6;
    int ON_POST_CREATE = 1 << 7;
    int ON_RESUME = 1 << 8;
    int ON_POST_RESUME = 1 << 9;
    int ON_PAUSE = 1 << 10;
    int ON_STOP = 1 << 11;
    int ON_DESTROY = 1 << 12;

    int ACTIVITY_EVENTS = ON_CREATE | ON_RESTART | ON_START | ON_POST_CREATE | ON_RESUME | ON_PAUSE | ON_STOP
            | ON_DESTROY;

    int CONTROLLER_EVENTS = BEFORE_CREATE | BEFORE_RECREATE | AFTER_CREATE | ON_RESTART | ON_START
            | ON_POST_CREATE | ON_RESUME | ON_PAUSE | ON_STOP | ON_DESTROY;

    public static class Helper {

        public static int merge(final int... events) {
            int res = 0;
            for (int i = 0; i < events.length; i++) {
                res |= events[i];
            }
            return res;
        }

        public static int exclude(final int orig, final int... excluding) {
            int res = orig;
            for (int i = 0; i < excluding.length; i++) {
                res &= ~excluding[i];
            }
            return res;
        }

        public static boolean enabled(final int events, final int mask) {
            return (events & mask) == mask;
        }
    }

}
