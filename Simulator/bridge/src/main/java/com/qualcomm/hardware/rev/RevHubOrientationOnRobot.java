package com.qualcomm.hardware.rev;

import com.qualcomm.robotcore.hardware.ImuOrientationOnRobot;

/**
 * Describes how a REV Control/Expansion Hub is mounted. Default (identity) orientation is
 * Logo UP, USB FORWARD, matching the real SDK.
 */
public class RevHubOrientationOnRobot implements ImuOrientationOnRobot {
    public enum LogoFacingDirection { UP, DOWN, FORWARD, BACKWARD, LEFT, RIGHT }
    public enum UsbFacingDirection { UP, DOWN, FORWARD, BACKWARD, LEFT, RIGHT }

    private final LogoFacingDirection logo;
    private final UsbFacingDirection usb;
    private final double[][] axes;

    public RevHubOrientationOnRobot(LogoFacingDirection logoFacingDirection, UsbFacingDirection usbFacingDirection) {
        this.logo = logoFacingDirection;
        this.usb = usbFacingDirection;
        double[] z = vec(logo.name());
        double[] y = vec(usb.name());
        double dot = z[0] * y[0] + z[1] * y[1] + z[2] * y[2];
        if (dot != 0) {
            throw new IllegalArgumentException("The specified REV Hub orientation is physically impossible: logo "
                    + logo + " and USB " + usb + " cannot face parallel directions");
        }
        double[] x = cross(y, z);
        axes = new double[][] { x, y, z };
    }

    public LogoFacingDirection getLogoFacingDirection() { return logo; }
    public UsbFacingDirection getUsbFacingDirection() { return usb; }

    /** Unit vector in the robot frame (+X right, +Y forward, +Z up). */
    public static double[] vec(String dir) {
        switch (dir) {
            case "UP": return new double[] {0, 0, 1};
            case "DOWN": return new double[] {0, 0, -1};
            case "FORWARD": return new double[] {0, 1, 0};
            case "BACKWARD": return new double[] {0, -1, 0};
            case "LEFT": return new double[] {-1, 0, 0};
            case "RIGHT": return new double[] {1, 0, 0};
            default: throw new IllegalArgumentException("Unknown direction " + dir);
        }
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }

    /** Rows are the hub's x, y, z axes expressed in the robot frame. */
    @Override public double[][] simHubAxesInRobotFrame() { return axes; }

    @Override public String simDescribe() { return "logo " + logo + ", USB " + usb; }
}
