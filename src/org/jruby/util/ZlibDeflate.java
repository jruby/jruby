/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
 * Copyright (C) 2006 Dave Brosius <dbrosius@mebigfatguy.com>
 * Copyright (C) 2006 Peter K Chan <peter@oaktop.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

import org.jruby.Ruby;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

public class ZlibDeflate {
    private final Deflater flater;
    private final Ruby runtime;
    private ByteList collected;

    public static final int BASE_SIZE = 100;

    public final static int DEF_MEM_LEVEL = 8;
    public final static int MAX_MEM_LEVEL = 9;

    public final static int MAX_WBITS = 15;

    public final static int NO_FLUSH = 0;
    public final static int SYNC_FLUSH = 2;
    public final static int FULL_FLUSH = 3;
    public final static int FINISH = 4;

    public ZlibDeflate(IRubyObject caller, int level, int win_bits, int memlevel, int strategy) {
        super();
        // Zlib behavior: negative win_bits means no header and no checksum.
        flater = new Deflater(level, win_bits < 0);
        flater.setStrategy(strategy);
        collected = new ByteList(BASE_SIZE);
        runtime = caller.getRuntime();
    }

    public static IRubyObject s_deflate(IRubyObject caller, ByteList str, int level) 
    	throws DataFormatException, IOException {
        ZlibDeflate zstream = new ZlibDeflate(caller, level, MAX_WBITS, DEF_MEM_LEVEL, Deflater.DEFAULT_STRATEGY);
        IRubyObject result = zstream.deflate(str, FINISH);
        zstream.close();
        return result;
    }

    public Deflater getDeflater() {
        return flater;
    }

    public void append(IRubyObject obj) throws IOException, UnsupportedEncodingException {
        append(obj.convertToString().getByteList());
    }

    public void append(ByteList obj) throws IOException {
        flater.setInput(obj.unsafeBytes(), obj.begin, obj.realSize);
        run();
    }

    public void params(int level, int strategy) {
        flater.setLevel(level);
        flater.setStrategy(strategy);
        run();
    }

    public IRubyObject set_dictionary(IRubyObject str) throws UnsupportedEncodingException {
        flater.setDictionary(str.convertToString().getBytes());
        run();
        return str;
    }

    public IRubyObject flush(int flush) throws IOException {
        return deflate(new ByteList(0), flush);
    }

    public IRubyObject deflate(ByteList str, int flush) throws IOException {
        if (null == str) {
            return finish();
        } else {
            append(str);
            if (flush == FINISH) {
                return finish();
            }
            return RubyString.newEmptyString(runtime);
        }
    }
    
    public IRubyObject finish() {
        flater.finish();
        run();
        return RubyString.newString(runtime, collected);
    }

    private void run() {
        byte[] outp = new byte[1024];
        int resultLength = -1;
        while (!flater.finished() && resultLength != 0) {
            resultLength = flater.deflate(outp);
            collected.append(outp, 0, resultLength);
            if (resultLength == outp.length) {
                outp = new byte[outp.length * 2];
            }
        }
    }
    
    public void close() {
    }
}
