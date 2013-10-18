package org.jruby.ir.persistence.parser;

import java.util.List;

import org.jruby.Ruby;
import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IREvalScript;
import org.jruby.ir.IRManager;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.Instr;
import org.jruby.parser.IRStaticScope;
import org.jruby.runtime.Arity;

public enum IRScopeBuilder {
    INSTANCE;

    private static final String SCRIPT_BODY_PSEUDO_CLASS_NAME = "_file_";

    public IRScope createScope(IRScopeType type, IRScope lexicalParent, String name, String lineNumberString, IRStaticScope staticScope, Ruby runtime, List<String> specificArguments) {
        int lineNumber = Integer.parseInt(lineNumberString);
        IRManager manager = runtime.getIRManager();
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
            boolean isForLoopBody = Boolean.parseBoolean(specificArguments.get(0));
            Arity arity = Arity.createArity(Integer.parseInt(specificArguments.get(1)));
            int argumentType = Integer.parseInt(specificArguments.get(2));
            
            scope = new IRClosure(manager, lexicalParent, isForLoopBody, lineNumber, staticScope, arity, argumentType, runtime.is1_9());
            break;
            
        case EVAL_SCRIPT:
            // FIXME?: File name may be other than file name of parent?
            String fileName = lexicalParent.getFileName();
            scope = new IREvalScript(manager, lexicalParent, fileName, lineNumber, staticScope);
            break;
            
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
