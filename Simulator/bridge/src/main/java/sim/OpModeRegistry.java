package sim;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/** Finds every class annotated @TeleOp or @Autonomous (and not @Disabled), like the Driver Station list. */
final class OpModeRegistry {

    static final class Entry {
        final String name;
        final String group;
        final String kind;
        final Class<? extends OpMode> cls;

        Entry(String name, String group, String kind, Class<? extends OpMode> cls) {
            this.name = name; this.group = group; this.kind = kind; this.cls = cls;
        }

        Map<String, Object> toJson() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", name);
            m.put("group", group);
            m.put("kind", kind);
            m.put("class", cls.getName());
            m.put("linear", com.qualcomm.robotcore.eventloop.opmode.LinearOpMode.class.isAssignableFrom(cls));
            return m;
        }
    }

    final List<Entry> entries = new ArrayList<>();
    final List<String> problems = new ArrayList<>();

    static OpModeRegistry scan() {
        OpModeRegistry r = new OpModeRegistry();
        List<String> classNames = new ArrayList<>();
        try {
            URL loc = OpModeRegistry.class.getProtectionDomain().getCodeSource().getLocation();
            Path p = Paths.get(loc.toURI());
            if (Files.isDirectory(p)) {
                try (Stream<Path> s = Files.walk(p)) {
                    s.filter(f -> f.toString().endsWith(".class")).forEach(f -> {
                        String rel = p.relativize(f).toString().replace('\\', '/');
                        classNames.add(rel.substring(0, rel.length() - 6).replace('/', '.'));
                    });
                }
            } else {
                try (JarFile jar = new JarFile(p.toFile())) {
                    for (JarEntry e : java.util.Collections.list(jar.entries())) {
                        if (e.getName().endsWith(".class")) {
                            classNames.add(e.getName().substring(0, e.getName().length() - 6).replace('/', '.'));
                        }
                    }
                }
            }
        } catch (IOException | URISyntaxException e) {
            r.problems.add("Could not list classes: " + e);
        }

        ClassLoader cl = OpModeRegistry.class.getClassLoader();
        for (String cn : classNames) {
            if (cn.startsWith("sim.") || cn.startsWith("com.qualcomm.") || cn.startsWith("org.firstinspires.ftc.robotcore.")) continue;
            if (cn.contains("$")) continue;
            Class<?> c;
            try {
                c = Class.forName(cn, false, cl);
            } catch (Throwable t) {
                r.problems.add("Could not load " + cn + ": " + t);
                continue;
            }
            TeleOp t = c.getAnnotation(TeleOp.class);
            Autonomous a = c.getAnnotation(Autonomous.class);
            if (t == null && a == null) continue;
            if (c.getAnnotation(Disabled.class) != null) continue;
            if (!OpMode.class.isAssignableFrom(c)) {
                r.problems.add(cn + " has @" + (t != null ? "TeleOp" : "Autonomous") + " but doesn't extend OpMode or LinearOpMode");
                continue;
            }
            if (Modifier.isAbstract(c.getModifiers())) continue;
            @SuppressWarnings("unchecked")
            Class<? extends OpMode> oc = (Class<? extends OpMode>) c;
            String name = t != null ? t.name() : a.name();
            String group = t != null ? t.group() : a.group();
            if (name.isEmpty()) name = c.getSimpleName();
            r.entries.add(new Entry(name, group, t != null ? "TeleOp" : "Autonomous", oc));
        }
        r.entries.sort(Comparator.comparing((Entry e) -> e.kind).thenComparing(e -> e.name.trim().toLowerCase()));

        Map<String, Entry> seen = new LinkedHashMap<>();
        for (Entry e : r.entries) {
            Entry prev = seen.put(e.name, e);
            if (prev != null) {
                r.problems.add("Two OpModes are both named \"" + e.name + "\" (" + prev.cls.getSimpleName() + ", "
                        + e.cls.getSimpleName() + "). The real robot app refuses to start with duplicate names.");
            }
        }
        return r;
    }

    Entry find(String name) {
        for (Entry e : entries) if (e.name.equals(name)) return e;
        for (Entry e : entries) if (e.cls.getSimpleName().equals(name) || e.name.trim().equalsIgnoreCase(name.trim())) return e;
        return null;
    }
}
