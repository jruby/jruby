package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;

/**
 * Created by headius on 5/8/15.
 */
public enum ArgumentType {

    key("key", 'k', false),
    keyreq("keyreq", 'K', false),
    keyrest("keyrest", 'e', false),
    block("block", 'b', false),
    opt("opt", 'o', false),
    rest("rest", 'r', false),
    req("req", 'q', false),
    anonreq("req", 'n', true),
    anonopt("opt", 'O', true),
    anonrest("rest", 'R', true),
    anonkeyrest("keyrest", 'N', true);

    public static final String ANONOPT = Character.toString( anonopt.prefix );
    public static final String ANONREST = Character.toString( anonrest.prefix );
    public static final String REQ = Character.toString( req.prefix );

    private ArgumentType(String symbolicName, char prefix, boolean anonymous) {
        this.symbolicName = symbolicName;
        this.prefix = prefix;
        this.anonymous = anonymous;
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

    public String renderPrefixForm(String name) {
        return anonymous ? String.valueOf(prefix) : prefix + name;
    }

    public RubyArray toArrayForm(Ruby runtime, String name) {
        // we check for null name here as a precaution, since it will certainly blow up newSymbol (#3086)
        return anonymous || name == null ? runtime.newArray(runtime.newSymbol(symbolicName)) : runtime.newArray(runtime.newSymbol(symbolicName), runtime.newSymbol(name));
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

    public final String symbolicName;
    private final char prefix;
    public final boolean anonymous;
}
