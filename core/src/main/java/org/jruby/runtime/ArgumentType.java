package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubySymbol;
import org.jruby.api.Convert;
import org.jruby.internal.runtime.methods.DescriptorInfo;

import static org.jruby.api.Create.newArray;
import static org.jruby.api.Convert.asSymbol;

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
    anonkeyrest("keyrest", true),
    nokey("nokey", true);

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
            case 'l': return nokey;
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
            case nokey: return 'l';
            case anonreq: return DescriptorInfo.ANONREQ_CHAR;
            case anonopt: return DescriptorInfo.ANONOPT_CHAR;
            case anonrest: return DescriptorInfo.ANONREST_CHAR;
            case anonkeyrest: return 'N';
            default: throw new IllegalArgumentException("Bogus type for ArgumentType: '" + type + "'");
        }
    }

    /**
     * @param runtime
     * @param name
     * @return ""
     * @deprecated Use #{@link ArgumentType#toArrayForm(ThreadContext, RubySymbol)} instead.
     */
    @Deprecated(since = "10.0", forRemoval = true)
    public RubyArray toArrayForm(Ruby runtime, RubySymbol name) {
        return toArrayForm(runtime.getCurrentContext(), name);
    }

    public RubyArray toArrayForm(ThreadContext context, RubySymbol name) {
        RubySymbol typeName = Convert.asSymbol(context, typeId);

        return anonymous ? newArray(context, typeName) : newArray(context, typeName, name);
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
