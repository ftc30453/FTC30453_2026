package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * This file is a basic starter for a Manual Control (TeleOp) OpMode.
 * It uses a "Linear" style which is easy for beginners to follow.
 */

@TeleOp(name="Spin 2 Motors", group="Linear Opmode")
public class Spin2Motors extends LinearOpMode {

    @Override
    public void runOpMode() {
        // Declare motor variables locally
        ElapsedTime runtime = new ElapsedTime();
        DcMotor leftDrive;
        DcMotor rightDrive;

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        // Initialize hardware. The strings ("left_drive", etc.) must match 
        // the names you set on the Robot Controller phone/Control Hub configuration.
        leftDrive  = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");

        // Most robots need one motor reversed to drive forward
        leftDrive.setDirection(DcMotor.Direction.FORWARD);
        rightDrive.setDirection(DcMotor.Direction.REVERSE);

        // Wait for the game to start (driver presses PLAY)
        waitForStart();
        runtime.reset();

        // Run until the end of the match (driver presses STOP)
        while (opModeIsActive()) {

            // POV Mode: Left stick moves forward/back, Right stick turns left/right.
            double drive = -gamepad1.left_stick_y;
            double turn  =  gamepad1.right_stick_x;
            
            double leftPower  = drive + turn;
            double rightPower = drive - turn;

            // Send calculated power to motors
            leftDrive.setPower(leftPower);
            rightDrive.setPower(rightPower);

            // Show data on the Driver Station phone
            telemetry.addData("Status", "Run Time: " + runtime);
            telemetry.addData("Motors", "Left (%.2f), Right (%.2f)", leftPower, rightPower);
            telemetry.update();
        }
    }
}
