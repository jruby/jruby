package org.jruby.runtime.encoding;

import org.jcodings.Encoding;

public interface MarshalEncoding {
    boolean shouldMarshalEncoding();
    Encoding getMarshalEncoding();
}
