extends RefCounted
## Reads a real gamepad and/or the keyboard into the FTC gamepad layout.
## FTC convention (same as Godot): pushing a stick UP gives a negative Y.

const DEADZONE := 0.06

const JOY_BUTTONS := {
	JOY_BUTTON_A: "a", JOY_BUTTON_B: "b", JOY_BUTTON_X: "x", JOY_BUTTON_Y: "y",
	JOY_BUTTON_LEFT_SHOULDER: "left_bumper", JOY_BUTTON_RIGHT_SHOULDER: "right_bumper",
	JOY_BUTTON_DPAD_UP: "dpad_up", JOY_BUTTON_DPAD_DOWN: "dpad_down",
	JOY_BUTTON_DPAD_LEFT: "dpad_left", JOY_BUTTON_DPAD_RIGHT: "dpad_right",
	JOY_BUTTON_BACK: "back", JOY_BUTTON_START: "start", JOY_BUTTON_GUIDE: "guide",
	JOY_BUTTON_LEFT_STICK: "left_stick_button", JOY_BUTTON_RIGHT_STICK: "right_stick_button",
}

const KEY_BUTTONS := {
	KEY_K: "a", KEY_L: "b", KEY_J: "x", KEY_I: "y",
	KEY_U: "left_bumper", KEY_O: "right_bumper",
	KEY_UP: "dpad_up", KEY_DOWN: "dpad_down", KEY_LEFT: "dpad_left", KEY_RIGHT: "dpad_right",
	KEY_BACKSPACE: "back", KEY_ENTER: "start",
}

const HELP := "Keyboard keys (when chosen as a source):\nWASD  left stick     Q/E  right stick X (turn)\nR/F  right stick Y   Arrows  D-pad\nI/J/K/L  Y/X/A/B     U/O  left/right bumper\nZ/C  left/right trigger   hold Shift = half stick"


static func empty() -> Dictionary:
	return {"lx": 0.0, "ly": 0.0, "rx": 0.0, "ry": 0.0, "lt": 0.0, "rt": 0.0, "buttons": []}


## reverse_wasd flips the keyboard's left stick (W = stick down, D = stick left), for drive code
## that inverts the left stick on purpose, so W still drives the robot forward.
static func read(device: int, use_keyboard: bool, reverse_wasd := false) -> Dictionary:
	var g := empty()
	var pressed := {}
	if device >= 0:
		g["lx"] = _dz(Input.get_joy_axis(device, JOY_AXIS_LEFT_X))
		g["ly"] = _dz(Input.get_joy_axis(device, JOY_AXIS_LEFT_Y))
		g["rx"] = _dz(Input.get_joy_axis(device, JOY_AXIS_RIGHT_X))
		g["ry"] = _dz(Input.get_joy_axis(device, JOY_AXIS_RIGHT_Y))
		g["lt"] = clampf(Input.get_joy_axis(device, JOY_AXIS_TRIGGER_LEFT), 0.0, 1.0)
		g["rt"] = clampf(Input.get_joy_axis(device, JOY_AXIS_TRIGGER_RIGHT), 0.0, 1.0)
		for b in JOY_BUTTONS:
			if Input.is_joy_button_pressed(device, b):
				pressed[JOY_BUTTONS[b]] = true
	if use_keyboard:
		var s := 0.5 if Input.is_key_pressed(KEY_SHIFT) else 1.0
		var ls := -s if reverse_wasd else s
		g["lx"] = _merge(g["lx"], _axis(KEY_A, KEY_D) * ls)
		g["ly"] = _merge(g["ly"], _axis(KEY_W, KEY_S) * ls)
		g["rx"] = _merge(g["rx"], _axis(KEY_Q, KEY_E) * s)
		g["ry"] = _merge(g["ry"], _axis(KEY_R, KEY_F) * s)
		if Input.is_key_pressed(KEY_Z):
			g["lt"] = maxf(g["lt"], s)
		if Input.is_key_pressed(KEY_C):
			g["rt"] = maxf(g["rt"], s)
		for k in KEY_BUTTONS:
			if Input.is_key_pressed(k):
				pressed[KEY_BUTTONS[k]] = true
	g["buttons"] = pressed.keys()
	return g


## Applies "name=value" overrides (used by --input in headless runs).
static func apply_overrides(g: Dictionary, overrides: Dictionary) -> void:
	var names := {
		"left_stick_x": "lx", "left_stick_y": "ly", "right_stick_x": "rx", "right_stick_y": "ry",
		"left_trigger": "lt", "right_trigger": "rt",
	}
	var buttons: Array = g["buttons"].duplicate()
	for k in overrides:
		if names.has(k):
			g[names[k]] = float(overrides[k])
		elif float(overrides[k]) != 0.0 and not buttons.has(k):
			buttons.append(k)
	g["buttons"] = buttons


static func _axis(neg: Key, pos: Key) -> float:
	var v := 0.0
	if Input.is_key_pressed(neg):
		v -= 1.0
	if Input.is_key_pressed(pos):
		v += 1.0
	return v


static func _merge(a: float, b: float) -> float:
	return a if absf(a) >= absf(b) else b


static func _dz(v: float) -> float:
	return 0.0 if absf(v) < DEADZONE else v
