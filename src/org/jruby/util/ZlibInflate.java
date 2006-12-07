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

import java.io.UnsupportedEncodingException;

import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import org.jruby.IRuby;
import org.jruby.runtime.builtin.IRubyObject;

public class ZlibInflate {
    private Inflater flater;
    private StringBuffer collected;
    private IRuby runtime;

    public ZlibInflate(IRubyObject caller) {
        super();
        flater = new Inflater(false);
        collected = new StringBuffer();
        runtime = caller.getRuntime();
    }

    public static IRubyObject s_inflate(IRubyObject caller, String str) 
    	throws UnsupportedEncodingException, DataFormatException {
        ZlibInflate zstream = new ZlibInflate(caller);
        IRubyObject result = zstream.inflate(str);
        zstream.finish();
        zstream.close();
        return result;
    }

    public Inflater getInflater() {
        return flater;
    }

    public void append(IRubyObject obj) {
        append(obj.convertToString().toString());
    }

    public void append(String obj) {
        collected.append(obj);
    }

    public IRubyObject sync_point() {
        return runtime.getFalse();
    }

    public IRubyObject set_dictionary(IRubyObject str) throws UnsupportedEncodingException {
        flater.setDictionary(str.convertToString().toString().getBytes("ISO8859_1"));
        
        return str;
    }

    public IRubyObject inflate(String str) throws UnsupportedEncodingException, DataFormatException {
        if (null != str) {
            append(str);
        }
        StringBuffer result = new StringBuffer();
        byte[] outp = new byte[1024];
        byte[] buf = collected.toString().getBytes("ISO8859_1");
        collected = new StringBuffer();
        flater.setInput(buf);
        int resultLength = -1;
        while (!flater.finished() && resultLength != 0) {
            resultLength = flater.inflate(outp);
            result.append(new String(outp, 0, resultLength, "ISO8859_1"));
        }
        return runtime.newString(result.toString());       
    }

    public IRubyObject sync(IRubyObject str) {
        append(str);
        return runtime.getFalse();
    }

    public void finish() {
        flater.end();
    }
    
    public void close() {
    }
}
