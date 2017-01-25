package org.jruby.ir.runtime;

/**
 * When we want to back off from optimized IR back to safe IR we will throw
 * this exception.
 */
public class IRDeoptimization extends RuntimeException {
    private final int ipc;
    private Object[] vars;
    private String[] varNames;

    public IRDeoptimization(int ipc) {
        super();

        this.ipc = ipc;
    }

    public int getIPC() {
        return ipc;
    }

    public void setVars(Object[] vars, String descriptor) {
        this.vars = vars;
        this.varNames = descriptor.split(";");
    }
}
