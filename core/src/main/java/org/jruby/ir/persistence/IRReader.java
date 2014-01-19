/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jruby.RubyInstanceConfig;
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
import org.jruby.parser.IRStaticScopeFactory;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;

/**
 *
 * @author enebo
 */
public class IRReader {
    public static IRScope load(IRManager manager, IRReaderDecoder file) throws IOException {
        int headersOffset = file.decodeIntRaw();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("header_offset = " + headersOffset);
        int poolOffset = file.decodeIntRaw();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("pool_offset = " + headersOffset);

        file.seek(headersOffset);
        int scopesToRead  = file.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("scopes to read = " + scopesToRead);

        IRScope script = decodeScopeHeader(manager, file);
        for (int i = 1; i < scopesToRead; i++) {
            decodeScopeHeader(manager, file);
        }

        return script;
    }

    private static IRScope decodeScopeHeader(IRManager manager, IRReaderDecoder decoder) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("DECODING SCOPE HEADER");
        IRScopeType type = decoder.decodeIRScopeType();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("IRScopeType = " + type);
        String name = decoder.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("NAME = " + name);
        int line = decoder.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("LINE = " + line);
        int tempVarsCount = decoder.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("# of Temp Vars = " + tempVarsCount);
        
        int labelIndicesSize = decoder.decodeInt();
        Map<String, Integer> indices = new HashMap<String, Integer>(labelIndicesSize);
        
        for (int i = 0; i < labelIndicesSize; i++) {
            indices.put(decoder.decodeString(), decoder.decodeInt());
        }

        IRScope parent = type != IRScopeType.SCRIPT_BODY ? decoder.decodeScope() : null;
        boolean isForLoopBody = type == IRScopeType.CLOSURE ? decoder.decodeBoolean() : false;
        int arity = type == IRScopeType.CLOSURE ? decoder.decodeInt() : -1;
        int argumentType = type == IRScopeType.CLOSURE ? decoder.decodeInt() : -1;
        StaticScope parentScope = parent == null ? null : parent.getStaticScope();
        StaticScope staticScope = decodeStaticScope(decoder, parentScope);
        IRScope scope = createScope(manager, type, name, line, parent, isForLoopBody, arity, argumentType, staticScope);
        
        scope.setTemporaryVariableCount(tempVarsCount);
        // FIXME: Replace since we are defining this...perhaps even make a persistence constructor
        scope.getVarIndices().putAll(indices);

        decoder.addScope(scope);

        scope.savePersistenceInfo(decoder.decodeInt(), decoder);

        return scope;
    }

    private static StaticScope decodeStaticScope(IRReaderDecoder decoder, StaticScope parentScope) {
        StaticScope scope = IRStaticScopeFactory.newStaticScope(parentScope, decoder.decodeStaticScopeType(), decoder.decodeStringArray());

        scope.setRequiredArgs(decoder.decodeInt()); // requiredArgs has no constructor ...

        return scope;
    }

    public static IRScope createScope(IRManager manager, IRScopeType type, String name, int line,
            IRScope lexicalParent, boolean isForLoopBody, int arity, int argumentType,
            StaticScope staticScope) {

        switch (type) {
        case CLASS_BODY:
            return new IRClassBody(manager, lexicalParent, name, line, staticScope);
        case METACLASS_BODY:
            return new IRMetaClassBody(manager, lexicalParent, manager.getMetaClassName(), line, staticScope);
        case INSTANCE_METHOD:
            return new IRMethod(manager, lexicalParent, name, true, line, staticScope);
        case CLASS_METHOD:
            return new IRMethod(manager, lexicalParent, name, false, line, staticScope);
        case MODULE_BODY:
            return new IRModuleBody(manager, lexicalParent, name, line, staticScope);
        case SCRIPT_BODY:
            return new IRScriptBody(manager, "__file__", name, staticScope);
        case CLOSURE:
            return new IRClosure(manager, lexicalParent, isForLoopBody, line, staticScope, Arity.createArity(arity), argumentType);
        case EVAL_SCRIPT:
            return new IREvalScript(manager, lexicalParent, lexicalParent.getFileName(), line, staticScope);
        }

        return null;
    }
}
