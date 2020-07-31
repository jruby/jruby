/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import org.jruby.EvalType;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.*;
import org.jruby.parser.StaticScope;
import org.jruby.parser.StaticScopeFactory;
import org.jruby.runtime.Signature;

import java.io.IOException;
import java.util.EnumSet;

import org.jruby.util.ByteList;

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

        file.seek(headersOffset);
        int scopesToRead  = file.decodeInt();
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("scopes to read = " + scopesToRead);

        IRScope firstScope = null;
        for (int i = 0; i < scopesToRead; i++) {
            IRScopeType type = file.decodeIRScopeType();
            int line = file.decodeInt();
            int tempVarsCount = file.decodeInt();
            int nextLabelInt = file.decodeInt();
            IRScope scope = decodeScopeHeader(manager, file, type, line);
            scope.setNextLabelIndex(nextLabelInt);
            EnumSet<IRFlags> flags = file.decodeIRFlags();
            if (file.decodeBoolean()) scope.setHasBreakInstructions();
            if (file.decodeBoolean()) scope.setHasLoops();
            if (file.decodeBoolean()) scope.setHasNonLocalReturns();
            if (file.decodeBoolean()) scope.setReceivesClosureArg();
            if (file.decodeBoolean()) scope.setReceivesKeywordArgs();
            if (file.decodeBoolean()) scope.setAccessesParentsLocalVariables();
            if (file.decodeBoolean()) scope.setIsMaybeUsingRefinements();
            if (file.decodeBoolean()) scope.setCanCaptureCallersBinding();
            if (file.decodeBoolean()) scope.setCanReceiveBreaks();
            if (file.decodeBoolean()) scope.setCanReceiveNonlocalReturns();
            if (file.decodeBoolean()) scope.setUsesZSuper();
            if (file.decodeBoolean()) scope.setNeedsCodeCoverage();
            if (file.decodeBoolean()) scope.setUsesEval();

            if (firstScope == null) firstScope = scope;
            int instructionsOffset = file.decodeInt();
            int poolOffset = file.decodeInt();

            scope.allocateInterpreterContext(() -> file.dup().decodeInstructionsAt(scope, poolOffset, instructionsOffset), tempVarsCount, flags);
        }

        return firstScope; // topmost scope;
    }

    private static IRScope decodeScopeHeader(IRManager manager, IRReaderDecoder decoder, IRScopeType type, int line) {
        if (RubyInstanceConfig.IR_READING_DEBUG) System.out.println("DECODING SCOPE HEADER");

        boolean isEND = false;

        Signature signature;
        if (type == IRScopeType.CLOSURE || type == IRScopeType.FOR) {
            isEND = decoder.decodeBoolean();
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
            parent = decoder.decodeScope();
        }

        StaticScope parentScope = parent == null ? null : parent.getStaticScope();
        // FIXME: It seems wrong we have static scope + local vars both being persisted.  They must have the same values
        // and offsets?
        StaticScope staticScope = decodeStaticScope(decoder, parentScope);
        IRScope scope = createScope(manager, type, name, file, line, parent, signature, staticScope);

        if (scope instanceof IRClosure && isEND) {
            ((IRClosure) scope).setIsEND();
        }

        decoder.addScope(scope);

        return scope;
    }

    private static StaticScope decodeStaticScope(IRReaderDecoder decoder, StaticScope parentScope) {
        StaticScope scope = StaticScopeFactory.newStaticScope(parentScope, decoder.decodeStaticScopeType(), decoder.decodeStringArray(), decoder.decodeInt());

        scope.setSignature(decoder.decodeSignature());

        return scope;
    }

    public static IRScope createScope(IRManager manager, IRScopeType type, ByteList byteName, String file, int line,
                                      IRScope lexicalParent, Signature signature, StaticScope staticScope) {
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
