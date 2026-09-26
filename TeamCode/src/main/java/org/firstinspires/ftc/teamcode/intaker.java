package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class intaker {
    // Intake Mechanism
    DcMotor intakeMotor;
    CRServo leftIntakeServo;
    CRServo rightIntakeServo;

    public intaker(HardwareMap hardwareMap) {

        // Intake DC Motor (Vertical)
        intakeMotor = hardwareMap.get(DcMotor.class, "intake_motor");

        // Intake Servos (Horizontal)
        leftIntakeServo = hardwareMap.get(CRServo.class, "left_intake");
        rightIntakeServo = hardwareMap.get(CRServo.class, "right_intake");
        // Intake direction: You may need to flip these (REVERSE) depending on your wiring
        intakeMotor.setDirection(DcMotor.Direction.REVERSE);
        leftIntakeServo.setDirection(CRServo.Direction.REVERSE);
        rightIntakeServo.setDirection(CRServo.Direction.FORWARD); // Usually one is reversed to "pull in"

    }

    public void intake(boolean gamepadlb,boolean gamepadrb,double gamepadrt,double gamepadlt,double gamepad1leftstick,double gamepad1rightstick,boolean aButton,boolean bButton,boolean xButton,boolean yButton,boolean dpadUp,boolean dpadDown,boolean dpadLeft,boolean dpadRight) {
        double intakePower = 0;

        if (gamepadrb = true) {
            intakePower = 1;
        } else if (gamepadlb = true) {
            intakePower = -1;
        }
        intakeMotor.setPower(intakePower);
        leftIntakeServo.setPower(intakePower);
        rightIntakeServo.setPower(intakePower);


    }
}