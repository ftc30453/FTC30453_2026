package com.qualcomm.robotcore.hardware;

/**
 * How the IMU is mounted. The real SDK exposes quaternions; the simulator only needs the
 * 3x3 matrix whose columns are the hub's x, y, z axes expressed in the robot frame
 * (robot frame: +X right, +Y forward, +Z up).
 */
public interface ImuOrientationOnRobot {
    double[][] simHubAxesInRobotFrame();
    String simDescribe();
}
