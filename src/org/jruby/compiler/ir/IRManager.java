package org.jruby.compiler.ir;

import org.jruby.compiler.ir.IRBody.BodyType;

/**
 */
public class IRManager {
    private int dummyMetaClassCount = 0;
    private IRBody classMetaClass = new IRBody(null, getMetaClassName(), null, BodyType.MetaClass);
    private IRBody object = new IRBody(null, "Object", null, BodyType.Class);
    
    public IRManager() {
    }

    public IRBody getObject() {
        return object;
    }
    
    public IRBody getClassMetaClass() {
        return classMetaClass;
    }
    
    public String getMetaClassName() {
        return "<DUMMY_MC:" + dummyMetaClassCount++ + ">";
    }
}
