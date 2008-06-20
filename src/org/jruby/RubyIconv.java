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
import org.jruby.anno.JRubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.Arity;

import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.ByteList;

@JRubyClass(name="Iconv")
public class RubyIconv extends RubyObject {
    //static private final String TRANSLIT = "//translit";
    static private final String IGNORE = "//ignore";

    private CharsetDecoder fromEncoding;
    private CharsetEncoder toEncoding;

    public RubyIconv(Ruby runtime, RubyClass type) {
        super(runtime, type);
    }
    
    private static final ObjectAllocator ICONV_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyIconv(runtime, klass);
        }
    };

    @JRubyModule(name="Iconv::Failure")
    public static class Failure {}
    @JRubyClass(name="Iconv::IllegalSequence", parent="ArgumentError", include="Iconv::Failure")
    public static class IllegalSequence {}
    @JRubyClass(name="Iconv::InvalidCharacter", parent="ArgumentError", include="Iconv::Failure")
    public static class InvalidCharacter {}
    @JRubyClass(name="Iconv::InvalidEncoding", parent="ArgumentError", include="Iconv::Failure")
    public static class InvalidEncoding {}
    @JRubyClass(name="Iconv::OutOfRange", parent="ArgumentError", include="Iconv::Failure")
    public static class OutOfRange {}
    @JRubyClass(name="Iconv::BrokenLibrary", parent="ArgumentError", include="Iconv::Failure")
    public static class BrokenLibrary {}

    public static void createIconv(Ruby runtime) {
        RubyClass iconvClass = runtime.defineClass("Iconv", runtime.getObject(), ICONV_ALLOCATOR);
        
        iconvClass.defineAnnotatedMethods(RubyIconv.class);

        RubyModule failure = iconvClass.defineModuleUnder("Failure");
        RubyClass argumentError = runtime.getArgumentError();

        String[] iconvErrors = {"IllegalSequence", "InvalidCharacter", "InvalidEncoding", 
                "OutOfRange", "BrokenLibrary"};
        
        for (int i = 0; i < iconvErrors.length; i++) {
            RubyClass subClass = iconvClass.defineClassUnder(iconvErrors[i], argumentError, RubyFailure.ICONV_FAILURE_ALLOCATOR);
            subClass.defineAnnotatedMethods(RubyFailure.class);
            subClass.includeModule(failure);
        }    
    }
    
    public static class RubyFailure extends RubyException {
        private IRubyObject success;
        private IRubyObject failed;

        public static RubyFailure newInstance(Ruby runtime, RubyClass excptnClass, String msg) {
            return new RubyFailure(runtime, excptnClass, msg);
        }

        protected static final ObjectAllocator ICONV_FAILURE_ALLOCATOR = new ObjectAllocator() {
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
            StringBuilder buffer = new StringBuilder("#<");
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

    @JRubyMethod(name = "open", required = 2, frame = true, meta = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject to, IRubyObject from, Block block) {
        Ruby runtime = context.getRuntime();

        RubyIconv iconv = newIconv(context, recv, to, from);

        if (!block.isGiven()) return iconv;

        IRubyObject result = runtime.getNil();
        try {
            result = block.yield(context, iconv);
        } finally {
            iconv.close();
        }

        return result;
    }
    
    private static RubyIconv newIconv(ThreadContext context, IRubyObject recv,
            IRubyObject to, IRubyObject from) {
        RubyClass klazz = (RubyClass)recv;

        return (RubyIconv) klazz.newInstance(
                context, new IRubyObject[] {to, from}, Block.NULL_BLOCK);
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
            throw runtime.newInvalidEncoding("invalid encoding");
        } catch (UnsupportedCharsetException e) {
            throw runtime.newInvalidEncoding("invalid encoding");
        } catch (Exception e) {
            throw runtime.newSystemCallError(e.toString());
        }

        return this;
    }

    @JRubyMethod(name = "close")
    public IRubyObject close() {
        toEncoding = null;
        fromEncoding = null;
        return RubyString.newEmptyString(getRuntime());
    }

    @JRubyMethod
    public IRubyObject iconv(IRubyObject str) {
        return iconv(str, 0, -1);
    }

    @JRubyMethod
    public IRubyObject iconv(IRubyObject str, IRubyObject startArg) {
        int start = 0;
        if (!startArg.isNil()) start = RubyNumeric.fix2int(startArg);
        return iconv(str, start, -1);
    }

    @JRubyMethod
    public IRubyObject iconv(IRubyObject str, IRubyObject startArg, IRubyObject endArg) {
        int start = 0;
        int end = -1;

        if (!startArg.isNil()) start = RubyNumeric.fix2int(startArg);
        if (!endArg.isNil()) end = RubyNumeric.fix2int(endArg);

        return iconv(str, start, end);
    }
    
    private IRubyObject iconv(IRubyObject str, int start, int end) {
        if (str.isNil()) {
            fromEncoding.reset();
            toEncoding.reset();
            return RubyString.newEmptyString(getRuntime());
        }

        return _iconv(str.convertToString(), start, end);
    }

    /**
     * Variable-arity version for compatibility. Not bound to Ruby.
     * @deprecated Use the versions with one, two or three arguments.
     */
    public IRubyObject iconv(IRubyObject[] args) {
        switch (args.length) {
        case 1:
            return iconv(args[0]);
        case 2:
            return iconv(args[0], args[1]);
        case 3:
            return iconv(args[0], args[1], args[2]);
        default:
            Arity.raiseArgumentError(getRuntime(), args.length, 1, 2);
            return null; // not reached
        }
    }

    // FIXME: We are assuming that original string will be raw bytes.  If -Ku is provided
    // this will not be true, but that is ok for now.  Deal with that when someone needs it.
    private IRubyObject _iconv(RubyString str, int start, int end) {
        if (fromEncoding == null) {
            throw getRuntime().newArgumentError("closed iconv");
        }
        
        ByteList bytes = str.getByteList();
        
        // treat start and end as start...end for end >= 0, start..end for end < 0
        if (start < 0) {
            start += bytes.length();
        }
        
        if (end < 0) {
            end += 1 + bytes.length();
        } else if (end > bytes.length()) {
            end = bytes.length();
        }
        
        if (start < 0 || end < start) { // invalid ranges result in an empty string
            return RubyString.newEmptyString(getRuntime());
        }
        
        ByteBuffer buf = ByteBuffer.wrap(bytes.unsafeBytes(), bytes.begin() + start, end - start);
        
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

    @JRubyMethod(name = "iconv", required = 2, rest = true, meta = true)
    public static IRubyObject iconv(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        return convertWithArgs(context, recv, args, "iconv");
    }
    
    @JRubyMethod(name = "conv", required = 3, rest = true, meta = true)
    public static IRubyObject conv(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block unusedBlock) {
        return convertWithArgs(context, recv, args, "conv").join(context, RubyString.newEmptyString(recv.getRuntime()));
    }

    @JRubyMethod(name = "charset_map", meta= true)
    public static IRubyObject charset_map_get(IRubyObject recv) {
        return recv.getRuntime().getCharsetMap();
    }

    private static String mapCharset(ThreadContext context, IRubyObject val) {
        RubyHash charset = val.getRuntime().getCharsetMap();
        if (charset.size() > 0) {
            RubyString key = val.callMethod(context, "downcase").convertToString();
            IRubyObject tryVal = charset.fastARef(key);
            if (tryVal != null) val = tryVal;
        }

        return val.convertToString().toString();
    }

    public static RubyArray convertWithArgs(ThreadContext context, IRubyObject recv, IRubyObject[] args, String function) {
        assert args.length >= 2;

        RubyArray array = context.getRuntime().newArray(args.length - 2);
        RubyIconv iconv = newIconv(context, recv, args[0], args[1]);

        try {
            for (int i = 2; i < args.length; i++) {
                array.append(iconv.iconv(args[i]));
            }
        } finally {
            iconv.close();
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
}
