package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

// Blue, starting at tile (4, 6) facing the hive. The routine and all settings are in AutoBase.
@Autonomous(name = "AutoOp Blue (4,6)", group = "Linear OpMode")
public class AutoOp_Blue_4_6 extends AutoBase {

    @Override protected double startCol() { return 4; }
    @Override protected double startRow() { return 6; }
    @Override protected boolean isRed() { return false; }
}
