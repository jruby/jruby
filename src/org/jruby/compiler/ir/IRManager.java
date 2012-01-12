package org.jruby.compiler.ir;

/**
 */
public class IRManager {
    private int dummyMetaClassCount = 0;
    private IRModuleBody classMetaClass = new IRMetaClassBody(null, getMetaClassName(), "", null);
    private IRModuleBody object = new IRClassBody(null, "Object", "", null);
    
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
