package sim;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.HardwareDevice;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Holds the robot configuration sent by the simulator, the latest sensor state, and the
 * device objects handed to the current OpMode.
 */
public final class SimHardware {

    public static final class Spec {
        final String name;
        final String type;
        final double ticksPerRev;
        final double maxRpm;
        final Map<String, Object> raw;

        Spec(Map<String, Object> m) {
            raw = m;
            name = Json.str(m.get("name"), "?");
            type = Json.str(m.get("type"), "motor");
            ticksPerRev = Json.num(m.get("ticks_per_rev"), 537.7);
            maxRpm = Json.num(m.get("max_rpm"), 312);
        }
    }

    private final Map<String, Spec> specs = new LinkedHashMap<>();
    private volatile Map<String, HardwareDevice> session = new ConcurrentHashMap<>();
    private volatile boolean configured = false;
    /** False while no OpMode is running: the robot gets zero power, like a stopped robot. */
    volatile boolean outputsEnabled = false;

    /** Raw (shaft-direction) encoder position and velocity in ticks and ticks/s, keyed by motor name. */
    final Map<String, double[]> encoders = new ConcurrentHashMap<>();
    /** Raw robot heading on the field in degrees, counter-clockwise positive, unwrapped. */
    volatile double headingDeg = 0;
    volatile double angularVelDeg = 0;
    volatile double battery = 12.8;
    volatile Map<String, Double> distances = new ConcurrentHashMap<>();
    volatile Map<String, Boolean> touches = new ConcurrentHashMap<>();

    /** Rows: the hub's x, y, z axes in the robot frame for the hub as it is physically mounted. */
    volatile double[][] physicalHubAxes = new RevHubOrientationOnRobot(
            RevHubOrientationOnRobot.LogoFacingDirection.UP, RevHubOrientationOnRobot.UsbFacingDirection.FORWARD)
            .simHubAxesInRobotFrame();
    volatile String physicalHubDesc = "logo UP, USB FORWARD";

    private final Consumer<String> logger;

    public SimHardware(Consumer<String> logger) { this.logger = logger; }

    void log(String msg) { logger.accept(msg); }

    // ---------------------------------------------------------------- config

    synchronized void configure(Map<String, Object> hello) {
        specs.clear();
        for (Object o : Json.arr(hello.get("devices"))) {
            Spec s = new Spec(Json.obj(o));
            specs.put(s.name, s);
        }
        boolean hasVoltage = false;
        for (Spec s : specs.values()) if (s.type.equals("voltage")) hasVoltage = true;
        if (!hasVoltage) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("name", "Control Hub");
            v.put("type", "voltage");
            specs.put("Control Hub", new Spec(v));
        }
        Map<String, Object> hub = Json.obj(hello.get("hub"));
        String logo = Json.str(hub.get("logo"), "UP");
        String usb = Json.str(hub.get("usb"), "FORWARD");
        try {
            RevHubOrientationOnRobot o = new RevHubOrientationOnRobot(
                    RevHubOrientationOnRobot.LogoFacingDirection.valueOf(logo),
                    RevHubOrientationOnRobot.UsbFacingDirection.valueOf(usb));
            physicalHubAxes = o.simHubAxesInRobotFrame();
            physicalHubDesc = o.simDescribe();
        } catch (IllegalArgumentException e) {
            log("robot.json has an invalid hub orientation (" + logo + "/" + usb + "): " + e.getMessage());
        }
        configured = true;
        log("Robot config received: " + specs.size() + " devices, hub mounted " + physicalHubDesc);
    }

    public synchronized List<String> deviceNames() { return new ArrayList<>(specs.keySet()); }

    public boolean deviceCompatible(String name, Class<?> type) {
        HardwareDevice d = device(name);
        return d != null && type.isInstance(d);
    }

    public HardwareDevice device(String name) {
        Spec s;
        synchronized (this) { s = specs.get(name); }
        if (s == null) return null;
        return session.computeIfAbsent(name, n -> create(s));
    }

    private HardwareDevice create(Spec s) {
        switch (s.type) {
            case "motor": return new SimMotor(this, s);
            case "crservo": return new SimCRServo(s.name);
            case "servo": return new SimServo(s.name);
            case "imu": return new SimIMU(this, s.name);
            case "distance": return new SimSensors.Distance(this, s.name);
            case "touch": return new SimSensors.Touch(this, s.name);
            case "voltage": return new SimSensors.Voltage(this, s.name);
            default:
                log("robot.json: device \"" + s.name + "\" has unknown type \"" + s.type + "\"");
                return null;
        }
    }

    public synchronized String hintFor(String name, Class<?> requested) {
        if (!configured) return " (the simulator has not sent its robot config yet)";
        Spec s = specs.get(name);
        if (s == null) {
            return ". The simulator's robot.json has no device with that name. Configured devices: "
                    + String.join(", ", specs.keySet())
                    + ". Fix the name in your code, or add it to Simulator/godot/config/robot.json.";
        }
        return ". In robot.json \"" + name + "\" is a " + s.type + ", but the code asked for a "
                + requested.getSimpleName() + ".";
    }

    // ---------------------------------------------------------------- session

    /** New OpMode: hand out fresh device objects (directions, modes, etc. reset like on a real robot). */
    HardwareMap newSession() {
        zeroAll();
        session = new ConcurrentHashMap<>();
        return new HardwareMap(this);
    }

    void zeroAll() {
        for (HardwareDevice d : session.values()) {
            if (d instanceof SimMotor) ((SimMotor) d).simZero();
            if (d instanceof SimCRServo) ((SimCRServo) d).simZero();
        }
    }

    // ---------------------------------------------------------------- I/O

    void updateFromState(Map<String, Object> state) {
        headingDeg = Json.num(state.get("heading"), headingDeg);
        angularVelDeg = Json.num(state.get("angvel"), 0);
        battery = Json.num(state.get("battery"), battery);
        for (Map.Entry<String, Object> e : Json.obj(state.get("motors")).entrySet()) {
            List<Object> pv = Json.arr(e.getValue());
            if (pv.size() >= 2) {
                encoders.put(e.getKey(), new double[] {Json.num(pv.get(0), 0), Json.num(pv.get(1), 0)});
            }
        }
        Map<String, Double> dist = new ConcurrentHashMap<>();
        for (Map.Entry<String, Object> e : Json.obj(state.get("distance")).entrySet()) {
            dist.put(e.getKey(), Json.num(e.getValue(), Double.MAX_VALUE));
        }
        distances = dist;
        Map<String, Boolean> touch = new ConcurrentHashMap<>();
        for (Map.Entry<String, Object> e : Json.obj(state.get("touch")).entrySet()) {
            touch.put(e.getKey(), Boolean.TRUE.equals(e.getValue()));
        }
        touches = touch;
    }

    Map<String, Object> outputs() {
        Map<String, Object> motors = new LinkedHashMap<>();
        Map<String, Object> crservos = new LinkedHashMap<>();
        Map<String, Object> servos = new LinkedHashMap<>();
        for (Map.Entry<String, HardwareDevice> e : outputsEnabled ? session.entrySet()
                : java.util.Collections.<String, HardwareDevice>emptyMap().entrySet()) {
            HardwareDevice d = e.getValue();
            if (d instanceof SimMotor) {
                SimMotor m = (SimMotor) d;
                Map<String, Object> mo = new LinkedHashMap<>();
                mo.put("p", m.simRawOutput());
                mo.put("brake", m.simBrake());
                motors.put(e.getKey(), mo);
            } else if (d instanceof SimCRServo) {
                crservos.put(e.getKey(), ((SimCRServo) d).simRawOutput());
            } else if (d instanceof SimServo) {
                double p = ((SimServo) d).simOutput();
                if (!Double.isNaN(p)) servos.put(e.getKey(), p);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", "out");
        out.put("motors", motors);
        out.put("crservos", crservos);
        out.put("servos", servos);
        return out;
    }
}
