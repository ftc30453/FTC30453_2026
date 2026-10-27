package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles;

/*
 * Checks the drive encoders and calibrates the autos' ticksPerInch and strafeEfficiency.
 * The motors stay off unless a D-pad button is held.
 *
 * Press A to zero, then:
 *   Hold D-pad UP   - the robot drives slowly the way the autos drive "forward".
 *                     All four encoders must have the same sign, and Forward must say FRONT.
 *   Hold D-pad LEFT - the robot turns in place.
 *                     FL and BL must have one sign, FR and BR the other, and Strafe must stay near 0.
 *                     IMU Yaw must go UP (the robot turns left / counter-clockwise).
 *                     If Yaw barely moves but Pitch or Roll does, the hub orientation below is wrong.
 *
 * Finding the hub orientation:
 *   Press X to try the next USB direction (the logo stays RIGHT). Press A, then turn the robot
 *   by hand a quarter turn to the LEFT (counter-clockwise seen from above). The right setting
 *   reads Yaw about +90 while Pitch and Roll stay near 0. Put that USB direction in AutoBase.
 *
 * Calibrating (motors off, push by hand):
 *   Push straight forward exactly one tile (24 in): "ticksPerInch should be" goes in AutoBase.
 *   Press A, push straight sideways one tile: "strafeEfficiency should be" goes in AutoBase.
 */
@TeleOp(name = "Odometry Test", group = "Test")
public class OdometryTest extends LinearOpMode {

    private static final double TILE_INCHES = 24;

    // Must match AutoBase
    private static final double TICKS_PER_INCH = 41.8;
    private static final boolean BACK_ENCODERS_SWAPPED = true;

    private DcMotor lf, rf, lr, rr;

    private int frontLeftTicks() {
        return lf.getCurrentPosition();
    }

    private int frontRightTicks() {
        return rf.getCurrentPosition();
    }

    private int backLeftTicks() {
        return BACK_ENCODERS_SWAPPED ? -rr.getCurrentPosition() : lr.getCurrentPosition();
    }

    private int backRightTicks() {
        return BACK_ENCODERS_SWAPPED ? -lr.getCurrentPosition() : rr.getCurrentPosition();
    }

    @Override
    public void runOpMode() {

        lf = hardwareMap.get(DcMotor.class, "front_left");
        rf = hardwareMap.get(DcMotor.class, "front_right");
        lr = hardwareMap.get(DcMotor.class, "back_left");
        rr = hardwareMap.get(DcMotor.class, "back_right");

        // Same directions as AutoBase
        rf.setDirection(DcMotor.Direction.REVERSE);
        rr.setDirection(DcMotor.Direction.REVERSE);

        // Starts with the same hub orientation as AutoBase; X tries the other USB directions
        RevHubOrientationOnRobot.UsbFacingDirection[] usbChoices = {
                RevHubOrientationOnRobot.UsbFacingDirection.UP,
                RevHubOrientationOnRobot.UsbFacingDirection.DOWN,
                RevHubOrientationOnRobot.UsbFacingDirection.FORWARD,
                RevHubOrientationOnRobot.UsbFacingDirection.BACKWARD
        };
        int usbChoice = 0;
        boolean xWasPressed = false;
        IMU imu = hardwareMap.get(IMU.class, "imu");
        imu.initialize(new IMU.Parameters(
                new RevHubOrientationOnRobot(
                        RevHubOrientationOnRobot.LogoFacingDirection.RIGHT,
                        usbChoices[usbChoice]
                )
        ));
        imu.resetYaw();

        DcMotor[] motors = {lf, rf, lr, rr};
        for (DcMotor m : motors) {
            // FLOAT so the robot is easy to push
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            m.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        telemetry.  addLine("Press START, then A to zero.");
        telemetry.update();

        waitForStart();

        int zeroLf = frontLeftTicks();
        int zeroRf = frontRightTicks();
        int zeroLr = backLeftTicks();
        int zeroRr = backRightTicks();

        // Raw readings, for comparing the two possible back-encoder fixes
        int rawZeroLr = lr.getCurrentPosition();
        int rawZeroRr = rr.getCurrentPosition();

        // Total turn since A, added up so it doesn't wrap around at +-180 like Yaw does
        double totalTurn = 0;
        double lastYaw = 0;

        while (opModeIsActive()) {

            // D-pad UP: the autos' forward (negative motor power, because of DRIVE_REVERSED).
            // D-pad LEFT: turn in place.
            double left = 0;
            double right = 0;
            if (gamepad1.dpad_up) {
                left = -0.25;
                right = -0.25;
            } else if (gamepad1.dpad_left) {
                left = -0.25;
                right = 0.25;
            }
            lf.setPower(left);
            lr.setPower(left);
            rf.setPower(right);
            rr.setPower(right);

            if (gamepad1.x && !xWasPressed) {
                usbChoice = (usbChoice + 1) % usbChoices.length;
                imu.initialize(new IMU.Parameters(
                        new RevHubOrientationOnRobot(
                                RevHubOrientationOnRobot.LogoFacingDirection.RIGHT,
                                usbChoices[usbChoice]
                        )
                ));
                imu.resetYaw();
                totalTurn = 0;
                lastYaw = 0;
            }
            xWasPressed = gamepad1.x;

            if (gamepad1.a) {
                imu.resetYaw();
                zeroLf = frontLeftTicks();
                zeroRf = frontRightTicks();
                zeroLr = backLeftTicks();
                zeroRr = backRightTicks();
                rawZeroLr = lr.getCurrentPosition();
                rawZeroRr = rr.getCurrentPosition();
                totalTurn = 0;
                lastYaw = 0;
            }

            int dLf = frontLeftTicks() - zeroLf;
            int dRf = frontRightTicks() - zeroRf;
            int dLr = backLeftTicks() - zeroLr;
            int dRr = backRightTicks() - zeroRr;

            double forwardTicks = (dLf + dRf + dLr + dRr) / 4.0;
            double strafeTicks = (dLf - dRf - dLr + dRr) / 4.0;

            // Same as the autos' DRIVE_REVERSED: the robot's real front is the motors' back
            double realForwardIn = -forwardTicks / TICKS_PER_INCH;

            telemetry.addLine("A = zero   hold D-pad UP = drive forward   hold D-pad LEFT = turn");
            telemetry.addData("Encoders FL FR BL BR", "%d %d %d %d", dLf, dRf, dLr, dRr);
            telemetry.addLine();
            telemetry.addData("Forward (in)", "%.1f  %s", realForwardIn,
                    realForwardIn > 1 ? "(toward the FRONT)" : realForwardIn < -1 ? "(toward the BACK)" : "");
            telemetry.addData("Strafe (in, before efficiency)", "%.1f", Math.abs(strafeTicks) / TICKS_PER_INCH);
            YawPitchRollAngles angles = imu.getRobotYawPitchRollAngles();
            telemetry.addData("IMU Yaw Pitch Roll", "%.0f  %.0f  %.0f",
                    angles.getYaw(AngleUnit.DEGREES), angles.getPitch(AngleUnit.DEGREES), angles.getRoll(AngleUnit.DEGREES));
            telemetry.addData("Hub setting (X = next)", "logo RIGHT, USB %s", usbChoices[usbChoice]);

            double yaw = angles.getYaw(AngleUnit.DEGREES);
            double yawStep = yaw - lastYaw;
            if (yawStep > 180) yawStep -= 360;
            if (yawStep < -180) yawStep += 360;
            totalTurn += yawStep;
            lastYaw = yaw;
            // D-pad LEFT must make this POSITIVE. If it goes negative, set TURN_REVERSED = true in AutoBase.
            telemetry.addData("Total turn since A (D-pad LEFT must be +)", "%.0f  %s", totalTurn,
                    totalTurn > 10 ? "OK" : totalTurn < -10 ? "-> set TURN_REVERSED = true in AutoBase" : "");

            // The two fixes that both pass the D-pad UP check. Only the right one keeps
            // Strafe near 0 while the robot turns (D-pad LEFT).
            int rawLr = lr.getCurrentPosition() - rawZeroLr;
            int rawRr = rr.getCurrentPosition() - rawZeroRr;
            double strafeIfReversed = (dLf - dRf - (-rawLr) + (-rawRr)) / 4.0 / TICKS_PER_INCH;
            double strafeIfSwapped = (dLf - dRf - (-rawRr) + (-rawLr)) / 4.0 / TICKS_PER_INCH;
            telemetry.addLine();
            telemetry.addLine("Turn check (A, then hold D-pad LEFT): which Strafe stays near 0?");
            telemetry.addData("  back encoders REVERSED -> Strafe", "%.1f", Math.abs(strafeIfReversed));
            telemetry.addData("  back encoders SWAPPED  -> Strafe", "%.1f", Math.abs(strafeIfSwapped));
            telemetry.addLine();
            telemetry.addLine("After pushing exactly one tile:");
            telemetry.addData("  forward -> ticksPerInch should be", "%.1f", Math.abs(forwardTicks) / TILE_INCHES);
            if (Math.abs(strafeTicks) > 1) {
                telemetry.addData("  sideways -> strafeEfficiency should be", "%.2f",
                        TILE_INCHES / (Math.abs(strafeTicks) / TICKS_PER_INCH));
            }
            telemetry.update();

            idle();
        }
    }
}
