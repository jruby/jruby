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
    public static class RubyEncodingClass extends RubyClass {

        public RubyEncodingClass(RubyClass objectClass) {
            super(null, objectClass, "Encoding");
        }

        @Override
        public RubyBasicObject newInstance() {
            return new RubyEncoding(getContext().getCoreLibrary().getEncodingClass(), USASCIIEncoding.INSTANCE);
        }

    }

    private RubyEncoding(RubyClass encodingClass, byte[] name, int p, int end, boolean isDummy) {
        super(encodingClass);
        rubyEncoding = org.jruby.RubyEncoding.newEncoding(getJRubyRuntime(), name, p, end, isDummy);
    }

    public RubyEncoding(RubyClass encodingClass, Encoding encoding) {
        super(encodingClass);
        rubyEncoding = org.jruby.RubyEncoding.newEncoding(getJRubyRuntime(), encoding);
    }

    public RubyEncoding(RubyClass encodingClass, org.jruby.RubyEncoding jrubyEncoding) {
        super(encodingClass);
        this.rubyEncoding = jrubyEncoding;
    }

    public static RubyEncoding findEncodingByName(RubyString name) {
        org.jruby.RubyString string = org.jruby.RubyString.newString(name.getJRubyRuntime(), name.toString());
        org.jruby.RubyEncoding enc = (org.jruby.RubyEncoding) name.getJRubyRuntime().getEncodingService().rubyEncodingFromObject(string);

        return new RubyEncoding(name.getRubyClass().getContext().getCoreLibrary().getEncodingClass(), enc);
    }


    public String toString(){
        return rubyEncoding.to_s(getJRubyRuntime().getCurrentContext()).asJavaString();
    }

    public String inspect() {
        return rubyEncoding.inspect(getJRubyRuntime().getCurrentContext()).asJavaString();
    }

}
