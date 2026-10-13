package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

@Autonomous(name = "AutoOp1", group = "Linear OpMode")
public class AutoOp1 extends LinearOpMode {

    private DcMotor lf, rf, lr, rr;
    private DcMotor shootmotor, intakemotor;
    private IMU imu;

    // Robot pose (in inches and degrees)
    private double x = 0;
    private double y = 0;
    private double heading = 0;

    // Previous encoder readings
    private double prevLf = 0;
    private double prevRf = 0;
    private double prevLr = 0;
    private double prevRr = 0;

    // Example value for Gobilda 537.7 + ~96mm wheel
    private final double ticksPerInch = 41.8;

    @Override
    public void runOpMode() {

        lf = hardwareMap.get(DcMotor.class, "front_left");
        rf = hardwareMap.get(DcMotor.class, "front_right");
        lr = hardwareMap.get(DcMotor.class, "back_left");
        rr = hardwareMap.get(DcMotor.class, "back_right");

        shootmotor = hardwareMap.get(DcMotor.class, "shooter");
        intakemotor = hardwareMap.get(DcMotor.class, "intake_motor");

        imu = hardwareMap.get(IMU.class, "imu");

        IMU.Parameters parameters = new IMU.Parameters(
                new RevHubOrientationOnRobot(
                        RevHubOrientationOnRobot.LogoFacingDirection.UP,
                        RevHubOrientationOnRobot.UsbFacingDirection.BACKWARD
                )
        );

        imu.initialize(parameters);

        lf.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rf.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lr.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rr.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        lf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        lr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        waitForStart();

        if (opModeIsActive()) {

            goToPosition(4, 5, 180);

            shoot(5000);

            goToPosition(6, 6, 0);

            intake(3000);

            goToPosition(4, 5, 180);

            shoot(5000);

            goToPosition(6, 2, 180);
        }
    }

    public void updatePose() {

        heading = imu.getRobotYawPitchRollAngles()
                .getYaw(AngleUnit.DEGREES);

        double lfPos = lf.getCurrentPosition();
        double rfPos = rf.getCurrentPosition();
        double lrPos = lr.getCurrentPosition();
        double rrPos = rr.getCurrentPosition();

        double dLf = lfPos - prevLf;
        double dRf = rfPos - prevRf;
        double dLr = lrPos - prevLr;
        double dRr = rrPos - prevRr;

        prevLf = lfPos;
        prevRf = rfPos;
        prevLr = lrPos;
        prevRr = rrPos;

        double forwardTicks =
                (dLf + dRf + dLr + dRr) / 4.0;

        double strafeTicks =
                (dLf - dRf - dLr + dRr) / 4.0;

        double forward = forwardTicks / ticksPerInch;
        double strafe = strafeTicks / ticksPerInch;

        double headingRad = Math.toRadians(heading);

        x += strafe * Math.cos(headingRad)
                - forward * Math.sin(headingRad);

        y += strafe * Math.sin(headingRad)
                + forward * Math.cos(headingRad);
    }

    public void goToPosition(
            double targetX,
            double targetY,
            double targetHeading) {

        double kP = 0.05;
        double kH = 0.01;

        long startTime = System.currentTimeMillis();

        while (opModeIsActive()) {

            updatePose();

            double dx = targetX - x;
            double dy = targetY - y;

            double distance = Math.hypot(dx, dy);

            if (distance < 1.0) {
                break;
            }

            if (System.currentTimeMillis() - startTime > 5000) {
                break;
            }

            double targetAngle =
                    Math.atan2(dy, dx);

            double headingRad =
                    Math.toRadians(heading);

            double angleError =
                    targetAngle - headingRad;

            double forward =
                    Math.cos(angleError) * distance * kP;

            double strafe =
                    Math.sin(angleError) * distance * kP;

            double headingError =
                    targetHeading - heading;

            while (headingError > 180)
                headingError -= 360;

            while (headingError < -180)
                headingError += 360;

            double turn = headingError * kH;

            mecanumDrive(forward, strafe, turn);

            telemetry.addData("X", x);
            telemetry.addData("Y", y);
            telemetry.addData("Heading", heading);
            telemetry.addData("Distance", distance);
            telemetry.update();

            idle();
        }

        mecanumDrive(0, 0, 0);
    }

    public void shoot(long timeMs) {
        shootmotor.setPower(1);
        sleep(timeMs);
        shootmotor.setPower(0);
    }

    public void intake(long timeMs) {
        intakemotor.setPower(1);
        sleep(timeMs);
        intakemotor.setPower(0);
    }

    public void mecanumDrive(
            double forward,
            double strafe,
            double turn) {

        double lfPower = forward + strafe + turn;
        double rfPower = forward - strafe - turn;
        double lrPower = forward - strafe + turn;
        double rrPower = forward + strafe - turn;

        double max =
                Math.max(
                        Math.max(Math.abs(lfPower), Math.abs(rfPower)),
                        Math.max(Math.abs(lrPower), Math.abs(rrPower))
                );

        if (max > 1.0) {
            lfPower /= max;
            rfPower /= max;
            lrPower /= max;
            rrPower /= max;
        }

        lf.setPower(lfPower);
        rf.setPower(rfPower);
        lr.setPower(lrPower);
        rr.setPower(rrPower);
    }
}