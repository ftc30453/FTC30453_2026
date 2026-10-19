package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class driver {

    private final Telemetry telemetry;

    private final DcMotorEx frontLeft;
    private final DcMotorEx frontRight;
    private final DcMotorEx backLeft;
    private final DcMotorEx backRight;

    private final IMU imu;

    private final ElapsedTime loopTimer = new ElapsedTime();
    private double lastLoopTimeMs = 0;

    // Change if using a different motor
    private static final double TICKS_PER_REV = 537.7;

    public driver(HardwareMap hardwareMap, Telemetry telemetry) {
        this.telemetry = telemetry;

        frontLeft = hardwareMap.get(DcMotorEx.class, "front_left");
        frontRight = hardwareMap.get(DcMotorEx.class, "front_right");
        backLeft = hardwareMap.get(DcMotorEx.class, "back_left");
        backRight = hardwareMap.get(DcMotorEx.class, "back_right");

        frontRight.setDirection(DcMotor.Direction.REVERSE);
        backRight.setDirection(DcMotor.Direction.REVERSE);

        frontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        imu = hardwareMap.get(IMU.class, "imu");

        IMU.Parameters parameters = new IMU.Parameters(
                new RevHubOrientationOnRobot(
                        RevHubOrientationOnRobot.LogoFacingDirection.RIGHT,
                        RevHubOrientationOnRobot.UsbFacingDirection.UP
                )
        );

        imu.initialize(parameters);

        loopTimer.reset();
    }

    public void drive(Gamepad gamepad) {

        lastLoopTimeMs = loopTimer.milliseconds();
        loopTimer.reset();

        if (gamepad.y) {
            imu.resetYaw();
        }

        double y;
        double x;
        double rx;

        if (gamepad.dpad_up) {

            y = 0.5;
            x = 0;
            rx = 0;

        } else if (gamepad.dpad_down) {

            y = -0.5;
            x = 0;
            rx = 0;

        } else if (gamepad.dpad_left) {

            y = 0;
            x = -0.5;
            rx = 0;

        } else if (gamepad.dpad_right) {

            y = 0;
            x = 0.5;
            rx = 0;

        } else {

            y = gamepad.left_stick_y;
            x = -gamepad.left_stick_x;
            rx = gamepad.right_stick_x;
        }

        double heading =
                imu.getRobotYawPitchRollAngles()
                        .getYaw(AngleUnit.RADIANS);

        double rotX =
                x * Math.cos(-heading)
                        - y * Math.sin(-heading);

        double rotY =
                x * Math.sin(-heading)
                        + y * Math.cos(-heading);

        double frontLeftPower = rotY + rotX + rx;
        double backLeftPower = rotY - rotX + rx;
        double frontRightPower = rotY - rotX - rx;
        double backRightPower = rotY + rotX - rx;

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

        frontLeft.setPower(frontLeftPower);
        frontRight.setPower(frontRightPower);
        backLeft.setPower(backLeftPower);
        backRight.setPower(backRightPower);

        double flRPM = frontLeft.getVelocity() * 60.0 / TICKS_PER_REV;
        double frRPM = frontRight.getVelocity() * 60.0 / TICKS_PER_REV;
        double blRPM = backLeft.getVelocity() * 60.0 / TICKS_PER_REV;
        double brRPM = backRight.getVelocity() * 60.0 / TICKS_PER_REV;

        double avgRPM =
                (Math.abs(flRPM)
                        + Math.abs(frRPM)
                        + Math.abs(blRPM)
                        + Math.abs(brRPM))
                        / 4.0;

        telemetry.addData("Heading (deg)",
                imu.getRobotYawPitchRollAngles()
                        .getYaw(AngleUnit.DEGREES));

        telemetry.addData("Loop Time (ms)", lastLoopTimeMs);

        if (lastLoopTimeMs > 0) {
            telemetry.addData("Loop Rate (Hz)", 1000.0 / lastLoopTimeMs);
        }

        telemetry.addData("FL RPM", "%.1f", flRPM);
        telemetry.addData("FR RPM", "%.1f", frRPM);
        telemetry.addData("BL RPM", "%.1f", blRPM);
        telemetry.addData("BR RPM", "%.1f", brRPM);
        telemetry.addData("Average RPM", "%.1f", avgRPM);

        telemetry.addData("Front Left", frontLeftPower);
        telemetry.addData("Front Right", frontRightPower);
        telemetry.addData("Back Left", backLeftPower);
        telemetry.addData("Back Right", backRightPower);

        telemetry.addData("Joystick Y", y);
        telemetry.addData("Joystick X", x);
        telemetry.addData("Turn", rx);

        telemetry.addData("rotated Y", rotY);
        telemetry.addData("rotated X", rotX);
    }
}