package org.jruby.ir.dataflow;

/**
 * Represents some data flow fact about some dataflow problem
 */
public class DataFlowVar {
    // Unique ID assigned to this variable
    public final int id;

    public DataFlowVar(DataFlowProblem prob) {
        id = prob.addDataFlowVar(this);
    }

    public int getId() {
        return id;
    }
}
