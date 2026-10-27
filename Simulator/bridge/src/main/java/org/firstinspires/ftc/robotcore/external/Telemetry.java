package org.firstinspires.ftc.robotcore.external;

/** The commonly used part of the FTC SDK Telemetry interface. */
public interface Telemetry {
    Item addData(String caption, String format, Object... args);
    Item addData(String caption, Object value);
    <T> Item addData(String caption, Func<T> valueProducer);
    <T> Item addData(String caption, String format, Func<T> valueProducer);

    Line addLine();
    Line addLine(String lineCaption);

    boolean removeItem(Item item);
    boolean removeLine(Line line);
    void clear();
    void clearAll();
    boolean update();

    boolean isAutoClear();
    void setAutoClear(boolean autoClear);
    int getMsTransmissionInterval();
    void setMsTransmissionInterval(int msTransmissionInterval);
    String getItemSeparator();
    void setItemSeparator(String itemSeparator);
    String getCaptionValueSeparator();
    void setCaptionValueSeparator(String captionValueSeparator);
    void setDisplayFormat(DisplayFormat displayFormat);
    void speak(String text);
    Log log();

    enum DisplayFormat { CLASSIC, MONOSPACE, HTML }

    interface Item {
        String getCaption();
        Item setCaption(String caption);
        Item setValue(String format, Object... args);
        Item setValue(Object value);
        <T> Item setValue(Func<T> valueProducer);
        Item setRetained(Boolean retained);
        boolean isRetained();
        Item addData(String caption, String format, Object... args);
        Item addData(String caption, Object value);
    }

    interface Line {
        Item addData(String caption, String format, Object... args);
        Item addData(String caption, Object value);
        <T> Item addData(String caption, Func<T> valueProducer);
    }

    interface Log {
        void add(String entry);
        void add(String format, Object... args);
        void clear();
        int getCapacity();
        void setCapacity(int capacity);
    }
}
