/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir;

import java.util.HashMap;
import java.util.Map;
import org.jruby.compiler.ir.instructions.ReceiveSelfInstruction;

/**
 */
public class IRManager {
    private static Map<String, IRClass> modules = new HashMap<String, IRClass>();
    
    public IRManager() {
        IRScript boostrapScript = new IRScript("[bootstrap]", "[bootstrap]", null);
        
        boostrapScript.addInstr(new ReceiveSelfInstruction(boostrapScript.getSelf()));
        addCoreClass("Object", boostrapScript);
        addCoreClass("Module", boostrapScript);
        addCoreClass("Class", boostrapScript);
    }
    
    private IRClass addCoreClass(String name, IRScope parent) {
        IRClass c = new IRClass(parent, null, name, null);
        c.addInstr(new ReceiveSelfInstruction(c.getSelf()));
        modules.put(c.getName(), c);
        return c;
    }

    public IRModule getModule(String n) {
        return modules.get(n);
    }    
}
