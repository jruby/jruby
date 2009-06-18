package org.jruby.compiler.ir;

import org.jruby.util.JavaNameMangler;

public class IR_Method extends IR_BaseContext
{
    String _name;        // Ruby name 
    String _irName;      // Generated name

    public IR_Method(IR_BuilderContext parent, String name, boolean isRoot)
    {
		 super(parent);
        _name = name;
        if (root && Boolean.getBoolean("jruby.compile.toplevel")) {
            _irName = name;
        } else {
            String mangledName = JavaNameMangler.mangleStringForCleanJavaIdentifier(name);
            // FIXME: What is this script business here?
            _irName = "method__" + script.getAndIncrementMethodIndex() + "$RUBY$" + mangledName;
        }
    }
}
