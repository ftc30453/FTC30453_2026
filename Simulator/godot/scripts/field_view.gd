extends Control
## Draws the field, field elements, trail and robot. Drag the robot to move it and use the
## mouse wheel to rotate it while no OpMode is running.

signal robot_moved

const RobotSim := preload("res://scripts/robot_sim.gd")

var robot      # RobotSim instance
var field_cfg: Dictionary = {}
var show_trail := true
var locked := false       # true while an OpMode is running
var status_text := ""

var _dragging := false
var _drag_offset := Vector2.ZERO
var _mouse_field := Vector2(INF, INF)
var _element_polys: Array = []
var _half := Vector2(76, 76)   # half the visible area in inches: the field plus anything drawn outside the walls


func _ready() -> void:
	mouse_filter = Control.MOUSE_FILTER_STOP
	clip_contents = true


func set_field(cfg: Dictionary) -> void:
	field_cfg = cfg
	_element_polys.clear()
	var ext := Vector2(72, 72)
	for e in cfg.get("elements", []):
		var poly := RobotSim.element_polygon(e)
		var mid := Vector2.ZERO
		for p in poly:
			ext = Vector2(maxf(ext.x, absf(p.x)), maxf(ext.y, absf(p.y)))
			mid += p
		if poly.size() > 0:
			mid /= poly.size()
		_element_polys.append({"e": e, "poly": poly, "label_at": mid})
	_half = ext + Vector2(4, 4)
	queue_redraw()


# ---------------------------------------------------------------- coordinates

func _scale() -> float:
	return minf(size.x / (2.0 * _half.x), size.y / (2.0 * _half.y))


func _center() -> Vector2:
	return size / 2.0


func f2s(p: Vector2) -> Vector2:
	return _center() + Vector2(p.x, -p.y) * _scale()


func s2f(p: Vector2) -> Vector2:
	var d := (p - _center()) / _scale()
	return Vector2(d.x, -d.y)


func _poly_to_screen(poly: PackedVector2Array) -> PackedVector2Array:
	var out := PackedVector2Array()
	for p in poly:
		out.append(f2s(p))
	return out


# ---------------------------------------------------------------- input

func _gui_input(event: InputEvent) -> void:
	if robot == null:
		return
	if event is InputEventMouseMotion:
		_mouse_field = s2f(event.position)
		if _dragging and not locked:
			robot.set_pose(_mouse_field + _drag_offset, robot.heading)
			robot_moved.emit()
		queue_redraw()
	elif event is InputEventMouseButton:
		var fp := s2f(event.position)
		if event.button_index == MOUSE_BUTTON_LEFT:
			if event.pressed and not locked and Geometry2D.is_point_in_polygon(fp, robot.corners()):
				_dragging = true
				_drag_offset = robot.pos - fp
			elif not event.pressed:
				_dragging = false
		elif event.pressed and not locked and (event.button_index == MOUSE_BUTTON_WHEEL_UP or event.button_index == MOUSE_BUTTON_WHEEL_DOWN):
			var stepdeg := 1.0 if event.shift_pressed else 15.0
			var dir := 1.0 if event.button_index == MOUSE_BUTTON_WHEEL_UP else -1.0
			var h := snappedf(robot.heading_deg() + dir * stepdeg, stepdeg)
			robot.set_pose(robot.pos, deg_to_rad(h))
			robot_moved.emit()
			queue_redraw()


func _notification(what: int) -> void:
	if what == NOTIFICATION_MOUSE_EXIT:
		_mouse_field = Vector2(INF, INF)
		queue_redraw()


# ---------------------------------------------------------------- drawing

func _draw() -> void:
	var font := get_theme_default_font()
	draw_rect(Rect2(Vector2.ZERO, size), Color("#1b1d22"))
	var s := _scale()

	# tiles
	for i in 6:
		for j in 6:
			var tl := f2s(Vector2(-72 + i * 24, 72 - j * 24))
			var c := Color("#5d6168") if (i + j) % 2 == 0 else Color("#575b62")
			draw_rect(Rect2(tl, Vector2(24, 24) * s), c)
	for k in 7:
		var v := -72.0 + k * 24.0
		draw_line(f2s(Vector2(v, -72)), f2s(Vector2(v, 72)), Color("#43464c"), 1.0)
		draw_line(f2s(Vector2(-72, v)), f2s(Vector2(72, v)), Color("#43464c"), 1.0)

	# elements
	for ep in _element_polys:
		var e: Dictionary = ep["e"]
		var poly: PackedVector2Array = ep["poly"]
		if poly.size() < 3:
			continue
		var col := Color.html(e.get("color", "#888888aa"))
		var sp := _poly_to_screen(poly)
		draw_colored_polygon(sp, col)
		var outline := sp.duplicate()
		outline.append(sp[0])
		draw_polyline(outline, col.lightened(0.35) if e.get("solid", false) else col.lightened(0.2), 1.5 if e.get("solid", false) else 1.0)
	# labels after all shapes, so small elements drawn later don't cover them
	for ep in _element_polys:
		var e: Dictionary = ep["e"]
		if e.get("name", "") != "":
			var label_pos := f2s(ep["label_at"])
			draw_string(font, label_pos + Vector2(-60, 4), e["name"], HORIZONTAL_ALIGNMENT_CENTER, 120, 10, Color(1, 1, 1, 0.85))

	# walls
	var wall := PackedVector2Array([f2s(Vector2(-72, 72)), f2s(Vector2(72, 72)), f2s(Vector2(72, -72)), f2s(Vector2(-72, -72)), f2s(Vector2(-72, 72))])
	draw_polyline(wall, Color("#d4d7dc"), 3.0)

	# axes + audience label
	var o := f2s(Vector2.ZERO)
	draw_line(o, f2s(Vector2(10, 0)), Color("#ff6b6b"), 2.0)
	draw_line(o, f2s(Vector2(0, 10)), Color("#6bff8f"), 2.0)
	draw_string(font, f2s(Vector2(11, -1.5)), "+x", HORIZONTAL_ALIGNMENT_LEFT, -1, 10, Color("#ff6b6b"))
	draw_string(font, f2s(Vector2(-2, 11.5)), "+y", HORIZONTAL_ALIGNMENT_LEFT, -1, 10, Color("#6bff8f"))
	draw_string(font, f2s(Vector2(-72, -73.5)) + Vector2(0, 10), "AUDIENCE SIDE", HORIZONTAL_ALIGNMENT_CENTER, 144 * s, 11, Color(1, 1, 1, 0.5))

	if robot == null:
		return

	# trail
	if show_trail and robot.trail.size() > 1:
		var tp := _poly_to_screen(robot.trail)
		tp.append(f2s(robot.pos))
		draw_polyline(tp, Color(1.0, 0.82, 0.3, 0.75), 2.0)

	# sensors
	for sd in robot.sensors:
		var p: Array = sd.get("pos", [0, 0])
		var origin: Vector2 = robot.to_field(float(p[0]), float(p[1]))
		if sd.get("type") == "distance":
			var d: float = robot.distance_readings.get(sd["name"], 1.0e9)
			var dir := Vector2.from_angle(robot.heading + deg_to_rad(float(sd.get("dir_deg", 0))))
			var reach := minf(d, float(sd.get("max_range_in", 80)))
			draw_line(f2s(origin), f2s(origin + dir * reach), Color(0.4, 1, 1, 0.5), 1.0)
		else:
			var hit: bool = robot.touch_readings.get(sd["name"], false)
			draw_circle(f2s(origin), 3.0, Color("#ffd24d") if hit else Color("#888"))

	# robot body
	var body: PackedVector2Array = _poly_to_screen(robot.corners())
	draw_colored_polygon(body, Color(0.15, 0.45, 0.85, 0.55))
	var outline := body.duplicate()
	outline.append(body[0])
	draw_polyline(outline, Color("#ff7043") if robot.in_contact else Color.WHITE, 2.0)

	# wheels, colored by commanded wheel direction (green = wheel rolls forward)
	var wp: Dictionary = robot.wheel_positions()
	for k in wp:
		var mname: String = robot.drive.get(k, "")
		var col := Color("#3a3a3a")
		if mname != "":
			var m: Dictionary = robot.motors[mname]
			var w: float = m["speed"] * (-1.0 if m["mirrored"] else 1.0)
			col = Color("#3a3a3a").lerp(Color("#4cff6a") if w > 0 else Color("#ff4c4c"), clampf(absf(w), 0, 1))
		var c: Vector2 = f2s(wp[k])
		var fwd: Vector2 = Vector2(robot.forward_vec().x, -robot.forward_vec().y) * 2.0 * s
		var lft: Vector2 = Vector2(robot.left_vec().x, -robot.left_vec().y) * 0.75 * s
		draw_colored_polygon(PackedVector2Array([c + fwd + lft, c - fwd + lft, c - fwd - lft, c + fwd - lft]), col)

	# front arrow
	var tip: Vector2 = robot.to_field(robot.length / 2.0 - 1.0, 0)
	var base_l: Vector2 = robot.to_field(robot.length / 2.0 - 6.0, 3.5)
	var base_r: Vector2 = robot.to_field(robot.length / 2.0 - 6.0, -3.5)
	draw_colored_polygon(PackedVector2Array([f2s(tip), f2s(base_l), f2s(base_r)]), Color("#ffd24d"))

	# overlays
	if field_cfg.get("placeholder", false):
		draw_string(font, Vector2(10, 18), field_cfg.get("placeholder_note", "Placeholder field layout"), HORIZONTAL_ALIGNMENT_LEFT, size.x - 20, 12, Color("#ffcc66"))
	if status_text != "":
		draw_string(font, Vector2(10, 36), status_text, HORIZONTAL_ALIGNMENT_LEFT, size.x - 20, 12, Color(1, 1, 1, 0.8))
	if _mouse_field.x != INF and absf(_mouse_field.x) <= _half.x and absf(_mouse_field.y) <= _half.y:
		draw_string(font, Vector2(10, size.y - 10), "x %.1f  y %.1f in" % [_mouse_field.x, _mouse_field.y], HORIZONTAL_ALIGNMENT_LEFT, -1, 12, Color(1, 1, 1, 0.7))
	if not locked:
		draw_string(font, Vector2(size.x - 330, size.y - 10), "Drag robot to move, mouse wheel to rotate (Shift = 1°)", HORIZONTAL_ALIGNMENT_RIGHT, 320, 11, Color(1, 1, 1, 0.5))
