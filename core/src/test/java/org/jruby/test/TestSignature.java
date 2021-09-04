package org.jruby.test;

import junit.framework.TestCase;
import org.jruby.runtime.Signature;

public class TestSignature extends TestCase {
    public void testEncodeDecode() {
        Signature sig = Signature.from(1, 1, 1, 1, 1, Signature.Rest.NORM, 1);

        assertEquals(sig, Signature.decode(sig.encode()));

        sig = Signature.from(1, 1, 1, 1, 1, Signature.Rest.ANON, -11);

        assertEquals(sig, Signature.decode(sig.encode()));
    }
}
