package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubySymbol;
import org.jruby.api.Convert;
import org.jruby.internal.runtime.methods.DescriptorInfo;

import static org.jruby.api.Create.newArray;

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
        return switch (prefix) {
            case 'k' -> key;
            case 'K' -> keyreq;
            case 'e' -> keyrest;
            case 'b' -> block;
            case 'o' -> opt;
            case 'r' -> rest;
            case 'q' -> req;
            case 'l' -> nokey;
            // These are sourced from DescriptorInfo because ArgumentType references Ruby.
            case DescriptorInfo.ANONREQ_CHAR -> anonreq;
            case DescriptorInfo.ANONOPT_CHAR -> anonopt;
            case DescriptorInfo.ANONREST_CHAR -> anonrest;
            case 'N' -> anonkeyrest;
            default -> null;
        };
    }

    // FIXME: This could have been part of enum but I was concerned about whether any extension uses ArgumentType.
    static char prefixFrom(ArgumentType type) {
        return switch (type) {
            case key -> 'k';
            case keyreq -> 'K';
            case keyrest -> 'e';
            case block -> 'b';
            case opt -> 'o';
            case rest -> 'r';
            case req -> 'q';
            case nokey -> 'l';
            case anonreq -> DescriptorInfo.ANONREQ_CHAR;
            case anonopt -> DescriptorInfo.ANONOPT_CHAR;
            case anonrest -> DescriptorInfo.ANONREST_CHAR;
            case anonkeyrest -> 'N';
        };
    }

    @Deprecated(since = "10.0.0.0")
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
