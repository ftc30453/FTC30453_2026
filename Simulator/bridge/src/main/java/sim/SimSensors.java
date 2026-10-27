package sim;

import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.hardware.VoltageSensor;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

final class SimSensors {
    private SimSensors() { }

    /** Distance from the sensor (robot.json position/direction) to the nearest wall or solid element. */
    static final class Distance implements DistanceSensor {
        private final SimHardware hw;
        private final String name;

        Distance(SimHardware hw, String name) { this.hw = hw; this.name = name; }

        @Override public double getDistance(DistanceUnit unit) {
            Double inches = hw.distances.get(name);
            if (inches == null || inches >= 1e6) return distanceOutOfRange;
            return unit.fromInches(inches);
        }

        @Override public String getDeviceName() { return "Simulated distance sensor " + name; }
    }

    /** Pressed while the sensor's point on the robot (robot.json) touches a wall or solid element. */
    static final class Touch implements TouchSensor {
        private final SimHardware hw;
        private final String name;

        Touch(SimHardware hw, String name) { this.hw = hw; this.name = name; }

        @Override public double getValue() { return isPressed() ? 1 : 0; }
        @Override public boolean isPressed() { return Boolean.TRUE.equals(hw.touches.get(name)); }
        @Override public String getDeviceName() { return "Simulated touch sensor " + name; }
    }

    static final class Voltage implements VoltageSensor {
        private final SimHardware hw;
        private final String name;

        Voltage(SimHardware hw, String name) { this.hw = hw; this.name = name; }

        @Override public double getVoltage() { return hw.battery; }
        @Override public String getDeviceName() { return "Simulated voltage sensor " + name; }
    }
}
