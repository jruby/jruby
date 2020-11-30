package org.jruby.ir.interpreter;

public class ExitableInterpreterEngineContext {
    private int ipc = 0;
    private Object[] temporaryVariables = null;

    // Normally we do not use specialized temp variables and when acquiring them they return null.
    boolean specializedTemporaryVariablesInitialized = false;
    double[] floats = null;
    long[] fixnums = null;
    boolean[] booleans = null;

    public Object[] getTemporaryVariables(InterpreterContext interpreterContext) {
        if (temporaryVariables == null) {
            temporaryVariables = interpreterContext.allocateTemporaryVariables();
        }

        return temporaryVariables;
    }

    public double[] getTemporaryFloatVariables(InterpreterContext interpreterContext) {
        if (specializedTemporaryVariablesInitialized) {
            floats = interpreterContext.allocateTemporaryFloatVariables();
        }

        return floats;
    }

    public long[] getTemporaryFixnumVariables(InterpreterContext interpreterContext) {
        if (specializedTemporaryVariablesInitialized) {
            fixnums = interpreterContext.allocateTemporaryFixnumVariables();
        }

        return fixnums;
    }

    public boolean[] getTemporaryBooleanVariables(InterpreterContext interpreterContext) {
        if (specializedTemporaryVariablesInitialized) {
            booleans  = interpreterContext.allocateTemporaryBooleanVariables();
        }

        return booleans;
    }

    public int getIPC() {
        return ipc;
    }
}
