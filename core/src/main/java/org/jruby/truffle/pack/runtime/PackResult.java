package org.jruby.truffle.pack.runtime;

public class PackResult {

    private final byte[] output;
    private final int outputLength;
    private final boolean tainted;
    private final PackEncoding encoding;

    public PackResult(byte[] output, int outputLength, boolean tainted, PackEncoding encoding) {
        this.output = output;
        this.outputLength = outputLength;
        this.tainted = tainted;
        this.encoding = encoding;
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

    public PackEncoding getEncoding() {
        return encoding;
    }
}
