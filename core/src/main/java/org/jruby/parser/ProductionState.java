package org.jruby.parser;

import org.jruby.util.ByteList;

public class ProductionState {
    public int state;
    public ByteList id;
    public Object value;
    public long start;
    public long end;

    public String toString() {
        return "STATE: " + state + ", VALUE: " + value +
                ", COLS: (" + column(start) + ", " + column(end) +
                "), ROW: (" + line(start) + ", " + line(end) + ")";
    }

    public int start() {
        return line(start);
    }

    public int end() {
        return line(end);
    }

    public static int line(long packed) {
        return (int) (packed >> 32);
    }

    public static long shift_line(long packed) {
        return packed << 32;
    }

    public static long pack(int line, int column) {
        return shift_line(line) | column;
    }

    public static int column(long packed) {
        return (int) (packed & 0xffff);
    }
}
