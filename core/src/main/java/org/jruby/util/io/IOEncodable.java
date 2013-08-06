package org.jruby.util.io;

import org.jcodings.Encoding;

/**
 * Represents an IO encodable object.  This is IO/File/GZipFile/....
 */
public interface IOEncodable {
    public void setEnc(Encoding enc);
    public void setEnc2(Encoding enc2);
    public void setBOM(boolean bom);
}
