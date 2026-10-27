package com.qualcomm.robotcore.hardware;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;

public interface DcMotorEx extends DcMotor {
    void setMotorEnable();
    void setMotorDisable();
    boolean isMotorEnabled();

    /** Ticks per second. */
    void setVelocity(double angularRate);
    void setVelocity(double angularRate, AngleUnit unit);
    /** Ticks per second. */
    double getVelocity();
    double getVelocity(AngleUnit unit);

    void setPIDFCoefficients(RunMode mode, PIDFCoefficients pidfCoefficients);
    PIDFCoefficients getPIDFCoefficients(RunMode mode);
    void setVelocityPIDFCoefficients(double p, double i, double d, double f);
    void setPositionPIDFCoefficients(double p);

    void setTargetPositionTolerance(int tolerance);
    int getTargetPositionTolerance();

    double getCurrent(CurrentUnit unit);
    double getCurrentAlert(CurrentUnit unit);
    void setCurrentAlert(double current, CurrentUnit unit);
    boolean isOverCurrent();
}
