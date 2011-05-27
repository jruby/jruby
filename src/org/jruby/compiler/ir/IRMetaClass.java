package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Operand;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.parser.StaticScope;

public class IRMetaClass extends IRClass {
    static int dummyMetaClassCount = 0;
    static IRMetaClass CLASS_METACLASS;    // SSS FIXME: Needs initialization

    public IRMetaClass(IRScope s, StaticScope staticScope) {
        // Super class is always <Class:Class>
        // This metaclass is always top-level, hence the null container.
        super(s, null, MetaObject.create(CLASS_METACLASS), "<DUMMY_MC:" + dummyMetaClassCount + ">", staticScope);
        dummyMetaClassCount += 1;
    }

    @Override
    public String getScopeName() {
        return "MetaClass";
    }
}
