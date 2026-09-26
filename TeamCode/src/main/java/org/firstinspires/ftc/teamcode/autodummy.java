package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@Autonomous(name="Basic Auto Example", group="Examples")
public class autodummy extends LinearOpMode {

    DcMotor leftFront, rightFront, leftRear, rightRear;

    @Override
    public void runOpMode() throws InterruptedException {

        leftFront  = hardwareMap.get(DcMotor.class, "leftFront");
        rightFront = hardwareMap.get(DcMotor.class, "rightFront");
        leftRear   = hardwareMap.get(DcMotor.class, "leftRear");
        rightRear  = hardwareMap.get(DcMotor.class, "rightRear");

        // Reverse motors so robot drives forward correctly
        rightFront.setDirection(DcMotor.Direction.REVERSE);
        rightRear.setDirection(DcMotor.Direction.REVERSE);

        // Use encoders
        leftFront.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightFront.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        leftRear.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rightRear.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        leftFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightFront.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        leftRear.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        rightRear.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        waitForStart();

        if (opModeIsActive()) {
            driveForward(1000, 0.5);   // drive forward 1000 ticks
            strafeRight(800, 0.5);     // strafe right
            turnLeft(600, 0.5);        // turn left
        }
    }

    public void driveForward(int ticks, double power) {
        setTargetPosition(ticks, ticks, ticks, ticks);
        runToPosition(power);
    }

    public void strafeRight(int ticks, double power) {
        setTargetPosition(ticks, -ticks, -ticks, ticks);
        runToPosition(power);
    }

    public void turnLeft(int ticks, double power) {
        setTargetPosition(-ticks, ticks, -ticks, ticks);
        runToPosition(power);
    }

    private void setTargetPosition(int lf, int rf, int lr, int rr) {
        leftFront.setTargetPosition(leftFront.getCurrentPosition() + lf);
        rightFront.setTargetPosition(rightFront.getCurrentPosition() + rf);
        leftRear.setTargetPosition(leftRear.getCurrentPosition() + lr);
        rightRear.setTargetPosition(rightRear.getCurrentPosition() + rr);

        leftFront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightFront.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        leftRear.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        rightRear.setMode(DcMotor.RunMode.RUN_TO_POSITION);
    }

    private void runToPosition(double power) {
        leftFront.setPower(power);
        rightFront.setPower(power);
        leftRear.setPower(power);
        rightRear.setPower(power);

        while (opModeIsActive() &&
                leftFront.isBusy() && rightFront.isBusy() &&
                leftRear.isBusy() && rightRear.isBusy()) {
            telemetry.addData("LF", leftFront.getCurrentPosition());
            telemetry.update();
        }

        stopAll();
    }

    private void stopAll() {
        leftFront.setPower(0);
        rightFront.setPower(0);
        leftRear.setPower(0);
        rightRear.setPower(0);
    }
}
