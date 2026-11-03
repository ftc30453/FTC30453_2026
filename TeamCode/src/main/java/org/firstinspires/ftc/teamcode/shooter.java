package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.CRServo;
import org.firstinspires.ftc.robotcore.external.Telemetry;


//STEVEN does this one
public class shooter {
    private final Telemetry telemetry;

    DcMotor shooterMotor;
    // Feeder / Indexer Motor (if present)
    CRServo feederMotor;
    public shooter(HardwareMap hardwareMap, Telemetry telemetry) {
        this.telemetry = telemetry;
        // Connect motors from HardwareMap
        shooterMotor = hardwareMap.get(DcMotor.class, "shooter");

        feederMotor = hardwareMap.get(CRServo.class, "feeder_motor");

        // Motor directions and modes
        shooterMotor.setDirection(DcMotor.Direction.FORWARD);
        shooterMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

    }
    public void shoot(Gamepad gamepad) {
        double shootPower = 0;
        double feederPower = 0;
        shooterMotor.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE
        );

        //use if and else if statements to tell robot what to do when specific keys are pressed
        if (gamepad.right_trigger > 0.1) {
            shootPower = gamepad.right_trigger; // <- edit this part when rt pressed set shooter power to rt
        } else if (gamepad.left_trigger > 0.1) {
            shootPower = -gamepad.left_trigger; //<- edit this part, same as up but reversed
        }
        if (gamepad.x) {
            feederPower = 1;
        }
        else if (gamepad.b) {
            feederPower = -1;
        }
        if (shootPower > 0) {
            shootPower = Math.min(0.5, shootPower);
        }
        else {
            shootPower = Math.max(-0.5, shootPower);
        }//^
        //|
        //logic
        shooterMotor.setPower(shootPower);
        feederMotor.setPower(feederPower);
        telemetry.addData("Shooter Power", shootPower);
        telemetry.addData("Feeder Power", feederPower);

    }



}

