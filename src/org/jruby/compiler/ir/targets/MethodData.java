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

/**
 *
 * @author headius
 */
class MethodData {

    public MethodData(SkinnyMethodAdapter method, int arity) {
        this.method = new IRBytecodeAdapter(method, arity);
    }
    
    public int local(Variable variable) {
        if (varMap.containsKey(variable)) return varMap.get(variable);

        String newName = variable.getName().replace('%', '$');
        int index = method.newLocal(newName, JVM.OBJECT_TYPE);
        varMap.put(variable, index);

        return index;
    }
    public IRBytecodeAdapter method;
    public Map<Variable, Integer> varMap = new HashMap<Variable, Integer>();
    public Map<Label, org.objectweb.asm.Label> labelMap = new HashMap<Label, org.objectweb.asm.Label>();
    
}
