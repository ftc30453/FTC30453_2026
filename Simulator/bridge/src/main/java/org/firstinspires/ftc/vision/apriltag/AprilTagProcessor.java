package org.firstinspires.ftc.vision.apriltag;

import org.firstinspires.ftc.vision.VisionProcessor;

import java.util.ArrayList;

/** Stand-in: the simulator has no cameras, so it never sees a tag. */
public abstract class AprilTagProcessor implements VisionProcessor {
    public static AprilTagProcessor easyCreateWithDefaults() {
        return new AprilTagProcessor() {
            @Override public ArrayList<AprilTagDetection> getDetections() {
                return new ArrayList<>();
            }
        };
    }

    public abstract ArrayList<AprilTagDetection> getDetections();
}
