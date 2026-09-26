package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.hardware.HardwareMap;
@TeleOp(name="Full Manual Control", group="Linear Opmode")
public class ManualControl extends LinearOpMode {

    private driver driver;
    private intaker intaker;
    private shooter shooter;


    public void runOpMode(HardwareMap hardwareMap) {
        driver = new driver(hardwareMap);
        intaker = new intaker(hardwareMap);



        // Intake Mechanism
        DcMotor intakeMotor;
        CRServo leftIntakeServo;
        CRServo rightIntakeServo;

        ElapsedTime runtime = new ElapsedTime();

        // 1. HARDWARE MAPPING
        // Drive wheels

        // Intake DC Motor (Vertical)
        intakeMotor = hardwareMap.get(DcMotor.class, "intake_motor");
        
        // Intake Servos (Horizontal)
        leftIntakeServo  = hardwareMap.get(CRServo.class, "left_intake");
        rightIntakeServo = hardwareMap.get(CRServo.class, "right_intake");

        // 2. DIRECTIONS

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
            double gamepad1leftstick = -gamepad1.left_stick_y;
            double gamepad1rightstick  =  gamepad1.right_stick_x;
            double gamepadrt = gamepad1.right_trigger;
            double gamepadlt = gamepad1.left_trigger;
            boolean gamepadrb = gamepad1.right_bumper;
            boolean gamepadlb = gamepad1.left_bumper;
            // Gamepad buttons for shooter actions (example using gamepad1)
            boolean aButton = gamepad1.a;        // ‘A’ button
            boolean bButton = gamepad1.b;        // ‘B’ button
            boolean xButton = gamepad1.x;        // ‘X’ button
            boolean yButton = gamepad1.y;        // ‘Y’ button
            boolean dpadUp = gamepad1.dpad_up;
            boolean dpadDown = gamepad1.dpad_down;
            boolean dpadLeft = gamepad1.dpad_left;
            boolean dpadRight = gamepad1.dpad_right;
            driver.drive(gamepadlb, gamepadrb, gamepadrt, gamepadlt, gamepad1leftstick, gamepad1rightstick, aButton, bButton, xButton, yButton, dpadUp, dpadDown, dpadLeft, dpadRight);
            shooter.shoot(gamepadlb, gamepadrb, gamepadrt, gamepadlt, gamepad1leftstick, gamepad1rightstick, aButton, bButton, xButton, yButton, dpadUp, dpadDown, dpadLeft, dpadRight);
            intaker.intake(gamepadlb, gamepadrb, gamepadrt, gamepadlt, gamepad1leftstick, gamepad1rightstick, aButton, bButton, xButton, yButton, dpadUp, dpadDown, dpadLeft, dpadRight);
            // --- INTAKE LOGIC (Triggers) ---
            // RT (Right Trigger) = Swallow (Positive Power)
            // LT (Left Trigger)  = Spit Out (Negative Power)


            // --- DASHBOARD ---
            telemetry.addData("Status", "Run Time: " + runtime);
            telemetry.addData("Drive", "L (%.2f), R (%.2f)");
            telemetry.addData("Intake", "Power (%.2f)");
            telemetry.update();
        }
    }

    @Override
    public void runOpMode() throws InterruptedException {

    }
}
