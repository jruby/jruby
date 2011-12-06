package org.jruby.compiler.ir;

import org.jruby.compiler.ir.instructions.ReceiveSelfInstruction;
import org.jruby.parser.StaticScope;

public class IRMetaClass extends IRClass {
    static int dummyMetaClassCount = 0;
    static IRMetaClass CLASS_METACLASS = new IRMetaClass(null, null);
    
    static {
        CLASS_METACLASS.addInstr(new ReceiveSelfInstruction(CLASS_METACLASS.getSelf()));
    }

    public IRMetaClass(IRScope s, StaticScope staticScope) {
        // Super class is always <Class:Class>
        super(s, CLASS_METACLASS, "<DUMMY_MC:" + dummyMetaClassCount + ">", staticScope);
        dummyMetaClassCount += 1;
    }

    @Override
    public String getScopeName() {
        return "MetaClass";
    }
}
