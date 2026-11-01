package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

/*
 * Two-driver version of Manual Control.
 *
 * gamepad1 (driver): left stick drive, right stick X turn, D-pad slow move, Y reset heading,
 *                    hold A aim at the hive
 * gamepad2 (operator): right trigger shoot, left trigger shooter reverse, X feed, B feed reverse,
 *                      right bumper intake, left bumper intake reverse
 */
@TeleOp(name = "Two Player Control", group = "Linear Opmode")
public class TwoPlayerTeleop extends LinearOpMode {

    @Override
    public void runOpMode() {
        driver driver = new driver(hardwareMap, telemetry);
        intaker intaker = new intaker(hardwareMap, telemetry);
        shooter shooter = new shooter(hardwareMap, telemetry);

        ElapsedTime runtime = new ElapsedTime();

        telemetry.addData("Status", "Initialized");
        telemetry.update();

        waitForStart();
        runtime.reset();

        while (opModeIsActive()) {

            driver.drive(gamepad1);
            shooter.shoot(gamepad2);
            intaker.intake(gamepad2);

            telemetry.addData("Status", "Run Time: " + runtime);
            telemetry.update();
        }
    }
}
