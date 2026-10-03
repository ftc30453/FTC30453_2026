package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Gamepad;
//STEVEN does this one
public class shooter {

    DcMotor shooterMotor;
    // Feeder / Indexer Motor (if present)
    DcMotor feederMotor;
    public shooter(HardwareMap hardwareMap) {
        // Connect motors from HardwareMap
        shooterMotor = hardwareMap.get(DcMotor.class, "shooter");

        feederMotor = hardwareMap.get(DcMotor.class, "feeder_motor");

        // Motor directions and modes
        shooterMotor.setDirection(DcMotor.Direction.FORWARD);
        shooterMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

    }
    public void shoot(Gamepad gamepad) {
        double shootPower = 0;
        //use if and else if statments to tell robot what to do when specific keys are pressed
        if (gamepad.right_trigger > 0.1) {
            shootPower = gamepad.right_trigger; // <- edit this part when rt pressed set shooter power to rt
        } else if (gamepad.left_trigger > 0.1) {
            shootPower = -gamepad.left_trigger; //<- edit this part, same as up but reversed
        }
        //^
        //|
        //logic
        shooterMotor.setPower(shootPower);
    }

}

