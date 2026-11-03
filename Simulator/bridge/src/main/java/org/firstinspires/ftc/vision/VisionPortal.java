package org.firstinspires.ftc.vision;

import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraName;

/** Stand-in: the simulator has no cameras, so no portal ever opens. */
public abstract class VisionPortal {
    public static VisionPortal easyCreateWithDefaults(CameraName camera, VisionProcessor... processors) {
        throw new UnsupportedOperationException("The simulator has no camera");
    }

    public abstract void saveNextFrameRaw(String filename);

    public abstract void close();
}
