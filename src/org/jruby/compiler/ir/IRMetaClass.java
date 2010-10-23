package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.operands.SelfVariable;
import org.jruby.parser.StaticScope;

public class IRMetaClass extends IRClass {
    static IRMetaClass CLASS_METACLASS;    // SSS FIXME: Needs initialization

    private Operand methodClassedObject;   // Object (classes too, obviously) that has been metaclassed

    public IRMetaClass(IRScope s, Operand receiver, StaticScope staticScope) {
        // Super class is always <Class:Class>
        // This metaclass is always top-level, hence the null container.
        // SSS FIXME: class name -- can be unknown at compile time ... How do we handle this? 
        super(s, null, new MetaObject(CLASS_METACLASS), "<FIXME>", staticScope);

        if ((receiver instanceof SelfVariable)) {
            IRMethod classRootMethod = (IRMethod)s;
            methodClassedObject = (MetaObject)classRootMethod.getContainer();
        } else {
            methodClassedObject = receiver;
        }
    }

    @Override
    public String getScopeName() {
        return "MetaClass";
    }
}
