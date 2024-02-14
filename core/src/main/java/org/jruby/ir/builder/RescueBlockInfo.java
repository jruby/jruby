package org.jruby.ir.builder;

import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Variable;

class RescueBlockInfo {
    final Label entryLabel;             // Entry of the rescue block
    final Variable savedExceptionVariable; // Variable that contains the saved $! variable

    public RescueBlockInfo(Label l, Variable v) {
        entryLabel = l;
        savedExceptionVariable = v;
    }
}
