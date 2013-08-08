package org.jruby.util.io;

import org.jcodings.Encoding;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Represents an IO encodable object.  This is IO/File/GZipFile/....
 */
public interface IOEncodable {
    public void setEnc(Encoding enc);
    public void setEnc2(Encoding enc2);
    public Encoding getEnc();
    public Encoding getEnc2();
    public void setEcflags(int ecflags);
    public int getEcflags();
    public void setEcopts(IRubyObject ecopts);
    public IRubyObject getEcopts();
    public void setBOM(boolean bom);
    public boolean getBOM();
}
