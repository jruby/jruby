package org.jruby.ir.persistence;

import java.io.IOException;
import java.util.List;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.Instr;
import org.jruby.parser.StaticScope;

/**
 *  Write IR data out to persistent store.  IRReader is capable of re-reading this
 * information back into live IR data again.  This class knows the logical order of how
 * information will be written out but the IRWriterEncoder actually knows how to encode that
 * information.
 */
public class IRWriter {
    public static void persist(IRWriterEncoder file, IRScope script) throws IOException {
        file.startEncoding(script);
        persistScopeInstructions(file, script); // recursive dump of all scopes instructions
        
        file.startEncodingScopeHeaders(script);
        persistScopeHeaders(file, script);      // recursive dump of all defined scope headers
        file.endEncodingScopeHeaders(script);
        
        file.endEncoding(script);
    }
    
    private static void persistScopeInstructions(IRWriterEncoder file, IRScope parent) {
        persistScopeInstrs(file, parent);
        
        for (IRScope scope: parent.getLexicalScopes()) {
            persistScopeInstructions(file, scope);
        }
    }
    
    // {operation, [operands]}*
    private static void persistScopeInstrs(IRWriterEncoder file, IRScope scope) {
        file.startEncodingScopeInstrs(scope);
        
        List<Instr> instrs = scope.getInstrs();

        for (Instr instr: instrs) {
            file.encode(instr);
        }
        
        file.endEncodingScopeInstrs(scope);
    }
    
    // recursive dump of all scopes.  Each scope records offset into persisted file where there
    // instructions reside.  That is extra logic here in currentInstrIndex + instrsLocations
    private static void persistScopeHeaders(IRWriterEncoder file, IRScope parent) {
        persistScopeHeader(file, parent);

        for (IRScope scope: parent.getLexicalScopes()) {
            persistScopeHeaders(file, scope);
        }
    }

    // script body: {type,name,linenumber,{static_scope},instrs_offset}
    // closure scopes: {type,name,linenumber,lexical_parent_name, lexical_parent_line,is_for,arity,arg_type,{static_scope},instrs_offset}
    // other scopes: {type,name,linenumber,lexical_parent_name, lexical_parent_line,{static_scope}, instrs_offset}
    // for non-for scopes is_for,arity, and arg_type will be 0.
    private static void persistScopeHeader(IRWriterEncoder file, IRScope scope) {
        file.startEncodingScopeHeader(scope);
        file.encode(scope.getScopeType()); // type is enum of kind of scope
        file.encode(scope.getName());
        file.encode(scope.getLineNumber());

        if (!(scope instanceof IRScriptBody)) {
            file.encode(scope.getLexicalParent().getName());
            file.encode(scope.getLexicalParent().getLineNumber());
        }

        if (scope instanceof IRClosure) {
            IRClosure closure = (IRClosure) scope;

            file.encode(closure.isForLoopBody());
            file.encode(closure.getArity().getValue());
            file.encode(closure.getArgumentType());
        }

        persistStaticScope(file, scope.getStaticScope());
        file.endEncodingScopeHeader(scope);
    }

    // {type,[variables],required_args}
    private static void persistStaticScope(IRWriterEncoder file, StaticScope staticScope) {
        file.encode(staticScope.getType());
        file.encode(staticScope.getVariables());
        file.encode(staticScope.getRequiredArgs());
    }
}