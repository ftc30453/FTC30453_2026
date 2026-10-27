package com.qualcomm.robotcore.util;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ElapsedTime {
    public enum Resolution { SECONDS, MILLISECONDS }

    public static final long SECOND_IN_NANO = 1_000_000_000L;
    public static final long MILLIS_IN_NANO = 1_000_000L;

    protected volatile long nsStartTime;
    protected final double resolution;

    public ElapsedTime() { this(Resolution.SECONDS); }

    public ElapsedTime(long startTime) {
        this.nsStartTime = startTime;
        this.resolution = SECOND_IN_NANO;
    }

    public ElapsedTime(Resolution resolution) {
        reset();
        this.resolution = resolution == Resolution.SECONDS ? SECOND_IN_NANO : MILLIS_IN_NANO;
    }

    protected long nsNow() { return System.nanoTime(); }

    public long now(TimeUnit unit) { return unit.convert(nsNow(), TimeUnit.NANOSECONDS); }
    public void reset() { nsStartTime = nsNow(); }
    public double startTime() { return nsStartTime / resolution; }
    public long startTimeNanoseconds() { return nsStartTime; }
    public double time() { return (nsNow() - nsStartTime) / resolution; }
    public long time(TimeUnit unit) { return unit.convert(nanoseconds(), TimeUnit.NANOSECONDS); }
    public double seconds() { return nanoseconds() / (double) SECOND_IN_NANO; }
    public double milliseconds() { return seconds() * 1000.0; }
    public long nanoseconds() { return nsNow() - nsStartTime; }

    public Resolution getResolution() {
        return resolution == SECOND_IN_NANO ? Resolution.SECONDS : Resolution.MILLISECONDS;
    }

    public void log(String label) {
        System.out.println(String.format(Locale.US, "TIMER: %20s - %1.3f %s", label, time(),
                getResolution() == Resolution.SECONDS ? "seconds" : "milliseconds"));
    }

    @Override public String toString() {
        return getResolution() == Resolution.SECONDS
                ? String.format(Locale.US, "%.4f seconds", time())
                : String.format(Locale.US, "%.4f milliseconds", time());
    }
}
