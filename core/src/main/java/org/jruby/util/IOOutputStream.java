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

import java.io.OutputStream;
import java.io.IOException;
import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.RubyIO;
import org.jruby.RubyString;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.MethodIndex;

/**
 * This class wraps a IRubyObject in an OutputStream. Depending on which messages
 * the IRubyObject answers to, it will have different functionality.
 * 
 * The point is that the IRubyObject could exhibit duck typing, in the style of IO versus StringIO, for example.
 *
 * At the moment, the only functionality supported is writing, and the only requirement on the io-object is
 * that it responds to write() and close() like IO.
 * 
 * @author <a href="mailto:Ola.Bini@ki.se">Ola Bini</a>
 */
public class IOOutputStream extends OutputStream {
    private final IRubyObject io;
    private final OutputStream out;
    private final CallSite writeAdapter;
    private static final CallSite closeAdapter = MethodIndex.getFunctionalCallSite("close");
    private final Encoding encoding;

    /**
     * Creates a new OutputStream with the object provided.
     *
     * @param io the ruby object
     */
    public IOOutputStream(final IRubyObject io, Encoding encoding, boolean checkAppend, boolean verifyCanWrite) {
        this.io = io;
        CallSite writeSite = MethodIndex.getFunctionalCallSite("write");
        if (io.respondsTo("write")) {
            writeAdapter = writeSite;
        } else if (checkAppend && io.respondsTo("<<")) {
            writeAdapter = MethodIndex.getFunctionalCallSite("<<");
        } else if (verifyCanWrite) {
            throw io.getRuntime().newArgumentError("Object: " + io + " is not a legal argument to this wrapper, " +
                        "cause it doesn't respond to \"write\".");
        } else {
            writeAdapter = writeSite;
        }
        this.out = io instanceof RubyIO && !((RubyIO)io).isClosed() && ((RubyIO)io).isBuiltin("write") ? ((RubyIO) io).getOutStream() : null;
        this.encoding = encoding;
    }
    
    public IOOutputStream(final IRubyObject io, boolean checkAppend, boolean verifyCanWrite) {
        this(io, ASCIIEncoding.INSTANCE, checkAppend, verifyCanWrite);
    }    

    /**
     * Creates a new OutputStream with the object provided.
     *
     * @param io the ruby object
     */
    public IOOutputStream(final IRubyObject io) {
        this(io, true, true);
    }
    
    public IOOutputStream(final IRubyObject io, Encoding encoding) {
        this(io, encoding, true, true);
    }    

    @Override
    public void write(final int bite) throws IOException {
        if (out != null) {
            out.write(bite);
        } else {
            writeAdapter.call(io.getRuntime().getCurrentContext(), io, io,
                    RubyString.newStringLight(io.getRuntime(), new ByteList(new byte[]{(byte)bite}, encoding, false)));
        }
    }

    @Override
    public void write(final byte[] b) throws IOException {
        write(b,0,b.length);
    }

    @Override
    public void write(final byte[] b,final int off, final int len) throws IOException {
        if (out != null) {
            out.write(b, off, len);
        } else {
            writeAdapter.call(io.getRuntime().getCurrentContext(), io, io, RubyString.newStringLight(io.getRuntime(), new ByteList(b, off, len, encoding, false)));
        }
    }
    
    @Override
    public void close() throws IOException {
        if (out != null) {
            out.close();
        } else if (io.respondsTo("close")) {
            closeAdapter.call(io.getRuntime().getCurrentContext(), io, io);
        }
    }
}
