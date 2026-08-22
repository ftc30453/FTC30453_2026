package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

@TeleOp(name="Full Manual Control", group="Linear Opmode")
public class ManualControl extends LinearOpMode {

    @Override
    public void runOpMode() {
        // Drive Motors
        DcMotor leftDrive;
        DcMotor rightDrive;
        
        // Intake Mechanism
        DcMotor intakeMotor;
        CRServo leftIntakeServo;
        CRServo rightIntakeServo;

        ElapsedTime runtime = new ElapsedTime();

        // 1. HARDWARE MAPPING
        // Drive wheels
        leftDrive  = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");
        
        // Intake DC Motor (Vertical)
        intakeMotor = hardwareMap.get(DcMotor.class, "intake_motor");
        
        // Intake Servos (Horizontal)
        leftIntakeServo  = hardwareMap.get(CRServo.class, "left_intake");
        rightIntakeServo = hardwareMap.get(CRServo.class, "right_intake");

        // 2. DIRECTIONS
        leftDrive.setDirection(DcMotor.Direction.FORWARD);
        rightDrive.setDirection(DcMotor.Direction.REVERSE);
        
        // Intake direction: You may need to flip these (REVERSE) depending on your wiring
        intakeMotor.setDirection(DcMotor.Direction.REVERSE);
        leftIntakeServo.setDirection(CRServo.Direction.REVERSE);
        rightIntakeServo.setDirection(CRServo.Direction.FORWARD); // Usually one is reversed to "pull in"

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();
        runtime.reset();

        while (opModeIsActive()) {

            // --- DRIVE LOGIC ---
            double drive = -gamepad1.left_stick_y;
            double turn  =  gamepad1.right_stick_x;
            
            double leftPower  = drive + turn;
            double rightPower = drive - turn;

            leftDrive.setPower(leftPower);
            rightDrive.setPower(rightPower);

            // --- INTAKE LOGIC (Triggers) ---
            // RT (Right Trigger) = Swallow (Positive Power)
            // LT (Left Trigger)  = Spit Out (Negative Power)
            double intakePower = 0;
            
            if (gamepad1.right_trigger > 0.1) {
                intakePower = gamepad1.right_trigger; // Scale power with trigger
            } else if (gamepad1.left_trigger > 0.1) {
                intakePower = -gamepad1.left_trigger; // Reverse power
            }

            // Apply power to all three intake components
            intakeMotor.setPower(intakePower);
            leftIntakeServo.setPower(intakePower);
            rightIntakeServo.setPower(intakePower);

            // --- DASHBOARD ---
            telemetry.addData("Status", "Run Time: " + runtime);
            telemetry.addData("Drive", "L (%.2f), R (%.2f)", leftPower, rightPower);
            telemetry.addData("Intake", "Power (%.2f)", intakePower);
            telemetry.update();
        }
    }
}
