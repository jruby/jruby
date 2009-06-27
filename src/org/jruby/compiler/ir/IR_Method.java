package org.jruby.compiler.ir;

import org.jruby.util.JavaNameMangler;

public class IR_Method extends IR_ScopeImpl
{
    String  _name;        // Ruby name 
    String  _irName;      // Generated name
    boolean _isInstanceMethod;

	 public final Label _startLabel;	// Label for the start of the method
	 public final Label _endLabel;	// Label for the end of the method

    public IR_Method(IR_Scope parent, String name, String javaName, boolean isInstanceMethod)
	 {
        super(parent);
        _name = name;
		  _irName = javaName;
		 _isInstanceMethod = isInstanceMethod;
		  _startLabel = new Label("_LBL_start");
		  _endLabel   = new Label("_LBL_end");
	 }

    public IR_Method(IR_Scope parent, String name, boolean isInstanceMethod)
    {
        super(parent);
        _name = name;
        if (root && Boolean.getBoolean("jruby.compile.toplevel")) {
            _irName = name;
        } else {
            // SSS FIXME: Copied this code verbatim from BaseBodyCompiler .. needs fixing
            String mangledName = JavaNameMangler.mangleStringForCleanJavaIdentifier(name);
            _irName = "method__" + script.getAndIncrementMethodIndex() + "$RUBY$" + mangledName;
        }

        _isInstanceMethod = isInstanceMethod;

        // SSS: FIXME: More stuff needs to happen here with the parent scope?
    }

    public Operand getConstantValue(String constRef)
    {
           // Constants are defined in classes & modules, not in methods!
           // So, this reference is actually defined in the containing class/module
       return _parent.getConstantValue(constRef);  
    }

    public void setConstantValue(String constRef, Operand val) 
    { 
       // SSS FIXME: Throw an exception here?
    }
}
