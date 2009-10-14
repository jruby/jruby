package org.jruby.compiler.ir;

import java.util.List;
import java.util.ArrayList;
import org.jruby.compiler.ir.instructions.CALL_Instr;
import org.jruby.compiler.ir.instructions.GET_CONST_Instr;
import org.jruby.compiler.ir.instructions.IR_Instr;
import org.jruby.compiler.ir.instructions.RECV_ARG_Instr;
import org.jruby.compiler.ir.instructions.RECV_CLOSURE_Instr;
import org.jruby.compiler.ir.instructions.RUBY_INTERNALS_CALL_Instr;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.MethAddr;
import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.Variable;

public class IR_Method extends IR_ExecutionScope
{
    public final String  _name;     // Ruby name 
    public final boolean _isInstanceMethod;

    public final Label _startLabel; // Label for the start of the method
    public final Label _endLabel;   // Label for the end of the method

    // SSS FIXME: Token can be final for a method -- implying that the token is only for this particular implementation of the method
    // But, if the mehod is modified, we create a new method object which in turn gets a new token.  What makes sense??  Intuitively,
    // it seems the first one ... but let us see ...
    private CodeVersion _token;   // Current code version token for this method -- can change during execution as methods get redefined!
    private List<Operand> _callArgs;

    /* *****************************************************************************************************
     * Does this method receive a block and use it in such a way that all of the caller's local variables
     * need to be stored in a heap frame?
     * Ex: 
     *    def foo(&b)
     *     eval 'puts a', b
     *    end
     *  
     *    def bar
     *      a = 1
     *      foo {} # prints out '1'
     *    end
     *
     * Here, 'foo' can access all of bar's variables because it captures the caller's closure.
     *
     * There are 2 scenarios when this can happen (even this is conservative -- but, good enough for now)
     * 1. This method receives an explicit block argument (in this case, the block can be stored, passed around,
     *    eval'ed against, called, etc.)
     * 2. This method has a 'super' call (ZSuper AST node -- RUBY_INTERNALS_CALL_Instr(MethAddr.ZSUPER, ..) IR instr)
     *    In this case, the parent (in the inheritance hierarchy) can access the block and store it, etc.  So, in reality,
     *    rather than assume that the parent will always do this, we can query the parent, if we can precisely identify
     *    the parent method (which in the face of Ruby's dynamic hierarchy, we cannot).  So, be pessimistic.
     *
     * This logic was extracted from an email thread on the JRuby mailing list -- Yehuda Katz & Charles Nutter
     * contributed this analysis above.
     * ********************************************************************************************************/
    private boolean _canCaptureCallersClosure;

    /* ****************************************************************************
     * Does this method define code, i.e. does it (or anybody in the downward call chain)
     * do class_eval, module_eval? In the absence of any other information, we default
     * to yes -- which basically leads to pessimistic but safe optimizations.  But, for
     * library and internal methods, this might be false.
     * **************************************************************************** */
    private boolean _canModifyCode;

    public IR_Method(IR_Scope parent, IR_Scope lexicalParent, String name, boolean isInstanceMethod) {
        super(parent, lexicalParent);
        _name = name;
        _isInstanceMethod = isInstanceMethod;
        _startLabel = getNewLabel("_METH_START_");
        _endLabel   = getNewLabel("_METH_END_");
        _callArgs = new ArrayList<Operand>();
        _token = CodeVersion.getVersionToken();
        _canModifyCode = true;
        _canCaptureCallersClosure = false;
    }

    public IR_Method(IR_Scope parent, IR_Scope lexicalParent, String name, String javaName, boolean isInstanceMethod) {
        this(parent, lexicalParent, name, isInstanceMethod);
    }

    public void addInstr(IR_Instr i) {
        // Accumulate call arguments
        if (i instanceof RECV_ARG_Instr)
            _callArgs.add(i._result);

        if (i instanceof RECV_CLOSURE_Instr)
            _canCaptureCallersClosure = true;

        // SSS FIXME: Should we build a ZSUPER IR Instr rather than have this code here?
        if (i instanceof RUBY_INTERNALS_CALL_Instr) {
            if (((CALL_Instr)i).getMethodAddr() == MethAddr.ZSUPER)
                _canCaptureCallersClosure = true;
        }

        super.addInstr(i);
    }

    public Operand[] getCallArgs() { 
        return _callArgs.toArray(new Operand[_callArgs.size()]);
    }

    public void setConstantValue(String constRef, Operand val) {
        if (isAClassRootMethod())
            ((MetaObject)_parent)._scope.setConstantValue(constRef, val);
        else
            throw new org.jruby.compiler.NotCompilableException("Unexpected: Encountered set constant value in a method!");
    }

    public boolean isAClassRootMethod() { 
        return IR_Module.isAClassRootMethod(this);
    }

    public void setCodeModificationFlag(boolean f) { 
        _canModifyCode = f;
    }

    public boolean modifiesCode() { 
        return _canModifyCode; 
    }

    public boolean canAccessAllOfCallersLocalVariables() {
        return _canCaptureCallersClosure;
    }

    // SSS FIXME: Incorect!
    public String getFullyQualifiedName() {
        IR_Module m = getDefiningModule();
        return (m == null) ? null : m._name + ":" + _name;
    }

    public IR_Module getDefiningModule() {
        return (_parent instanceof MetaObject) ? (IR_Module)((MetaObject)_parent)._scope : null;
    }

    public CodeVersion getCodeVersionToken() { 
        return _token; 
    }

    public String toString() {
        return "Method: " + _name + super.toString();
    }
}
