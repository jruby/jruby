package org.jruby.compiler.ir;

import java.util.List;
import java.util.ArrayList;
import org.jruby.compiler.ir.instructions.GET_CONST_Instr;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.RECV_ARG_Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

public class IR_Method extends IR_ScopeImpl
{
    public final String  _name;     // Ruby name 
    public final boolean _isInstanceMethod;

    public final Label _startLabel; // Label for the start of the method
    public final Label _endLabel;   // Label for the end of the method

    // SSS FIXME: Token can be final for a method -- implying that the token is only for this particular implementation of the method
    // But, if the mehod is modified, we create a new method object which in turn gets a new token.  What makes sense??  Intuitively,
    // it seems the first one ... but let us see ...
    private CodeVersion _token;   // Current code version token for this method -- can change during execution as methods get redefined!

    private boolean _optimizable;
    private boolean _canModifyCode;
    private List<Operand> _callArgs;

    public IR_Method(IR_Scope parent, IR_Scope lexicalParent, String name, String javaName, boolean isInstanceMethod)
    {
        this(parent, lexicalParent, name, isInstanceMethod);
    }

    public IR_Method(IR_Scope parent, IR_Scope lexicalParent, String name, boolean isInstanceMethod)
    {
        super(parent, lexicalParent);
        _name = name;
        _isInstanceMethod = isInstanceMethod;
        _startLabel = getNewLabel("_METH_START_");
        _endLabel   = getNewLabel("_METH_END_");
        _callArgs = new ArrayList<Operand>();
        _token = CodeVersion.getVersionToken();
        _optimizable = true;
            // Does this method define code? 
            // Default is yes -- which basically leads to pessimistic but safe optimizations
            // But, for library and internal methods, this might be false.
        _canModifyCode = true;
    }

    public void addInstr(IR_Instr i)
    {
        // Accumulate call arguments
        if (i instanceof RECV_ARG_Instr)
            _callArgs.add(i._result);

        super.addInstr(i);
    }

    public Operand[] getCallArgs() { return _callArgs.toArray(new Operand[_callArgs.size()]); }

    public void setConstantValue(String constRef, Operand val) 
    {
        if (isAClassRootMethod())
            ((MetaObject)_parent)._scope.setConstantValue(constRef, val);
        else
		    throw new org.jruby.compiler.NotCompilableException("Unexpected: Encountered set constant value in a method!");
    }

    public boolean isAClassRootMethod() { return IR_Class.isAClassRootMethod(this); }

    public void setOptimizatableFlag(boolean f) { _optimizable = f; }

    public boolean isUnoptimizable() { return _optimizable; }

    public void setCodeModificationFlag(boolean f) { _canModifyCode = f; }

    public boolean modifiesCode() { return _canModifyCode; }

    // SSS FIXME: Incorect!
    public String getFullyQualifiedName() {
        IR_Module m = getDefiningModule();
        return (m == null) ? null : m._name + ":" + _name;
    }

    public IR_Module getDefiningModule() {
        return (_parent instanceof MetaObject) ? (IR_Module)((MetaObject)_parent)._scope : null;
    }

    public CodeVersion getCodeVersionToken() { return _token; }

    public String toString() {
        return "Method: " + _name + super.toString();
    }
}
