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
 * Copyright (C) 2009 Aurelian Oancea <aurelian@locknet.ro>
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

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyString;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.builtin.IRubyObject;

public class ZlibInflate {
    private final Ruby runtime;
    private Inflater flater;    
    private ByteList collected;
    private ByteList appended;
    
    public static final int BASE_SIZE = 100;

    public ZlibInflate(IRubyObject caller) {
        super();
        flater = new Inflater(false);
        collected = new ByteList(BASE_SIZE);
        runtime = caller.getRuntime();
        appended= new ByteList();
    }

    public static IRubyObject s_inflate(IRubyObject caller, ByteList str) 
    	throws DataFormatException {
        ZlibInflate zstream = new ZlibInflate(caller);
        IRubyObject result = zstream.inflate(str);
        zstream.finish();
        zstream.close();
        return result;
    }

    public Inflater getInflater() {
        return flater;
    }

    public IRubyObject sync_point() {
        return runtime.getFalse();
    }

    public IRubyObject sync(IRubyObject str) {
        append(str);
        return runtime.getFalse();
    }

    public void append(IRubyObject obj) {
        append(obj.convertToString().getByteList());
    }

    public void append(ByteList obj) {
        flater.setInput(obj.unsafeBytes(), obj.begin, obj.realSize);
        appended.append(obj);
        run();
    }

    private void run() {
        byte[] outp = new byte[1024];
        int resultLength = -1;

        while (!flater.finished() && resultLength != 0) {
            try {
                resultLength = flater.inflate(outp);
                if(flater.needsDictionary()) {
                    RubyClass errorClass = runtime.fastGetModule("Zlib").fastGetClass("NeedDict");
                    throw new RaiseException(RubyException.newException(runtime, errorClass, "need dictionary"));
                }
            } catch (DataFormatException ex) {
                flater= new Inflater(true);
                flater.setInput(appended.unsafeBytes(), appended.begin, appended.realSize);
                appended= new ByteList();
                run();
                return;
            }
            collected.append(outp, 0, resultLength);
            if (resultLength == outp.length) {
                outp = new byte[outp.length * 2];
            }
            
        }

    }

    public IRubyObject set_dictionary(IRubyObject str) throws UnsupportedEncodingException {
        flater.setDictionary(str.convertToString().getBytes());
        run();
        return str;
    }

    public IRubyObject inflate(ByteList str) {
        if (null == str) {
            return finish();
        } else {
            append(str);
            if(flater.finished()) {
                return finish();
            }
            return RubyString.newEmptyString(runtime);
        }
    }

    /*
     * scenario:
     * append ( str )
     * flush -> str
     * finished== true
     * flush -> ""
     */
    public IRubyObject flush() {
        IRubyObject ro= inflate(new ByteList(0));
        collected= new ByteList(0);
        return ro;
    }

    public IRubyObject finish() {
        flater.end();
        run();
        return RubyString.newString(runtime, collected);
    }
    
    public void close() {
    }
}
