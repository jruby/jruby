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
 * Copyright (C) 2006 Thomas E Enebo <enebo@acm.org>
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
package org.jruby;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.UnmappableCharacterException;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyIconv extends RubyObject {
    //private String toEncoding;
    //private String fromEncoding;
    
    public RubyIconv(IRuby runtime, RubyClass type) {
        super(runtime, type);
    }

    public static void createIconv(IRuby runtime) {
        RubyClass iconvClass = runtime.defineClass("Iconv", runtime.getObject());

        iconvClass.defineClassUnder("IllegalEncoding", runtime.getClass("ArgumentError"));
        iconvClass.defineClassUnder("IllegalSequence", runtime.getClass("ArgumentError"));
        iconvClass.defineClassUnder("InvalidCharacter", runtime.getClass("ArgumentError"));
        
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyIconv.class);

        iconvClass.defineFastSingletonMethod("new", callbackFactory.getOptSingletonMethod("newInstance"));
        iconvClass.defineFastSingletonMethod("iconv", callbackFactory.getOptSingletonMethod("iconv"));
        iconvClass.defineFastSingletonMethod("conv", callbackFactory.getOptSingletonMethod("conv"));
        
        iconvClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        //iconvClass.defineMethod("iconv", callbackFactory.getOptMethod("iconv"));

        // FIXME: JRUBY-310: Add all other iconv methods...Sopen, Iclose, Iiconv
        // FIXME: JRUBY-309: Implement IConv Exception classes (e.g. Iconv::IllegalSequence and friends)
    }

    // FIXME: I believe that we are suppose to keep partial character contents between calls
    // so that we can pass in arbitrary chunks of bytes.  Charset Encoder needs to be able to
    // handle this or we need to be able detect it somehow.
    /*
    public IRubyObject iconv(IRubyObject[] args) {
        RubyArray array = getRuntime().newArray();
        
        for (int i = 0; i < args.length; i++) {
            try {
                array.append(convert(fromEncoding, toEncoding, args[i].convertToString()));
            } catch (UnsupportedEncodingException e) {
                throw getRuntime().newErrnoEINVALError("iconv(" + toEncoding + ", " + 
                        fromEncoding + ", " + args[i] + ")");
            }
        }

        return array;
    }
    */
    
    public IRubyObject initialize(IRubyObject[] args) {
        checkArgumentCount(args, 2, 2);
        
        //toEncoding = args[0].convertToString().toString();
        //fromEncoding = args[1].convertToString().toString();
        
        return this;
    }
    
    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyIconv result = new RubyIconv(recv.getRuntime(), (RubyClass)recv);
        result.callInit(args);
        return result;
    }

    public static IRubyObject iconv(IRubyObject recv, IRubyObject[] args) {
        return convertWithArgs(recv, args, "iconv");
    }
    
    public static IRubyObject conv(IRubyObject recv, IRubyObject[] args) {
        return convertWithArgs(recv, args, "conv").join(recv.getRuntime().newString(""));
    }
    
    public static RubyArray convertWithArgs(IRubyObject recv, IRubyObject[] args, String function) {
        recv.checkArgumentCount(args, 3, -1);

        String fromEncoding = args[1].convertToString().toString();
        String toEncoding = args[0].convertToString().toString();
        RubyArray array = recv.getRuntime().newArray();
        
        for (int i = 2; i < args.length; i++) {
            array.append(convert2(fromEncoding, toEncoding, args[i].convertToString()));
        }

        return array;
    }
    
    /*
    private static IRubyObject convert(String fromEncoding, String toEncoding, RubyString original) 
        throws UnsupportedEncodingException {
        // Get all bytes from ISO8859 string pretend they are not encoded in any way.
        byte[] string = original.toString().getBytes("ISO8859-1");
        // Now create a string pretending it is from fromEncoding
        string = new String(string, fromEncoding).getBytes(toEncoding);
        // Finally recode back to ISO8859-1
        return original.getRuntime().newString(new String(string, "ISO8859-1"));
    }
    */

    // FIXME: We are assuming that original string will be ISO8859-1 String.  If -Ku is provided
    // this will not be true, but that is ok for now.  Deal with that when someone needs it.
    private static IRubyObject convert2(String fromEncoding, String toEncoding, RubyString original) {
        try {
            // Get all bytes from ISO8859 string and pretend they are not encoded in any way.
            byte[] bytes = original.toString().getBytes("ISO8859-1");
            ByteBuffer buf = ByteBuffer.wrap(bytes);

            CharsetDecoder decoder = Charset.forName(fromEncoding).newDecoder();
            
            decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            CharBuffer cbuf = decoder.decode(buf);
            CharsetEncoder encoder = Charset.forName(toEncoding).newEncoder();
            
            encoder.onUnmappableCharacter(CodingErrorAction.REPORT);
            buf = encoder.encode(cbuf);
            byte[] arr = buf.array();
            bytes = new byte[buf.limit()];
            System.arraycopy(arr, 0, bytes, 0, bytes.length);

            return original.getRuntime().newString(new String(bytes, "ISO8859-1"));
        } catch (UnmappableCharacterException e) {
        } catch (CharacterCodingException e) {
        } catch (UnsupportedEncodingException e) {

        }
        return original.getRuntime().getNil();
    }


}
