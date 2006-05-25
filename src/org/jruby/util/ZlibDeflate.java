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

import org.jruby.IRuby;
import org.jruby.runtime.builtin.IRubyObject;

public class ZlibDeflate {
    private Deflater flater;
    private StringBuffer collected;
    private IRuby runtime;

    public final static int DEF_MEM_LEVEL = 8;
    public final static int MAX_MEM_LEVEL = 9;

    public final static int MAX_WBITS = 15;

    public final static int NO_FLUSH = 0;
    public final static int SYNC_FLUSH = 2;
    public final static int FULL_FLUSH = 3;
    public final static int FINISH = 4;

    public ZlibDeflate(IRubyObject caller, int level, int win_bits, int memlevel, int strategy) {
        super();
        flater = new Deflater(level);
        flater.setStrategy(strategy);
        collected = new StringBuffer();
        runtime = caller.getRuntime();
    }

    public static IRubyObject s_deflate(IRubyObject caller, String str, int level) 
    	throws UnsupportedEncodingException, DataFormatException, IOException {
        ZlibDeflate zstream = new ZlibDeflate(caller, level, Deflater.DEFAULT_STRATEGY, MAX_WBITS, DEF_MEM_LEVEL);
        IRubyObject result = zstream.deflate(str, new Long(FINISH));
        zstream.close();
        
        return result;
    }

    public void append(IRubyObject obj) throws IOException, UnsupportedEncodingException {
        append(obj.convertToString());
    }

    public void append(String obj) throws IOException, UnsupportedEncodingException {
        collected.append(obj);
    }

    public void params(int level, int strategy) {
        flater.setLevel(level);
        flater.setStrategy(strategy);
    }

    public IRubyObject set_dictionary(IRubyObject str) throws UnsupportedEncodingException {
        flater.setDictionary(str.convertToString().toString().getBytes("ISO-8859-1"));
        
        return str;
    }

    public IRubyObject flush(Long flush) throws IOException {
        return deflate("", flush);
    }

    public IRubyObject deflate(String str, Long flush_x) throws IOException {
        int flush = flush_x.intValue();
        
        if (null == str) {
            StringBuffer result = new StringBuffer();
            byte[] outp = new byte[1024];
            byte[] buf = collected.toString().getBytes("ISO-8859-1");
            collected = new StringBuffer();
            flater.setInput(buf);
            flater.finish();
            int resultLength = -1;
            while (!flater.finished() && resultLength != 0) {
                resultLength = flater.deflate(outp);
                result.append(new String(outp, 0, resultLength,"ISO-8859-1"));
            }
            
            return runtime.newString(result.toString());       
        } else {
            append(str);
            if (flush == FINISH) {
                StringBuffer result = new StringBuffer();
                byte[] outp = new byte[1024];
                byte[] buf = collected.toString().getBytes("ISO-8859-1");
                collected = new StringBuffer();
                flater.setInput(buf);
                flater.finish();
                int resultLength = -1;
                while (!flater.finished() && resultLength != 0) {
                    resultLength = flater.deflate(outp);
                    result.append(new String(outp, 0, resultLength,"ISO-8859-1"));
                }
                
                return runtime.newString(result.toString());
            }
            
            return runtime.getNil();
        }
    }
    
    public void finish() {
        flater.finish();
    }
    
    public void close() {
    }
}
