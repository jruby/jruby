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

    public ArgumentDescriptor(ArgumentType type, String name) {
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

    public RubyArray toArrayForm(Ruby runtime, boolean isLambda) {
        if (type == ArgumentType.req && !isLambda) {
            return ArgumentType.opt.toArrayForm(runtime, name);
        } else {
            return type.toArrayForm(runtime, name);
        }
    }
}
