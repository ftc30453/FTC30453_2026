package sim;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Newline-delimited JSON over TCP to the Godot simulator. */
final class SimLink implements Closeable {
    private final Socket socket;
    private final BufferedReader in;
    private final Writer out;
    private volatile boolean open = true;

    SimLink(Socket socket) throws IOException {
        this.socket = socket;
        socket.setTcpNoDelay(true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8);
    }

    /** Blocks for the next message; null when the simulator disconnects. */
    Map<String, Object> read() throws IOException {
        while (true) {
            String line = in.readLine();
            if (line == null) return null;
            line = line.trim();
            if (line.isEmpty()) continue;
            try {
                return Json.obj(Json.parse(line));
            } catch (RuntimeException e) {
                System.err.println("[bridge] bad message from simulator: " + e.getMessage());
            }
        }
    }

    synchronized void send(Map<String, Object> msg) {
        if (!open) return;
        try {
            out.write(Json.write(msg));
            out.write('\n');
            out.flush();
        } catch (IOException e) {
            open = false;
        }
    }

    @Override public void close() {
        open = false;
        try { socket.close(); } catch (IOException ignored) { }
    }
}
