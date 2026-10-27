package org.firstinspires.ftc.robotcore.external.navigation;

public enum DistanceUnit {
    METER(1.0), CM(0.01), MM(0.001), INCH(0.0254);

    public static final double infinity = Double.MAX_VALUE;
    private final double metersPerUnit;

    DistanceUnit(double metersPerUnit) { this.metersPerUnit = metersPerUnit; }

    public double fromMeters(double meters) { return meters == infinity ? infinity : meters / metersPerUnit; }
    public double toMeters(double value) { return value == infinity ? infinity : value * metersPerUnit; }
    public double fromInches(double inches) { return fromMeters(INCH.toMeters(inches)); }
    public double toInches(double value) { return INCH.fromMeters(toMeters(value)); }
    public double fromMm(double mm) { return fromMeters(MM.toMeters(mm)); }
    public double toMm(double value) { return MM.fromMeters(toMeters(value)); }
    public double fromCm(double cm) { return fromMeters(CM.toMeters(cm)); }
    public double toCm(double value) { return CM.fromMeters(toMeters(value)); }
    public double fromUnit(DistanceUnit them, double theirs) { return fromMeters(them.toMeters(theirs)); }
}
