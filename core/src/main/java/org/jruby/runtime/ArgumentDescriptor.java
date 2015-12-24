package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;

/**
 * A description of a single argument in a Ruby argument list.
 */
public class ArgumentDescriptor {
    /** The type of the argument */
    public final ArgumentType type;

    /** The name of the argument */
    public final String name;

    public static final ArgumentDescriptor[] EMPTY_ARRAY = new ArgumentDescriptor[0];
    public static final ArgumentDescriptor[] ANON_REST = {new ArgumentDescriptor(ArgumentType.anonrest)};

    public ArgumentDescriptor(ArgumentType type, String name) {
        if (name == null && !type.anonymous) {
            throw new RuntimeException("null argument name given for non-anonymous argument type");
        }

        this.type = type;
        this.name = name;
    }

    public ArgumentDescriptor(ArgumentType type) {
        this(type, null);
    }

    /**
     * Generate the prefixed version of this descriptor.
     *
     * @see org.jruby.internal.runtime.methods.MethodArgs2
     */
    public String toPrefixForm() {
        return type.renderPrefixForm(name);
    }

    public final RubyArray toArrayForm(Ruby runtime, boolean isLambda) {
        if ( type == ArgumentType.req && ! isLambda ) {
            return ArgumentType.opt.toArrayForm(runtime, name);
        }
        return type.toArrayForm(runtime, name);
    }
}
