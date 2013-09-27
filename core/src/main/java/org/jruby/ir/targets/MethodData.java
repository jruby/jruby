/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Variable;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

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

    public org.objectweb.asm.Label getLabel(Label label) {
        org.objectweb.asm.Label asmLabel = labelMap.get(label);
        if (asmLabel == null) {
            asmLabel = method.newLabel();
            labelMap.put(label, asmLabel);
        }
        return asmLabel;
    }

    public IRBytecodeAdapter method;
    public Map<String, Integer> varMap = new HashMap<String, Integer>();
    public Map<Label, org.objectweb.asm.Label> labelMap = new HashMap<Label, org.objectweb.asm.Label>();

}
