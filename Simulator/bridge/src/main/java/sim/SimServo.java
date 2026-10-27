package sim;

import com.qualcomm.robotcore.hardware.Servo;

final class SimServo implements Servo {
    private final String name;
    private volatile Direction direction = Direction.FORWARD;
    private volatile double position = Double.NaN;
    private volatile double min = 0, max = 1;

    SimServo(String name) { this.name = name; }

    /** Actual PWM position 0..1, or NaN while the servo has never been commanded (unpowered). */
    double simOutput() {
        if (Double.isNaN(position)) return Double.NaN;
        double p = direction == Direction.REVERSE ? 1 - position : position;
        return min + p * (max - min);
    }

    @Override public void setDirection(Direction d) { direction = d; }
    @Override public Direction getDirection() { return direction; }
    @Override public void setPosition(double p) { position = Math.max(0, Math.min(1, p)); }
    @Override public double getPosition() { return Double.isNaN(position) ? 0 : position; }

    @Override public void scaleRange(double min, double max) {
        if (min < 0 || max > 1 || min >= max) {
            throw new IllegalArgumentException("scaleRange(" + min + ", " + max + ") needs 0 <= min < max <= 1");
        }
        this.min = min;
        this.max = max;
    }

    @Override public String getDeviceName() { return "Simulated servo " + name; }
}
