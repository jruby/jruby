package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyEncoding;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.api.Convert;

import java.util.Arrays;

import static org.jruby.api.Create.newString;

/**
 * A description of a single argument in a Ruby argument list.  Primarily used in Method.to_proc.
 */
public class ArgumentDescriptor {
    /** The type of the argument */
    public final ArgumentType type;

    /** The name of the argument */
    public final RubySymbol name;

    public static final ArgumentDescriptor[] EMPTY_ARRAY = new ArgumentDescriptor[0];
    public static final ArgumentDescriptor[] ANON_REST = {new ArgumentDescriptor(ArgumentType.anonrest)};
    public static final ArgumentDescriptor[] SYMBOL_PROC = {new ArgumentDescriptor(ArgumentType.anonreq), new ArgumentDescriptor(ArgumentType.anonrest)};
    static final String ENCODING_DELIMETER = ";";
    static final byte[] ANONYMOUS_ENCODED = new byte[] { '$', 'n', 'u', 'l', 'l', '$' };

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

    @Deprecated(since = "10.0")
    public final RubyArray toArrayForm(Ruby runtime, boolean isLambda) {
        return toArrayForm(runtime.getCurrentContext(), isLambda);
    }

    public final RubyArray toArrayForm(ThreadContext context, boolean isLambda) {
        ArgumentType argType = type == ArgumentType.req && !isLambda ? ArgumentType.opt : type;
        RubySymbol name = this.name;

        // FIXME: When consolidating block and method parameter arg descriptors eliminate this special *,** handling.

        // Normal {,key}rest arguments have a name but no `*`.  Empty `*` and `**` also have no name but
        // are displayed as `*` and `**`.  We do not set name properly when defining these because for
        // methods we have no reference to runtime to construct the symbol for `*` or `**`.
        if (!type.anonymous && name.getBytes().length() == 0) {
            if (type == ArgumentType.rest) {
                name = Convert.asSymbol(context, "*");
            } else if (type == ArgumentType.keyrest) {
                name = Convert.asSymbol(context, "**");
            }
        }

        return argType.toArrayForm(context, name);
    }

    public RubyString asParameterName(ThreadContext context) {
        return switch (type) {
            case req -> name.asString();
            case opt -> ((RubyString) name.asString().dup()).catString("=...");
            case key -> ((RubyString) name.asString().dup()).catString(": ...");
            case keyreq -> ((RubyString) name.asString().dup()).catString(":");
            case keyrest -> newString(context, "**").cat(name.asString());
            case block -> newString(context, "&").cat(name.asString());
            case rest -> newString(context, "*").cat(name.asString());
            case anonrest -> newString(context, "*");
            case anonkeyrest -> newString(context, "**");
            case anonreq, anonopt -> newString(context, "_");
            case nokey -> newString(context, "**nil");
        };

        // not reached
    }
    /**
     * Allow JIT/AOT to store argument descriptors as a single String constant.
     *
     * @param descriptors the list to convert into a string
     * @return the encoded descriptors
     */
    public static String encode(ArgumentDescriptor[] descriptors) {
        int length = descriptors.length;

        if (length == 0) return "";

        String[] encodedStringDescriptors = new String[descriptors.length];

        for (int i = 0; i < length; i++) {
            byte[] id;
            if (descriptors[i].name == null) { // anonymous
                id = ANONYMOUS_ENCODED;
            } else {
                id = descriptors[i].name.idString().getBytes();
            }
            byte[] entry = new byte[id.length + 1];
            entry[0] = (byte) ArgumentType.prefixFrom(descriptors[i].type);
            System.arraycopy(id, 0, entry, 1, id.length);
            encodedStringDescriptors[i] = new String(entry, RubyEncoding.ISO);
        }

        return String.join(ENCODING_DELIMETER, encodedStringDescriptors);
    }

    /**
     * Translate encoded String of argument descriptors back into an ArgumentDescriptor list
     * @param runtime to reify ids back into symbols
     * @param encodedDescriptors the encoded String
     * @return an array of descriptors
     */
    public static ArgumentDescriptor[] decode(Ruby runtime, String encodedDescriptors) {
        if (encodedDescriptors.isEmpty()) return EMPTY_ARRAY;

        String[] encodedStringDescriptors = encodedDescriptors.split(ENCODING_DELIMETER);
        int length = encodedStringDescriptors.length;

        ArgumentDescriptor[] descriptors = new ArgumentDescriptor[length];

        for (int i = 0; i < length; i++) {
            String descriptor = encodedStringDescriptors[i];
            char type = descriptor.charAt(0);
            String id = descriptor.length() == 1 ? "" : descriptor.substring(1);
            boolean anonymous = Arrays.equals(ANONYMOUS_ENCODED, id.getBytes(RubyEncoding.ISO));
            RubySymbol symbol = anonymous ? null : runtime.newSymbol(id);

            descriptors[i] = new ArgumentDescriptor(ArgumentType.valueOf(type), symbol);
        }

        return descriptors;
    }
}