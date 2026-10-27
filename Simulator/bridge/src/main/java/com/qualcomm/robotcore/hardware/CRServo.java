package com.qualcomm.robotcore.hardware;

/** Continuous-rotation servo. Power -1..1, like a motor without an encoder. */
public interface CRServo extends DcMotorSimple {
    default int getPortNumber() { return 0; }
}
