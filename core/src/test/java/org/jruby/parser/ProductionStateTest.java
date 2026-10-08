package org.jruby.parser;

import junit.framework.TestCase;

public class ProductionStateTest extends TestCase {
    public void testPackPreservesLineAndColumn() {
        int[] values = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE};

        for (int line : values) {
            for (int column : values) {
                long packed = ProductionState.pack(line, column);

                assertEquals(line, ProductionState.line(packed));
                assertEquals(column, ProductionState.column(packed));
            }
        }
    }

    public void testShiftLineLeavesColumnEmpty() {
        long packed = ProductionState.shift_line(Integer.MIN_VALUE);

        assertEquals(Integer.MIN_VALUE, ProductionState.line(packed));
        assertEquals(0, ProductionState.column(packed));
    }
}
