package org.maowtm.mc.warden_warn;

public abstract class Utils {
    public static String ticksToHumanTime(int ticks) {
        boolean negative = false;
        if (ticks < 0) {
            negative = true;
            ticks = -ticks;
        }
        int nb_secs = ticks / 20;
        int nb_minutes = nb_secs / 60;
        int rem_secs = nb_secs - nb_minutes * 60;
        var sign = "";
        if (negative) {
            sign = "-";
        }
        if (nb_minutes == 0) {
            return String.format("%s%ds", sign, nb_secs);
        } else {
            return String.format("%s%dm%ds", sign, nb_minutes, rem_secs);
        }
    }
}
