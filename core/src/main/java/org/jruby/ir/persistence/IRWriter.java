package org.jruby.ir.persistence;

import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.interpreter.InterpreterContext;
import org.jruby.parser.StaticScope;

import java.io.IOException;
import java.util.BitSet;

/**
 * Write IR data out to persistent store.  IRReader is capable of re-reading this
 * information back into live IR data again.  This class knows the logical order of how
 * information will be written out but the IRWriterEncoder actually knows how to encode that
 * information.
 */
public class IRWriter {

    private IRWriter() { /* static methods only, for now */ }

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

        // Currently methods are only lazy scopes so we need to build them if we decide to persist them.
        InterpreterContext context = scope.builtInterpreterContext();

        for (Instr instr: context.getInstructions()) {
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
        if (shouldLog(file)) System.out.println("persistScopeHeader(start)");
        file.startEncodingScopeHeader(scope);
        scope.persistScopeHeader(file); // impls in IRClosure and IRScope.

        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("NAME = " + scope.getId());
        if (scope instanceof IRScriptBody) {
            // filename comes in at load time, to allow for precompilation and relocating sources
        } else {

            if (shouldLog(file)) System.out.println("persistScopeHeader: id   = " + scope.getId());
            file.encodeRaw(scope.getName());
            if (shouldLog(file)) System.out.println("persistScopeHeader(encode parent)");
            file.encode(scope.getLexicalParent());
        }

        persistStaticScope(file, scope.getStaticScope());
        scope.persistScopeFlags(file);
        file.endEncodingScopeHeader(scope);
    }

    // {type,[variables],signature}
    private static void persistStaticScope(IRWriterEncoder file, StaticScope staticScope) {
        if (shouldLog(file)) System.out.println("persistStaticScope");
        file.encode(staticScope.getType());
        // This naively looks like a bug because these represent id's and not properly encoded names BUT all of those
        // symbols for these ids will be created when IRScope loads the LocalVariable versions of these...so this is ok.
        file.encode(staticScope.getVariables());
        file.encode(staticScope.getKeywordIndices().toByteArray());
        file.encode(staticScope.getSignature());
    }

    public static boolean shouldLog(IRWriterEncoder encoder) {
        return RubyInstanceConfig.IR_WRITING_DEBUG && !encoder.isAnalyzer();
    }
}
