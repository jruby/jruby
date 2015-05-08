package org.jruby.internal.runtime.methods;

import org.jruby.runtime.Signature;

public interface IRMethodArgs {
    // FIXME: Should get pushed to DynamicMethod
    public Signature getSignature();
    public ArgumentDescriptor[] getArgumentDescriptors();

    public enum ArgumentType {
        key, keyreq, keyrest, block, opt, rest, req
    }

    public class ArgumentDescriptor {
        public final ArgumentType type;
        public final String name;
        public static final ArgumentDescriptor[] EMPTY_ARRAY = new ArgumentDescriptor[0];

        public ArgumentDescriptor(ArgumentType type, String name) {
            this.type = type;
            this.name = name;
        }

        public String toShortDesc() {
            switch (type) {
                case keyreq: return "K" + name;
                case keyrest: return "e" + name;
                case req: return "q" + name;
                default: return type.name().charAt(0) + name;
            }
        }
    }
}
