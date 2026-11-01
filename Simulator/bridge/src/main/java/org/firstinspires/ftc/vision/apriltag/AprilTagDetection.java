package org.firstinspires.ftc.vision.apriltag;

import org.opencv.core.Point;

/** Stand-in with the fields TeamCode reads. */
public class AprilTagDetection {
    public final int id;
    public final Point center;

    public AprilTagDetection(int id, Point center) {
        this.id = id;
        this.center = center;
    }
}
