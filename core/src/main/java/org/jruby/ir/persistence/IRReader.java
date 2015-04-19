/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jruby.EvalType;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.*;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Signature;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.jruby.util.KeyValuePair;

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

        KeyValuePair<IRScope, Integer>[] scopes = new KeyValuePair[scopesToRead];
        for (int i = 0; i < scopesToRead; i++) {
            scopes[i] = decodeScopeHeader(manager, file);
        }

        // Lifecycle woes.  All IRScopes need to exist before we can decodeInstrs.
        for (KeyValuePair<IRScope, Integer> pair: scopes) {
            IRScope scope = pair.getKey();
            int instructionsOffset = pair.getValue();

            scope.allocateInterpreterContext(file.decodeInstructionsAt(scope, instructionsOffset));
        }

        return scopes[0].getKey(); // topmost scope;
    }

    private static KeyValuePair<IRScope, Integer> decodeScopeHeader(IRManager manager, IRReaderDecoder decoder) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("DECODING SCOPE HEADER");
        IRScopeType type = decoder.decodeIRScopeType();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("IRScopeType = " + type);
        String name = decoder.decodeString();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("NAME = " + name);
        int line = decoder.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("LINE = " + line);
        int tempVarsCount = decoder.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("# of Temp Vars = " + tempVarsCount);
        Map<String, Integer> indices = decodeScopeLabelIndices(decoder);

        IRScope parent = type != IRScopeType.SCRIPT_BODY ? decoder.decodeScope() : null;
        Signature signature;

        if (type == IRScopeType.CLOSURE || type == IRScopeType.FOR) {
            signature = Signature.decode(decoder.decodeLong());
        } else {
            signature = Signature.OPTIONAL;
        }
        StaticScope parentScope = parent == null ? null : parent.getStaticScope();
        // FIXME: It seems wrong we have static scope + local vars both being persisted.  They must have the same values
        // and offsets?
        StaticScope staticScope = decodeStaticScope(decoder, parentScope);
        IRScope scope = createScope(manager, type, name, line, parent, signature, staticScope);

        scope.setTemporaryVariableCount(tempVarsCount);
        // FIXME: Replace since we are defining this...perhaps even make a persistence constructor
        scope.setLabelIndices(indices);

        // FIXME: This is odd, but ClosureLocalVariable wants it's defining closure...feels wrong.
        // But because of this we have to push decoding lvars to the end of the scope info.
        scope.setLocalVariables(decodeScopeLocalVariables(decoder, scope));

        decoder.addScope(scope);

        int instructionsOffset = decoder.decodeInt();

        return new KeyValuePair<>(scope, instructionsOffset);
    }

    private static Map<String, LocalVariable> decodeScopeLocalVariables(IRReaderDecoder decoder, IRScope scope) {
        int size = decoder.decodeInt();
        Map<String, LocalVariable> localVariables = new HashMap(size);
        for (int i = 0; i < size; i++) {
            String name = decoder.decodeString();
            int offset = decoder.decodeInt();

            localVariables.put(name, scope instanceof IRClosure ?
                    // SSS FIXME: do we need to read back locallyDefined boolean?
                    new ClosureLocalVariable(name, 0, offset) : new LocalVariable(name, 0, offset));
        }

        return localVariables;
    }

    private static Map<String, Integer> decodeScopeLabelIndices(IRReaderDecoder decoder) {
        int labelIndicesSize = decoder.decodeInt();
        Map<String, Integer> indices = new HashMap<String, Integer>(labelIndicesSize);
        for (int i = 0; i < labelIndicesSize; i++) {
            indices.put(decoder.decodeString(), decoder.decodeInt());
        }
        return indices;
    }

    private static StaticScope decodeStaticScope(IRReaderDecoder decoder, StaticScope parentScope) {
        StaticScope scope = StaticScopeFactory.newStaticScope(parentScope, decoder.decodeStaticScopeType(), decoder.decodeStringArray());

        scope.setSignature(decoder.decodeSignature());

        return scope;
    }

    public static IRScope createScope(IRManager manager, IRScopeType type, String name, int line,
            IRScope lexicalParent, Signature signature, StaticScope staticScope) {

        switch (type) {
        case CLASS_BODY:
            return new IRClassBody(manager, lexicalParent, name, line, staticScope);
        case METACLASS_BODY:
            return new IRMetaClassBody(manager, lexicalParent, manager.getMetaClassName(), line, staticScope);
        case INSTANCE_METHOD:
            return new IRMethod(manager, lexicalParent, null, name, true, line, staticScope);
        case CLASS_METHOD:
            return new IRMethod(manager, lexicalParent, null, name, false, line, staticScope);
        case MODULE_BODY:
            return new IRModuleBody(manager, lexicalParent, name, line, staticScope);
        case SCRIPT_BODY:
            return new IRScriptBody(manager, name, staticScope);
        case FOR:
            return new IRFor(manager, lexicalParent, line, staticScope, signature);
        case CLOSURE:
            return new IRClosure(manager, lexicalParent, line, staticScope, signature);
        case EVAL_SCRIPT:
            // SSS FIXME: This is broken right now -- the isModuleEval arg has to be persisted and then read back.
            return new IREvalScript(manager, lexicalParent, lexicalParent.getFileName(), line, staticScope, EvalType.NONE);
        }

        throw new RuntimeException("No such scope type: " + type);
    }
}
