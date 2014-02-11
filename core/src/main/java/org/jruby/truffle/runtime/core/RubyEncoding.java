package org.jruby.truffle.runtime.core;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.truffle.runtime.objects.RubyBasicObject;

/**
 * This is a bridge between JRuby encoding and Truffle encoding
 */
public class RubyEncoding extends RubyObject{

    private final org.jruby.RubyEncoding rubyEncoding;

    /**
     * The class from which we create the object that is {@code Encoding}. A subclass of
     * {@link RubyClass} so that we can override {@link #newInstance} and allocate a
     * {@link RubyEncoding} rather than a normal {@link org.jruby.truffle.runtime.objects.RubyBasicObject}.
     */
    public static class RubyStringClass extends RubyClass {

        public RubyStringClass(RubyClass objectClass) {
            super(null, objectClass, "Encoding");
        }

        @Override
        public RubyBasicObject newInstance() {
            return new RubyEncoding(getContext().getCoreLibrary().getEncodingClass(), USASCIIEncoding.INSTANCE);
        }

    }

    private RubyEncoding(RubyClass symbolClass, byte[] name, int p, int end, boolean isDummy) {
        super(symbolClass);
        rubyEncoding = org.jruby.RubyEncoding.newEncoding(getJRubyRuntime(), name, p, end, isDummy);
    }

    public RubyEncoding(RubyClass symbolClass, Encoding encoding) {
        super(symbolClass);
        rubyEncoding = org.jruby.RubyEncoding.newEncoding(getJRubyRuntime(), encoding);
    }

}
