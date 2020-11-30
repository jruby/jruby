package org.jruby.ir.interpreter;

public class ExitableInterpreterEngineState {
    // What IC this is executing.
    private ExitableInterpreterContext interpreterContext;

    // The current index of the instruction we are executing.
    private int ipc = 0;
    private Object[] temporaryVariables = null;

    public ExitableInterpreterEngineState(ExitableInterpreterContext interpreterContext) {
        this.interpreterContext = interpreterContext;
    }

    public Object[] getTemporaryVariables() {
        if (temporaryVariables == null) {
            temporaryVariables = interpreterContext.allocateTemporaryVariables();
        }

        return temporaryVariables;
    }

    public int getIPC() {
        return ipc;
    }

    public void setIPC(int ipc) {
        this.ipc = ipc;
    }
}
