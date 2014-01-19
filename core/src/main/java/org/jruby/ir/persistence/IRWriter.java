package org.jruby.ir.persistence;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.ir.operands.Self;
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
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("Writing Scope Header");
        file.startEncodingScopeHeader(scope);
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("IRScopeType = " + scope.getScopeType());
        file.encode(scope.getScopeType()); // type is enum of kind of scope
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("NAME = " + scope.getName());
        file.encode(scope.getName());
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("Line # = " + scope.getLineNumber());
        file.encode(scope.getLineNumber());
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("# of temp vars = " + scope.getTemporaryVariablesCount());
        file.encode(scope.getTemporaryVariablesCount());

        persistScopeLabelIndices(scope, file);
        
        if (!(scope instanceof IRScriptBody)) file.encode(scope.getLexicalParent());

        if (scope instanceof IRClosure) {
            IRClosure closure = (IRClosure) scope;

            file.encode(closure.isForLoopBody());
            file.encode(closure.getArity().getValue());
            file.encode(closure.getArgumentType());
        }

        persistStaticScope(file, scope.getStaticScope());
        persistLocalVariables(scope, file);
        file.endEncodingScopeHeader(scope);
    }

    // FIXME: I hacked around our lvar types for now but this hsould be done in a less ad-hoc fashion.
    private static void persistLocalVariables(IRScope scope, IRWriterEncoder file) {
        Map<String, LocalVariable> localVariables = scope.getLocalVariables();
        file.encode(localVariables.size());
        for (String name: localVariables.keySet()) {
            file.encode(name);
            file.encode(localVariables.get(name).getOffset()); // No need to write depth..it is zero.
        }
    }

    private static void persistScopeLabelIndices(IRScope scope, IRWriterEncoder file) {
        Map<String,Integer> labelIndices = scope.getVarIndices();
        file.encode(labelIndices.size());
        for (String key : labelIndices.keySet()) {
            file.encode(key);
            file.encode(labelIndices.get(key).intValue());
        }
    }

    // {type,[variables],required_args}
    private static void persistStaticScope(IRWriterEncoder file, StaticScope staticScope) {
        file.encode(staticScope.getType());
        file.encode(staticScope.getVariables());
        file.encode(staticScope.getRequiredArgs());
    }
}
