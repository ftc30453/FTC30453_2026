package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

// Red, starting at tile (2, 1) facing the hive: AutoOp_Blue_5_6's start and route turned 180 degrees.
// The routine and all settings are in AutoBase.
@Autonomous(name = "AutoOp Red (2,1)", group = "Linear OpMode")
public class AutoOp_Red_2_1 extends AutoBase {

    // Blue coordinates; on red (5, 6) becomes (2, 1)
    @Override protected double startCol() { return 5; }
    @Override protected double startRow() { return 6; }
    @Override protected boolean isRed() { return true; }
}
