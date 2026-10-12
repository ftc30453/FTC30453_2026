package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
//DON'T touch this
@TeleOp(name=" Manual Control", group="Linear Opmode")
public class mainteleop extends LinearOpMode {

    private driver driver;
    private intaker intaker;
    private shooter shooter;



    @Override
    public void runOpMode() throws InterruptedException {
        driver = new driver(hardwareMap);
        intaker = new intaker(hardwareMap);
        shooter = new shooter(hardwareMap);


        // Intake Mechanism

        ElapsedTime runtime = new ElapsedTime();

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();
        runtime.reset();

        while (opModeIsActive()) {

            driver.drive(gamepad1);
            shooter.shoot(gamepad1);
            intaker.intake(gamepad1);

            // --- DASHBOARD ---
            telemetry.addData("Status", "Run Time: " + runtime);
            telemetry.update();
        }
    }
    }

