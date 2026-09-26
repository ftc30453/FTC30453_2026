package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class driver {
    private final DcMotor leftDrive;
    private final DcMotor rightDrive;

    public driver(HardwareMap hardwareMap) {
        leftDrive = hardwareMap.get(DcMotor.class, "left_drive");
        rightDrive = hardwareMap.get(DcMotor.class, "right_drive");

    }
    public void drive(boolean gamepadlb,boolean gamepadrb,double gamepadrt,double gamepadlt,double gamepad1leftstick,double gamepad1rightstick,boolean aButton,boolean bButton,boolean xButton,boolean yButton,boolean dpadUp,boolean dpadDown,boolean dpadLeft,boolean dpadRight) {
        double leftPower = gamepad1leftstick + gamepad1rightstick;
        double rightPower = gamepad1leftstick - gamepad1rightstick;

        leftDrive.setPower(leftPower);
        rightDrive.setPower(rightPower);
    }
}

