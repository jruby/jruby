package org.jruby.parser;

import static org.jruby.util.BitPacker.packInts;
import static org.jruby.util.BitPacker.unpackHigh;
import static org.jruby.util.BitPacker.unpackLow;
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
        return unpackHigh(packed);
    }

    public static long shift_line(long line) {
        return packInts((int) line, 0);
    }

    public static long pack(int line, int column) {
        return packInts(line, column);
    }

    public static int column(long packed) {
        return unpackLow(packed);
    }
}
