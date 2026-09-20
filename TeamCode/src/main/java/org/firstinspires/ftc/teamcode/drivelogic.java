package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.HardwareMap;

public class drivelogic {
    private final DcMotor leftDrive;
    private final DcMotor rightDrive;
    public drivelogic(HardwareMap hardwaremap) {
        leftDrive = hardwaremap.get(DcMotor.class, "left_drive");
        rightDrive = hardwaremap.get(DcMotor.class, "right_drive");
    }
    public void Driverlogic(double drive, double turn) {
        double leftPower = drive + turn;
        double rightPower = drive - turn;

        leftDrive.setPower(leftPower);
        rightDrive.setPower(rightPower);
    }
}

