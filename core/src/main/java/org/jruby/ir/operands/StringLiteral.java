package org.jruby.ir.operands;

import org.jruby.util.ByteList;

public interface StringLiteral {
    ByteList getByteList();
    int getCodeRange();
}
