/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jruby.ir.targets;

import com.headius.invokebinder.Signature;
import org.jruby.ir.IRScope;
import org.jruby.ir.operands.Label;
import org.jruby.ir.operands.Variable;
import org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.Map;

public class MethodData {

    public MethodData(IRBytecodeAdapter method, IRScope scope, String scopeField, Signature signature, int specificArity) {
        this.method = method;
        this.scope = scope;
        this.signature = signature;
        this.specificArity = specificArity;
        this.scopeField = scopeField;

        // incoming arguments
        for (int i = 0; i < signature.argCount(); i++) {
            String argName = signature.argName(i);
            argName = switch (argName) {
                case "self" -> "self";
                case "blockArg" -> JVMVisitor.BLOCK_ARG_LOCAL_NAME;
                default -> "$" + argName;
            };
            local(argName, Type.getType(signature.argType(i)));
        }
    }

    public int local(Variable variable, Type type) {
        String newName = variable.getId().replace('%', '$');
        return local(newName, type);
    }

    public int local(Variable variable) {
        return local(variable, JVM.OBJECT_TYPE);
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

    public final IRBytecodeAdapter method;
    public final IRScope scope;
    public final Signature signature;
    public final int specificArity;
    public final String scopeField;
    public final Map<String, Integer> varMap = new HashMap<String, Integer>();
    public final Map<Label, org.objectweb.asm.Label> labelMap = new HashMap<Label, org.objectweb.asm.Label>();

}
