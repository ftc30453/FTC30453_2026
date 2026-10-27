package com.qualcomm.robotcore.hardware;

/** Simulator stand-in for the FTC SDK HardwareDevice interface. */
public interface HardwareDevice {
    enum Manufacturer { Unknown, Other, Lego, HiTechnic, ModernRobotics, Adafruit, Matrix, Lynx, AMS, STMicroelectronics, Broadcom, DFRobot, SparkFun, DigitalChickenLabs, GoBilda }

    default Manufacturer getManufacturer() { return Manufacturer.Other; }
    default String getDeviceName() { return "Simulated device"; }
    default String getConnectionInfo() { return "simulator"; }
    default int getVersion() { return 1; }
    default void resetDeviceConfigurationForOpMode() { }
    default void close() { }
}
