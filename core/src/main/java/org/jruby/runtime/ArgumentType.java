package org.jruby.runtime;

/**
 * Created by headius on 5/8/15.
 */
public enum ArgumentType {
    key('k'), keyreq('K'), keyrest('e'), block('b'), opt('o'), rest('r'), req('q');

    ArgumentType(char prefix) {
        this.prefix = prefix;
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
            // for 'R' used by prefix for to represent anonymous restarg
            case 'R': return rest;
            // for 'nil' used by old compiler to represent array destructuring
            case 'n': return req;
            default: return null;
        }
    }

    public final char prefix;
}
