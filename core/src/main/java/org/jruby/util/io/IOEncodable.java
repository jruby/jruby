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
    
    public static class ConvConfig implements IOEncodable {
        public Encoding enc;
        public Encoding enc2;
        public int ecflags;
        public IRubyObject ecopts;
        public boolean bom;

        public Encoding getEnc() {
            return enc;
        }

        public void setEnc(Encoding enc) {
            this.enc = enc;
        }

        public Encoding getEnc2() {
            return enc2;
        }

        public void setEnc2(Encoding enc2) {
            this.enc2 = enc2;
        }

        public int getEcflags() {
            return ecflags;
        }

        public void setEcflags(int ecflags) {
            this.ecflags = ecflags;
        }

        public IRubyObject getEcopts() {
            return ecopts;
        }

        public void setEcopts(IRubyObject ecopts) {
            this.ecopts = ecopts;
        }

        public boolean getBOM() {
            return bom;
        }

        public void setBOM(boolean bom) {
            this.bom = bom;
        }
    }
}
