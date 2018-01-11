package org.jruby.runtime;

import org.jcodings.specific.USASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubySymbol;
import org.jruby.util.ByteList;

/**
 * The diffierent types of arguments identified in a method.
 */
public enum ArgumentType {
    key("key", false),
    keyreq("keyreq", false),
    keyrest("keyrest", false),
    block("block", false),
    opt("opt", false),
    rest("rest", false),
    req("req", false),
    anonreq("req", true),
    anonopt("opt", true),
    anonrest("rest", true),
    anonkeyrest("keyrest", true);

    ArgumentType(String typeName, boolean anonymous) {
        this.typeName = new ByteList(typeName.getBytes(), USASCIIEncoding.INSTANCE);
        this.anonymous = anonymous;
        this.name = new ByteList(toString().getBytes(), USASCIIEncoding.INSTANCE);
    }

    public static ArgumentType valueOf(char prefix) {
        switch (prefix) {
            case 'k': return key;
            case 'K': return keyreq;
            case 'e': return keyrest;
            case 'b': return block;
            case 'o': return opt;
            case 'r': return rest;
            case 'q': return req;
            case 'n': return anonreq;
            case 'O': return anonopt;
            case 'R': return anonrest;
            case 'N': return anonkeyrest;
            default: return null;
        }
    }

    public RubyArray toArrayForm(Ruby runtime, ByteList name) {
        RubySymbol typeName = runtime.newSymbol(this.typeName);

        return anonymous ? runtime.newArray(typeName) : runtime.newArray(typeName, runtime.newSymbol(name));
    }

    public ArgumentType anonymousForm() {
        switch (this) {
            case opt: return anonopt;
            case req: return anonreq;
            case rest: return anonrest;
            case keyrest: return anonkeyrest;
        }
        return this;
    }

    public final ByteList name;
    public final ByteList typeName;
    public final boolean anonymous;
}
