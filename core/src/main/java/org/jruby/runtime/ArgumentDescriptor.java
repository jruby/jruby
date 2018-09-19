package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubySymbol;

/**
 * A description of a single argument in a Ruby argument list.
 */
public class ArgumentDescriptor {
    /** The type of the argument */
    public final ArgumentType type;

    /** The name of the argument */
    public final RubySymbol name;

    public static final ArgumentDescriptor[] EMPTY_ARRAY = new ArgumentDescriptor[0];
    public static final ArgumentDescriptor[] ANON_REST = {new ArgumentDescriptor(ArgumentType.anonrest)};

    public ArgumentDescriptor(ArgumentType type, RubySymbol name) {
        if (name == null && !type.anonymous) {
            throw new RuntimeException("null argument name given for non-anonymous argument type");
        }

        this.type = type;
        this.name = name;
    }

    public ArgumentDescriptor(ArgumentType type) {
        this(type, null);
    }

    public final RubyArray toArrayForm(Ruby runtime, boolean isLambda) {
        ArgumentType argType = type == ArgumentType.req && !isLambda ? ArgumentType.opt : type;

        return argType.toArrayForm(runtime, name);
    }
}