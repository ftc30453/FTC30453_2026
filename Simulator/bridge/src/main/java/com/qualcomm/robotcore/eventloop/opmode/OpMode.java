package com.qualcomm.robotcore.eventloop.opmode;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

/** Iterative OpMode: init(), init_loop(), start(), loop(), stop(). */
public abstract class OpMode {
    public Gamepad gamepad1 = new Gamepad();
    public Gamepad gamepad2 = new Gamepad();
    public Telemetry telemetry;
    public HardwareMap hardwareMap;
    /** Seconds since the OpMode was initialized (updated every loop). */
    public double time = 0.0;

    long startNanos = System.nanoTime();
    volatile boolean stopRequestedBySelf = false;

    public OpMode() { }

    public abstract void init();
    public void init_loop() { }
    public void start() { }
    public abstract void loop();
    public void stop() { }

    public void requestOpModeStop() { stopRequestedBySelf = true; }
    public final void terminateOpModeNow() { requestOpModeStop(); throw new OpModeInternals.ForceStopException(); }

    public double getRuntime() { return (System.nanoTime() - startNanos) / 1e9; }
    public void resetRuntime() { startNanos = System.nanoTime(); }

    public void updateTelemetry(Telemetry telemetry) { telemetry.update(); }
}
