/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.compiler.ir.targets;

import java.util.HashMap;
import java.util.Map;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.compiler.ir.operands.Label;
import org.jruby.compiler.ir.operands.Variable;
import org.objectweb.asm.Type;

/**
 *
 * @author headius
 */
public class MethodData {

    public MethodData(SkinnyMethodAdapter method, int arity) {
        this.method = new IRBytecodeAdapter(method, arity);
    }
    
    public int local(Variable variable) {
        String newName = variable.getName().replace('%', '$');
        return local(newName, JVM.OBJECT_TYPE);
    }

    public int local(String newName) {
        return local(newName, JVM.OBJECT_TYPE);
    }

    public int local(String newName, Type type) {
        if (varMap.containsKey(newName)) return varMap.get(newName);

        int index = method.newLocal(newName, type);
        varMap.put(newName, index);

        return index;
    }

    public org.objectweb.asm.Label getLabel(int pc) {
        org.objectweb.asm.Label asmLabel = labelMap.get(pc);
        if (asmLabel == null) {
            asmLabel = method.newLabel();
            labelMap.put(pc, asmLabel);
        }
        return asmLabel;
    }
    
    public IRBytecodeAdapter method;
    public Map<String, Integer> varMap = new HashMap<String, Integer>();
    public Map<Integer, org.objectweb.asm.Label> labelMap = new HashMap<Integer, org.objectweb.asm.Label>();
    
}
