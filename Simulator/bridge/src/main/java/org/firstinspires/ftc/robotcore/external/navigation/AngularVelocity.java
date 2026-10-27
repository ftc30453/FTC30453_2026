package org.firstinspires.ftc.robotcore.external.navigation;

public class AngularVelocity {
    public AngleUnit unit;
    public float xRotationRate, yRotationRate, zRotationRate;
    public long acquisitionTime;

    public AngularVelocity() { this(AngleUnit.DEGREES, 0, 0, 0, 0); }

    public AngularVelocity(AngleUnit unit, float x, float y, float z, long acquisitionTime) {
        this.unit = unit; this.xRotationRate = x; this.yRotationRate = y; this.zRotationRate = z;
        this.acquisitionTime = acquisitionTime;
    }

    public AngularVelocity toAngleUnit(AngleUnit u) {
        return new AngularVelocity(u, (float) u.fromUnit(unit, xRotationRate), (float) u.fromUnit(unit, yRotationRate),
                (float) u.fromUnit(unit, zRotationRate), acquisitionTime);
    }
}
