package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import java.util.List;

/*
 * Sets up AprilTag aiming. The motors stay off unless A is held.
 *
 *   Point the camera at a hive: the screen lists each tag's ID. Hive IDs go in TagAimer's
 *   RED_HIVE_TAG_IDS and BLUE_HIVE_TAG_IDS.
 *   Hold A with a tag off to one side: the robot must turn to bring it to the middle, and stop.
 *   If it turns away from the tag, set AIM_TURN_REVERSED = true in TagAimer.
 */
@TeleOp(name = "AprilTag Test", group = "Test")
public class AprilTagTest extends LinearOpMode {

    @Override
    public void runOpMode() {

        DcMotor lf = hardwareMap.get(DcMotor.class, "front_left");
        DcMotor rf = hardwareMap.get(DcMotor.class, "front_right");
        DcMotor lr = hardwareMap.get(DcMotor.class, "back_left");
        DcMotor rr = hardwareMap.get(DcMotor.class, "back_right");

        // Same directions as AutoBase
        rf.setDirection(DcMotor.Direction.REVERSE);
        rr.setDirection(DcMotor.Direction.REVERSE);

        TagAimer aimer = new TagAimer(hardwareMap);

        telemetry.addLine(aimer.hasCamera() ? "Camera found. Press START."
                : "No camera: add \"Webcam 1\" to the Robot Controller configuration.");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {

            List<AprilTagDetection> tags = aimer.visibleTags();
            AprilTagDetection closest = null;

            telemetry.addLine("Hold A = turn toward the tag closest to center");
            telemetry.addData("Camera", aimer.hasCamera() ? "found" : "NOT FOUND");
            telemetry.addData("Tags in view", tags.size());

            for (AprilTagDetection tag : tags) {
                double bearing = aimer.bearingTo(tag);
                telemetry.addData("  Tag ID " + tag.id, "%.1f deg %s", bearing, bearing > 0 ? "left" : "right");
                if (closest == null || Math.abs(bearing) < Math.abs(aimer.bearingTo(closest))) {
                    closest = tag;
                }
            }

            double turn = 0;
            if (gamepad1.a && closest != null) {
                turn = aimer.turnPower(aimer.bearingTo(closest));
            }
            lf.setPower(turn);
            lr.setPower(turn);
            rf.setPower(-turn);
            rr.setPower(-turn);

            telemetry.addData("Turn power", "%.2f", turn);
            telemetry.update();

            idle();
        }
    }
}
