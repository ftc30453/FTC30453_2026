extends RefCounted
## TCP server the Java bridge connects to. Messages are one JSON object per line.

var port := 7777
var server := TCPServer.new()
var peer: StreamPeerTCP = null
var listening := false
var listen_error := ""
var _buffer := PackedByteArray()
var _inbox: Array = []


func start(p: int) -> void:
	port = p
	var err := server.listen(port, "127.0.0.1")
	listening = err == OK
	if not listening:
		listen_error = "Could not listen on port %d (error %d). Is another simulator already open?" % [port, err]


func is_connected_to_bridge() -> bool:
	return peer != null and peer.get_status() == StreamPeerTCP.STATUS_CONNECTED


## Returns "connected", "disconnected" or "" for this frame.
func poll() -> String:
	var event := ""
	if listening and server.is_connection_available():
		var incoming := server.take_connection()
		if peer != null and peer.get_status() == StreamPeerTCP.STATUS_CONNECTED:
			incoming.disconnect_from_host()   # only one bridge at a time
		else:
			peer = incoming
			peer.set_no_delay(true)
			_buffer.clear()
			event = "connected"
	if peer == null:
		return event
	peer.poll()
	var status := peer.get_status()
	if status != StreamPeerTCP.STATUS_CONNECTED:
		peer = null
		_buffer.clear()
		return "disconnected"
	var avail := peer.get_available_bytes()
	if avail > 0:
		var res := peer.get_data(avail)
		if res[0] == OK:
			_buffer.append_array(res[1])
			_split_lines()
	return event


func _split_lines() -> void:
	while true:
		var idx := _buffer.find(10)
		if idx < 0:
			break
		var line := _buffer.slice(0, idx).get_string_from_utf8().strip_edges()
		_buffer = _buffer.slice(idx + 1)
		if line.is_empty():
			continue
		var msg = JSON.parse_string(line)
		if msg is Dictionary:
			_inbox.append(msg)


func take_messages() -> Array:
	var m := _inbox
	_inbox = []
	return m


func send(msg: Dictionary) -> void:
	if not is_connected_to_bridge():
		return
	peer.put_data((JSON.stringify(msg) + "\n").to_utf8_buffer())


func close() -> void:
	if peer != null:
		peer.disconnect_from_host()
	server.stop()
