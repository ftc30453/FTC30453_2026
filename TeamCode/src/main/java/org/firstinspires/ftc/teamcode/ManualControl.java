package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;
//DONT touch this
@TeleOp(name="Full Manual Control", group="Linear Opmode")
public class ManualControl extends LinearOpMode {

    private driver driver;
    private intaker intaker;
    private shooter shooter;



    @Override
    public void runOpMode() throws InterruptedException {
        driver = new driver(hardwareMap);
        intaker = new intaker(hardwareMap);
        shooter = new shooter(hardwareMap);


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

            driver.drive(gamepad1);
            shooter.shoot(gamepad1);
            intaker.intake(gamepad1);
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
    }

