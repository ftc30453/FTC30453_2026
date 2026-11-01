package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.ArrayList;
import java.util.List;

/*
 * Finds the hive's AprilTag with the webcam and works out how to turn the robot to face it.
 * Used by the autos (before each shot) and by driver.java (hold A in teleop).
 *
 * The angle comes from where the tag sits in the picture, so this needs no tag library or camera
 * calibration. The SDK's tag library has no BIOBUZZ tags, so the SDK's ftcPose would be null here.
 *
 * Setup:
 *   1. Mount the camera on the shooter end, pointing the way the shooter fires.
 *   2. Add it to the Robot Controller configuration as "Webcam 1".
 *   3. Run "AprilTag Test" to check the camera sees the hive tags and turns the right way.
 * An alliance with no tag IDs below does not aim. No camera in the configuration is fine too:
 * the robot then runs exactly as it does without aiming.
 */
public class TagAimer {

    private static final String WEBCAM_NAME = "Webcam 1";

    // The scoring tags on each hive (tag36h11). An empty list means that alliance doesn't aim.
    // The audience tags (red 34, 37; blue 40, 41) are not on the hives, so they are left out.
    public static final int[] RED_HIVE_TAG_IDS = {30, 31, 32, 33};
    public static final int[] BLUE_HIVE_TAG_IDS = {42, 43, 44, 45};

    // If shots land to one side, the camera isn't lined up with the shooter.
    // Positive = aim further left, negative = aim further right.
    private static final double AIM_OFFSET_DEG = 0;

    private static final double IMAGE_WIDTH = 640;     // the VisionPortal default size
    private static final double FOCAL_PIXELS = 600;    // rough is fine: the aim closes in on 0

    // Set to true if AprilTag Test turns AWAY from the tag while holding A
    // (for example when the camera is mounted upside down).
    private static final boolean AIM_TURN_REVERSED = false;

    // Turn power from the angle to the tag
    private static final double TURN_PER_DEG = 0.02;
    private static final double MAX_TURN = 0.3;
    private static final double MIN_TURN = 0.12;       // enough to overcome friction

    public static final double AIMED_DEG = 1.5;        // this close counts as aimed

    private AprilTagProcessor aprilTag;
    private final boolean hasCamera;

    public TagAimer(HardwareMap hardwareMap) {
        WebcamName webcam = hardwareMap.tryGet(WebcamName.class, WEBCAM_NAME);
        hasCamera = webcam != null;
        if (hasCamera) {
            aprilTag = AprilTagProcessor.easyCreateWithDefaults();
            VisionPortal.easyCreateWithDefaults(webcam, aprilTag);
        }
    }

    public boolean hasCamera() {
        return hasCamera;
    }

    // True when this alliance can aim: a camera, and its hive's tag IDs filled in
    public boolean canAim(boolean red) {
        return hasCamera && (red ? RED_HIVE_TAG_IDS : BLUE_HIVE_TAG_IDS).length > 0;
    }

    public List<AprilTagDetection> visibleTags() {
        return hasCamera ? aprilTag.getDetections() : new ArrayList<AprilTagDetection>();
    }

    // Degrees to turn to face this alliance's hive, positive = turn left: the average of its
    // scoring tags in view. Null when none of them are in view.
    public Double bearingToHive(boolean red) {
        double total = 0;
        int count = 0;
        for (AprilTagDetection tag : visibleTags()) {
            for (int id : red ? RED_HIVE_TAG_IDS : BLUE_HIVE_TAG_IDS) {
                if (tag.id == id) {
                    total += bearingTo(tag);
                    count++;
                }
            }
        }
        return count > 0 ? (Double) (total / count) : null;
    }

    // Teleop doesn't know its alliance, so it aims at whichever hive is closest to center
    public Double bearingToEitherHive() {
        Double blue = bearingToHive(false);
        Double red = bearingToHive(true);
        if (blue == null) {
            return red;
        }
        if (red == null) {
            return blue;
        }
        return Math.abs(blue) < Math.abs(red) ? blue : red;
    }

    public double bearingTo(AprilTagDetection tag) {
        return Math.toDegrees(Math.atan2(IMAGE_WIDTH / 2 - tag.center.x, FOCAL_PIXELS)) + AIM_OFFSET_DEG;
    }

    // Power for mecanum "turn" / driver.java's rx that turns the robot toward the tag.
    // Turning the robot left moves the tag right in the picture wherever the camera is mounted,
    // so this works from either end of the robot.
    public double turnPower(double bearingDeg) {
        if (Math.abs(bearingDeg) < AIMED_DEG) {
            return 0;
        }
        double power = Math.max(MIN_TURN, Math.min(MAX_TURN, Math.abs(bearingDeg) * TURN_PER_DEG));
        // A positive rx turns the robot right in teleop, so turning left takes a negative one.
        // This doesn't use AutoBase.TURN_REVERSED: that one is about the IMU, and this uses no IMU.
        double turnLeft = AIM_TURN_REVERSED ? power : -power;
        return bearingDeg > 0 ? turnLeft : -turnLeft;
    }
}
