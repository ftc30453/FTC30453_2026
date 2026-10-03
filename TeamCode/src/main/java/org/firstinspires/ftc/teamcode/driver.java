package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class driver {

    // Mecanum drive motors
    private final DcMotor frontLeft;
    private final DcMotor frontRight;
    private final DcMotor backLeft;
    private final DcMotor backRight;

    // IMU for field-centric driving
    private final IMU imu;

    public driver(HardwareMap hardwareMap) {

        // Hardware mapping
        frontLeft = hardwareMap.get(DcMotor.class, "front_left");
        frontRight = hardwareMap.get(DcMotor.class, "front_right");
        backLeft = hardwareMap.get(DcMotor.class, "back_left");
        backRight = hardwareMap.get(DcMotor.class, "back_right");

        // Reverse right side motors so all wheels move
        // forward when given positive power
        frontRight.setDirection(DcMotor.Direction.REVERSE);
        backRight.setDirection(DcMotor.Direction.REVERSE);

        // Get built-in IMU from the Control Hub
        imu = hardwareMap.get(IMU.class, "imu");

        // Tell the IMU how the Control Hub is mounted
        // Change these if your hub is mounted differently
        IMU.Parameters parameters = new IMU.Parameters(
                new RevHubOrientationOnRobot(
                        RevHubOrientationOnRobot.LogoFacingDirection.UP,
                        RevHubOrientationOnRobot.UsbFacingDirection.FORWARD
                )
        );

        imu.initialize(parameters);
    }

    public void drive(Gamepad gamepad) {

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
            y = 0.5;
            x = 0;
            rx = 0;

        } else if (gamepad.dpad_down) {

            // Move toward driver station
            y = -0.5;
            x = 0;
            rx = 0;

        } else if (gamepad.dpad_left) {

            // Move left on the field
            y = 0;
            x = -0.5;
            rx = 0;

        } else if (gamepad.dpad_right) {

            // Move right on the field
            y = 0;
            x = 0.5;
            rx = 0;

        } else {

            // Normal joystick driving

            // Negative because FTC stick Y is backwards
            y = -gamepad.left_stick_y;

            // Left stick X strafes
            x = gamepad.left_stick_x;

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
        // MECANUM DRIVE EQUATIONS
        //
        // Combines:
        // Forward/Backward
        // Strafing
        // Rotation
        // =====================================

        double frontLeftPower =
                rotY + rotX + rx;

        double backLeftPower =
                rotY - rotX + rx;

        double frontRightPower =
                rotY - rotX - rx;

        double backRightPower =
                rotY + rotX - rx;

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
    }
}