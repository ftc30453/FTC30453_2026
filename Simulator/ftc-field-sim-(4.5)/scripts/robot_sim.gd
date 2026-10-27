extends RefCounted
## Mecanum robot physics on a 144 x 144 in field.
##
## Field frame: origin at field center, +x right, +y up (away from the audience), inches.
## Heading: radians, counter-clockwise from +x.
## Robot frame: forward = +a, left = +b.

const FIELD_HALF := 72.0
const WHEELS := ["FL", "FR", "BL", "BR"]

var errors: PackedStringArray = []

# pose
var pos := Vector2.ZERO
var heading := 0.0          # radians, unwrapped
var velocity := Vector2.ZERO   # in/s, field frame
var omega := 0.0            # rad/s CCW
var in_contact := false

# geometry
var length := 18.0
var width := 18.0
var half_track := 7.5
var half_base := 6.5
var wheel_circ_in := PI * 104.0 / 25.4
var strafe_eff := 0.85
var start_pose := {"x": 0.0, "y": 0.0, "heading": 90.0}

# devices
var motors := {}       # name -> {power, brake, speed, pos, vel, max_tps, max_rpm, mirrored, wheel}
var drive := {}        # "FL" -> motor name
var crservos := {}     # name -> power
var servos := {}       # name -> position
var sensors := []      # distance / touch definitions
var distance_readings := {}
var touch_readings := {}

# field
var obstacles: Array = []     # Array[PackedVector2Array] (convex, solid)
var trail := PackedVector2Array()
var _trail_timer := 0.0


func setup(cfg: Dictionary, field: Dictionary) -> void:
	errors.clear()
	var size: Array = cfg.get("size_in", [18, 18])
	length = float(size[0])
	width = float(size[1])
	var dt: Dictionary = cfg.get("drivetrain", {})
	half_track = float(dt.get("track_width_in", 15.0)) / 2.0
	half_base = float(dt.get("wheelbase_in", 13.0)) / 2.0
	wheel_circ_in = PI * float(dt.get("wheel_diameter_mm", 104.0)) / 25.4
	strafe_eff = float(dt.get("strafe_efficiency", 0.85))
	start_pose = cfg.get("start_pose", start_pose)

	motors.clear()
	drive.clear()
	crservos.clear()
	servos.clear()
	sensors.clear()
	for d in cfg.get("devices", []):
		var n: String = d.get("name", "")
		match d.get("type", ""):
			"motor":
				var tpr := float(d.get("ticks_per_rev", 537.7))
				var rpm := float(d.get("max_rpm", 312.0))
				motors[n] = {
					"power": 0.0, "brake": true, "speed": 0.0, "pos": 0.0, "vel": 0.0,
					"max_tps": tpr * rpm / 60.0, "max_rpm": rpm,
					"mirrored": bool(d.get("mirrored", false)), "wheel": d.get("drive_wheel", ""),
				}
				if d.has("drive_wheel"):
					drive[d["drive_wheel"]] = n
			"crservo":
				crservos[n] = 0.0
			"servo":
				servos[n] = NAN
			"distance", "touch":
				sensors.append(d)
	for w in WHEELS:
		if not drive.has(w):
			errors.append("robot.json: no motor has \"drive_wheel\": \"%s\"" % w)

	obstacles.clear()
	var t := 40.0
	var h := FIELD_HALF
	obstacles.append(_rect_poly(Vector2(0, h + t / 2), Vector2(2 * h + 2 * t, t), 0))
	obstacles.append(_rect_poly(Vector2(0, -h - t / 2), Vector2(2 * h + 2 * t, t), 0))
	obstacles.append(_rect_poly(Vector2(h + t / 2, 0), Vector2(t, 2 * h + 2 * t), 0))
	obstacles.append(_rect_poly(Vector2(-h - t / 2, 0), Vector2(t, 2 * h + 2 * t), 0))
	for e in field.get("elements", []):
		if bool(e.get("solid", false)):
			var poly := element_polygon(e)
			if poly.size() >= 3:
				obstacles.append(poly)
	reset_pose()


func reset_pose() -> void:
	pos = Vector2(float(start_pose.get("x", 0)), float(start_pose.get("y", 0)))
	heading = deg_to_rad(float(start_pose.get("heading", 90)))
	velocity = Vector2.ZERO
	omega = 0.0
	for m in motors.values():
		m["speed"] = 0.0
	trail.clear()


func set_pose(p: Vector2, h: float) -> void:
	pos = p
	heading = h
	velocity = Vector2.ZERO
	omega = 0.0
	_resolve_collisions()


## Outputs from the bridge: {"motors": {name: {"p", "brake"}}, "crservos": {...}, "servos": {...}}
func apply_outputs(out: Dictionary) -> void:
	var mo: Dictionary = out.get("motors", {})
	for n in motors:
		var m: Dictionary = motors[n]
		if mo.has(n):
			m["power"] = clampf(float(mo[n].get("p", 0.0)), -1.0, 1.0)
			m["brake"] = bool(mo[n].get("brake", true))
		else:
			m["power"] = 0.0
	var cr: Dictionary = out.get("crservos", {})
	for n in crservos:
		crservos[n] = clampf(float(cr.get(n, 0.0)), -1.0, 1.0)
	var sv: Dictionary = out.get("servos", {})
	for n in sv:
		servos[n] = float(sv[n])


func step(dt: float) -> void:
	# Motors: first-order response toward commanded power. Encoders follow the motor shaft.
	for m in motors.values():
		var target: float = m["power"]
		var tau := 0.09
		if absf(target) < 1e-6:
			tau = 0.05 if m["brake"] else 0.6
		m["speed"] += (target - m["speed"]) * (1.0 - exp(-dt / tau))
		m["vel"] = m["speed"] * m["max_tps"]
		m["pos"] += m["vel"] * dt

	if drive.size() == 4:
		var w := {}
		for k in WHEELS:
			var m: Dictionary = motors[drive[k]]
			var surface: float = m["speed"] * m["max_rpm"] / 60.0 * wheel_circ_in
			w[k] = -surface if m["mirrored"] else surface
		var fwd_speed: float = (w["FL"] + w["FR"] + w["BL"] + w["BR"]) / 4.0
		var strafe_right: float = (w["FL"] - w["FR"] - w["BL"] + w["BR"]) / 4.0 * strafe_eff
		var turn_cw: float = (w["FL"] - w["FR"] + w["BL"] - w["BR"]) / 4.0 / (half_track + half_base)
		var f := Vector2(cos(heading), sin(heading))
		var r := Vector2(sin(heading), -cos(heading))
		velocity = f * fwd_speed + r * strafe_right
		omega = -turn_cw
	else:
		velocity = Vector2.ZERO
		omega = 0.0

	var before := pos
	pos += velocity * dt
	heading += omega * dt
	_resolve_collisions()
	if dt > 0:
		velocity = (pos - before) / dt

	_trail_timer += dt
	if _trail_timer >= 0.05:
		_trail_timer = 0.0
		if trail.is_empty() or trail[trail.size() - 1].distance_to(pos) > 0.4:
			trail.append(pos)
			if trail.size() > 4000:
				trail.remove_at(0)
	_update_sensors()


func _resolve_collisions() -> void:
	in_contact = false
	for _i in 4:
		var moved := false
		var body := corners()
		for obs in obstacles:
			var mtv := sat_mtv(body, obs)
			if mtv != Vector2.ZERO:
				pos += mtv
				body = corners()
				in_contact = true
				moved = true
		if not moved:
			break


func heading_deg() -> float:
	return rad_to_deg(heading)


func forward_vec() -> Vector2:
	return Vector2(cos(heading), sin(heading))


func left_vec() -> Vector2:
	return Vector2(-sin(heading), cos(heading))


## Robot-frame point (forward a, left b) to field coordinates.
func to_field(a: float, b: float) -> Vector2:
	return pos + forward_vec() * a + left_vec() * b


func corners() -> PackedVector2Array:
	var a := length / 2.0
	var b := width / 2.0
	return PackedVector2Array([to_field(a, b), to_field(-a, b), to_field(-a, -b), to_field(a, -b)])


func wheel_positions() -> Dictionary:
	return {
		"FL": to_field(half_base, half_track), "FR": to_field(half_base, -half_track),
		"BL": to_field(-half_base, half_track), "BR": to_field(-half_base, -half_track),
	}


## Encoder state for the bridge: {name: [raw_ticks, raw_ticks_per_sec]}
func encoder_state() -> Dictionary:
	var s := {}
	for n in motors:
		s[n] = [motors[n]["pos"], motors[n]["vel"]]
	return s


func _update_sensors() -> void:
	distance_readings.clear()
	touch_readings.clear()
	for s in sensors:
		var p: Array = s.get("pos", [0, 0])
		var origin := to_field(float(p[0]), float(p[1]))
		if s.get("type") == "distance":
			var dir := Vector2.from_angle(heading + deg_to_rad(float(s.get("dir_deg", 0))))
			var d := raycast(origin, dir)
			var max_r := float(s.get("max_range_in", 80))
			distance_readings[s["name"]] = d if d <= max_r else 1.0e9
		else:
			var hit := false
			for obs in obstacles:
				if Geometry2D.is_point_in_polygon(origin, obs) or _dist_to_poly(origin, obs) < 0.6:
					hit = true
					break
			touch_readings[s["name"]] = hit


func raycast(origin: Vector2, dir: Vector2) -> float:
	var best := INF
	var far := origin + dir * 1000.0
	for obs in obstacles:
		var n: int = obs.size()
		for i in n:
			var hit = Geometry2D.segment_intersects_segment(origin, far, obs[i], obs[(i + 1) % n])
			if hit != null:
				best = minf(best, origin.distance_to(hit))
	return best


static func _dist_to_poly(p: Vector2, poly: PackedVector2Array) -> float:
	var best := INF
	var n := poly.size()
	for i in n:
		var c := Geometry2D.get_closest_point_to_segment(p, poly[i], poly[(i + 1) % n])
		best = minf(best, p.distance_to(c))
	return best


static func _rect_poly(center: Vector2, size: Vector2, rot_deg: float) -> PackedVector2Array:
	var hx := size.x / 2.0
	var hy := size.y / 2.0
	var r := deg_to_rad(rot_deg)
	var pts := PackedVector2Array()
	for c in [Vector2(-hx, -hy), Vector2(hx, -hy), Vector2(hx, hy), Vector2(-hx, hy)]:
		pts.append(center + (c as Vector2).rotated(r))
	return pts


static func element_polygon(e: Dictionary) -> PackedVector2Array:
	var c: Array = e.get("center", [0, 0])
	var center := Vector2(float(c[0]), float(c[1]))
	match e.get("shape", "rect"):
		"circle":
			var r := float(e.get("radius", 5))
			var pts := PackedVector2Array()
			for i in 20:
				pts.append(center + Vector2.from_angle(TAU * i / 20.0) * r)
			return pts
		"polygon":
			var pts := PackedVector2Array()
			for p in e.get("points", []):
				pts.append(Vector2(float(p[0]), float(p[1])))
			return pts
		_:
			var s: Array = e.get("size", [12, 12])
			return _rect_poly(center, Vector2(float(s[0]), float(s[1])), float(e.get("rotation", 0)))


## Minimum translation vector that pushes convex polygon a out of convex polygon b.
static func sat_mtv(a: PackedVector2Array, b: PackedVector2Array) -> Vector2:
	var min_overlap := INF
	var axis_best := Vector2.ZERO
	for poly in [a, b]:
		var n: int = poly.size()
		for i in n:
			var edge: Vector2 = poly[(i + 1) % n] - poly[i]
			var axis := Vector2(-edge.y, edge.x).normalized()
			var ra := _project(a, axis)
			var rb := _project(b, axis)
			var overlap := minf(ra.y, rb.y) - maxf(ra.x, rb.x)
			if overlap <= 0.0:
				return Vector2.ZERO
			if overlap < min_overlap:
				min_overlap = overlap
				axis_best = axis
	if (_centroid(a) - _centroid(b)).dot(axis_best) < 0:
		axis_best = -axis_best
	return axis_best * (min_overlap + 0.001)


static func _project(poly: PackedVector2Array, axis: Vector2) -> Vector2:
	var lo := INF
	var hi := -INF
	for p in poly:
		var d := p.dot(axis)
		lo = minf(lo, d)
		hi = maxf(hi, d)
	return Vector2(lo, hi)


static func _centroid(poly: PackedVector2Array) -> Vector2:
	var c := Vector2.ZERO
	for p in poly:
		c += p
	return c / float(poly.size())
