package sim;

import com.qualcomm.robotcore.hardware.Gamepad;

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Entry point of the bridge. Connects to the Godot simulator, lists the team's OpModes, and
 * runs whichever one the simulator's Driver Station panel selects.
 *
 * Usage: java -cp build/classes sim.SimMain [--host 127.0.0.1] [--port 7777]
 */
public final class SimMain {
    private static volatile SimLink currentLink;

    private SimMain() { }

    static void log(String msg) {
        System.out.println("[bridge] " + msg);
        SimLink l = currentLink;
        if (l != null) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "log");
            m.put("msg", msg);
            l.send(m);
        }
    }

    public static void main(String[] args) throws InterruptedException {
        String host = "127.0.0.1";
        int port = 7777;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--host") && i + 1 < args.length) host = args[++i];
            else if (args[i].equals("--port") && i + 1 < args.length) port = Integer.parseInt(args[++i]);
        }

        OpModeRegistry registry = OpModeRegistry.scan();
        System.out.println("[bridge] Found " + registry.entries.size() + " OpModes:");
        for (OpModeRegistry.Entry e : registry.entries) {
            System.out.println("[bridge]   " + e.kind + "  \"" + e.name + "\"  (" + e.cls.getName() + ")");
        }
        for (String p : registry.problems) System.out.println("[bridge] Warning: " + p);

        boolean announcedWait = false;
        while (true) {
            Socket socket;
            try {
                socket = new Socket(host, port);
            } catch (ConnectException e) {
                if (!announcedWait) {
                    System.out.println("[bridge] Waiting for the simulator on " + host + ":" + port
                            + " - start the Godot project (it keeps retrying).");
                    announcedWait = true;
                }
                Thread.sleep(1000);
                continue;
            } catch (IOException e) {
                System.out.println("[bridge] Connection problem: " + e);
                Thread.sleep(1000);
                continue;
            }
            announcedWait = false;
            runSession(socket, registry);
            System.out.println("[bridge] Simulator disconnected. Waiting for it to come back...");
        }
    }

    private static void runSession(Socket socket, OpModeRegistry registry) {
        SimLink link;
        try {
            link = new SimLink(socket);
        } catch (IOException e) {
            System.out.println("[bridge] " + e);
            return;
        }
        currentLink = link;
        System.out.println("[bridge] Connected to simulator.");
        SimHardware hw = new SimHardware(SimMain::log);
        OpModeRunner runner = new OpModeRunner(hw, link, registry);

        List<Object> list = new ArrayList<>();
        for (OpModeRegistry.Entry e : registry.entries) list.add(e.toJson());
        Map<String, Object> hello = new LinkedHashMap<>();
        hello.put("type", "opmodes");
        hello.put("list", list);
        hello.put("problems", new ArrayList<>(registry.problems));
        link.send(hello);

        try {
            Map<String, Object> msg;
            while ((msg = link.read()) != null) {
                String type = Json.str(msg.get("type"), "");
                switch (type) {
                    case "hello":
                        hw.configure(msg);
                        break;
                    case "state":
                        hw.updateFromState(msg);
                        applyGamepad(runner.gamepad1, Json.obj(msg.get("gp1")));
                        applyGamepad(runner.gamepad2, Json.obj(msg.get("gp2")));
                        runner.tick();
                        link.send(hw.outputs());
                        break;
                    case "cmd":
                        String cmd = Json.str(msg.get("cmd"), "");
                        if (cmd.equals("init")) runner.init(Json.str(msg.get("opmode"), ""));
                        else if (cmd.equals("start")) runner.start();
                        else if (cmd.equals("stop")) runner.stop(Json.str(msg.get("reason"), null));
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("[bridge] " + e.getMessage());
        } finally {
            runner.shutdown();
            currentLink = null;
            link.close();
        }
    }

    private static void applyGamepad(Gamepad g, Map<String, Object> m) {
        if (m.isEmpty()) return;
        g.left_stick_x = (float) Json.num(m.get("lx"), 0);
        g.left_stick_y = (float) Json.num(m.get("ly"), 0);
        g.right_stick_x = (float) Json.num(m.get("rx"), 0);
        g.right_stick_y = (float) Json.num(m.get("ry"), 0);
        g.left_trigger = (float) Json.num(m.get("lt"), 0);
        g.right_trigger = (float) Json.num(m.get("rt"), 0);
        Set<String> b = new HashSet<>();
        for (Object o : Json.arr(m.get("buttons"))) b.add(String.valueOf(o));
        g.a = b.contains("a");
        g.b = b.contains("b");
        g.x = b.contains("x");
        g.y = b.contains("y");
        g.dpad_up = b.contains("dpad_up");
        g.dpad_down = b.contains("dpad_down");
        g.dpad_left = b.contains("dpad_left");
        g.dpad_right = b.contains("dpad_right");
        g.left_bumper = b.contains("left_bumper");
        g.right_bumper = b.contains("right_bumper");
        g.left_stick_button = b.contains("left_stick_button");
        g.right_stick_button = b.contains("right_stick_button");
        g.back = b.contains("back");
        g.start = b.contains("start");
        g.guide = b.contains("guide");
        g.syncAliases();
    }
}
