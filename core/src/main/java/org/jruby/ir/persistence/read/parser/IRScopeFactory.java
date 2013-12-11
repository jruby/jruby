package org.jruby.ir.persistence.read.parser;

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
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;

public class IRScopeFactory {

    private static final String SCRIPT_BODY_PSEUDO_CLASS_NAME = "_file_";
    
    private final IRParsingContext context;
    
    public IRScopeFactory(IRParsingContext context) {
        this.context = context;
    }

    public IRScope createScope(final IRScopeType type, final List<Object> parameters) {
        final Ruby runtime = context.getRuntime();
        final IRManager manager = runtime.getIRManager();
        
        final ParametersIterator parametersIterator = new ParametersIterator(context, parameters);
        
        final String name = parametersIterator.nextString();
        final int lineNumber = parametersIterator.nextInt();
        final IRScope lexicalParent = parametersIterator.nextScope();
        final StaticScope staticScope = parametersIterator.nextStaticScope(lexicalParent);
        
        
        switch (type) {
        case CLASS_BODY:            
            return new IRClassBody(manager, lexicalParent, name, lineNumber, staticScope);
        case METACLASS_BODY:
            return new IRMetaClassBody(manager, lexicalParent, manager.getMetaClassName(), lineNumber, staticScope);
        case INSTANCE_METHOD:
            return new IRMethod(manager, lexicalParent, name, true, lineNumber, staticScope);
        case CLASS_METHOD:
            return new IRMethod(manager, lexicalParent, name, false, lineNumber, staticScope);
        case MODULE_BODY:
            return new IRModuleBody(manager, lexicalParent, name, lineNumber, staticScope);
        case SCRIPT_BODY:
            return new IRScriptBody(manager, SCRIPT_BODY_PSEUDO_CLASS_NAME, name, staticScope);

        case CLOSURE:   
            return createClosure(manager, lexicalParent,
                    staticScope, runtime, parametersIterator, lineNumber);
            
        case EVAL_SCRIPT:
            final String fileName = lexicalParent.getFileName();
            return new IREvalScript(manager, lexicalParent, fileName, lineNumber, staticScope);
            
        default:
            throw new UnsupportedOperationException(type.toString());
        }
    }

    private IRScope createClosure(IRManager manager, IRScope lexicalParent,
            StaticScope staticScope, Ruby runtime, ParametersIterator parametersIterator,
            int lineNumber) {
        final boolean isForLoopBody = parametersIterator.nextBoolean();
        final int arityInt = parametersIterator.nextInt();
        final int argumentType = parametersIterator.nextInt();
        
        final Arity arity = Arity.createArity(arityInt);
        
        return new IRClosure(manager, lexicalParent, isForLoopBody, lineNumber, staticScope, arity, argumentType);
    }
}

