package org.firstinspires.ftc.teamcode;



import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.Telemetry;
public class driver {
    private final Telemetry telemetry;
    // Mecanum drive motors
    private final DcMotorEx frontLeft;
    private final DcMotorEx frontRight;
    private final DcMotorEx backLeft;
    private final DcMotorEx backRight;
    private static final double TICKS_PER_REV = 537.7;
    // IMU for field-centric driving
    private final IMU imu;
    private long lastLoopTime = System.nanoTime();
    private double loopHz = 0;
    private double loopMs = 0;
    public driver(HardwareMap hardwareMap, Telemetry telemetry) {
        this.telemetry = telemetry;


        // Hardware mapping
        frontLeft = hardwareMap.get(DcMotorEx.class, "front_left");
        frontRight = hardwareMap.get(DcMotorEx.class, "front_right");
        backLeft = hardwareMap.get(DcMotorEx.class, "back_left");
        backRight = hardwareMap.get(DcMotorEx.class, "back_right");
        frontRight.setDirection(DcMotorEx.Direction.REVERSE);
        backRight.setDirection(DcMotorEx.Direction.REVERSE);
        // Reverse right side motors so all wheels move
        // forward when given positive power
        frontLeft.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
        frontRight.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
        backLeft.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);
        backRight.setMode(DcMotorEx.RunMode.RUN_WITHOUT_ENCODER);

        // Get built-in IMU from the Control Hub
        imu = hardwareMap.get(IMU.class, "imu");

        // Tell the IMU how the Control Hub is mounted
        // Change these if your hub is mounted differently
        IMU.Parameters parameters = new IMU.Parameters(
                new RevHubOrientationOnRobot(
                        RevHubOrientationOnRobot.LogoFacingDirection.RIGHT,
                        RevHubOrientationOnRobot.UsbFacingDirection.UP
                )
        );

        imu.initialize(parameters);
    }

    public void drive(Gamepad gamepad) {
        long currentLoopTime = System.nanoTime();

        loopMs = (currentLoopTime - lastLoopTime) / 1_000_000.0;
        loopHz = 1000.0 / loopMs;

        lastLoopTime = currentLoopTime;

        // =====================================
        // IMU RESET
        // Press Y to reset heading.
        // Do this before a match when the robot
        // is facing away from the driver station.
        // =====================================

        if (gamepad.y) {
            imu.resetYaw();
        }

        // =====================================
        // DRIVER INPUT VARIABLES
        // y  = forward/backward
        // x  = strafe left/right
        // rx = rotate
        // =====================================

        double y;
        double x;
        double rx;

        // =====================================
        // DPAD PRECISION MODE
        // Field-centric movement at 50% speed
        // Overrides joystick controls
        // =====================================

        if (gamepad.dpad_up) {

            // Move away from driver station
            y = -0.5;
            x = 0;
            rx = 0;

        } else if (gamepad.dpad_down) {

            // Move toward driver station
            y = 0.5;
            x = 0;
            rx = 0;

        } else if (gamepad.dpad_left) {

            // Move left on the field
            y = 0;
            x = 0.5;
            rx = 0;

        } else if (gamepad.dpad_right) {

            // Move right on the field
            y = 0;
            x = -0.5;
            rx = 0;

        } else {

            // Normal joystick driving

            // Negative because FTC stick Y is backwards
            y = gamepad.left_stick_y;
            x = -gamepad.left_stick_x;


            // Right stick X rotates
            rx = gamepad.right_stick_x;
        }

        // =====================================
        // GET ROBOT HEADING FROM IMU
        // Heading is measured in radians
        // =====================================

        double heading =
                imu.getRobotYawPitchRollAngles()
                        .getYaw(AngleUnit.RADIANS);

        telemetry.addData("Heading", heading);
        // =====================================
        // FIELD-CENTRIC TRANSFORMATION
        //
        // Converts joystick direction from
        // field coordinates to robot coordinates.
        //
        // Example:
        // Robot facing right:
        // Stick forward still moves away from
        // driver station.
        // =====================================

        double rotX =
                x * Math.cos(-heading)
                        - y * Math.sin(-heading);

        double rotY =
                x * Math.sin(-heading)
                        + y * Math.cos(-heading);

        // =====================================
        // Mecanum DRIVE EQUATIONS
        //
        // Combines:
        // Forward/Backward
        // Strafing
        // Rotation
        // =====================================

        double frontLeftPower  = rotY + rotX + rx;
        double backLeftPower   = rotY - rotX + rx;
        double frontRightPower = rotY - rotX - rx;
        double backRightPower  = rotY + rotX - rx;
        // =====================================
        // NORMALIZATION
        //
        // Keeps motor powers between
        // -1 and +1
        // =====================================

        double max = Math.max(
                Math.max(
                        Math.abs(frontLeftPower),
                        Math.abs(frontRightPower)),
                Math.max(
                        Math.abs(backLeftPower),
                        Math.abs(backRightPower))
        );

        if (max > 1.0) {
            frontLeftPower /= max;
            frontRightPower /= max;
            backLeftPower /= max;
            backRightPower /= max;
        }

        // =====================================
        // SEND POWER TO MOTORS
        // =====================================
        frontLeft.setPower(frontLeftPower);
        frontRight.setPower(frontRightPower);
        backLeft.setPower(backLeftPower);
        backRight.setPower(backRightPower);
        // Motor powers
        telemetry.addData("Front Left Power", frontLeftPower);
        telemetry.addData("Front Right Power", frontRightPower);
        telemetry.addData("Back Left Power", backLeftPower);
        telemetry.addData("Back Right Power", backRightPower);

// Encoders
        telemetry.addData("FL Encoder", frontLeft.getCurrentPosition());
        telemetry.addData("FR Encoder", frontRight.getCurrentPosition());
        telemetry.addData("BL Encoder", backLeft.getCurrentPosition());
        telemetry.addData("BR Encoder", backRight.getCurrentPosition());

// RPM calculations
        double flRPM = (frontLeft.getVelocity() * 60.0) / TICKS_PER_REV;
        double frRPM = (frontRight.getVelocity() * 60.0) / TICKS_PER_REV;
        double blRPM = (backLeft.getVelocity() * 60.0) / TICKS_PER_REV;
        double brRPM = (backRight.getVelocity() * 60.0) / TICKS_PER_REV;

        telemetry.addData("FL RPM", flRPM);
        telemetry.addData("FR RPM", frRPM);
        telemetry.addData("BL RPM", blRPM);
        telemetry.addData("BR RPM", brRPM);

// Driver inputs
        telemetry.addData("Joystick Y", y);
        telemetry.addData("Joystick X", x);
        telemetry.addData("Turn", rx);

// Field-centric diagnostics
        telemetry.addData("Heading (deg)", Math.toDegrees(heading));
        telemetry.addData("RotX", rotX);
        telemetry.addData("RotY", rotY);
        //loop speed read
        telemetry.addData("Loop Time ms", loopMs);
        telemetry.addData("Loop Rate Hz", loopHz);
        telemetry.update();
    }
}