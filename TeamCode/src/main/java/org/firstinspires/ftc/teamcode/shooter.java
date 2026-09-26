package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
public class shooter {

    DcMotor shooterMotorLeft;
    DcMotor shooterMotorRight;

    // Feeder / Indexer Motor (if present)
    DcMotor feederMotor;
    public shooter(HardwareMap hardwareMap) {
        // Connect motors from HardwareMap
        shooterMotorLeft = hardwareMap.get(DcMotor.class, "shooter_left");
        shooterMotorRight = hardwareMap.get(DcMotor.class, "shooter_right");

        feederMotor = hardwareMap.get(DcMotor.class, "feeder_motor");

        // Motor directions and modes
        shooterMotorLeft.setDirection(DcMotor.Direction.FORWARD);
        shooterMotorRight.setDirection(DcMotor.Direction.REVERSE);
        shooterMotorLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooterMotorRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }
    public void shoot(boolean gamepadlb,boolean gamepadrb,double gamepadrt,double gamepadlt,double gamepad1leftstick,double gamepad1rightstick,boolean aButton,boolean bButton,boolean xButton,boolean yButton,boolean dpadUp,boolean dpadDown,boolean dpadLeft,boolean dpadRight) {
        double shootPower = 0;

        if (gamepadrt > 0.1) {
            shootPower = gamepadrt; // Scale power with trigger
        } else if (gamepadlt > 0.1) {
            shootPower = -gamepadlt; // Reverse power
        }
        shooterMotorLeft.setPower(shootPower);
        shooterMotorRight.setPower(shootPower);
    }

}

