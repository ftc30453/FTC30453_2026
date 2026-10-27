package com.qualcomm.robotcore.hardware;

/**
 * Simulator Gamepad. Same public fields as the FTC SDK. Sticks: -1..1 (stick pushed UP gives
 * left_stick_y = -1, as on a real gamepad). Triggers: 0..1.
 */
public class Gamepad {
    public volatile float left_stick_x, left_stick_y, right_stick_x, right_stick_y;
    public volatile float left_trigger, right_trigger;
    public volatile boolean dpad_up, dpad_down, dpad_left, dpad_right;
    public volatile boolean a, b, x, y;
    public volatile boolean guide, start, back;
    public volatile boolean left_bumper, right_bumper;
    public volatile boolean left_stick_button, right_stick_button;
    // PlayStation aliases
    public volatile boolean circle, cross, triangle, square, share, options, ps, touchpad;

    private final java.util.Map<String, Boolean> lastQueried = new java.util.HashMap<>();

    public Gamepad() { }

    public void copy(Gamepad o) {
        left_stick_x = o.left_stick_x; left_stick_y = o.left_stick_y;
        right_stick_x = o.right_stick_x; right_stick_y = o.right_stick_y;
        left_trigger = o.left_trigger; right_trigger = o.right_trigger;
        dpad_up = o.dpad_up; dpad_down = o.dpad_down; dpad_left = o.dpad_left; dpad_right = o.dpad_right;
        a = o.a; b = o.b; x = o.x; y = o.y; guide = o.guide; start = o.start; back = o.back;
        left_bumper = o.left_bumper; right_bumper = o.right_bumper;
        left_stick_button = o.left_stick_button; right_stick_button = o.right_stick_button;
        syncAliases();
    }

    public void syncAliases() {
        cross = a; circle = b; square = x; triangle = y; share = back; options = start; ps = guide;
    }

    public boolean atRest() {
        return left_stick_x == 0 && left_stick_y == 0 && right_stick_x == 0 && right_stick_y == 0
                && left_trigger == 0 && right_trigger == 0;
    }

    // Edge detection helpers (newer SDKs). True once per press/release.
    private synchronized boolean edge(String key, boolean now, boolean rising) {
        Boolean prev = lastQueried.put(key + rising, now);
        boolean p = prev != null && prev;
        return rising ? (now && !p) : (!now && p);
    }
    public boolean aWasPressed() { return edge("a", a, true); }
    public boolean bWasPressed() { return edge("b", b, true); }
    public boolean xWasPressed() { return edge("x", x, true); }
    public boolean yWasPressed() { return edge("y", y, true); }
    public boolean aWasReleased() { return edge("a", a, false); }
    public boolean bWasReleased() { return edge("b", b, false); }
    public boolean xWasReleased() { return edge("x", x, false); }
    public boolean yWasReleased() { return edge("y", y, false); }
    public boolean dpadUpWasPressed() { return edge("du", dpad_up, true); }
    public boolean dpadDownWasPressed() { return edge("dd", dpad_down, true); }
    public boolean dpadLeftWasPressed() { return edge("dl", dpad_left, true); }
    public boolean dpadRightWasPressed() { return edge("dr", dpad_right, true); }
    public boolean leftBumperWasPressed() { return edge("lb", left_bumper, true); }
    public boolean rightBumperWasPressed() { return edge("rb", right_bumper, true); }

    // Feedback features do nothing in the simulator.
    public void rumble(int durationMs) { }
    public void rumble(double rumble1, double rumble2, int durationMs) { }
    public void stopRumble() { }
    public boolean isRumbling() { return false; }
    public void setLedColor(double r, double g, double b, int durationMs) { }
}
