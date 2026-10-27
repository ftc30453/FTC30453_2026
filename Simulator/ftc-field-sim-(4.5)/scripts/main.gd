extends Control
## FTC 2D field simulator: field view + Driver Station panel. The Java bridge
## (Simulator/bridge) runs the team's real OpModes and connects on localhost.
##
## Headless test run (no window):
##   godot --headless --path Simulator/godot -- --opmode AutoOp1 --seconds 30
##   options: --input left_stick_y=-1,right_trigger=1   --trace out.csv   --port 7777
##            --pose x,y,heading   --wait 30 (seconds to wait for the bridge)

const RobotSim := preload("res://scripts/robot_sim.gd")
const FieldView := preload("res://scripts/field_view.gd")
const Bridge := preload("res://scripts/bridge_server.gd")
const GamepadInput := preload("res://scripts/gamepad_input.gd")

const AUTO_SECONDS := 30.0
const TELEOP_SECONDS := 120.0

var robot_cfg: Dictionary = {}
var field_cfg: Dictionary = {}
var robot
var bridge
var field_view

var opmodes: Array = []
var state := "IDLE"          # bridge OpMode state: IDLE INIT RUNNING STOPPING STOPPED ERROR
var active_name := ""
var active_kind := ""
var run_time := 0.0
var sim_time := 0.0
var telemetry: PackedStringArray = []
var last_error := ""
var _ui_tick := 0

# UI
var conn_label: Label
var opmode_select: OptionButton
var init_btn: Button
var start_btn: Button
var stop_btn: Button
var state_label: Label
var timer_label: Label
var auto_stop_check: CheckBox
var g1_source: OptionButton
var g2_source: OptionButton
var _known_joys: Array = []
var _settings := ConfigFile.new()
const SETTINGS_PATH := "user://settings.cfg"
var trail_check: CheckBox
var telemetry_box: RichTextLabel
var mech_label: Label
var pose_label: Label
var log_box: RichTextLabel
var gamepad_label: Label

# headless / scripted runs
var cli := {}
var headless := false
var forced_input := {}
var test_phase := ""
var test_timer := 0.0
var trace_file: FileAccess = null
var _print_timer := 0.0
var _last_printed_tel := ""
var _shot_taken := false


func _ready() -> void:
	headless = DisplayServer.get_name() == "headless"
	_parse_cli()
	robot_cfg = _load_json("res://config/robot.json")
	field_cfg = _load_json("res://config/field.json")

	robot = RobotSim.new()
	robot.setup(robot_cfg, field_cfg)
	if cli.has("pose"):
		var p: PackedStringArray = String(cli["pose"]).split(",")
		if p.size() == 3:
			robot.start_pose = {"x": float(p[0]), "y": float(p[1]), "heading": float(p[2])}
			robot.reset_pose()

	bridge = Bridge.new()
	bridge.start(int(cli.get("port", robot_cfg.get("port", 7777))))

	_build_ui()
	for e in robot.errors:
		_log("[color=#ff8a80]%s[/color]" % e)
	if not bridge.listening:
		_log("[color=#ff8a80]%s[/color]" % bridge.listen_error)
	else:
		_log("Listening for the Java bridge on 127.0.0.1:%d. Run Simulator/bridge/run-bridge (.bat on Windows, .sh on Mac/Linux)." % bridge.port)

	if cli.has("opmode"):
		test_phase = "wait_bridge"
		test_timer = 0.0
		if cli.has("input"):
			for pair in String(cli["input"]).split(",", false):
				var kv := pair.split("=")
				if kv.size() == 2:
					forced_input[kv[0].strip_edges()] = float(kv[1])
		if cli.has("trace"):
			trace_file = FileAccess.open(String(cli["trace"]), FileAccess.WRITE)
			if trace_file:
				trace_file.store_line("t,x_in,y_in,heading_deg,state")
		print("[sim] headless test: opmode=%s seconds=%s" % [cli["opmode"], cli.get("seconds", "30")])


func _parse_cli() -> void:
	var args := OS.get_cmdline_user_args()
	var i := 0
	while i < args.size():
		var a: String = args[i]
		if a.begins_with("--"):
			var key := a.substr(2)
			if "=" in key:
				var parts := key.split("=", true, 1)
				cli[parts[0]] = parts[1]
			elif i + 1 < args.size() and not args[i + 1].begins_with("--"):
				cli[key] = args[i + 1]
				i += 1
			else:
				cli[key] = "true"
		i += 1


func _load_json(path: String) -> Dictionary:
	var txt := FileAccess.get_file_as_string(path)
	if txt.is_empty():
		push_error("Missing " + path)
		return {}
	var json := JSON.new()
	if json.parse(txt) != OK:
		var msg := "%s line %d: %s" % [path, json.get_error_line(), json.get_error_message()]
		push_error(msg)
		call_deferred("_log", "[color=#ff8a80]JSON error in " + msg + "[/color]")
		return {}
	return json.data if json.data is Dictionary else {}


# ================================================================== UI

func _build_ui() -> void:
	var root := HBoxContainer.new()
	root.set_anchors_preset(Control.PRESET_FULL_RECT)
	root.add_theme_constant_override("separation", 0)
	add_child(root)

	field_view = FieldView.new()
	field_view.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	field_view.size_flags_vertical = Control.SIZE_EXPAND_FILL
	field_view.robot = robot
	root.add_child(field_view)
	field_view.set_field(field_cfg)

	var panel_bg := PanelContainer.new()
	panel_bg.custom_minimum_size = Vector2(430, 0)
	var sb := StyleBoxFlat.new()
	sb.bg_color = Color("#24272e")
	sb.content_margin_left = 12
	sb.content_margin_right = 12
	sb.content_margin_top = 10
	sb.content_margin_bottom = 10
	panel_bg.add_theme_stylebox_override("panel", sb)
	root.add_child(panel_bg)

	var scroll := ScrollContainer.new()
	scroll.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
	panel_bg.add_child(scroll)
	var v := VBoxContainer.new()
	v.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	v.add_theme_constant_override("separation", 6)
	scroll.add_child(v)

	var title := Label.new()
	title.text = "FTC Field Sim · team %s" % robot_cfg.get("name", "")
	title.add_theme_font_size_override("font_size", 18)
	v.add_child(title)

	conn_label = Label.new()
	v.add_child(conn_label)

	v.add_child(_section("OpMode"))
	opmode_select = OptionButton.new()
	opmode_select.focus_mode = Control.FOCUS_NONE
	opmode_select.fit_to_longest_item = false
	v.add_child(opmode_select)

	var btns := HBoxContainer.new()
	btns.add_theme_constant_override("separation", 6)
	init_btn = _button("INIT", _on_init)
	start_btn = _button("▶ START", _on_start)
	stop_btn = _button("■ STOP", _on_stop)
	for b in [init_btn, start_btn, stop_btn]:
		b.size_flags_horizontal = Control.SIZE_EXPAND_FILL
		btns.add_child(b)
	v.add_child(btns)

	var row := HBoxContainer.new()
	state_label = Label.new()
	state_label.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	row.add_child(state_label)
	timer_label = Label.new()
	timer_label.add_theme_font_size_override("font_size", 18)
	row.add_child(timer_label)
	v.add_child(row)

	auto_stop_check = CheckBox.new()
	auto_stop_check.text = "Stop at match time (auto 0:30, teleop 2:00)"
	auto_stop_check.button_pressed = true
	auto_stop_check.focus_mode = Control.FOCUS_NONE
	v.add_child(auto_stop_check)

	v.add_child(_section("Telemetry"))
	telemetry_box = RichTextLabel.new()
	telemetry_box.custom_minimum_size = Vector2(0, 230)
	telemetry_box.scroll_following = false
	telemetry_box.add_theme_font_override("normal_font", _mono())
	telemetry_box.add_theme_font_size_override("normal_font_size", 12)
	telemetry_box.add_theme_stylebox_override("normal", _box("#15171b"))
	v.add_child(telemetry_box)

	v.add_child(_section("Mechanisms (commanded power)"))
	mech_label = Label.new()
	mech_label.add_theme_font_override("font", _mono())
	mech_label.add_theme_font_size_override("font_size", 12)
	v.add_child(mech_label)

	v.add_child(_section("Robot"))
	pose_label = Label.new()
	pose_label.add_theme_font_override("font", _mono())
	pose_label.add_theme_font_size_override("font_size", 12)
	v.add_child(pose_label)

	var tools := HBoxContainer.new()
	tools.add_child(_button("Reset robot", _on_reset_robot))
	tools.add_child(_button("Clear trail", func(): robot.trail.clear()))
	trail_check = CheckBox.new()
	trail_check.text = "Trail"
	trail_check.button_pressed = true
	trail_check.focus_mode = Control.FOCUS_NONE
	trail_check.toggled.connect(func(on): field_view.show_trail = on)
	tools.add_child(trail_check)
	v.add_child(tools)

	v.add_child(_section("Gamepad"))
	_settings.load(SETTINGS_PATH)
	g1_source = _source_picker(v, "gamepad1")
	g2_source = _source_picker(v, "gamepad2")
	Input.joy_connection_changed.connect(func(_d, _c): _refresh_sources())
	_refresh_sources()
	gamepad_label = Label.new()
	gamepad_label.add_theme_font_size_override("font_size", 11)
	gamepad_label.modulate = Color(1, 1, 1, 0.75)
	v.add_child(gamepad_label)

	v.add_child(_section("Log"))
	log_box = RichTextLabel.new()
	log_box.bbcode_enabled = true
	log_box.custom_minimum_size = Vector2(0, 170)
	log_box.scroll_following = true
	log_box.selection_enabled = true
	log_box.add_theme_font_size_override("normal_font_size", 11)
	log_box.add_theme_stylebox_override("normal", _box("#15171b"))
	v.add_child(log_box)

	_refresh_opmode_list()


func _source_picker(parent: Control, label: String) -> OptionButton:
	var row := HBoxContainer.new()
	var l := Label.new()
	l.text = label
	l.custom_minimum_size = Vector2(80, 0)
	row.add_child(l)
	var o := OptionButton.new()
	o.focus_mode = Control.FOCUS_NONE
	o.size_flags_horizontal = Control.SIZE_EXPAND_FILL
	o.item_selected.connect(func(_i): _save_sources())
	row.add_child(o)
	parent.add_child(row)
	return o


## Source choices: "none", "keyboard", "joy:<name>", "keyboard+joy:<name>".
## Gamepads are remembered by name so the choice survives replugging.
func _refresh_sources() -> void:
	_known_joys = Input.get_connected_joypads()
	var names: Array = _joy_keys().values()
	var want1: String = _settings.get_value("input", "gamepad1", "keyboard+joy:" + (names[0] if names.size() > 0 else ""))
	var want2: String = _settings.get_value("input", "gamepad2", "joy:" + (names[1] if names.size() > 1 else ""))
	_fill_source(g1_source, names, want1, true)
	_fill_source(g2_source, names, want2, false)


func _fill_source(o: OptionButton, names: Array, want: String, kb_default: bool) -> void:
	o.clear()
	var choices: Array = [["none", "Nothing"], ["keyboard", "Keyboard"]]
	for i in names.size():
		var n: String = names[i]
		choices.append(["joy:" + n, "Gamepad %d: %s" % [i + 1, n.get_slice(" #", 0)]])
		choices.append(["keyboard+joy:" + n, "Keyboard + Gamepad %d" % (i + 1)])
	var pick := -1
	for i in choices.size():
		o.add_item(choices[i][1])
		o.set_item_metadata(i, choices[i][0])
		if choices[i][0] == want:
			pick = i
	if pick < 0:
		pick = 1 if kb_default else 0   # chosen gamepad unplugged: fall back
	o.select(pick)


func _save_sources() -> void:
	_settings.set_value("input", "gamepad1", String(g1_source.get_item_metadata(g1_source.selected)))
	_settings.set_value("input", "gamepad2", String(g2_source.get_item_metadata(g2_source.selected)))
	_settings.save(SETTINGS_PATH)


## Reads one FTC gamepad from the source chosen in its picker.
func _read_source(o: OptionButton) -> Dictionary:
	if o.selected < 0:
		return GamepadInput.empty()
	var src := String(o.get_item_metadata(o.selected))
	var use_kb := src.begins_with("keyboard") and (headless or get_window().has_focus())
	var dev := -1
	var at := src.find("joy:")
	if at >= 0:
		var want := src.substr(at + 4)
		var keys := _joy_keys()
		for j in keys:
			if keys[j] == want:
				dev = j
				break
	return GamepadInput.read(dev, use_kb)


## device id -> stable key: the gamepad's name, plus " #2", " #3"... for identical models.
func _joy_keys() -> Dictionary:
	var keys := {}
	var seen := {}
	for j in _known_joys:
		var n := Input.get_joy_name(j)
		seen[n] = int(seen.get(n, 0)) + 1
		keys[j] = n if seen[n] == 1 else "%s #%d" % [n, seen[n]]
	return keys


func _source_text(o: OptionButton) -> String:
	return o.get_item_text(o.selected) if o.selected >= 0 else "Nothing"


func _section(text: String) -> Label:
	var l := Label.new()
	l.text = text.to_upper()
	l.add_theme_font_size_override("font_size", 11)
	l.modulate = Color("#9aa4b2")
	return l


func _button(text: String, cb: Callable) -> Button:
	var b := Button.new()
	b.text = text
	b.focus_mode = Control.FOCUS_NONE
	b.pressed.connect(cb)
	return b


func _box(color: String) -> StyleBoxFlat:
	var s := StyleBoxFlat.new()
	s.bg_color = Color(color)
	s.content_margin_left = 6
	s.content_margin_right = 6
	s.content_margin_top = 4
	s.content_margin_bottom = 4
	return s


func _mono() -> Font:
	var f := SystemFont.new()
	f.font_names = PackedStringArray(["JetBrains Mono", "Consolas", "Menlo", "DejaVu Sans Mono", "monospace"])
	return f


func _refresh_opmode_list() -> void:
	var prev := opmode_select.get_item_text(opmode_select.selected) if opmode_select.selected >= 0 else ""
	opmode_select.clear()
	if opmodes.is_empty():
		opmode_select.add_item("(no OpModes - bridge not connected)")
		opmode_select.disabled = true
		return
	opmode_select.disabled = false
	var idx := 0
	var select := 0
	for kind in ["Autonomous", "TeleOp"]:
		for om in opmodes:
			if om.get("kind") != kind:
				continue
			opmode_select.add_item("%s  ·  %s" % [kind, String(om["name"]).strip_edges()])
			opmode_select.set_item_metadata(idx, om["name"])
			if opmode_select.get_item_text(idx) == prev:
				select = idx
			idx += 1
	opmode_select.select(select)


func _log(msg: String) -> void:
	if log_box:
		log_box.append_text(msg + "\n")
	if headless:
		print("[sim] " + _strip_bbcode(msg))


func _strip_bbcode(s: String) -> String:
	var re := RegEx.new()
	re.compile("\\[/?[a-z]+(=[^\\]]*)?\\]")
	return re.sub(s, "", true)


# ================================================================== buttons

func _selected_opmode() -> String:
	if opmode_select.disabled or opmode_select.selected < 0:
		return ""
	var md = opmode_select.get_item_metadata(opmode_select.selected)
	return String(md) if md != null else ""


func _on_init() -> void:
	var om_name := _selected_opmode()
	if om_name == "":
		return
	init_opmode(om_name)


func init_opmode(om_name: String) -> void:
	telemetry = []
	last_error = ""
	bridge.send({"type": "cmd", "cmd": "init", "opmode": om_name})


func _on_start() -> void:
	bridge.send({"type": "cmd", "cmd": "start"})


func _on_stop() -> void:
	bridge.send({"type": "cmd", "cmd": "stop"})


func _on_reset_robot() -> void:
	robot.reset_pose()


# ================================================================== main loop

func _physics_process(delta: float) -> void:
	sim_time += delta
	var ev: String = bridge.poll()
	if ev == "connected":
		_log("[color=#a5d6a7]Bridge connected.[/color]")
		bridge.send({"type": "hello", "devices": robot_cfg.get("devices", []), "hub": robot_cfg.get("hub", {})})
	elif ev == "disconnected":
		_log("[color=#ffcc80]Bridge disconnected. Restart it after changing code; this window keeps waiting.[/color]")
		opmodes = []
		state = "IDLE"
		robot.apply_outputs({})
		_refresh_opmode_list()

	for msg in bridge.take_messages():
		_handle(msg)

	var g1: Dictionary = _read_source(g1_source)
	var g2: Dictionary = _read_source(g2_source)
	if not forced_input.is_empty() and state == "RUNNING":
		GamepadInput.apply_overrides(g1, forced_input)

	robot.step(delta)

	if state == "RUNNING":
		run_time += delta
		var limit := AUTO_SECONDS if active_kind == "Autonomous" else TELEOP_SECONDS
		if auto_stop_check.button_pressed and run_time >= limit and not cli.has("opmode"):
			_log("Match time over - STOP.")
			bridge.send({"type": "cmd", "cmd": "stop", "reason": "match timer"})

	bridge.send({
		"type": "state", "t": sim_time,
		"heading": robot.heading_deg(), "angvel": rad_to_deg(robot.omega),
		"motors": robot.encoder_state(),
		"distance": robot.distance_readings, "touch": robot.touch_readings,
		"gp1": g1, "gp2": g2,
	})

	field_view.locked = state == "RUNNING" or state == "INIT"
	field_view.queue_redraw()
	_ui_tick += 1
	if _ui_tick % 3 == 0:
		_update_ui(g1, g2)
	_headless_step(delta)
	if cli.has("screenshot") and not _shot_taken and sim_time >= float(cli.get("screenshot-at", "3")):
		_shot_taken = true
		var img := get_viewport().get_texture().get_image()
		img.save_png(String(cli["screenshot"]))
		print("[sim] screenshot saved to " + String(cli["screenshot"]))


func _handle(msg: Dictionary) -> void:
	match msg.get("type", ""):
		"opmodes":
			opmodes = msg.get("list", [])
			_refresh_opmode_list()
			_log("Bridge found %d OpModes." % opmodes.size())
			for p in msg.get("problems", []):
				_log("[color=#ffcc80]%s[/color]" % p)
		"out":
			robot.apply_outputs(msg)
		"telemetry":
			telemetry = PackedStringArray(msg.get("lines", []))
		"status":
			var prev := state
			state = msg.get("state", "IDLE")
			active_name = String(msg.get("opmode", "")).strip_edges()
			active_kind = msg.get("kind", "")
			if state == "RUNNING" and prev != "RUNNING":
				run_time = 0.0
			if state == "INIT" and prev != "INIT":
				run_time = 0.0
			if msg.has("error"):
				last_error = msg["error"]
				_log("[color=#ff8a80]%s[/color]" % last_error)
		"log":
			var text: String = msg.get("msg", "")
			var color := "#ff8a80" if text.begins_with("ERROR") else ("#ffcc80" if ("Warning" in text or "IMU" in text or "outside" in text) else "#cfd8dc")
			_log("[color=%s]%s[/color]" % [color, text])


func _update_ui(g1: Dictionary, g2: Dictionary) -> void:
	var connected: bool = bridge.is_connected_to_bridge()
	conn_label.text = "● Bridge connected" if connected else "○ Waiting for bridge on port %d…" % bridge.port
	conn_label.modulate = Color("#a5d6a7") if connected else Color("#ffcc80")

	init_btn.disabled = not connected or opmodes.is_empty() or state == "RUNNING" or state == "STOPPING"
	start_btn.disabled = not connected or state != "INIT"
	stop_btn.disabled = not connected or not (state == "INIT" or state == "RUNNING")

	var shown_state := state
	if active_name != "" and state != "IDLE":
		shown_state = "%s  %s" % [state, active_name]
	state_label.text = shown_state
	state_label.modulate = {"RUNNING": Color("#a5d6a7"), "INIT": Color("#90caf9"), "ERROR": Color("#ff8a80")}.get(state, Color.WHITE)

	var limit := AUTO_SECONDS if active_kind == "Autonomous" else TELEOP_SECONDS
	var shown := run_time if state == "RUNNING" or state == "STOPPED" or state == "STOPPING" else 0.0
	timer_label.text = "%d:%04.1f / %d:%02d" % [int(shown) / 60, fmod(shown, 60.0), int(limit) / 60, int(limit) % 60]

	var tel := "\n".join(telemetry)
	if state == "ERROR" and last_error != "":
		tel = "[ERROR]\n" + last_error + ("\n\n" + tel if tel != "" else "")
	if telemetry_box.text != tel:
		telemetry_box.text = tel

	var lines: PackedStringArray = []
	for n in robot.motors:
		var m: Dictionary = robot.motors[n]
		if m["wheel"] != "":
			continue
		lines.append("%-13s %s %+.2f  %5.0f rpm" % [n, _bar(m["power"]), m["power"], m["speed"] * m["max_rpm"]])
	for n in robot.crservos:
		lines.append("%-13s %s %+.2f  (CR servo)" % [n, _bar(robot.crservos[n]), robot.crservos[n]])
	for n in robot.servos:
		var p: float = robot.servos[n]
		lines.append("%-13s pos %s" % [n, "--" if is_nan(p) else "%.3f" % p])
	mech_label.text = "\n".join(lines) if not lines.is_empty() else "(none)"

	var dl: PackedStringArray = []
	for k in ["FL", "FR", "BL", "BR"]:
		if robot.drive.has(k):
			dl.append("%s %+.2f" % [k, robot.motors[robot.drive[k]]["power"]])
	pose_label.text = "x %7.1f in   y %7.1f in   heading %6.1f°\nspeed %5.1f in/s   turn %6.1f °/s%s\nraw power  %s" % [
		robot.pos.x, robot.pos.y, wrapf(robot.heading_deg(), -180, 180), robot.velocity.length(),
		rad_to_deg(robot.omega), "   (touching)" if robot.in_contact else "", "  ".join(dl)]

	var pad := ""
	if _known_joys.is_empty():
		pad = "No gamepad detected (plug one in; Logitech: set the back switch to X)."
	var active: PackedStringArray = []
	for k in ["lx", "ly", "rx", "ry", "lt", "rt"]:
		if absf(g1[k]) > 0.01:
			active.append("%s %+.2f" % [k, g1[k]])
	for b in g1["buttons"]:
		active.append(String(b))
	var active2: PackedStringArray = []
	for k in ["lx", "ly", "rx", "ry", "lt", "rt"]:
		if absf(g2[k]) > 0.01:
			active2.append("%s %+.2f" % [k, g2[k]])
	for b in g2["buttons"]:
		active2.append(String(b))
	gamepad_label.text = (pad + "\n" if pad != "" else "") + "gamepad1 input: " + (", ".join(active) if not active.is_empty() else "-") \
		+ "\ngamepad2 input: " + (", ".join(active2) if not active2.is_empty() else "-") + "\n\n" + GamepadInput.HELP

	field_view.status_text = ""


func _bar(v: float) -> String:
	var n := int(round(absf(v) * 8))
	var empty := "·".repeat(8)
	if v >= 0:
		return empty + "|" + "█".repeat(n) + "·".repeat(8 - n)
	return "·".repeat(8 - n) + "█".repeat(n) + "|" + empty


# ================================================================== headless test

func _headless_step(delta: float) -> void:
	if test_phase == "":
		return
	test_timer += delta
	var seconds := float(cli.get("seconds", "30"))
	match test_phase:
		"wait_bridge":
			if not opmodes.is_empty():
				var target := String(cli["opmode"])
				var found := ""
				for om in opmodes:
					if String(om["name"]).strip_edges() == target.strip_edges() or String(om.get("class", "")).ends_with("." + target):
						found = om["name"]
				if found == "":
					print("[sim] OpMode '%s' not found. Available: %s" % [target, ", ".join(PackedStringArray(opmodes.map(func(o): return String(o["name"]).strip_edges())))])
					_finish(2)
					return
				init_opmode(found)
				test_phase = "init"
				test_timer = 0.0
			elif test_timer > float(cli.get("wait", "30")):
				print("[sim] No bridge connected after %s s." % cli.get("wait", "30"))
				_finish(3)
		"init":
			if test_timer > 0.5:
				if state == "ERROR":
					_finish(1)
					return
				_on_start()
				test_phase = "run"
				test_timer = 0.0
		"run":
			_print_timer += delta
			if trace_file:
				trace_file.store_line("%.3f,%.2f,%.2f,%.2f,%s" % [test_timer, robot.pos.x, robot.pos.y, wrapf(robot.heading_deg(), -180, 180), state])
			if _print_timer >= 0.5:
				_print_timer = 0.0
				_print_status()
			if state == "ERROR":
				_print_status()
				_finish(1)
				return
			if test_timer >= seconds or state == "STOPPED":
				_on_stop()
				test_phase = "stopping"
				test_timer = 0.0
		"stopping":
			if state == "STOPPED" or state == "ERROR" or test_timer > 3.0:
				_print_status()
				print("[sim] final pose: x=%.1f y=%.1f heading=%.1f  (start x=%.1f y=%.1f heading=%.1f)" % [
					robot.pos.x, robot.pos.y, wrapf(robot.heading_deg(), -180, 180),
					float(robot.start_pose["x"]), float(robot.start_pose["y"]), float(robot.start_pose["heading"])])
				_finish(1 if state == "ERROR" else 0)


func _print_status() -> void:
	print("[sim] t=%5.1f  %-8s pose x=%7.1f y=%7.1f h=%7.1f  speed=%5.1f" % [
		test_timer, state, robot.pos.x, robot.pos.y, wrapf(robot.heading_deg(), -180, 180), robot.velocity.length()])
	var tel := " | ".join(telemetry)
	if tel != _last_printed_tel and tel != "":
		_last_printed_tel = tel
		print("[sim]   telemetry: " + tel)


func _finish(code: int) -> void:
	test_phase = ""
	if trace_file:
		trace_file.close()
	bridge.close()
	get_tree().quit(code)
