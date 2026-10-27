package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

// Red, starting at tile (3, 1) facing the hive: AutoOp_Blue_4_6's start and route turned 180 degrees.
// The routine and all settings are in AutoBase.
@Autonomous(name = "AutoOp Red (3,1)", group = "Linear OpMode")
public class AutoOp_Red_3_1 extends AutoBase {

    // Blue coordinates; on red (4, 6) becomes (3, 1)
    @Override protected double startCol() { return 4; }
    @Override protected double startRow() { return 6; }
    @Override protected boolean isRed() { return true; }
}
