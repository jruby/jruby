/* Represents some data flow fact about some dataflow problem */

package org.jruby.compiler.ir.dataflow;

public class DataFlowVar
{
/* ******************** PUBLIC INTERFACE ******************* */
    public final int _id;   // Unique ID assigned to this variable

    public DataFlowVar(DataFlowProblem prob) { _id = prob.addDataFlowVar(this); }
}
