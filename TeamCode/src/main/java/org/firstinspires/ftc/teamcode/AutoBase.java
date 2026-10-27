package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.IMU;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/*
 * Field coordinates are TILES, matching the flowchart grid:
 *   column 1..6 left to right (red side to blue side),
 *   row    1..6 bottom to top (audience side to far wall).
 * A whole number is the CENTER of that tile, so (4, 6) is the center of column 4, top row.
 *
 * Heading in degrees: 0 = facing the far wall (up on the flowchart), 90 = facing left (red side),
 * 180 = facing the audience (down, toward the hive), -90 = facing right (blue side).
 *
 * The shared autonomous routine. Each start position is its own small OpMode file that extends this
 * class and says where the robot starts and which alliance it is on (AutoOp_Blue_4_6, AutoOp_Blue_5_6, AutoOp_Red_3_1, AutoOp_Red_2_1).
 * All the settings below live only here, so they are changed once for every start.
 *
 * The route is written for BLUE. On RED the field is the same turned 180 degrees, so every tile
 * (col, row) becomes (7 - col, 7 - row) and every heading turns 180 degrees.
 */
public abstract class AutoBase extends LinearOpMode {

    // Each start file fills these in. Start positions are in blue coordinates (see above):
    // the robot is placed there facing the hive (heading 180), or turned 180 degrees on red.
    protected abstract double startCol();
    protected abstract double startRow();
    protected abstract boolean isRed();

    // Blue route coordinates -> real field coordinates
    private double fieldCol(double col) {
        return isRed() ? 7 - col : col;
    }

    private double fieldRow(double row) {
        return isRed() ? 7 - row : row;
    }

    private double fieldHeading(double heading) {
        return isRed() ? (heading + 180) % 360 : heading;
    }

    private DcMotor lf, rf, lr, rr;
    private DcMotor shootmotor, intakemotor;
    private CRServo feeder, leftIntake, rightIntake;
    private IMU imu;

    private static final double TILE_INCHES = 24;

    // Robot pose (in inches from the bottom-left field corner, and degrees), set from the start
    // position when the OpMode runs
    private double startHeading;
    private double x;
    private double y;
    private double heading;

    // Previous encoder readings
    private double prevLf = 0;
    private double prevRf = 0;
    private double prevLr = 0;
    private double prevRr = 0;

    // Gobilda 537.7 ticks/rev on a 104 mm wheel
    private final double ticksPerInch = 41.8;

    // Mecanum wheels slip sideways, so a strafe moves less than the encoders count.
    // Measure on the robot: strafe a known distance and divide real inches by what odometry says.
    private final double strafeEfficiency = 0.85;

    // The robot's real front is the motors' back, same as driver.java (which flips stick Y and X
    // but not turning). Drive and strafe powers are reversed; turning is not.
    private static final boolean DRIVE_REVERSED = true;

    // The back-left and back-right ENCODER cables are plugged into each other's ports on our robot
    // (Odometry Test turn check: SWAPPED kept Strafe at 1.9, REVERSED climbed to 130).
    // Set to false once the cables are swapped back on the hub.
    private static final boolean BACK_ENCODERS_SWAPPED = true;

    // Set to true if Odometry Test's "Total turn since A" goes NEGATIVE while holding D-pad LEFT.
    // Then the robot turns the opposite way to how the IMU measures it, and the auto would spin forever.
    private static final boolean TURN_REVERSED = true;

    @Override
    public void runOpMode() {

        lf = hardwareMap.get(DcMotor.class, "front_left");
        rf = hardwareMap.get(DcMotor.class, "front_right");
        lr = hardwareMap.get(DcMotor.class, "back_left");
        rr = hardwareMap.get(DcMotor.class, "back_right");

        // Right side motors are mounted mirrored (same as driver.java)
        rf.setDirection(DcMotor.Direction.REVERSE);
        rr.setDirection(DcMotor.Direction.REVERSE);

        shootmotor = hardwareMap.get(DcMotor.class, "shooter");
        intakemotor = hardwareMap.get(DcMotor.class, "intake_motor");

        // Same shooter setup as teleop's shooter: with RUN_USING_ENCODER, power 0.5 = half speed, held steady
        shootmotor.setDirection(DcMotor.Direction.FORWARD);
        shootmotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shootmotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        feeder = hardwareMap.get(CRServo.class, "feeder_motor");
        leftIntake = hardwareMap.get(CRServo.class, "left_intake");
        rightIntake = hardwareMap.get(CRServo.class, "right_intake");

        // Same directions as teleop's intaker, so power 1 pulls balls in
        intakemotor.setDirection(DcMotor.Direction.REVERSE);
        leftIntake.setDirection(CRServo.Direction.REVERSE);
        rightIntake.setDirection(CRServo.Direction.FORWARD);

        imu = hardwareMap.get(IMU.class, "imu");

        // Same hub mounting as driver.java
        IMU.Parameters parameters = new IMU.Parameters(
                new RevHubOrientationOnRobot(
                        RevHubOrientationOnRobot.LogoFacingDirection.RIGHT,
                        RevHubOrientationOnRobot.UsbFacingDirection.UP )
        );

        imu.initialize(parameters);
        imu.resetYaw();

        // Where the robot is placed before INIT, facing the hive
        x = tileToInches(fieldCol(startCol()));
        y = tileToInches(fieldRow(startRow()));
        startHeading = fieldHeading(180);
        heading = startHeading;

        lf.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rf.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        lr.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        rr.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        lf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rf.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        lr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        rr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // Stop hard at each target instead of coasting past it
        lf.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rf.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        lr.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rr.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        waitForStart();

        if (opModeIsActive()) {

            // The shooter is on the robot's back end, so the robot faces 0 (away from the hive) to shoot.

            // 00 start on the top row facing the hive (the start file says which tile)
            // The shooter spins up while the robot drives and turns, instead of after it arrives.
            startShooter();
            // 01 move to (4, 5) without turning: on the top row the corners would hit the wall
            goToTile(4, 5, 180);
            // turn around here, where there is room, so the shooter faces the hive
            goToTile(4, 5, 0);
            // 02 shoot
            shoot();

            // From here on the robot keeps facing 0, so it never turns near a wall.
            // The intake starts on the way, so it is already running when the robot arrives.
            startIntake();
            // 03 + 04 go on a diagonal (the floor is clear) to 6 inches short of (6, 6)
            goToTile(6, 5.75, 0);
            // 05 intake: creep forward onto the balls at (6, 6), then keep the intake running
            creepToTile(6, 6, 0);
            intake();

            // Spin the shooter up again on the way back
            startShooter();
            // 06 + 07 go straight back to (4, 5) on a diagonal
            goToTile(4, 5, 0);
            // 08 shoot (still facing 0, like step 02)
            shoot();

            // 09 move right, then 10 down. Column 5.6 keeps the robot clear of the post on the right
            // wall, even with a little drift from not stopping at the corners.
            passTile(5.6, 5, 0);
            passTile(5.6, 2, 0);
            goToTile(6, 2, 0);
        }
    }

    private static double tileToInches(double tile) {
        return (tile - 0.5) * TILE_INCHES;
    }

    // Drive to a tile and stop there (for shooting, intaking, parking, or turning in place).
    // Tiles and headings are in blue route coordinates; on red they are turned 180 degrees.
    public void goToTile(double col, double row, double targetHeading) {
        goToPosition(tileToInches(fieldCol(col)), tileToInches(fieldRow(row)), fieldHeading(targetHeading), true);
    }

    // Drive through a tile without stopping, for the corners of a path. Saves the time of
    // slowing down, settling and speeding up again.
    public void passTile(double col, double row, double targetHeading) {
        goToPosition(tileToInches(fieldCol(col)), tileToInches(fieldRow(row)), fieldHeading(targetHeading), false);
    }

    private int frontLeftTicks() {
        return lf.getCurrentPosition();
    }

    private int frontRightTicks() {
        return rf.getCurrentPosition();
    }

    // With the cables swapped, each back wheel's encoder is read through the other back motor.
    // The sign flips too, because only the right motor is set to REVERSE.
    private int backLeftTicks() {
        return BACK_ENCODERS_SWAPPED ? -rr.getCurrentPosition() : lr.getCurrentPosition();
    }

    private int backRightTicks() {
        return BACK_ENCODERS_SWAPPED ? -lr.getCurrentPosition() : rr.getCurrentPosition();
    }

    public void updatePose() {

        heading = startHeading + imu.getRobotYawPitchRollAngles()
                .getYaw(AngleUnit.DEGREES);

        double lfPos = frontLeftTicks();
        double rfPos = frontRightTicks();
        double lrPos = backLeftTicks();
        double rrPos = backRightTicks();

        double dLf = lfPos - prevLf;
        double dRf = rfPos - prevRf;
        double dLr = lrPos - prevLr;
        double dRr = rrPos - prevRr;

        prevLf = lfPos;
        prevRf = rfPos;
        prevLr = lrPos;
        prevRr = rrPos;

        double forwardTicks =
                (dLf + dRf + dLr + dRr) / 4.0;

        double strafeTicks =
                (dLf - dRf - dLr + dRr) / 4.0;

        double forward = forwardTicks / ticksPerInch;
        double strafe = strafeTicks / ticksPerInch * strafeEfficiency;

        if (DRIVE_REVERSED) {
            forward = -forward;
            strafe = -strafe;
        }

        double headingRad = Math.toRadians(heading);

        x += strafe * Math.cos(headingRad)
                - forward * Math.sin(headingRad);

        y += strafe * Math.sin(headingRad)
                + forward * Math.cos(headingRad);
    }

    // Top drive power. creepToTile() lowers it for the slow push forward while intaking.
    private static final double DRIVE_MAX_POWER = 0.6;
    private double driveSpeedLimit = DRIVE_MAX_POWER;

    // Last drive powers sent, kept between moves so passing through a waypoint doesn't jerk
    private double lastForward = 0;
    private double lastStrafe = 0;
    private double lastTurn = 0;

    public void goToPosition(
            double targetX,
            double targetY,
            double targetHeading,
            boolean stopThere) {

        double kP = 0.04;           // drive power per inch of error (full speed beyond ~15 in)
        double kH = 0.012;          // turn power per degree of error
        double maxPower = driveSpeedLimit;
        double maxTurn = 0.4;
        double minPower = 0.12;     // enough to keep the robot creeping in the last few inches
        double minTurn = 0.08;      // same for turning, so the last few degrees don't stall
        double turnFirstDeg = 15;   // more than this off: turn in place before driving
        double maxRampPerSec = 2.0; // how fast power may change, so starts and stops don't jerk

        // Done when this close...
        double doneInches = 1.5;
        double doneDegrees = 3;
        // ...or when nearly there and the robot has stopped moving (friction won't let it creep closer)
        double closeInches = 3;
        double closeDegrees = 6;
        long stoppedMs = 300;

        // Passing through: keep at least this much power and move on once this close
        double passPower = 0.35;
        double passInches = 4;
        if (!stopThere) {
            minPower = passPower;
        }

        long startTime = System.currentTimeMillis();
        long lastLoopTime = startTime;
        long stillSince = startTime;
        double lastX = x;
        double lastY = y;
        double lastHeading = heading;
        double lastHeadingError = 0;

        while (opModeIsActive()) {

            updatePose();

            double dx = targetX - x;
            double dy = targetY - y;

            double distance = Math.hypot(dx, dy);

            double headingError =
                    targetHeading - heading;

            while (headingError > 180)
                headingError -= 360;

            while (headingError < -180)
                headingError += 360;

            // About 180 degrees off, turning left and right are equally short, and IMU noise
            // flips between them every loop, which makes the robot shake. Keep turning the way it started.
            if (Math.abs(headingError) > 150 && headingError * lastHeadingError < 0) {
                headingError -= 360 * Math.signum(headingError);
            }
            lastHeadingError = headingError;

            if (!stopThere && distance < passInches && Math.abs(headingError) < turnFirstDeg) {
                return;   // keep the motors running into the next move
            }

            if (distance < doneInches && Math.abs(headingError) < doneDegrees) {
                break;
            }

            // Restart the "stopped" clock whenever the robot moves more than a hair
            long now = System.currentTimeMillis();
            if (Math.hypot(x - lastX, y - lastY) > 0.25 || Math.abs(heading - lastHeading) > 1) {
                stillSince = now;
                lastX = x;
                lastY = y;
                lastHeading = heading;
            }
            if (distance < closeInches && Math.abs(headingError) < closeDegrees
                    && now - stillSince > stoppedMs) {
                break;
            }

            if (now - startTime > 5000) {
                break;
            }

            // Turn the field direction to the target into the robot's own forward / strafe
            double headingRad =
                    Math.toRadians(heading);

            double forward =
                    (-dx * Math.sin(headingRad) + dy * Math.cos(headingRad)) * kP;

            double strafe =
                    (dx * Math.cos(headingRad) + dy * Math.sin(headingRad)) * kP;

            double drive = Math.hypot(forward, strafe);
            if (drive > maxPower) {
                forward *= maxPower / drive;
                strafe *= maxPower / drive;
            } else if (distance >= 1.0 && drive < minPower) {
                forward *= minPower / drive;
                strafe *= minPower / drive;
            }

            // Big turns happen in place first. Turning while driving makes the robot swing
            // in an arc, and its corners can hit the wall.
            if (Math.abs(headingError) > turnFirstDeg) {
                forward = 0;
                strafe = 0;
            }

            // A positive turn in mecanumDrive spins clockwise, which lowers the heading
            double turn = Math.max(-maxTurn, Math.min(maxTurn, -headingError * kH));
            if (Math.abs(headingError) >= doneDegrees && Math.abs(turn) < minTurn) {
                turn = -Math.signum(headingError) * minTurn;
            }
            if (TURN_REVERSED) {
                turn = -turn;
            }

            // Ramp power instead of jumping, so the robot doesn't rock at starts and stops
            double maxStep = maxRampPerSec * (now - lastLoopTime) / 1000.0;
            lastLoopTime = now;
            forward = lastForward + Math.max(-maxStep, Math.min(maxStep, forward - lastForward));
            strafe = lastStrafe + Math.max(-maxStep, Math.min(maxStep, strafe - lastStrafe));
            turn = lastTurn + Math.max(-maxStep, Math.min(maxStep, turn - lastTurn));
            lastForward = forward;
            lastStrafe = strafe;
            lastTurn = turn;

            mecanumDrive(forward, strafe, turn);

            telemetry.addData("Col", x / TILE_INCHES + 0.5);
            telemetry.addData("Row", y / TILE_INCHES + 0.5);
            telemetry.addData("Heading", heading);
            telemetry.addData("Distance", distance);
            // All four should change by about the same amount when driving straight.
            // One stuck at 0 or going the opposite way = encoder cable or direction problem.
            // (Uses the readings from updatePose, so the encoders aren't read twice per loop.)
            telemetry.addData("Encoders FL FR BL BR", "%.0f %.0f %.0f %.0f",
                    prevLf, prevRf, prevLr, prevRr);
            telemetry.update();

            idle();
        }

        mecanumDrive(0, 0, 0);
        lastForward = 0;
        lastStrafe = 0;
        lastTurn = 0;
    }

    // Shooting: the shooter needs SHOOTER_SPINUP_MS to get up to speed (it can spin up while the
    // robot drives), then the feeder pushes balls in for FEED_MS. The feeder only runs once the robot
    // has stopped at its spot, so no ball is fired on the way.
    private static final double SHOOTER_POWER = 0.5;   // same as teleop's shooter cap
    private static final long SHOOTER_SPINUP_MS = 1000;
    private static final long FEED_MS = 3000;
    // Intake: runs INTAKE_TOTAL_MS in all, and at least INTAKE_MIN_AT_SPOT_MS after the creep forward
    private static final long INTAKE_TOTAL_MS = 3000;
    private static final long INTAKE_MIN_AT_SPOT_MS = 2000;
    // Drive power while creeping forward into the balls with the intake running
    private static final double INTAKE_CREEP_POWER = 0.2;

    private long shooterStartedAt = -1;
    private long intakeStartedAt = -1;

    // Spin up the shooter now, while the robot drives to where it shoots (the feeder stays off)
    public void startShooter() {
        shootmotor.setPower(SHOOTER_POWER);
        shooterStartedAt = System.currentTimeMillis();
    }

    // Wait until the shooter is up to speed, then feed balls in for FEED_MS
    public void shoot() {
        if (shooterStartedAt < 0) {
            startShooter();
        }
        long ranMs = System.currentTimeMillis() - shooterStartedAt;
        if (ranMs < SHOOTER_SPINUP_MS) {
            sleep(SHOOTER_SPINUP_MS - ranMs);
        }
        feeder.setPower(1);
        sleep(FEED_MS);
        feeder.setPower(0);
        shootmotor.setPower(0);
        shooterStartedAt = -1;
    }

    // Start the intake (motor and both side servos, same as teleop's intaker) while the robot drives
    public void startIntake() {
        intakemotor.setPower(1);
        leftIntake.setPower(1);
        rightIntake.setPower(1);
        intakeStartedAt = System.currentTimeMillis();
    }

    public void intake() {
        if (intakeStartedAt < 0) {
            startIntake();
        }
        long ranMs = System.currentTimeMillis() - intakeStartedAt;
        sleep(Math.max(INTAKE_MIN_AT_SPOT_MS, INTAKE_TOTAL_MS - ranMs));
        intakemotor.setPower(0);
        leftIntake.setPower(0);
        rightIntake.setPower(0);
        intakeStartedAt = -1;
    }

    // Drive slowly to a tile, so the intake can pull balls in as the robot moves onto them
    public void creepToTile(double col, double row, double targetHeading) {
        driveSpeedLimit = INTAKE_CREEP_POWER;
        goToTile(col, row, targetHeading);
        driveSpeedLimit = DRIVE_MAX_POWER;
    }

    public void mecanumDrive(
            double forward,
            double strafe,
            double turn) {

        if (DRIVE_REVERSED) {
            forward = -forward;
            strafe = -strafe;
        }

        double lfPower = forward + strafe + turn;
        double rfPower = forward - strafe - turn;
        double lrPower = forward - strafe + turn;
        double rrPower = forward + strafe - turn;

        double max =
                Math.max(
                        Math.max(Math.abs(lfPower), Math.abs(rfPower)),
                        Math.max(Math.abs(lrPower), Math.abs(rrPower))
                );

        if (max > 1.0) {
            lfPower /= max;
            rfPower /= max;
            lrPower /= max;
            rrPower /= max;
        }

        lf.setPower(lfPower);
        rf.setPower(rfPower);
        lr.setPower(lrPower);
        rr.setPower(rrPower);
    }
}
