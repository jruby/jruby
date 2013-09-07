package org.jruby.ext.openssl;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.util.io.ReadBuffered;

/**
 * Represents an SSLSocket that can report whether data is buffered.
 * 
 * In order to introduce ReadBuffered interface in JRuby 1.7.5 without causing
 * SSLSocket to be incompatible with older JRuby versions, we must reflectively
 * check if the ReadBuffered interface is available. If it is, we return
 * instances of SSLSocket2 that implement that interface. Otherwise, we return
 * instances of the superclass SSLSocket that does not reference it in any way.
 * 
 * Because of the lazy nature of class linking in the JVM, this allows us to
 * add an interface impl to our SSL socket implementation without breaking
 * backward compatibility.
 */
public class SSLSocket2 extends SSLSocket implements ReadBuffered {
    SSLSocket2(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }
}
