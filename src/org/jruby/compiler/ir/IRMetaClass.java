package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.parser.StaticScope;

public class IRMetaClass extends IRClass {
    static IRMetaClass CLASS_METACLASS;    // SSS FIXME: Needs initialization

    public IRMetaClass(IRScope s, Operand receiver, StaticScope staticScope) {
        // Super class is always <Class:Class>
        // This metaclass is always top-level, hence the null container.
        // SSS FIXME: class name -- can be unknown at compile time ... How do we handle this? 
        super(s, null, MetaObject.create(CLASS_METACLASS), "<FIXME>", staticScope);
    }

    @Override
    public String getScopeName() {
        return "MetaClass";
    }
}
