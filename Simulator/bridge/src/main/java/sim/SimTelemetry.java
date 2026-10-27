package sim;

import org.firstinspires.ftc.robotcore.external.Func;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Telemetry that behaves like the Driver Station: nothing shows until update() is called,
 * and update() clears the items (auto-clear) unless they are retained.
 */
final class SimTelemetry implements Telemetry {

    private final class ItemImpl implements Item {
        String caption;
        Object value;
        String format;
        Object[] args;
        Func<?> producer;
        boolean retained = false;
        final LineImpl parent;

        ItemImpl(LineImpl parent, String caption) { this.parent = parent; this.caption = caption; }

        String render() {
            String v;
            try {
                if (producer != null) {
                    Object pv = producer.value();
                    v = format != null ? String.format(Locale.US, format, pv) : String.valueOf(pv);
                } else if (format != null) {
                    v = String.format(Locale.US, format, args);
                } else {
                    v = String.valueOf(value);
                }
            } catch (RuntimeException e) {
                v = "<format error: " + e.getMessage() + ">";
            }
            return caption + captionSep + v;
        }

        @Override public String getCaption() { return caption; }
        @Override public Item setCaption(String c) { caption = c; return this; }
        @Override public Item setValue(String f, Object... a) { format = f; args = a; producer = null; return this; }
        @Override public Item setValue(Object v) { value = v; format = null; producer = null; return this; }
        @Override public <T> Item setValue(Func<T> p) { producer = p; return this; }
        @Override public Item setRetained(Boolean r) { retained = r != null && r; return this; }
        @Override public boolean isRetained() { return retained; }
        @Override public Item addData(String c, String f, Object... a) { return (parent != null ? parent : root()).addData(c, f, a); }
        @Override public Item addData(String c, Object v) { return (parent != null ? parent : root()).addData(c, v); }
    }

    private final class LineImpl implements Line {
        final String caption;
        final List<ItemImpl> items = new ArrayList<>();

        LineImpl(String caption) { this.caption = caption; }

        @Override public Item addData(String c, String f, Object... a) {
            ItemImpl i = new ItemImpl(this, c);
            i.setValue(f, a);
            items.add(i);
            return i;
        }

        @Override public Item addData(String c, Object v) {
            ItemImpl i = new ItemImpl(this, c);
            i.setValue(v);
            items.add(i);
            return i;
        }

        @Override public <T> Item addData(String c, Func<T> p) {
            ItemImpl i = new ItemImpl(this, c);
            i.setValue(p);
            items.add(i);
            return i;
        }

        String render() {
            StringBuilder sb = new StringBuilder(caption);
            for (int k = 0; k < items.size(); k++) {
                if (k > 0 || caption.length() > 0) sb.append(k > 0 ? itemSep : " ");
                sb.append(items.get(k).render());
            }
            return sb.toString();
        }
    }

    /** Top-level entries: either a standalone item (a line with no caption and one item) or a line. */
    private final List<LineImpl> entries = new ArrayList<>();
    private final List<String> logEntries = new ArrayList<>();
    private int logCapacity = 9;
    private boolean autoClear = true;
    private int msInterval = 250;
    private String itemSep = " | ";
    private String captionSep = " : ";
    private volatile List<String> published = new ArrayList<>();
    private volatile boolean dirty = false;

    private LineImpl rootLine;

    private LineImpl root() {
        LineImpl l = new LineImpl("");
        entries.add(l);
        return l;
    }

    private synchronized ItemImpl standalone(String caption) {
        LineImpl l = new LineImpl("");
        entries.add(l);
        ItemImpl i = new ItemImpl(null, caption);
        l.items.add(i);
        return i;
    }

    @Override public synchronized Item addData(String caption, String format, Object... args) {
        ItemImpl i = standalone(caption);
        i.setValue(format, args);
        return i;
    }

    @Override public synchronized Item addData(String caption, Object value) {
        ItemImpl i = standalone(caption);
        i.setValue(value);
        return i;
    }

    @Override public synchronized <T> Item addData(String caption, Func<T> producer) {
        ItemImpl i = standalone(caption);
        i.setValue(producer);
        return i;
    }

    @Override public synchronized <T> Item addData(String caption, String format, Func<T> producer) {
        ItemImpl i = standalone(caption);
        i.format = format;
        i.producer = producer;
        return i;
    }

    @Override public synchronized Line addLine() { return addLine(""); }

    @Override public synchronized Line addLine(String caption) {
        LineImpl l = new LineImpl(caption == null ? "" : caption);
        entries.add(l);
        return l;
    }

    @Override public synchronized boolean removeItem(Item item) {
        for (LineImpl l : entries) if (l.items.remove(item)) return true;
        return false;
    }

    @Override public synchronized boolean removeLine(Line line) { return entries.remove(line); }

    @Override public synchronized void clear() {
        entries.removeIf(l -> {
            l.items.removeIf(i -> !i.retained);
            return l.items.isEmpty();
        });
    }

    @Override public synchronized void clearAll() { entries.clear(); logEntries.clear(); }

    @Override public synchronized boolean update() {
        List<String> lines = new ArrayList<>();
        for (LineImpl l : entries) lines.add(l.render());
        if (!logEntries.isEmpty()) {
            lines.add("");
            lines.addAll(logEntries);
        }
        published = lines;
        dirty = true;
        if (autoClear) clear();
        return true;
    }

    /** Called by the bridge: returns new lines if update() was called since the last poll. */
    List<String> poll() {
        if (!dirty) return null;
        dirty = false;
        return published;
    }

    int intervalMs() { return msInterval; }

    @Override public synchronized boolean isAutoClear() { return autoClear; }
    @Override public synchronized void setAutoClear(boolean a) { autoClear = a; }
    @Override public int getMsTransmissionInterval() { return msInterval; }
    @Override public void setMsTransmissionInterval(int ms) { msInterval = Math.max(0, ms); }
    @Override public String getItemSeparator() { return itemSep; }
    @Override public void setItemSeparator(String s) { itemSep = s; }
    @Override public String getCaptionValueSeparator() { return captionSep; }
    @Override public void setCaptionValueSeparator(String s) { captionSep = s; }
    @Override public void setDisplayFormat(DisplayFormat f) { }
    @Override public void speak(String text) { }

    @Override public Log log() {
        return new Log() {
            @Override public void add(String entry) {
                synchronized (SimTelemetry.this) {
                    logEntries.add(entry);
                    while (logEntries.size() > logCapacity) logEntries.remove(0);
                }
            }
            @Override public void add(String format, Object... args) { add(String.format(Locale.US, format, args)); }
            @Override public void clear() { synchronized (SimTelemetry.this) { logEntries.clear(); } }
            @Override public int getCapacity() { return logCapacity; }
            @Override public void setCapacity(int c) { logCapacity = Math.max(1, c); }
        };
    }
}
