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
    private final IRubyObject io;
    private final InputStream in;
    private final IRubyObject numOne;
    private static final CallSite readAdapter = MethodIndex.getFunctionalCallSite("read");
    private static final CallSite closeAdapter = MethodIndex.getFunctionalCallSite("close");

    /**
     * Creates a new InputStream with the object provided.
     *
     * @param io the ruby object
     */
    public IOInputStream(IRubyObject io) {
        if (!io.respondsTo("read")) {
            throw new IllegalArgumentException("Object: " + io + " is not a legal argument to this wrapper, cause it doesn't respond to \"read\".");
        }
        this.io = io;
        this.in = ((io instanceof RubyIO) && !((RubyIO)io).isClosed() && ((RubyIO)io).isBuiltin("read"))
                ? ((RubyIO)io).getInStream() : null;
        this.numOne = RubyFixnum.one(this.io.getRuntime());
    }

    @Override
    public void close() throws IOException {
        if (in != null) {
            in.close();
        } else if (io.respondsTo("close")) {
            closeAdapter.call(io.getRuntime().getCurrentContext(), io, io);
        }
    }

    // Note: this method produces meaningful results
    // only for RubyIO objects. For everything else returns 0.
    @Override
    public int available() throws IOException {
        if (in != null) {
            return in.available();
        } else {
            return 0;
        }
    }
    
    public int read() throws IOException {
        if (in != null) {
            return in.read();
        }
        IRubyObject readValue = readAdapter.call(io.getRuntime().getCurrentContext(), io, io, numOne);
        int returnValue = -1;
        if (!readValue.isNil()) {
            returnValue = readValue.convertToString().getByteList().get(0) & 0xff;
        }
        return returnValue;
    }

    @Override
    public int read(byte[] b) throws IOException {
        if (in != null) {
            return in.read(b, 0, b.length);
        }
        IRubyObject readValue = readAdapter.call(io.getRuntime().getCurrentContext(), io, io, io.getRuntime().newFixnum(b.length));
        int returnValue = -1;
        if (!readValue.isNil()) {
            ByteList str = readValue.convertToString().getByteList();
            System.arraycopy(str.getUnsafeBytes(), str.getBegin(), b, 0, str.getRealSize());
            returnValue = str.getRealSize();
        }
        return returnValue;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (in != null) {
            return in.read(b, off, len);
        }
        IRubyObject readValue = readAdapter.call(io.getRuntime().getCurrentContext(), io, io, io.getRuntime().newFixnum(len));
        int returnValue = -1;
        if (!readValue.isNil()) {
            ByteList str = readValue.convertToString().getByteList();
            System.arraycopy(str.getUnsafeBytes(), str.getBegin(), b, off, str.getRealSize());
            returnValue = str.getRealSize();
        }
        return returnValue;
     }
}
