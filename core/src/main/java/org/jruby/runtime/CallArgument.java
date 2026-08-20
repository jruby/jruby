package org.jruby.runtime;

import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.Arrays;

public record CallArgument(int index, Type type, Identifier identifier) {
    public enum Type {
        POSITIONAL('p', IRubyObject.class),
        POSITIONAL_SPLAT('P', IRubyObject.class),
        KEYWORD('k', IRubyObject.class),
        KEYWORD_SPLAT('K', IRubyObject.class),
        CONTEXT('x', ThreadContext.class),
        STATIC_SCOPE('s', StaticScope.class),
        RECEIVER('r', IRubyObject.class),
        CALLER('l', IRubyObject.class),
        BLOCK('b', Block.class),
        BLOCK_PASS('B', Block.class);

        public final char sigil;
        public final Class klass;

        Type(char sigil, Class klass) {
            this.sigil = sigil;
            this.klass = klass;
        }

        public static Type forSigil(char sigil) {
            return switch (sigil) {
                case 'p' -> POSITIONAL;
                case 'P' -> POSITIONAL_SPLAT;
                case 'k' -> KEYWORD;
                case 'K' -> KEYWORD_SPLAT;
                case 'x' -> CONTEXT;
                case 's' -> STATIC_SCOPE;
                case 'r' -> RECEIVER;
                case 'l' -> CALLER;
                case 'b' -> BLOCK;
                case 'B' -> BLOCK_PASS;
                default -> throw new IllegalArgumentException("unknown sigil: " + sigil);
            };
        }
    }

    public record Identifier(String id) {
        public static final Identifier CONTEXT = new Identifier("%context");
        public static final Identifier STATIC_SCOPE = new Identifier("%static_scope");
        public static final Identifier RECEIVER = new Identifier("%receiver");
        public static final Identifier CALLER = new Identifier("%caller");
        public static final Identifier BLOCK = new Identifier("%block");

        public static Identifier forName(String name) {
            return switch (name) {
                case "%context" -> CONTEXT;
                case "%static_scope" -> STATIC_SCOPE;
                case "%receiver" -> RECEIVER;
                case "%caller" -> CALLER;
                case "%block" -> BLOCK;
                default -> new Identifier(name);
            };
        }
    }

    public String encode() {
        return String.format("%s:%s:%s", index, type.sigil, identifier.id);
    }

    public static CallArgument decodeSingle(String encoded) {
        String[] split = encoded.split(":");
        return new CallArgument(Integer.parseInt(split[0]), Type.forSigil(split[1].charAt(0)), Identifier.forName(split[2]));
    }

    public static String encode(CallArgument[] callArguments) {
        // build keywords descriptor
        StringBuilder descriptorBuilder = new StringBuilder();
        for (var callArgument : callArguments) {
            if (!descriptorBuilder.isEmpty()) descriptorBuilder.append(',');
            descriptorBuilder.append(callArgument.encode()); // TODO: encoding etc
        }
        return descriptorBuilder.toString();
    }

    public static CallArgument[] decodeMulti(String encoded) {
        return Arrays.stream(encoded.split(","))
                .map(CallArgument::decodeSingle)
                .toArray(CallArgument[]::new);
    }

    public static Class[] classes(CallArgument[] callArguments) {
        return Arrays.stream(callArguments)
                .map(c -> c.type.klass)
                .toArray(Class[]::new);
    }

}
