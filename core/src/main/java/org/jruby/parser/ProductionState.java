package org.jruby.parser;

public class ProductionState {
    int state;
    Object value;
    public int start;
    public int end;

    public String toString() {
        int start_col = start % 0xffff;
        int end_line = end >> 16;
        int end_col = end % 0xffff;

        return "STATE: " + state + ", VALUE: " + value +
                ", COLS: (" + start_col + ", " + end_col + "), ROW: (" + startLine() + ", " + end_line + ")";
    }

    public int startLine() {
        return start >> 16;
    }
}
