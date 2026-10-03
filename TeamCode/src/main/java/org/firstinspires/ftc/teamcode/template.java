package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class template {
    //DcMotors go here
    DcMotor templateMotor;
    CRServo templateServo;
    public template(HardwareMap hardwareMap) {
        //hardwaremap goes here
        templateMotor = hardwareMap.get(DcMotor.class, "template_motor");
        templateServo = hardwareMap.get(CRServo.class, "template_servo");
        //set direction of motor
        templateMotor.setDirection(DcMotor.Direction.REVERSE);
        templateServo.setDirection(DcMotor.Direction.REVERSE);
        public void templates(Gamepad gamepad);
        //put logic here
        double templatepower = 0;

        if (gamepad.right_bumper) {
            templatePower = 1;
        }
        else if (gamepad.left_bumper) {
            templatePower = -1;
        }
        templateMotor.setPower(templatepower);
        templateServo.setPower(templatepower);
    }
}
//this is a dummy code i made to serve as a framework for your code, do not use any values from this but only use as starting point