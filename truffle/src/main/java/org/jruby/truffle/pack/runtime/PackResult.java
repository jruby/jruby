package org.jruby.truffle.pack.runtime;

public class PackResult {

    private final byte[] output;
    private final int outputLength;
    private final boolean tainted;

    public PackResult(byte[] output, int outputLength, boolean tainted) {
        this.output = output;
        this.outputLength = outputLength;
        this.tainted = tainted;
    }

    public byte[] getOutput() {
        return output;
    }

    public int getOutputLength() {
        return outputLength;
    }

    public boolean isTainted() {
        return tainted;
    }
}
