package org.jruby.util.io;

import org.jcodings.Encoding;

/**
 * Represents an IO encodable object.  This is IO/File/GZipFile/....
 */
public interface IOEncodable {
    public void setReadEncoding(Encoding readEncoding);
    public void setWriteEncoding(Encoding writeEncoding);
    public void setBOM(boolean bom);
}
