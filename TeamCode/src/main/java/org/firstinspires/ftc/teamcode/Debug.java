package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "Gamepad Debug", group = "Debug")
public class Debug extends LinearOpMode {

    @Override
    public void runOpMode() {

        telemetry.addLine("Gamepad Debug Ready");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {

            // Joysticks
            telemetry.addData("Left Stick X", gamepad1.left_stick_x);
            telemetry.addData("Left Stick Y", gamepad1.left_stick_y);
            telemetry.addData("Right Stick X", gamepad1.right_stick_x);
            telemetry.addData("Right Stick Y", gamepad1.right_stick_y);

            // Triggers
            telemetry.addData("Left Trigger", gamepad1.left_trigger);
            telemetry.addData("Right Trigger", gamepad1.right_trigger);

            // Buttons
            telemetry.addData("A", gamepad1.a);
            telemetry.addData("B", gamepad1.b);
            telemetry.addData("X", gamepad1.x);
            telemetry.addData("Y", gamepad1.y);

            // D-Pad
            telemetry.addData("DPad Up", gamepad1.dpad_up);
            telemetry.addData("DPad Down", gamepad1.dpad_down);
            telemetry.addData("DPad Left", gamepad1.dpad_left);
            telemetry.addData("DPad Right", gamepad1.dpad_right);

            // Bumpers
            telemetry.addData("Left Bumper", gamepad1.left_bumper);
            telemetry.addData("Right Bumper", gamepad1.right_bumper);

            // Stick Buttons
            telemetry.addData("Left Stick Button", gamepad1.left_stick_button);
            telemetry.addData("Right Stick Button", gamepad1.right_stick_button);

            telemetry.update();
        }
    }
}