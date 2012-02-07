package org.jruby.compiler.ir;

import org.jruby.compiler.ir.operands.Nil;

/**
 */
public class IRManager {
    private int dummyMetaClassCount = 0;
    private final IRModuleBody classMetaClass = new IRMetaClassBody(this, null, getMetaClassName(), "", 0, null);
    private final IRModuleBody object = new IRClassBody(this, null, "Object", "", 0, null);
    private final Nil nil = new Nil();
    
    public IRManager() {
    }
    
    public Nil getNil() {
        return nil;
    }

    public IRModuleBody getObject() {
        return object;
    }
    
    public IRModuleBody getClassMetaClass() {
        return classMetaClass;
    }
    
    public String getMetaClassName() {
        return "<DUMMY_MC:" + dummyMetaClassCount++ + ">";
    }
}
