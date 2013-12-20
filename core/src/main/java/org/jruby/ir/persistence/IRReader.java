/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ir.persistence;

import java.io.IOException;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScopeType;
import org.jruby.parser.IRStaticScope;
import org.jruby.parser.IRStaticScopeFactory;
import org.jruby.parser.StaticScope;

/**
 *
 * @author enebo
 */
public class IRReader {
    public static void load(IRReaderDecoder file, IRScope script) throws IOException {
        int headersOffset = file.decodeInt();
        int poolOffset = file.decodeInt();
        
        file.seek(headersOffset);
        int scopesToRead  = file.decodeInt();
        
        for (int i = 0; i < scopesToRead; i++) {
            decodeScopeHeader(file, null);
        }
    }
    
    private static void decodeScopeHeader(IRReaderDecoder decoder, IRScope parent) {
        IRScopeType type = decoder.decodeIRScopeType();
        String name = decoder.decodeString();
        int line = decoder.decodeInt();
        
        if (type != IRScopeType.SCRIPT_BODY) {
            String parentName = decoder.decodeString();
            int parentLine = decoder.decodeInt();
        }
        
        if (type == IRScopeType.CLOSURE) {
            boolean isForLoopBody = decoder.decodeBoolean();
            int arity = decoder.decodeInt();
            int argumentType = decoder.decodeInt();
        }
        
        StaticScope parentScope = parent == null ? null : parent.getStaticScope();
        StaticScope scope = decodeStaticScope(decoder, parentScope);
    }

    private static StaticScope decodeStaticScope(IRReaderDecoder decoder, StaticScope parentScope) {
        StaticScope scope = IRStaticScopeFactory.newStaticScope(parentScope, decoder.decodeStaticScopeType(), decoder.decodeStringArray());
        
        scope.setRequiredArgs(decoder.decodeInt()); // requiredArgs has no constructor ...
        
        return scope;
    }
}
