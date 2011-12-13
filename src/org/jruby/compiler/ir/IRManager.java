/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir;

import java.util.HashMap;
import java.util.Map;
import org.jruby.compiler.ir.IRBody.BodyType;
import org.jruby.compiler.ir.instructions.ReceiveSelfInstruction;

/**
 */
public class IRManager {
    private int dummyMetaClassCount = 0;
    private IRBody classMetaClass = new IRBody(null, getMetaClassName(), null, BodyType.MetaClass);
    private Map<String, IRBody> modules = new HashMap<String, IRBody>();
    
    public IRManager() {
        IRScriptBody boostrapScript = new IRScriptBody("[bootstrap]", "[bootstrap]", null);
        
        boostrapScript.addInstr(new ReceiveSelfInstruction(boostrapScript.getSelf()));
        addCoreClass("Object", boostrapScript);
        addCoreClass("Module", boostrapScript);
        addCoreClass("Class", boostrapScript);
        
        classMetaClass.addInstr(new ReceiveSelfInstruction(classMetaClass.getSelf()));
    }
    
    private IRBody addCoreClass(String name, IRScope parent) {
        IRBody c = new IRBody(parent, name, null, BodyType.Class);
        c.addInstr(new ReceiveSelfInstruction(c.getSelf()));
        modules.put(c.getName(), c);
        return c;
    }

    public IRBody getModule(String n) {
        return modules.get(n);
    }
    
    public IRBody getClassMetaClass() {
        return classMetaClass;
    }
    
    public String getMetaClassName() {
        return "<DUMMY_MC:" + dummyMetaClassCount++ + ">";
    }
}
