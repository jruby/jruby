package org.jruby.ext.openssl;

import org.jruby.Ruby;
import org.jruby.ext.openssl.OpenSSLReal;
import org.jruby.runtime.load.Library;

import java.io.IOException;

public class OSSLLibrary implements Library {
    public void load(Ruby runtime, boolean wrap) throws IOException {
        OpenSSLReal.createOpenSSL(runtime);
    }
}
