package org.jruby.ir.persistence.parser;

import java.util.List;

import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.Instr;
import org.jruby.parser.IRStaticScope;

public enum IRScopeBuilder {
    INSTANCE;

    private static final String SCRIPT_BODY_PSEUDO_CLASS_NAME = "_file_";

    public IRScope createScope(IRScopeType type, IRScope lexicalParent, String name, String lineNumberString, IRStaticScope staticScope, IRManager manager) {
        int lineNumber = Integer.parseInt(lineNumberString);
        IRScope scope;
        switch (type) {
        case CLASS_BODY:
            scope = new IRClassBody(manager, lexicalParent, name, lineNumber, staticScope);
            break;
        case METACLASS_BODY:
            scope = new IRMetaClassBody(manager, lexicalParent, name, lineNumber, staticScope);
            break;
        case INSTANCE_METHOD:
            scope = new IRMethod(manager, lexicalParent, name, true, lineNumber, staticScope);
            break;
        case CLASS_METHOD:
            scope = new IRMethod(manager, lexicalParent, name, false, lineNumber, staticScope);
            break;
        case MODULE_BODY:
            scope = new IRModuleBody(manager, lexicalParent, name, lineNumber, staticScope);
            break;
        case SCRIPT_BODY:
            scope = new IRScriptBody(manager, SCRIPT_BODY_PSEUDO_CLASS_NAME, name, staticScope);
            break;

        case CLOSURE:
        case EVAL_SCRIPT:
        default:
            throw new UnsupportedOperationException(type.toString());
        }
        
        return scope;
    }
    
    public IRScope addToScope(IRScope scope, List<Instr> instrs) {        
        for (Instr instr : instrs) {
            scope.addInstr(instr);
        }
        
        return scope;
    }
}
