package com.qualcomm.robotcore.eventloop.opmode;

/** Simulator-only hooks the OpMode runner uses to drive the OpMode lifecycle. Not part of the FTC SDK. */
public final class OpModeInternals {
    private OpModeInternals() { }

    public static final class ForceStopException extends RuntimeException {
        public ForceStopException() { super("terminateOpModeNow()"); }
    }

    public static void signalStart(LinearOpMode op) {
        synchronized (op.startLock) {
            op.started = true;
            op.startLock.notifyAll();
        }
    }

    public static void signalStop(LinearOpMode op) {
        synchronized (op.startLock) {
            op.stopRequested = true;
            op.startLock.notifyAll();
        }
    }

    public static boolean selfStopRequested(OpMode op) { return op.stopRequestedBySelf; }

    public static void resetClock(OpMode op) { op.startNanos = System.nanoTime(); }
}
