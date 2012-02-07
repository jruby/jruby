package org.jruby.compiler.ir;

/**
 */
public class IRManager {
    private int dummyMetaClassCount = 0;
    private IRModuleBody classMetaClass = new IRMetaClassBody(this, null, getMetaClassName(), "", 0, null);
    private IRModuleBody object = new IRClassBody(this, null, "Object", "", 0, null);
    
    public IRManager() {
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
