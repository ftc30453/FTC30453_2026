package com.qualcomm.robotcore.eventloop.opmode;

/** Linear OpMode: everything happens inside runOpMode(). */
public abstract class LinearOpMode extends OpMode {
    volatile boolean started = false;
    volatile boolean stopRequested = false;
    final Object startLock = new Object();

    public LinearOpMode() { }

    public abstract void runOpMode() throws InterruptedException;

    public void waitForStart() {
        synchronized (startLock) {
            while (!started && !stopRequested) {
                try {
                    startLock.wait(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public final void idle() { Thread.yield(); }

    public final void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public final boolean opModeIsActive() {
        boolean active = !isStopRequested() && isStarted();
        if (active) idle();
        return active;
    }

    public final boolean opModeInInit() { return !isStarted() && !isStopRequested(); }

    public final boolean isStarted() { return started || Thread.currentThread().isInterrupted(); }

    public final boolean isStopRequested() {
        return stopRequested || stopRequestedBySelf || Thread.currentThread().isInterrupted();
    }

    @Override public final void init() { }
    @Override public final void init_loop() { }
    @Override public final void start() { }
    @Override public final void loop() { }
    @Override public final void stop() { }
}
