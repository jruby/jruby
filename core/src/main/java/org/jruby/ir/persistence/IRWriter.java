package org.jruby.ir.persistence;

import java.io.IOException;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.Instr;
import org.jruby.parser.StaticScope;

/**
 *  Write IR data out to persistent store.  IRReader is capable of re-reading this
 * information back into live IR data again.  This class knows the logical order of how
 * information will be written out but the IRPersistedFile actually knows how to encode that
 * information.
 */
public class IRWriter {
    public static int NULL = -1;
    
    public static void persist(IRPersistedFile file, IRScope script) throws IOException {
        persistScopeInstructions(file, script); // recursive dump of all scopes instructions
        persistScopeHeaders(file, script);      // recursive dump of all defined scope headers
        
        file.commit(); // persist file to disk.
    }
    
    private static void persistScopeInstructions(IRPersistedFile file, IRScope parent) {
        persistScopeInstrs(file, parent);
        
        for (IRScope scope: parent.getLexicalScopes()) {
            persistScopeInstructions(file, scope);
        }
    }
    
    // {operation, [operands]}*
    private static void persistScopeInstrs(IRPersistedFile file, IRScope scope) {
        // record offset so when scopes are persisted they know where their instructions are located.
        file.addScopeInstructionOffset(scope);
        
        for (Instr instr : scope.getInstrs()) {
            file.write(instr.getOperation());
            file.write(instr.getOperands());
        }        
    }
    
    // recursive dump of all scopes.  Each scope records offset into persisted file where there
    // instructions reside.  That is extra logic here in currentInstrIndex + instrsLocations
    private static void persistScopeHeaders(IRPersistedFile file, IRScope parent) {
        persistScopeHeader(file, parent);

        for (IRScope scope: parent.getLexicalScopes()) {
            persistScopeHeaders(file, scope);
        }
    }

    // script body: {type,name,linenumber,{static_scope},instrs_offset}
    // closure scopes: {type,name,linenumber,lexical_parent_name, lexical_parent_line,is_for,arity,arg_type,{static_scope},instrs_offset}
    // other scopes: {type,name,linenumber,lexical_parent_name, lexical_parent_line,{static_scope}, instrs_offset}
    // for non-for scopes is_for,arity, and arg_type will be 0.
    private static void persistScopeHeader(IRPersistedFile file, IRScope scope) {
        file.write(scope.getScopeType()); // type is enum of kind of scope
        file.write(scope.getName());
        file.write(scope.getLineNumber());

        if (!(scope instanceof IRScriptBody)) {
            file.write(scope.getLexicalParent().getName());
            file.write(scope.getLexicalParent().getLineNumber());
        }

        if (scope instanceof IRClosure) {
            IRClosure closure = (IRClosure) scope;

            file.write(closure.isForLoopBody());
            file.write(closure.getArity().getValue());
            file.write(closure.getArgumentType());
        }

        persistStaticScope(file, scope.getStaticScope());
        file.write(file.getScopeInstructionOffset(scope));
    }

    // {type,[variables],required_args}
    private static void persistStaticScope(IRPersistedFile file, StaticScope staticScope) {
        file.write(staticScope.getType());
        file.write(staticScope.getVariables());
        file.write(staticScope.getRequiredArgs());
    }
}