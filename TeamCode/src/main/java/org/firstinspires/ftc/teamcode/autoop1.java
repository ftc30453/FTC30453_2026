package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@Autonomous(name = "AutoOp1", group = "Linear OpMode")
public class autoop1 extends LinearOpMode {

    private DcMotor lf, rf, lr, rr;
    private DcMotor shootmotor, intakemotor;

    // Robot pose
    public double x = 4;
    public double y = 6;
    public double heading = 180;

    double ticksPerInch = 45 * 24;

    @Override
    public void runOpMode() {

        // HardwareMap initialization
        lf = hardwareMap.get(DcMotor.class, "front_left");
        rf = hardwareMap.get(DcMotor.class, "front_right");
        lr = hardwareMap.get(DcMotor.class, "back_left");
        rr = hardwareMap.get(DcMotor.class, "back_right");

        shootmotor = hardwareMap.get(DcMotor.class, "shooter");
        intakemotor = hardwareMap.get(DcMotor.class, "intake_motor");

        // Reset encoders
        lf.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rf.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lr.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rr.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        shootmotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        intakemotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        // Run modes
        lf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        lr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        shootmotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        intakemotor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

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
        double lfPos = lf.getCurrentPosition();
        double rfPos = rf.getCurrentPosition();
        double lrPos = lr.getCurrentPosition();
        double rrPos = rr.getCurrentPosition();

        double forward = (lfPos + rfPos + lrPos + rrPos) / 4.0;
        double strafe = (lfPos - rfPos - lrPos + rrPos) / 4.0;

        x = strafe / ticksPerInch;
        y = forward / ticksPerInch;
    }

    public void goToPosition(double targetX, double targetY, double targetHeading) {

        double kP = 0.02;
        double kH = 0.01;

        while (opModeIsActive()) {

            updatePose();

            double dx = targetX - x;
            double dy = targetY - y;

            double distance = Math.hypot(dx, dy);

            if (distance < 1.0) {
                break;
            }

            double angleToTarget = Math.atan2(dy, dx);

            // heading should be radians for this math
            double angleError = angleToTarget - Math.toRadians(heading);

            double forward = Math.cos(angleError) * distance * kP;
            double strafe = Math.sin(angleError) * distance * kP;

            double turn = (targetHeading - heading) * kH;

            mecanumDrive(forward, strafe, turn);

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

    public void mecanumDrive(double forward, double strafe, double turn) {

        double lfPower = forward + strafe + turn;
        double rfPower = forward - strafe - turn;
        double lrPower = forward - strafe + turn;
        double rrPower = forward + strafe - turn;

        lf.setPower(lfPower);
        rf.setPower(rfPower);
        lr.setPower(lrPower);
        rr.setPower(rrPower);
    }
}