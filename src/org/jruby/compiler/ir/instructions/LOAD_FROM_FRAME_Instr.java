package org.jruby.compiler.ir.instructions;

import org.jruby.compiler.ir.Operation;
import org.jruby.compiler.ir.operands.Variable;
import org.jruby.compiler.ir.operands.MetaObject;
import org.jruby.compiler.ir.IRExecutionScope;
import org.jruby.compiler.ir.IRMethod;
import org.jruby.compiler.ir.representations.InlinerInfo;

/*
 * All frame accessible variables are allocated in the nearest method ancestor.  Additionally, all variables 
 * with the same name in all closures (however deeply nested) get a single shared slot in the method's frame.
 *
 * So, when we encounter a load from frame instruction in some execution scope, we traverse the scope
 * tree till we find a method.  We are guaranteed to find one since closures dont float free -- 
 * they are always tethered to a surrounding scope!  This also means that we can find the neareast
 * method ancestor by simply traversing lexical scopes -- no need to traverse the dynamic scopes *
 *
 * SSS FIXME: except perhaps when we use class_eval, module_eval, or instance_eval??
 */

public class LOAD_FROM_FRAME_Instr extends GET_Instr {
    public LOAD_FROM_FRAME_Instr(Variable v, IRExecutionScope scope, String slotName) {
        super(Operation.FRAME_LOAD, v, new MetaObject(getClosestMethodAncestor(scope)), slotName);
    }

    private static IRMethod getClosestMethodAncestor(IRExecutionScope scope) {
        while (!(scope instanceof IRMethod)) {
            scope = (IRExecutionScope)scope.getLexicalParent();
        }

        return (IRMethod) scope;
    }

    @Override
    public String toString() {
        return "\t" + result + " = FRAME(" + _source + ")." + _ref;
    }

    public Instr cloneForInlining(InlinerInfo ii) {
        return new LOAD_FROM_FRAME_Instr(ii.getRenamedVariable(result), (IRExecutionScope)((MetaObject)_source).scope, _ref);
    }
}
