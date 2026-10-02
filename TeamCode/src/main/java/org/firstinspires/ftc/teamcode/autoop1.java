package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.DcMotor;

public class autoop1 {

    DcMotor lf, rf, lr, rr, shootmoter, intakemoter;
    // Robot pose
    public double x = 0;
    public double y = 0;
    public double heading = 0;

    // Tune this for your robot
    double ticksPerInch = 45*24;

    public autoop1(DcMotor lf, DcMotor rf, DcMotor lr, DcMotor rr) {
        this.lf = lf;
        this.rf = rf;
        this.lr = lr;
        this.rr = rr;
        lf.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rf.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lr.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rr.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shootmoter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        intakemoter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        lr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        shootmoter.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        intakemoter.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        goToPosition(4, 5, 180, true);
        shoot(5000);
        goToPosition(6, 6, 0, true);
        intake(3000);
        intakemoter.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        goToPosition(4, 5, 180, true);
        shoot(5000);
        goToPosition(6, 2, 180, true);
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

    public void goToPosition(double targetX, double targetY, double targetHeading, boolean opMode) {

        double kP = 0.02;
        double kH = 0.01;

        while (opMode) {

            updatePose();

            double dx = targetX - x;
            double dy = targetY - y;

            double distance = Math.hypot(dx, dy);
            if (distance < 1.0) break;

            double angleToTarget = Math.atan2(dy, dx);
            double angleError = angleToTarget - heading;

            double forward = Math.cos(angleError) * distance * kP;
            double strafe = Math.sin(angleError) * distance * kP;

            double turn = (targetHeading - heading) * kH;

            mecanumDrive(forward, strafe, turn);
        }

        mecanumDrive(0, 0, 0);
    }
    public void shoot(double time){
        shootmoter.setPower(1);
        sleep(time);
        shootmoter.setPower(0);
    }
    public void intake(double time){
        intakemoter.setPower(1);
        sleep(time);
        intakemoter.setPower(0);
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
