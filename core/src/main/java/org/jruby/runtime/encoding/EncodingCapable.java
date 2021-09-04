package org.jruby.runtime.encoding;

import org.jcodings.Encoding;

public interface EncodingCapable {
    Encoding getEncoding();
    void setEncoding(Encoding e);
}