/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2008 Ola Bini <ola.bini@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.ext.openssl.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import javax.crypto.Cipher;

/** c: BIO
 *
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class BIO {
    public final static int TYPE_DESCRIPTOR  = 0x0100;
    public final static int TYPE_FILTER      = 0x0200;
    public final static int TYPE_SOURCE_SINK = 0x0400;
    
    public final static int TYPE_NONE            =    0;
    public final static int TYPE_MEM             =    1  | TYPE_SOURCE_SINK;
    public final static int TYPE_FILE            =    2  | TYPE_SOURCE_SINK;
    public final static int TYPE_FD              =    4  | TYPE_SOURCE_SINK | TYPE_DESCRIPTOR;
    public final static int TYPE_SOCKET          =    5  | TYPE_SOURCE_SINK | TYPE_DESCRIPTOR;
    public final static int TYPE_NULL            =    6  | TYPE_SOURCE_SINK;
    public final static int TYPE_SSL             =    7  | TYPE_FILTER;
    public final static int TYPE_MD              =    8  | TYPE_FILTER;
    public final static int TYPE_BUFFER          =    9  | TYPE_FILTER;
    public final static int TYPE_CIPHER          =    10 | TYPE_FILTER;
    public final static int TYPE_BASE64          =    11 | TYPE_FILTER;
    public final static int TYPE_CONNECT         =    12 | TYPE_SOURCE_SINK | TYPE_DESCRIPTOR;
    public final static int TYPE_ACCEPT          =    13 | TYPE_SOURCE_SINK | TYPE_DESCRIPTOR;
    public final static int TYPE_PROXY_CLIENT    =    14 | TYPE_FILTER;
    public final static int TYPE_PROXY_SERVER    =    15 | TYPE_FILTER;
    public final static int TYPE_NBIO_TEST       =    16 | TYPE_FILTER;
    public final static int TYPE_NULL_FILTER     =    17 | TYPE_FILTER;
    public final static int TYPE_BER             =    18 | TYPE_FILTER;
    public final static int TYPE_BIO             =    19 | TYPE_SOURCE_SINK;

    private static final class BIOInputStream extends InputStream {
        private BIO bio;

        public BIOInputStream(BIO bio) {
            this.bio = bio;
        }

        @Override
        public int read() throws IOException {
            byte[] buffer = new byte[1];
            int read = bio.read(buffer, 0, 1);
            if(read == 0) {
                return -1;
            }
            return ((int)buffer[0])&0xFF;
        }

        @Override
        public int read(byte[] into) throws IOException {
            return this.read(into, 0, into.length);
        }

        @Override
        public int read(byte[] into, int off, int len) throws IOException {
            int read = bio.read(into, off, len);
            if(read == 0) {
                return -1;
            }
            return read;
        }
    }

    private static final class BIOOutputStream extends OutputStream {
        private BIO bio;

        public BIOOutputStream(BIO bio) {
            this.bio = bio;
        }

        @Override
        public void write(int b) throws IOException {
        }

        @Override
        public void write(byte[] out) throws IOException {
            this.write(out, 0, out.length);
        }

        @Override
        public void write(byte[] out, int off, int len) throws IOException {
            bio.write(out, off, len);
        }
    }

    public static InputStream asInputStream(BIO input) {
        return new BIOInputStream(input);
    }

    public static OutputStream asOutputStream(BIO output) {
        return new BIOOutputStream(output);
    }

    public static BIO base64Filter(BIO real) {
        BIO b64 = new Base64BIOFilter();
        b64.push(real);
        return b64;
    }

    public static BIO mdFilter(MessageDigest md) {
        return new MessageDigestBIOFilter(md);
    }

    public static BIO cipherFilter(Cipher cipher) {
        return new CipherBIOFilter(cipher);
    }

    public static BIO fromString(String input) {
        MemBIO bio = new MemBIO();
        byte[] buf = null;
        try {
            buf = input.getBytes("ISO8859-1");
            bio.write(buf, 0, buf.length);
        } catch(Exception e) {}
        return bio;
    }

    /** c: BIO_new(BIO_f_buffered())
     *
     */
    public static BIO buffered() {
        return null;
    }

    /** c: BIO_new(BIO_s_mem())
     *
     */
    public static BIO mem() {
        return new MemBIO();
    }

    /** c: BIO_new(BIO_s_null())
     *
     */
    public static BIO nullSink() {
        return new NullSinkBIO();
    }

    /** c: BIO_new_mem_buf
     *
     */
    public static BIO memBuf(byte[] arr) {
        return memBuf(arr, 0, arr.length);
    }

    /** c: BIO_new_mem_buf
     *
     */
    public static BIO memBuf(byte[] arr, int offset, int length) {
        // TODO: create real readonly version of MemBIO.
        try {
            BIO bio = new MemBIO();
            bio.write(arr, offset, length);
            return bio;
        } catch(IOException e) {
            return null;
        }
    }

    protected BIO nextBio;
    protected BIO prevBio;
    
    
    /** c: BIO_flush
     *
     */
    public void flush() throws IOException, PKCS7Exception {
    }

    private final static byte[] CONTENT_TEXT;
    static {
        byte[] val = null;
        try {
            val = "Content-Type: text/plain\r\n\r\n".getBytes("ISO8859-1");
        } catch(Exception e) {
            val = null;
        }
        CONTENT_TEXT = val;
    }

    /** c: SMIME_crlf_copy
     *
     */
    public void crlfCopy(BIO out, int flags) throws IOException {
        BIO in = this;
        byte[] linebuf = new byte[SMIME.MAX_SMLEN];
        int[] len = new int[]{0};

        if((flags & PKCS7.BINARY) > 0 ) {
            while((len[0] = in.read(linebuf, 0, SMIME.MAX_SMLEN)) > 0) {
                out.write(linebuf, 0, len[0]);
            }
            return;
        }
        if((flags & PKCS7.TEXT) > 0) {
            out.write(CONTENT_TEXT, 0, CONTENT_TEXT.length);
        }
        while((len[0] = in.gets(linebuf, SMIME.MAX_SMLEN)) > 0) {
            boolean eol = SMIME.stripEol(linebuf, len);
            if(len[0] != 0) {
                out.write(linebuf, 0, len[0]);
            }
            if(eol) {
                out.write(SMIME.NEWLINE, 0, 2);
            }
        }
    }

    /** c: BIO_gets
     *
     */
    public int gets(byte[] in, int len) throws IOException {
        throw new UnsupportedOperationException("for " + this.getClass().getName());
    }

    /** c: BIO_write
     *
     */
    public int write(byte[] out, int offset, int len) throws IOException {
        throw new UnsupportedOperationException("for " + this.getClass().getName());
    }

    /** c: BIO_read
     *
     */
    public int read(byte[] into, int offset, int len) throws IOException {
        throw new UnsupportedOperationException("for " + this.getClass().getName());
    }

    /** c: BIO_set_mem_eof_return
     *
     */
    public void setMemEofReturn(int value) {
        throw new UnsupportedOperationException("for " + this.getClass().getName());
    }

    /** c: BIO_push
     *
     */
    public BIO push(BIO bio) {
        BIO lb = this;
        while(lb.nextBio != null) {
            lb = lb.nextBio;
        }
        if(bio != null) {
            bio.prevBio = lb;
        }
        lb.nextBio = bio;
        return this;
    }

    /** c: BIO_pop
     *
     */
    public BIO pop() {
        BIO ret = this.nextBio;
        if(this.prevBio != null) {
            this.prevBio.nextBio = this.nextBio;
        }
        if(this.nextBio != null) {
            this.nextBio.prevBio = this.prevBio;
        }
        this.nextBio = null;
        this.prevBio = null;
        return ret;
    }

    /** c: BIO_find_type
     *
     */
    public BIO findType(int type) {
        int mask = type & 0xFF;
        BIO bio = this;
        do {
            int mt = bio.getType();
            if(mask == 0) {
                if((mt & type) != 0) {
                    return bio;
                }
            } else if(mt == type) {
                return bio;
            }
            bio = bio.nextBio;
        } while(bio != null);

        return null;
    }

    /** c: BIO_next
     *
     */
    public BIO next() {
        return this.nextBio;
    }

    public int getType() {
        return TYPE_BIO;
    }

    /** c: BIO_reset
     *
     */
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        String[] names = getClass().getName().split("\\.");
        return "#<BIO:" + names[names.length-1] + " next=" + next() + ">";
    }
}// BIO
