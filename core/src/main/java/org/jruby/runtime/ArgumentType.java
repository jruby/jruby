package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubySymbol;
import org.jruby.internal.runtime.methods.DescriptorInfo;

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

    ArgumentType(String typeId, boolean anonymous) {
        this.typeId = typeId;
        this.anonymous = anonymous;
        this.name = toString();
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
            // These are sourced from DescriptorInfo because ArgumentType references Ruby.
            case DescriptorInfo.ANONREQ_CHAR: return anonreq;
            case DescriptorInfo.ANONOPT_CHAR: return anonopt;
            case DescriptorInfo.ANONREST_CHAR: return anonrest;
            case 'N': return anonkeyrest;
            default: return null;
        }
    }

    // FIXME: This could have been part of enum but I was concerned about whether any extension uses ArgumentType.
    static char prefixFrom(ArgumentType type) {
        switch (type) {
            case key: return 'k';
            case keyreq: return 'K';
            case keyrest: return 'e';
            case block: return 'b';
            case opt: return 'o';
            case rest: return 'r';
            case req: return 'q';
            case anonreq: return DescriptorInfo.ANONREQ_CHAR;
            case anonopt: return DescriptorInfo.ANONOPT_CHAR;
            case anonrest: return DescriptorInfo.ANONREST_CHAR;
            case anonkeyrest: return 'N';
            default: throw new IllegalArgumentException("Bogus type for ArgumentType: '" + type + "'");
        }
    }

    public RubyArray toArrayForm(Ruby runtime, RubySymbol name) {
        RubySymbol typeName = runtime.newSymbol(typeId);

        return anonymous ? runtime.newArray(typeName) : runtime.newArray(typeName, name);
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

    public final String name;
    public final String typeId;
    public final boolean anonymous;
}
