package org.firstinspires.ftc.robotcore.external.navigation;

public enum AngleUnit {
    DEGREES, RADIANS;

    public static final double Pi = Math.PI;
    public static final float Pif = (float) Math.PI;

    public double toDegrees(double angle) { return this == DEGREES ? angle : Math.toDegrees(angle); }
    public double toRadians(double angle) { return this == RADIANS ? angle : Math.toRadians(angle); }
    public float toDegrees(float angle) { return (float) toDegrees((double) angle); }
    public float toRadians(float angle) { return (float) toRadians((double) angle); }

    public double fromDegrees(double degrees) { return this == DEGREES ? degrees : Math.toRadians(degrees); }
    public double fromRadians(double radians) { return this == RADIANS ? radians : Math.toDegrees(radians); }
    public double fromUnit(AngleUnit them, double theirs) { return fromRadians(them.toRadians(theirs)); }

    public double normalize(double angle) { return this == DEGREES ? normalizeDegrees(angle) : normalizeRadians(angle); }

    public static double normalizeDegrees(double degrees) {
        while (degrees >= 180.0) degrees -= 360.0;
        while (degrees < -180.0) degrees += 360.0;
        return degrees;
    }

    public static double normalizeRadians(double radians) {
        while (radians >= Math.PI) radians -= 2 * Math.PI;
        while (radians < -Math.PI) radians += 2 * Math.PI;
        return radians;
    }

    public UnnormalizedAngleUnit getUnnormalized() {
        return this == DEGREES ? UnnormalizedAngleUnit.DEGREES : UnnormalizedAngleUnit.RADIANS;
    }
}
