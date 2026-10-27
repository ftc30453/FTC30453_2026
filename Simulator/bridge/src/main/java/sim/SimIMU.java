package sim;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.ImuOrientationOnRobot;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AngularVelocity;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;

/**
 * Simulated IMU. Yaw is counter-clockwise positive and zero where initialize()/resetYaw() was
 * called. If the orientation given to initialize() doesn't match how the hub is mounted
 * (robot.json "hub"), the reported yaw is wrong in the same way it would be on the real robot:
 * reversed, or stuck near zero while the turn shows up as pitch/roll.
 */
final class SimIMU implements IMU {
    private final SimHardware hw;
    private final String name;
    private volatile double yawOffsetDeg = 0;
    private volatile double yawFactor = 1;
    private volatile boolean initialized = false;
    private volatile boolean warnedUninit = false;

    SimIMU(SimHardware hw, String name) {
        this.hw = hw;
        this.name = name;
        yawFactor = factorFor(new RevHubOrientationOnRobot(
                RevHubOrientationOnRobot.LogoFacingDirection.UP, RevHubOrientationOnRobot.UsbFacingDirection.FORWARD));
    }

    /** How much of a robot yaw rotation the SDK reports as yaw, given the declared orientation. */
    private double factorFor(ImuOrientationOnRobot declared) {
        double[][] d = declared.simHubAxesInRobotFrame();
        double[][] p = hw.physicalHubAxes;
        double f = 0;
        for (int k = 0; k < 3; k++) f += d[k][2] * p[k][2];
        return Math.abs(f) < 1e-9 ? 0 : f;
    }

    @Override public boolean initialize(Parameters parameters) {
        ImuOrientationOnRobot o = parameters.imuOrientationOnRobot;
        yawFactor = factorFor(o);
        yawOffsetDeg = hw.headingDeg;
        initialized = true;
        if (yawFactor != 1) {
            String effect = yawFactor == 0
                    ? "yaw will stay near 0 when the robot turns (the turn shows up as pitch/roll instead)"
                    : "yaw will have the wrong sign (turning left reads as turning right)";
            hw.log("IMU \"" + name + "\": the code says the hub is mounted " + o.simDescribe()
                    + ", but robot.json says it is " + hw.physicalHubDesc + ", so " + effect + ".");
        }
        return true;
    }

    @Override public void resetYaw() { yawOffsetDeg = hw.headingDeg; }

    private void checkInit() {
        if (!initialized && !warnedUninit) {
            warnedUninit = true;
            hw.log("IMU \"" + name + "\" was read before initialize() was called.");
        }
    }

    @Override public YawPitchRollAngles getRobotYawPitchRollAngles() {
        checkInit();
        double turned = hw.headingDeg - yawOffsetDeg;
        double yaw = AngleUnit.normalizeDegrees(yawFactor * turned);
        double tilt = yawFactor == 0 ? AngleUnit.normalizeDegrees(turned) : 0;
        return new YawPitchRollAngles(AngleUnit.DEGREES, yaw, tilt, 0, System.nanoTime());
    }

    @Override public AngularVelocity getRobotAngularVelocity(AngleUnit unit) {
        checkInit();
        float z = (float) (yawFactor * hw.angularVelDeg);
        return new AngularVelocity(AngleUnit.DEGREES, 0, 0, z, System.nanoTime()).toAngleUnit(unit);
    }

    @Override public String getDeviceName() { return "Simulated IMU " + name; }
}
