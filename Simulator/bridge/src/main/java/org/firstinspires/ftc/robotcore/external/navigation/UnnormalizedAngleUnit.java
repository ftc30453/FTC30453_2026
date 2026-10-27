package org.firstinspires.ftc.robotcore.external.navigation;

public enum UnnormalizedAngleUnit {
    DEGREES, RADIANS;

    public double toDegrees(double angle) { return this == DEGREES ? angle : Math.toDegrees(angle); }
    public double toRadians(double angle) { return this == RADIANS ? angle : Math.toRadians(angle); }
    public double fromDegrees(double degrees) { return this == DEGREES ? degrees : Math.toRadians(degrees); }
    public double fromRadians(double radians) { return this == RADIANS ? radians : Math.toDegrees(radians); }
    public AngleUnit getNormalized() { return this == DEGREES ? AngleUnit.DEGREES : AngleUnit.RADIANS; }
}
