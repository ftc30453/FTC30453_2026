package sim;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

/**
 * Simulated DC motor. Direction works like the real SDK: REVERSE flips both the power sent to
 * the motor and the encoder reading. Whether a motor physically spins its wheel "forward" is
 * decided by the robot in the simulator (robot.json "mirrored"), not by this class.
 */
final class SimMotor implements DcMotorEx {
    private final SimHardware hw;
    private final String name;
    private final double ticksPerRev;
    private final double maxTicksPerSec;

    private volatile Direction direction = Direction.FORWARD;
    private volatile RunMode mode = RunMode.RUN_WITHOUT_ENCODER;
    private volatile ZeroPowerBehavior zeroPowerBehavior = ZeroPowerBehavior.BRAKE;
    private volatile double power = 0;
    private volatile double encoderOffset = 0;
    private volatile Integer targetPosition = null;
    private volatile int tolerance = 10;
    private volatile boolean enabled = true;
    private volatile boolean warnedClip = false;
    private final PIDFCoefficients velocityPidf = new PIDFCoefficients(10, 3, 0, 0);
    private final PIDFCoefficients positionPidf = new PIDFCoefficients(10, 0, 0, 0);

    SimMotor(SimHardware hw, SimHardware.Spec spec) {
        this.hw = hw;
        this.name = spec.name;
        this.ticksPerRev = spec.ticksPerRev;
        this.maxTicksPerSec = spec.ticksPerRev * spec.maxRpm / 60.0;
    }

    private double sign() { return direction == Direction.REVERSE ? -1 : 1; }

    private double rawPos() {
        double[] e = hw.encoders.get(name);
        return e == null ? 0 : e[0];
    }

    private double rawVel() {
        double[] e = hw.encoders.get(name);
        return e == null ? 0 : e[1];
    }

    // ---- simulator side
    double simRawOutput() {
        if (!enabled || mode == RunMode.STOP_AND_RESET_ENCODER) return 0;
        if (mode == RunMode.RUN_TO_POSITION && targetPosition != null) {
            double err = targetPosition - getCurrentPosition();
            double limit = Math.abs(power);
            double cmd = Math.max(-limit, Math.min(limit, err / 120.0));
            if (Math.abs(err) <= tolerance) cmd = 0;
            return sign() * cmd;
        }
        return sign() * power;
    }

    boolean simBrake() { return zeroPowerBehavior != ZeroPowerBehavior.FLOAT; }

    void simZero() { power = 0; }

    // ---- DcMotorSimple
    @Override public void setDirection(Direction direction) { this.direction = direction; }
    @Override public Direction getDirection() { return direction; }

    @Override public void setPower(double p) {
        if (Double.isNaN(p)) {
            throw new IllegalArgumentException("setPower(NaN) on motor \"" + name + "\" - check for a divide by zero");
        }
        if (Math.abs(p) > 1.0 && !warnedClip) {
            warnedClip = true;
            hw.log("Motor \"" + name + "\": setPower(" + p + ") is outside -1..1; the SDK clips it.");
        }
        power = Math.max(-1, Math.min(1, p));
    }

    @Override public double getPower() { return power; }

    // ---- DcMotor
    @Override public int getPortNumber() { return 0; }
    @Override public void setZeroPowerBehavior(ZeroPowerBehavior z) { zeroPowerBehavior = z; }
    @Override public ZeroPowerBehavior getZeroPowerBehavior() { return zeroPowerBehavior; }
    @Override @Deprecated public void setPowerFloat() { zeroPowerBehavior = ZeroPowerBehavior.FLOAT; power = 0; }
    @Override public boolean getPowerFloat() { return zeroPowerBehavior == ZeroPowerBehavior.FLOAT && power == 0; }

    @Override public void setTargetPosition(int position) { targetPosition = position; }
    @Override public int getTargetPosition() { return targetPosition == null ? 0 : targetPosition; }

    @Override public boolean isBusy() {
        return mode == RunMode.RUN_TO_POSITION && targetPosition != null
                && Math.abs(targetPosition - getCurrentPosition()) > tolerance;
    }

    @Override public int getCurrentPosition() { return (int) Math.round(sign() * (rawPos() - encoderOffset)); }

    @Override public void setMode(RunMode m) {
        m = m.migrate();
        if (m == RunMode.RUN_TO_POSITION && targetPosition == null) {
            throw new IllegalStateException("Failed to enable motor \"" + name
                    + "\". You must set a target position before switching to RUN_TO_POSITION mode");
        }
        if (m == RunMode.STOP_AND_RESET_ENCODER) {
            encoderOffset = rawPos();
            power = 0;
        }
        mode = m;
    }

    @Override public RunMode getMode() { return mode; }

    // ---- DcMotorEx
    @Override public void setMotorEnable() { enabled = true; }
    @Override public void setMotorDisable() { enabled = false; }
    @Override public boolean isMotorEnabled() { return enabled; }

    @Override public void setVelocity(double ticksPerSecond) {
        if (mode == RunMode.RUN_WITHOUT_ENCODER) {
            hw.log("Motor \"" + name + "\": setVelocity() in RUN_WITHOUT_ENCODER mode - on a real robot use RUN_USING_ENCODER.");
        }
        power = Math.max(-1, Math.min(1, ticksPerSecond / maxTicksPerSec));
    }

    @Override public void setVelocity(double angularRate, AngleUnit unit) {
        setVelocity(unit.toRadians(angularRate) / (2 * Math.PI) * ticksPerRev);
    }

    @Override public double getVelocity() { return sign() * rawVel(); }

    @Override public double getVelocity(AngleUnit unit) {
        return unit.fromRadians(getVelocity() / ticksPerRev * 2 * Math.PI);
    }

    @Override public void setPIDFCoefficients(RunMode mode, PIDFCoefficients c) {
        PIDFCoefficients t = mode == RunMode.RUN_TO_POSITION ? positionPidf : velocityPidf;
        t.p = c.p; t.i = c.i; t.d = c.d; t.f = c.f;
    }

    @Override public PIDFCoefficients getPIDFCoefficients(RunMode mode) {
        PIDFCoefficients t = mode == RunMode.RUN_TO_POSITION ? positionPidf : velocityPidf;
        return new PIDFCoefficients(t.p, t.i, t.d, t.f);
    }

    @Override public void setVelocityPIDFCoefficients(double p, double i, double d, double f) {
        velocityPidf.p = p; velocityPidf.i = i; velocityPidf.d = d; velocityPidf.f = f;
    }

    @Override public void setPositionPIDFCoefficients(double p) { positionPidf.p = p; }
    @Override public void setTargetPositionTolerance(int t) { tolerance = t; }
    @Override public int getTargetPositionTolerance() { return tolerance; }

    @Override public double getCurrent(CurrentUnit unit) {
        double amps = Math.abs(simRawOutput()) * 2.0 + Math.abs(rawVel()) / Math.max(1, maxTicksPerSec) * 0.5;
        return unit.convert(amps, CurrentUnit.AMPS);
    }

    @Override public double getCurrentAlert(CurrentUnit unit) { return unit.convert(7.5, CurrentUnit.AMPS); }
    @Override public void setCurrentAlert(double current, CurrentUnit unit) { }
    @Override public boolean isOverCurrent() { return false; }

    @Override public String getDeviceName() { return "Simulated motor " + name; }
}
