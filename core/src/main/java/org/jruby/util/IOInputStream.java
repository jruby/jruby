/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <Ola.Bini@ki.se>
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

package org.jruby.util;

import java.io.InputStream;
import java.io.IOException;
import org.jcodings.Encoding;

import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyString;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.MethodIndex;

/**
 * This class wraps a IRubyObject in an InputStream. Depending on which messages
 * the IRubyObject answers to, it will have different functionality.
 * 
 * The point is that the IRubyObject could exhibit duck typing, in the style of IO versus StringIO, for example.
 *
 * At the moment, the only functionality supported is reading, and the only requirement on the io-object is
 * that it responds to read() like IO.
 */
public class IOInputStream extends InputStream {
    private final Ruby runtime;
    private final IRubyObject io;
    private final InputStream in;
    private final IRubyObject numOne;
    private final CallSite readAdapter = MethodIndex.getFunctionalCallSite("read");
    private final CallSite closeAdapter = MethodIndex.getFunctionalCallSite("close");

    /**
     * Creates a new InputStream with the object provided.
     *
     * @param io the ruby object
     */
    public IOInputStream(IRubyObject io) {
        this(io, true);
    }

    public IOInputStream(final IRubyObject io, boolean verifyCanRead) {
        this.runtime = io.getRuntime();
        this.io = io;
        this.in = ( io instanceof RubyIO && !((RubyIO) io).isClosed() &&
                ((RubyIO) io).isBuiltin("read") ) ?
                    ((RubyIO) io).getInStream() : null;
        if (this.in == null) {
            if (verifyCanRead && !io.respondsTo("read")) {
                throw new IllegalArgumentException("Object: " + io + " is not a legal argument to this wrapper, " +
                        "cause it doesn't respond to \"read\".");
            }
        }
        this.numOne = RubyFixnum.one(runtime);
    }

    @Override
    public void close() throws IOException {
        InputStream in = this.in;
        if (in != null) {
            in.close();
        } else {
            IRubyObject io = this.io;
            if (io.respondsTo("close")) {
                closeAdapter.call(runtime.getCurrentContext(), io, io);
            }
        }
    }

    // Note: this method produces meaningful results
    // only for RubyIO objects. For everything else returns 0.
    @Override
    public int available() throws IOException {
        InputStream in = this.in;
        if (in != null) {
            return in.available();
        } else {
            return 0;
        }
    }
    
    public int read() throws IOException {
        InputStream in = this.in;

        if (in != null) {
            return in.read();
        }

        IRubyObject io = this.io;
        IRubyObject readValue = readAdapter.call(runtime.getCurrentContext(), io, io, numOne);

        if (readValue.isNil()) return -1;

        return readValue.convertToString().getByteList().get(0) & 0xff;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        InputStream in = this.in;

        if (in != null) {
            return in.read(b, off, len);
        }

        Ruby runtime = this.runtime;
        IRubyObject io = this.io;
        IRubyObject readValue = readAdapter.call(runtime.getCurrentContext(), io, io, runtime.newFixnum(len));

        if (readValue.isNil()) return -1;

        ByteList str = readValue.convertToString().getByteList();
        System.arraycopy(str.getUnsafeBytes(), str.getBegin(), b, off, str.getRealSize());

        return str.getRealSize();
     }
}
