package org.jruby.ir.persistence;

import org.jruby.RubyInstanceConfig;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.parser.StaticScope;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.jruby.runtime.Signature;

/**
 *  Write IR data out to persistent store.  IRReader is capable of re-reading this
 * information back into live IR data again.  This class knows the logical order of how
 * information will be written out but the IRWriterEncoder actually knows how to encode that
 * information.
 */
public class IRWriter {
    protected IRWriterEncoder file;

    @Deprecated
    public IRWriter() {
        // This is so anyone who might have extended this pre-OOd will not fail to compile.
        throw new RuntimeException("Old format was all static so this noarg constructor should not get called.");
    }

    public IRWriter(IRWriterEncoder file) {
        this.file = file;
    }

    public void persist(IRScope scope) throws IOException {
        startEncodingScope(scope);

        startInstructionsSection(scope);
        persistScopeInstructions(scope); // recursive dump of all scopes instructions
        endInstructionsSection(scope);

        startScopeHeadersSection(scope);
        persistScopeHeaders(scope, true);      // recursive dump of all defined scope headers
        endScopeHeadersSection(scope);

        endEncodingScope(scope);
    }

    protected void persistScopeInstructions(IRScope parent) {
        persistScopeInstrs(parent);

        for (IRScope scope: parent.getLexicalScopes()) {
            persistScopeInstructions(scope);
        }
    }

    // {operation, [operands]}*
    protected void persistScopeInstrs(IRScope scope) {
        file.startEncodingScopeInstrs(scope);

        // FIXME IRScope should provide a guaranteed IC which will build if lazy or return what it has.
        // Currently methods are only lazy scopes so we need to build them if we decide to persist them.
        if (scope instanceof IRMethod && !scope.hasBeenBuilt()) {
            ((IRMethod) scope).lazilyAcquireInterpreterContext();
        }

        for (Instr instr: scope.getInterpreterContext().getInstructions()) {
            file.encode(instr);
        }

        file.endEncodingScopeInstrs(scope);
    }

    // recursive dump of all scopes.  Each scope records offset into persisted file where there
    // instructions reside.  That is extra logic here in currentInstrIndex + instrsLocations
    protected void persistScopeHeaders(IRScope parent, boolean topScope) {
        persistScopeHeader(parent, topScope, false);

        for (IRScope scope: parent.getLexicalScopes()) {
            persistScopeHeaders(scope, false);
        }
    }

    // script body: {type,name,linenumber,{static_scope},instrs_offset}
    // closure scopes: {type,name,linenumber,lexical_parent_name, lexical_parent_line,is_for,arity,arg_type,{static_scope},instrs_offset}
    // other scopes: {type,name,linenumber,lexical_parent_name, lexical_parent_line,{static_scope}, instrs_offset}
    // for non-for scopes is_for,arity, and arg_type will be 0.
    protected void persistScopeHeader(IRScope scope, boolean topScope, boolean doNotRecordInstrOffset) {
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

        persistScopeLabelIndices(scope);

        // topScope is for persisting closures directly.  In this case we are not interested in its parent.
        if (!(scope instanceof IRScriptBody) && !topScope) {
            file.encode(scope.getLexicalParent());
        }

        if (scope instanceof IRClosure) file.encode(((IRClosure) scope).getSignature().encode());

        file.encode(scope.getStaticScope());
        persistLocalVariables(scope);
        if (doNotRecordInstrOffset) {
            file.encode(0);
        } else {
            file.endEncodingScopeHeader(scope);
        }
    }

    protected void persistLocalVariables(IRScope scope) {
        Map<String, LocalVariable> localVariables = scope.getLocalVariables();
        int numberOfVariables = localVariables.size();
        file.encode(numberOfVariables);
        for (int i = 0; i < numberOfVariables; i++) {
            file.encode(localVariables.get(i));
        }
    }

    protected void persistScopeLabelIndices(IRScope scope) {
        Map<String,Integer> labelIndices = scope.getVarIndices();
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("LABEL_SIZE: " + labelIndices.size());
        file.encode(labelIndices.size());
        for (String key : labelIndices.keySet()) {
            if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("LABEL: " + key);
            file.encode(key);
            file.encode(labelIndices.get(key).intValue());
            if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("LABEL(num): " + labelIndices.get(key).intValue());
        }
        if (RubyInstanceConfig.IR_WRITING_DEBUG) System.out.println("DONE LABELS: " + labelIndices.size());
    }

    protected void startEncodingScope(IRScope scope) {
        file.startEncoding(scope);
    }

    protected void endEncodingScope(IRScope scope) {
        file.endEncoding(scope);
    }

    protected void startInstructionsSection(IRScope scope) {
    }

    protected void endInstructionsSection(IRScope scope) {
    }

    protected void startScopeHeadersSection(IRScope scope) {
        file.startEncodingScopeHeaders(scope);
    }

    protected void endScopeHeadersSection(IRScope scope) {
        file.endEncodingScopeHeaders(scope);
    }

    @Deprecated
    public static void persist(IRWriterEncoder file, IRScope script) throws IOException {
        new IRWriter(file).persist(script);
    }
}
