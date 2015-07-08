package org.jruby.ir.operands;

import org.jruby.ir.persistence.IRReaderDecoder;
import org.jruby.ir.persistence.IRWriterEncoder;
import org.jruby.util.ByteList;

/**
 * Represents the script's __FILE__. Isolated as its own operand because we need to be able to replace it when loading
 * persisted IR from a location different than original script.
 */
public class Filename extends StringLiteral {
    public Filename(ByteList filename) {
        super(OperandType.FILENAME, filename, 0);
    }

    @Override
    public boolean hasKnownValue() {
        return false;
    }

    @Override
    public void encode(IRWriterEncoder e) {
        // we only do base encoding because filename must be provided while deserializing (#3109)
        e.encode(getOperandType().getCoded());
    }

    public static Filename decode(IRReaderDecoder d) {
        return new Filename(d.getFilename());
    }
}
