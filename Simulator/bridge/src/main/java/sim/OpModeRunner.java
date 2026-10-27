package sim;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeInternals;
import com.qualcomm.robotcore.hardware.Gamepad;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runs one OpMode at a time through INIT -> START -> STOP, like the Driver Station. */
final class OpModeRunner {
    enum State { IDLE, INIT, RUNNING, STOPPING, STOPPED, ERROR }

    private final SimHardware hw;
    private final SimLink link;
    private final OpModeRegistry registry;

    final Gamepad gamepad1 = new Gamepad();
    final Gamepad gamepad2 = new Gamepad();

    private volatile State state = State.IDLE;
    private volatile OpModeRegistry.Entry entry;
    private volatile OpMode op;
    private volatile Thread thread;
    private volatile SimTelemetry telemetry;
    private volatile boolean startFlag = false;
    private volatile boolean stopFlag = false;
    private volatile String error = null;
    private long lastTelemetryNanos = 0;
    private int session = 0;

    OpModeRunner(SimHardware hw, SimLink link, OpModeRegistry registry) {
        this.hw = hw;
        this.link = link;
        this.registry = registry;
    }

    // ------------------------------------------------------------ commands

    synchronized void init(String name) {
        if (state == State.INIT || state == State.RUNNING) stop("new INIT");
        Thread old = thread;
        if (old != null && old.isAlive()) {
            SimMain.log("Warning: the previous OpMode thread is still running (it never returned from runOpMode/loop).");
        }
        OpModeRegistry.Entry e = registry.find(name);
        if (e == null) {
            fail("No OpMode named \"" + name + "\"", null);
            return;
        }
        entry = e;
        error = null;
        startFlag = false;
        stopFlag = false;
        OpMode instance;
        try {
            instance = e.cls.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException | RuntimeException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            fail("Could not create " + e.cls.getSimpleName() + " (it needs a public no-argument constructor)", cause);
            return;
        }
        SimTelemetry tel = new SimTelemetry();
        instance.hardwareMap = hw.newSession();
        instance.telemetry = tel;
        instance.gamepad1 = gamepad1;
        instance.gamepad2 = gamepad2;
        OpModeInternals.resetClock(instance);
        op = instance;
        telemetry = tel;
        hw.outputsEnabled = true;
        state = State.INIT;
        final int mySession = ++session;
        Thread t = new Thread(() -> run(instance, mySession), "OpMode-" + e.name.trim());
        t.setDaemon(true);
        thread = t;
        sendStatus();
        SimMain.log("INIT " + e.kind + " \"" + e.name.trim() + "\" (" + e.cls.getSimpleName() + ")");
        t.start();
    }

    synchronized void start() {
        if (state != State.INIT) return;
        startFlag = true;
        state = State.RUNNING;
        OpModeInternals.resetClock(op);
        if (op instanceof LinearOpMode) OpModeInternals.signalStart((LinearOpMode) op);
        sendStatus();
        SimMain.log("START \"" + entry.name.trim() + "\"");
    }

    synchronized void stop(String reason) {
        if (state != State.INIT && state != State.RUNNING) return;
        stopFlag = true;
        state = State.STOPPING;
        OpMode o = op;
        Thread t = thread;
        if (o instanceof LinearOpMode) {
            OpModeInternals.signalStop((LinearOpMode) o);
            if (t != null) t.interrupt();
        }
        sendStatus();
        SimMain.log("STOP \"" + entry.name.trim() + "\"" + (reason != null ? " (" + reason + ")" : ""));
        final int mySession = session;
        Thread watcher = new Thread(() -> {
            try {
                if (t != null) t.join(1500);
            } catch (InterruptedException ignored) { }
            synchronized (OpModeRunner.this) {
                if (mySession != session) return;
                hw.outputsEnabled = false;
                hw.zeroAll();
                if (t != null && t.isAlive()) {
                    fail("The OpMode did not stop within 1.5 s after STOP. A real robot would show \"stuck in stop()\" and restart the app. "
                            + "Check loops that don't test opModeIsActive() / isStopRequested().", null);
                } else if (state == State.STOPPING) {
                    state = State.STOPPED;
                    sendStatus();
                }
            }
        }, "stop-watcher");
        watcher.setDaemon(true);
        watcher.start();
    }

    // ------------------------------------------------------------ OpMode thread

    private void run(OpMode o, int mySession) {
        try {
            if (o instanceof LinearOpMode) {
                ((LinearOpMode) o).runOpMode();
            } else {
                runIterative(o);
            }
            o.telemetry.update();
        } catch (InterruptedException | OpModeInternals.ForceStopException e) {
            // normal ways to end
        } catch (Throwable t) {
            synchronized (this) {
                if (mySession == session) fail("User code threw an uncaught exception: " + t.getClass().getSimpleName()
                        + (t.getMessage() != null ? " - " + t.getMessage() : ""), t);
            }
            return;
        }
        synchronized (this) {
            if (mySession != session) return;
            if (state == State.INIT || state == State.RUNNING) {
                SimMain.log("\"" + entry.name.trim() + "\" finished (runOpMode returned).");
                hw.outputsEnabled = false;
                hw.zeroAll();
                state = State.STOPPED;
                sendStatus();
            }
        }
    }

    private void runIterative(OpMode o) throws InterruptedException {
        o.init();
        o.telemetry.update();
        while (!startFlag && !stopFlag && !OpModeInternals.selfStopRequested(o)) {
            o.init_loop();
            o.telemetry.update();
            Thread.sleep(10);
        }
        if (startFlag && !stopFlag) {
            o.start();
            while (!stopFlag && !OpModeInternals.selfStopRequested(o)) {
                o.time = o.getRuntime();
                o.loop();
                o.telemetry.update();
                Thread.sleep(10);
            }
        }
        o.stop();
        if (OpModeInternals.selfStopRequested(o)) {
            synchronized (this) { if (state == State.RUNNING || state == State.INIT) stop("requestOpModeStop()"); }
        }
    }

    // ------------------------------------------------------------ comm thread

    /** Called every simulator tick: forward telemetry at the OpMode's transmission rate. */
    void tick() {
        OpMode o = op;
        if (o != null) o.time = o.getRuntime();
        SimTelemetry tel = telemetry;
        if (tel == null) return;
        long now = System.nanoTime();
        if ((now - lastTelemetryNanos) / 1e6 < tel.intervalMs()) return;
        List<String> lines = tel.poll();
        if (lines == null) return;
        lastTelemetryNanos = now;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "telemetry");
        m.put("lines", new ArrayList<>(lines));
        link.send(m);
    }

    void shutdown() {
        stop("simulator disconnected");
        hw.outputsEnabled = false;
    }

    // ------------------------------------------------------------ helpers

    private void fail(String msg, Throwable t) {
        stopFlag = true;
        hw.outputsEnabled = false;
        hw.zeroAll();
        StringBuilder sb = new StringBuilder(msg);
        if (t != null) {
            sb.append('\n');
            int shown = 0;
            for (StackTraceElement el : t.getStackTrace()) {
                String c = el.getClassName();
                if (c.startsWith("java.") || c.startsWith("jdk.") || c.startsWith("sun.")) continue;
                if (c.startsWith("sim.OpModeRunner")) break;
                sb.append("    at ").append(el).append('\n');
                if (++shown >= 12) break;
            }
            Throwable cause = t.getCause();
            if (cause != null && cause != t) sb.append("  caused by ").append(cause).append('\n');
        }
        error = sb.toString().trim();
        state = State.ERROR;
        SimMain.log("ERROR: " + error);
        sendStatus();
    }

    private void sendStatus() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "status");
        m.put("state", state.name());
        m.put("opmode", entry != null ? entry.name : "");
        m.put("kind", entry != null ? entry.kind : "");
        if (error != null) m.put("error", error);
        link.send(m);
    }
}
