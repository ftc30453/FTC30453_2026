package com.qualcomm.robotcore.hardware;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import sim.SimHardware;

/**
 * Simulator HardwareMap. Device names and types come from Simulator/godot/config/robot.json,
 * just like the real one comes from the Robot Controller configuration. Asking for a name or
 * type that isn't configured throws the same exception a real robot does.
 */
public class HardwareMap implements Iterable<HardwareDevice> {

    public class DeviceMapping<T extends HardwareDevice> implements Iterable<T> {
        private final Class<T> type;
        DeviceMapping(Class<T> type) { this.type = type; }

        public T get(String deviceName) { return HardwareMap.this.get(type, deviceName); }

        @Override public Iterator<T> iterator() {
            List<T> list = new ArrayList<>();
            for (String name : sim.deviceNames()) {
                if (sim.deviceCompatible(name, type)) list.add(HardwareMap.this.get(type, name));
            }
            return list.iterator();
        }

        public int size() {
            int n = 0;
            for (T ignored : this) n++;
            return n;
        }
    }

    public final DeviceMapping<DcMotor> dcMotor = new DeviceMapping<>(DcMotor.class);
    public final DeviceMapping<Servo> servo = new DeviceMapping<>(Servo.class);
    public final DeviceMapping<CRServo> crservo = new DeviceMapping<>(CRServo.class);
    public final DeviceMapping<VoltageSensor> voltageSensor = new DeviceMapping<>(VoltageSensor.class);
    public final DeviceMapping<DistanceSensor> distanceSensor = new DeviceMapping<>(DistanceSensor.class);
    public final DeviceMapping<TouchSensor> touchSensor = new DeviceMapping<>(TouchSensor.class);

    private final SimHardware sim;

    public HardwareMap(SimHardware sim) { this.sim = sim; }

    public <T> T get(Class<? extends T> classOrInterface, String deviceName) {
        T result = tryGet(classOrInterface, deviceName);
        if (result == null) {
            throw new IllegalArgumentException(String.format(
                    "Unable to find a hardware device with name \"%s\" and type %s%s",
                    deviceName, classOrInterface.getSimpleName(), sim.hintFor(deviceName, classOrInterface)));
        }
        return result;
    }

    public <T> T tryGet(Class<? extends T> classOrInterface, String deviceName) {
        HardwareDevice d = sim.device(deviceName);
        if (d == null || !classOrInterface.isInstance(d)) return null;
        return classOrInterface.cast(d);
    }

    public HardwareDevice get(String deviceName) {
        HardwareDevice d = sim.device(deviceName);
        if (d == null) throw new IllegalArgumentException("No device with name \"" + deviceName + "\"" + sim.hintFor(deviceName, HardwareDevice.class));
        return d;
    }

    public SortedSet<String> getAllNames(Class<? extends HardwareDevice> type) {
        SortedSet<String> names = new TreeSet<>();
        for (String n : sim.deviceNames()) if (sim.deviceCompatible(n, type)) names.add(n);
        return names;
    }

    public <T> List<T> getAll(Class<? extends T> type) {
        List<T> list = new ArrayList<>();
        for (String n : sim.deviceNames()) {
            T d = tryGet(type, n);
            if (d != null) list.add(d);
        }
        return list;
    }

    public int size() { return sim.deviceNames().size(); }

    @Override public Iterator<HardwareDevice> iterator() {
        List<HardwareDevice> list = new ArrayList<>();
        for (String n : sim.deviceNames()) list.add(sim.device(n));
        return list.iterator();
    }
}
