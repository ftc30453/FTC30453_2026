package sim;

import com.qualcomm.robotcore.hardware.CRServo;

final class SimCRServo implements CRServo {
    private final String name;
    private volatile Direction direction = Direction.FORWARD;
    private volatile double power = 0;

    SimCRServo(String name) { this.name = name; }

    double simRawOutput() { return direction == Direction.REVERSE ? -power : power; }
    void simZero() { power = 0; }

    @Override public void setDirection(Direction d) { direction = d; }
    @Override public Direction getDirection() { return direction; }

    @Override public void setPower(double p) {
        if (Double.isNaN(p)) throw new IllegalArgumentException("setPower(NaN) on CRServo \"" + name + "\"");
        power = Math.max(-1, Math.min(1, p));
    }

    @Override public double getPower() { return power; }
    @Override public String getDeviceName() { return "Simulated CR servo " + name; }
}
