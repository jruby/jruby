package org.jruby.ir;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.ast.Node;
import org.jruby.ast.RootNode;
import org.jruby.ir.persistence.IRPersistenceException;
import org.jruby.ir.persistence.IRPersistenceFacade;
import org.perf4j.LoggingStopWatch;
import org.perf4j.StopWatch;

/**
 * Abstract class that contains general logic for both IR Compiler and IR
 * Interpreter
 * 
 * @param <R>
 *            type of returned object by translator
 * @param <S>
 *            type of specific for translator object
 */
public abstract class IRTranslator<R, S> {
    
    private Queue<List<IRScope>> cachedScopes = new LinkedList<List<IRScope>>();

    public R performTranslation(Ruby runtime, Node node, S specificObject) {
        R result = null;
        try {

            IRScope producedIRScope = null;
            if (isIRPersistenceRequired()) {
                StopWatch interpretWatch = new LoggingStopWatch("AST -> IR");
                producedIRScope = produceIrScope(runtime, node, true);
                interpretWatch.stop();
                IRPersistenceFacade.persist(producedIRScope, runtime);
            } else if (isIRReadingRequired()) {
                StopWatch readWatch = new LoggingStopWatch(".ir -> IR");
                if(cachedScopes.isEmpty()) {
                    
                    List<IRScope> allScopes = IRPersistenceFacade.read(runtime);
                    
                    cacheAllTheSepateScopeStacks(allScopes);
                    
                }
                readWatch.stop();
                
                // Deal with single scope stack at a time
                List<IRScope> currentScopes = cachedScopes.remove();
                for(IRScope currentScope : currentScopes) {
                    //System.out.print(currentScope.toPersistableString() + "\n");
                }
            } else {
                producedIRScope = produceIrScope(runtime, node, false);
                result = translationSpecificLogic(runtime, producedIRScope, specificObject);
            }

        } catch (IRPersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return result;
    }

    private void cacheAllTheSepateScopeStacks(List<IRScope> allScopes) {
        List<IRScope> scopeWithLexicalChildrens = new ArrayList<IRScope>();
        for (IRScope irScope : allScopes) {
            if(irScope.getLexicalParent() == null) { // if scope has no lexical parents
                scopeWithLexicalChildrens = startFromToplevelScope(
                        scopeWithLexicalChildrens, irScope);
            } else { // it has lexical parents, lets append to scope stack
                scopeWithLexicalChildrens.add(irScope);
            }
        }
        cachedScopes.add(scopeWithLexicalChildrens); // finish last scope stack
    }

    private List<IRScope> startFromToplevelScope(List<IRScope> scopeWithLexicalChildrens,
            IRScope irScope) {
        if(!scopeWithLexicalChildrens.isEmpty()) {
            cachedScopes.add(scopeWithLexicalChildrens);
        }
        scopeWithLexicalChildrens = new ArrayList<IRScope>();
        scopeWithLexicalChildrens.add(irScope);
        return scopeWithLexicalChildrens;
    }

    protected abstract R translationSpecificLogic(Ruby runtime, IRScope producedIrScope,
            S specificObject);

    private static boolean isIRPersistenceRequired() {
        return RubyInstanceConfig.IR_PERSISTENCE;
    }

    private static boolean isIRReadingRequired() {
        return RubyInstanceConfig.IR_READING;
    }

    private IRScope produceIrScope(Ruby runtime, Node node, boolean isDryRun) {
        IRManager irManager = runtime.getIRManager();
        irManager.setDryRun(isDryRun);
        IRBuilder irBuilder = IRBuilder.createIRBuilder(irManager);

        final IRScope irScope = irBuilder.buildRoot((RootNode) node);
        return irScope;
    }

}
