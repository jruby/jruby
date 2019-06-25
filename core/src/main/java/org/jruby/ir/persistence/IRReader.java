/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jruby.EvalType;
import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubySymbol;
import org.jruby.ir.*;
import org.jruby.ir.instructions.Instr;
import org.jruby.ir.operands.ClosureLocalVariable;
import org.jruby.ir.operands.LocalVariable;
import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.runtime.Signature;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.jruby.util.ByteList;
import org.jruby.util.KeyValuePair;

/**
 *
 * @author enebo
 */
public class IRReader implements IRPersistenceValues {
    public static IRScope load(IRManager manager, final IRReaderDecoder file) throws IOException {
        int version = file.decodeIntRaw();

        if (version != VERSION) {
            throw new IOException("Trying to read incompatible persistence format (version found: " +
                    version + ", version expected: " + VERSION);
        }
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
            final IRScope scope = pair.getKey();
            final int instructionsOffset = pair.getValue();

            scope.allocateInterpreterContext(new Callable<List<Instr>>() {
                public List<Instr> call() {
                    return file.decodeInstructionsAt(scope, instructionsOffset);
                }
            });
        }

        // Run through all scopes again and ensure they've calculated flags.
        // This also forces lazy instrs from above to eagerly decode.
        for (KeyValuePair<IRScope, Integer> pair: scopes) {
            final IRScope scope = pair.getKey();
            scope.computeScopeFlags();
        }

        return scopes[0].getKey(); // topmost scope;
    }

    private static KeyValuePair<IRScope, Integer> decodeScopeHeader(IRManager manager, IRReaderDecoder decoder) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("DECODING SCOPE HEADER");
        IRScopeType type = decoder.decodeIRScopeType();
        int line = decoder.decodeInt();
        int tempVarsCount = decoder.decodeInt();
        int nextLabelInt = decoder.decodeInt();

        boolean isEND = false;
        if (type == IRScopeType.CLOSURE) {
            isEND = decoder.decodeBoolean();
        }

        Signature signature;
        if (type == IRScopeType.CLOSURE || type == IRScopeType.FOR) {
            signature = Signature.decode(decoder.decodeLong());
        } else {
            signature = Signature.OPTIONAL;
        }

        // Wackiness we decode as bytelist when we encoded as symbol because currentScope is not defined yet on first
        // name of first scope.  We will use manager in this method to finish the job in constructing our symbol.
        String file = null;
        ByteList name = null;
        IRScope parent = null;
        if (type == IRScopeType.SCRIPT_BODY) {
            file = decoder.decodeString();
        } else {
            name = decoder.decodeByteList();
            parent = type != IRScopeType.SCRIPT_BODY ? decoder.decodeScope() : null;

        }

        StaticScope parentScope = parent == null ? null : parent.getStaticScope();
        // FIXME: It seems wrong we have static scope + local vars both being persisted.  They must have the same values
        // and offsets?
        StaticScope staticScope = decodeStaticScope(decoder, parentScope);
        IRScope scope = createScope(manager, type, name, file, line, parent, signature, staticScope);

        if (scope instanceof IRClosure && isEND) {
            ((IRClosure) scope).setIsEND();
        }

        scope.setTemporaryVariableCount(tempVarsCount);
        scope.setNextLabelIndex(nextLabelInt);

        // FIXME: This is odd, but ClosureLocalVariable wants it's defining closure...feels wrong.
        // But because of this we have to push decoding lvars to the end of the scope info.
        scope.setLocalVariables(decodeScopeLocalVariables(decoder, scope));

        decoder.addScope(scope);

        int instructionsOffset = decoder.decodeInt();

        return new KeyValuePair<>(scope, instructionsOffset);
    }

    private static Map<RubySymbol, LocalVariable> decodeScopeLocalVariables(IRReaderDecoder decoder, IRScope scope) {
        int size = decoder.decodeInt();
        Map<RubySymbol, LocalVariable> localVariables = new HashMap(size);
        for (int i = 0; i < size; i++) {
            RubySymbol name = scope.getManager().getRuntime().newSymbol(decoder.decodeByteList());
            int offset = decoder.decodeInt();

            localVariables.put(name, scope instanceof IRClosure ?
                    // SSS FIXME: do we need to read back locallyDefined boolean?
                    new ClosureLocalVariable(name, 0, offset) : new LocalVariable(name, 0, offset));
        }

        return localVariables;
    }

    private static StaticScope decodeStaticScope(IRReaderDecoder decoder, StaticScope parentScope) {
        StaticScope scope = StaticScopeFactory.newStaticScope(parentScope, decoder.decodeStaticScopeType(), decoder.decodeStringArray(), decoder.decodeInt());

        scope.setSignature(decoder.decodeSignature());

        return scope;
    }

    public static IRScope createScope(IRManager manager, IRScopeType type, ByteList byteName, String file, int line,
                                      IRScope lexicalParent, Signature signature, StaticScope staticScope) {
        Ruby runtime = manager.getRuntime();

        switch (type) {
        case CLASS_BODY:
            // FIXME: add saving on noe-time usage to writeer/reader
            return new IRClassBody(manager, lexicalParent, byteName, line, staticScope, false);
        case METACLASS_BODY:
            return new IRMetaClassBody(manager, lexicalParent, manager.getMetaClassName().getBytes(), line, staticScope);
        case INSTANCE_METHOD:
            return new IRMethod(manager, lexicalParent, null, byteName, true, line, staticScope, false);
        case CLASS_METHOD:
            return new IRMethod(manager, lexicalParent, null, byteName, false, line, staticScope, false);
        case MODULE_BODY:
            // FIXME: add saving on noe-time usage to writeer/reader
            return new IRModuleBody(manager, lexicalParent, byteName, line, staticScope, false);
        case SCRIPT_BODY:
            return new IRScriptBody(manager, file, staticScope);
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
