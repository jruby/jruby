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
 * Copyright (C) 2007 Koichiro Ohba <koichiro@meadowy.org>
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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.nio.charset.UnsupportedCharsetException;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;

public class RubyIconv extends RubyObject {
    //static private final String TRANSLIT = "//translit";
    static private final String IGNORE = "//ignore";

    private CharsetDecoder fromEncoding;
    private CharsetEncoder toEncoding;

    public RubyIconv(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    private static ObjectAllocator ICONV_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyIconv(runtime, klass);
        }
    };

    public static void createIconv(Ruby runtime) {
        RubyClass iconvClass = runtime.defineClass("Iconv", runtime.getObject(), ICONV_ALLOCATOR);
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyIconv.class);
        
        iconvClass.defineAnnotatedMethods(RubyIconv.class, callbackFactory);

        RubyModule failure = iconvClass.defineModuleUnder("Failure");
        CallbackFactory failureCallbackFactory = runtime.callbackFactory(RubyFailure.class);
        RubyClass argumentError = runtime.getClass("ArgumentError");

        String[] iconvErrors = {"IllegalSequence", "InvalidCharacter", "InvalidEncoding", 
                "OutOfRange", "BrokenLibrary"};
        
        for (int i = 0; i < iconvErrors.length; i++) {
            RubyClass subClass = iconvClass.defineClassUnder(iconvErrors[i], argumentError, RubyFailure.ICONV_FAILURE_ALLOCATOR);
            subClass.defineAnnotatedMethods(RubyFailure.class, failureCallbackFactory);
            subClass.includeModule(failure);
        }    
    }
    
    public static class RubyFailure extends RubyException {
        private IRubyObject success;
        private IRubyObject failed;

        public static RubyFailure newInstance(Ruby runtime, RubyClass excptnClass, String msg) {
            return new RubyFailure(runtime, excptnClass, msg);
        }

        protected static ObjectAllocator ICONV_FAILURE_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
                return new RubyFailure(runtime, klass);
            }
        };

        protected RubyFailure(Ruby runtime, RubyClass rubyClass) {
            this(runtime, rubyClass, null);
        }

        public RubyFailure(Ruby runtime, RubyClass rubyClass, String message) {
            super(runtime, rubyClass, message);
        }

        @JRubyMethod(name = "initialize", required = 1, optional = 2, frame = true)
        public IRubyObject initialize(IRubyObject[] args, Block block) {
            Arity.checkArgumentCount(getRuntime(), args, 1, 3);
            super.initialize(args, block);
            success = args.length >= 2 ? args[1] : getRuntime().getNil();
            failed = args.length == 3 ? args[2] : getRuntime().getNil();

            return this;
        }

        @JRubyMethod(name = "success")
        public IRubyObject success() {
            return success;
        }

        @JRubyMethod(name = "failed")
        public IRubyObject failed() {
            return failed;
        }

        @JRubyMethod(name = "inspect")
        public IRubyObject inspect() {
            RubyModule rubyClass = getMetaClass();
            StringBuffer buffer = new StringBuffer("#<");
            buffer.append(rubyClass.getName()).append(": ").append(success.inspect().toString());
            buffer.append(", ").append(failed.inspect().toString()).append(">");

            return getRuntime().newString(buffer.toString());
        }
    }

    private static String getCharset(String encoding) {
        int index = encoding.indexOf("//");
        if (index == -1) return encoding;
        return encoding.substring(0, index);
    }
    
    /* Currently dead code, but useful when we figure out how to actually perform translit.
    private static boolean isTranslit(String encoding) {
        return encoding.toLowerCase().indexOf(TRANSLIT) != -1 ? true : false;
    }*/
    
    private static boolean isIgnore(String encoding) {
        return encoding.toLowerCase().indexOf(IGNORE) != -1 ? true : false;
    }

    @JRubyMethod(name = "open", required = 2, frame = true, singleton = true)
    public static IRubyObject open(IRubyObject recv, IRubyObject to, IRubyObject from, Block block) {
        Ruby runtime = recv.getRuntime();
        RubyIconv iconv =
            (RubyIconv) runtime.getClass("Iconv").newInstance(
                    new IRubyObject[] { to, from }, Block.NULL_BLOCK);
        if (!block.isGiven()) return iconv;

        IRubyObject result = runtime.getNil();
        try {
            result = block.yield(recv.getRuntime().getCurrentContext(), iconv);
        } finally {
            iconv.close();
        }

        return result;
    }
    
    @JRubyMethod(name = "initialize", required = 2, frame = true)
    public IRubyObject initialize(IRubyObject arg1, IRubyObject arg2, Block unusedBlock) {
        Ruby runtime = getRuntime();
        if (!arg1.respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + arg1.getMetaClass() + " into String");
        }
        if (!arg2.respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + arg2.getMetaClass() + " into String");
        }

        String to = arg1.convertToString().toString();
        String from = arg2.convertToString().toString();

        try {

            fromEncoding = Charset.forName(getCharset(from)).newDecoder();
            toEncoding = Charset.forName(getCharset(to)).newEncoder();

            if (!isIgnore(from)) fromEncoding.onUnmappableCharacter(CodingErrorAction.REPORT);
            if (!isIgnore(to)) toEncoding.onUnmappableCharacter(CodingErrorAction.REPORT);
        } catch (IllegalCharsetNameException e) {
            throw runtime.newArgumentError("invalid encoding");
        } catch (UnsupportedCharsetException e) {
            throw runtime.newArgumentError("invalid encoding");
        } catch (Exception e) {
            throw runtime.newSystemCallError(e.toString());
        }

        return this;
    }

    @JRubyMethod(name = "close")
    public IRubyObject close() {
        toEncoding = null;
        fromEncoding = null;
        return getRuntime().newString("");
    }

    @JRubyMethod(name = "iconv", required = 1, optional = 2)
    public IRubyObject iconv(IRubyObject[] args) {
        Ruby runtime = getRuntime();
        args = Arity.scanArgs(runtime, args, 1, 2);
        int start = 0;
        int length = -1;

        if (args[0].isNil()) {
            fromEncoding.reset();
            toEncoding.reset();
            return runtime.newString("");
        }
        if (!args[0].respondsTo("to_str")) {
            throw runtime.newTypeError("can't convert " + args[0].getMetaClass() + " into String");
        }
        if (!args[1].isNil()) start = RubyNumeric.fix2int(args[1]);
        if (!args[2].isNil()) length = RubyNumeric.fix2int(args[2]);
        
        IRubyObject result = _iconv(args[0].convertToString(), start, length);
        return result;
    }

    // FIXME: We are assuming that original string will be raw bytes.  If -Ku is provided
    // this will not be true, but that is ok for now.  Deal with that when someone needs it.
    private IRubyObject _iconv(RubyString str, int start, int length) {
        ByteList bytes = str.getByteList();
        
        if (length < 0) length = bytes.length() - start;
        
        ByteBuffer buf = ByteBuffer.wrap(bytes.unsafeBytes(), start, length);
        
        try {
            CharBuffer cbuf = fromEncoding.decode(buf);
            buf = toEncoding.encode(cbuf);
        } catch (MalformedInputException e) {
        } catch (UnmappableCharacterException e) {
        } catch (CharacterCodingException e) {
            throw getRuntime().newInvalidEncoding("invalid sequence");
        } catch (IllegalStateException e) {
        }
        byte[] arr = buf.array();
        
        return getRuntime().newString(new ByteList(arr, 0, buf.limit()));
    }

    @JRubyMethod(name = "iconv", required = 1, optional = 1, singleton = true)
    public static IRubyObject iconv(IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        return convertWithArgs(recv, args, "iconv");
    }
    
    @JRubyMethod(name = "conv", required = 3, rest = true, singleton = true)
    public static IRubyObject conv(IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        return convertWithArgs(recv, args, "conv").join(recv.getRuntime().newString(""));
    }
    
    public static RubyArray convertWithArgs(IRubyObject recv, IRubyObject[] args, String function) {
        Arity.checkArgumentCount(recv.getRuntime(), args, 3, -1);

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
        // Get all bytes from PLAIN string pretend they are not encoded in any way.
        byte[] string = original.getBytes();
        // Now create a string pretending it is from fromEncoding
        string = new String(string, fromEncoding).getBytes(toEncoding);
        // Finally recode back to PLAIN
        return RubyString.newString(original.getRuntime(), string);
    }
    */

    // FIXME: We are assuming that original string will be raw bytes.  If -Ku is provided
    // this will not be true, but that is ok for now.  Deal with that when someone needs it.
    private static IRubyObject convert2(String fromEncoding, String toEncoding, RubyString original) {
        // Don't bother to convert if it is already in toEncoding
        if (fromEncoding.equals(toEncoding)) return original;
        
        try {
            // Get all bytes from string and pretend they are not encoded in any way.
            ByteList bytes = original.getByteList();
            ByteBuffer buf = ByteBuffer.wrap(bytes.unsafeBytes(), bytes.begin(), bytes.length());

            CharsetDecoder decoder = Charset.forName(getCharset(fromEncoding)).newDecoder();
            
            if (!isIgnore(fromEncoding)) decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

            CharBuffer cbuf = decoder.decode(buf);
            CharsetEncoder encoder = Charset.forName(getCharset(toEncoding)).newEncoder();
            
            if (!isIgnore(toEncoding)) encoder.onUnmappableCharacter(CodingErrorAction.REPORT);

            buf = encoder.encode(cbuf);
            byte[] arr = buf.array();
            return RubyString.newString(original.getRuntime(), new ByteList(arr,0,buf.limit()));
        } catch (UnsupportedCharsetException e) {
            throw original.getRuntime().newInvalidEncoding("invalid encoding");
        } catch (UnmappableCharacterException e) {
        } catch (CharacterCodingException e) {
        }
        return original.getRuntime().getNil();
    }
}
