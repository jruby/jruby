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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 * Copyright (C) 2006 Ryan Bell <ryan.l.bell@gmail.com>
 * Copyright (C) 2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2008 Vladimir Sizikov <vsizikov@gmail.com>
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
package org.jruby.ext.stringio;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyKernel;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.addons.IOJavaAddons;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingCapable;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.OpenFile;

import java.util.Arrays;

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.runtime.Visibility.PRIVATE;

@JRubyClass(name="StringIO")
@SuppressWarnings("deprecation")
public class StringIO extends RubyObject implements EncodingCapable {
    static class StringIOData {
        /**
         * ATTN: the value of internal might be reset to null
         * (during StringIO.open with block), so watch out for that.
         */
        RubyString string;
        int pos;
        int lineno;
        int flags;
    }
    StringIOData ptr;

    private static final int STRIO_READABLE = USER4_F;
    private static final int STRIO_WRITABLE = USER5_F;
    private static final int STRIO_READWRITE = (STRIO_READABLE | STRIO_WRITABLE);

    private static ObjectAllocator STRINGIO_ALLOCATOR = new ObjectAllocator() {
            public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new StringIO(runtime, klass);
        }
    };

    public static RubyClass createStringIOClass(final Ruby runtime) {
        RubyClass stringIOClass = runtime.defineClass(
                "StringIO", runtime.getClass("Data"), STRINGIO_ALLOCATOR);

        stringIOClass.defineAnnotatedMethods(StringIO.class);
        stringIOClass.includeModule(runtime.getEnumerable());

        if (runtime.getObject().isConstantDefined("Java")) {
            stringIOClass.defineAnnotatedMethods(IOJavaAddons.AnyIO.class);
        }

        return stringIOClass;
    }

    public Encoding getEncoding() {
        return ptr.string.getEncoding();
    }

    public void setEncoding(Encoding e) {
        ptr.string.setEncoding(e);
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        StringIO strio = (StringIO)((RubyClass)recv).newInstance(context, args, Block.NULL_BLOCK);
        IRubyObject val = strio;

        if (block.isGiven()) {
            try {
                val = block.yield(context, strio);
            } finally {
                strio.ptr.string = null;
                strio.flags &= ~STRIO_READWRITE;
            }
        }
        return val;
    }

    protected StringIO(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
    }

    @JRubyMethod(optional = 2, visibility = PRIVATE)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        if (ptr == null) {
            ptr = new StringIOData();
        }

        // does not dispatch quite right and is not really necessary for us
        //Helpers.invokeSuper(context, this, metaClass, "initialize", IRubyObject.NULL_ARRAY, Block.NULL_BLOCK);
        strioInit(context, args);
        return this;
    }

    // MRI: strio_init
    private void strioInit(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        RubyString string;
        IRubyObject mode;
        boolean trunc = false;
        
        switch (args.length) {
            case 2:
                mode = args[1];
                if (mode instanceof RubyFixnum) {
                    int flags = RubyFixnum.fix2int(mode);
                    ptr.flags = ModeFlags.getOpenFileFlagsFor(flags);
                    trunc = (flags & ModeFlags.TRUNC) != 0;
                } else {
                    String m = args[1].convertToString().toString();
                    ptr.flags = OpenFile.ioModestrFmode(runtime, m);
                    trunc = m.charAt(0) == 'w';
                }
                string = args[0].convertToString();
                if ((ptr.flags & OpenFile.WRITABLE) != 0 && string.isFrozen()) {
                    throw runtime.newErrnoEACCESError("Permission denied");
                }
                if (trunc) {
                    string.resize(0);
                }
                break;
            case 1:
                string = args[0].convertToString();
                ptr.flags = string.isFrozen() ? OpenFile.READABLE : OpenFile.READWRITE;
                break;
            case 0:
                string = RubyString.newEmptyString(runtime, runtime.getDefaultExternalEncoding());
                ptr.flags = OpenFile.READWRITE;
                break;
            default:
                throw runtime.newArgumentError(args.length, 2);
        }

        ptr.string = string;
        ptr.pos = 0;
        ptr.lineno = 0;
        // funky way of shifting readwrite flags into object flags
        flags |= (ptr.flags & OpenFile.READWRITE) * (STRIO_READABLE / OpenFile.READABLE);
    }

    // MRI: strio_copy
    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize_copy(ThreadContext context, IRubyObject other) {
        StringIO otherIO = (StringIO) TypeConverter.convertToType(other,
                context.runtime.getClass("StringIO"), "to_strio");

        if (this == otherIO) return this;

        ptr = otherIO.ptr;
        infectBy(otherIO);
        flags &= ~STRIO_READWRITE;
        flags |= otherIO.flags & STRIO_READWRITE;

        return this;
    }

    @JRubyMethod(name = {"binmode", "flush"})
    public IRubyObject strio_self() {
        return this;
    }

    @JRubyMethod(name = {"fcntl"}, rest = true)
    public IRubyObject strio_unimpl(ThreadContext context, IRubyObject[] args) {
        throw context.runtime.newNotImplementedError("");
    }

    @JRubyMethod(name = {"fsync"})
    public IRubyObject strioZero(ThreadContext context) {
        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod(name = {"sync="})
    public IRubyObject strioFirst(IRubyObject arg) {
        checkInitialized();
        return arg;
    }

    @JRubyMethod(name = {"isatty", "tty?"})
    public IRubyObject strioFalse(ThreadContext context) {
        return context.runtime.getFalse();
    }

    @JRubyMethod(name = {"pid", "fileno"})
    public IRubyObject strioNil(ThreadContext context) {
        return context.nil;
    }

    @JRubyMethod(name = "<<", required = 1)
    public IRubyObject append(ThreadContext context, IRubyObject arg) {
        // Claims conversion is done via 'to_s' in docs.
        callMethod(context, "write", arg);
        
        return this; 
    }

    @JRubyMethod
    public IRubyObject close(ThreadContext context) {
        checkInitialized();
        checkOpen();

        // NOTE: This is 2.0 behavior to allow dup'ed StringIO to remain open when original is closed
        flags &= ~STRIO_READWRITE;

        return context.nil;
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject closed_p() {
        checkInitialized();
        return getRuntime().newBoolean(closed());
    }

    @JRubyMethod
    public IRubyObject close_read(ThreadContext context) {
        checkReadable();
        flags &= ~STRIO_READABLE;
        return context.nil;
    }

    @JRubyMethod(name = "closed_read?")
    public IRubyObject closed_read_p() {
        checkInitialized();
        return getRuntime().newBoolean(!readable());
    }

    @JRubyMethod
    public IRubyObject close_write(ThreadContext context) {
        checkWritable();
        flags &= ~STRIO_WRITABLE;
        return context.nil;
    }

    @JRubyMethod(name = "closed_write?")
    public IRubyObject closed_write_p() {
        checkInitialized();
        return getRuntime().newBoolean(!writable());
    }

    // MRI: strio_each
    @JRubyMethod(name = "each", optional = 2, writes = FrameField.LASTLINE)
    public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each", args);

        IRubyObject line;
        
        if (args.length > 0 && !args[args.length - 1].isNil() && args[args.length - 1].checkStringType19().isNil() &&
                RubyNumeric.num2long(args[args.length - 1]) == 0) {
            throw context.runtime.newArgumentError("invalid limit: 0 for each_line");
        }

        checkReadable();
        while (!(line = getline(context, args)).isNil()) {
            block.yieldSpecific(context, line);
        }
        return this;
    }

    @JRubyMethod(name = "each_line", optional = 2, writes = FrameField.LASTLINE)
    public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_line", args);
        
        return each(context, args, block);
    }

    @JRubyMethod(name = "lines", optional = 2)
    public IRubyObject lines(ThreadContext context, IRubyObject[] args, Block block) {
        context.runtime.getWarnings().warn("StringIO#lines is deprecated; use #each_line instead");
        return block.isGiven() ? each(context, args, block) : enumeratorize(context.runtime, this, "each_line", args);
    }

    @JRubyMethod(name = {"each_byte", "bytes"})
    public IRubyObject each_byte(ThreadContext context, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_byte");

        checkReadable();
        Ruby runtime = context.runtime;
        ByteList bytes = ptr.string.getByteList();

        // Check the length every iteration, since
        // the block can modify this string.
        while (ptr.pos < bytes.length()) {
            block.yield(context, runtime.newFixnum(bytes.get((int) ptr.pos++) & 0xFF));
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject each_char(final ThreadContext context, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_char");

        IRubyObject c;
        while (!(c = getc(context)).isNil()) {
            block.yieldSpecific(context, c);
        }
        return this;
    }

    @JRubyMethod
    public IRubyObject chars(final ThreadContext context, final Block block) {
        context.runtime.getWarnings().warn("StringIO#chars is deprecated; use #each_char instead");

        return each_char(context, block);
    }

    @JRubyMethod(name = {"eof", "eof?"})
    public IRubyObject eof(ThreadContext context) {
        checkReadable();
        Ruby runtime = context.runtime;
        if (ptr.pos < ptr.string.size()) return runtime.getFalse();
        return runtime.getTrue();
    }
    
    private boolean isEndOfString() {
        return ptr.pos >= ptr.string.size();
    }

    @JRubyMethod(name = "getc")
    public IRubyObject getc(ThreadContext context) {
        checkReadable();

        if (isEndOfString()) return context.runtime.getNil();

        int start = ptr.pos;
        int total = 1 + StringSupport.bytesToFixBrokenTrailingCharacter(ptr.string.getByteList(), start + 1);
        
        ptr.pos += total;
        
        return context.runtime.newString(ptr.string.getByteList().makeShared(start, total));
    }

    @JRubyMethod(name = "getbyte")
    public IRubyObject getbyte(ThreadContext context) {
        checkReadable();

        if (isEndOfString()) return context.runtime.getNil();

        int c = ptr.string.getByteList().get(ptr.pos++) & 0xFF;

        return context.runtime.newFixnum(c);
    }
    
    private RubyString strioSubstr(Ruby runtime, int pos, int len) {
        RubyString str = ptr.string;
        ByteList strByteList = str.getByteList();
        byte[] strBytes = strByteList.getUnsafeBytes();
        Encoding enc = str.getEncoding();
        int rlen = str.size() - pos;
        
        if (len > rlen) len = rlen;
        if (len < 0) len = 0;
        
        if (len == 0) return RubyString.newEmptyString(runtime);
        return RubyString.newStringShared(runtime, strBytes, strByteList.getBegin() + pos, len, enc);
    }

    private static final int CHAR_BIT = 8;
    
    private static void bm_init_skip(int[] skip, byte[] pat, int patPtr, int m) {
        int c;
        
        for (c = 0; c < (1 << CHAR_BIT); c++) {
            skip[c] = m;
        }
        while ((--m) > 0) {
            skip[pat[patPtr++]] = m;
        }
    }
    
    // Note that this is substantially more complex in 2.0 (Onigmo)
    private static int bm_search(byte[] little, int lstart, int llen, byte[] big, int bstart, int blen, int[] skip) {
        int i, j, k;
        
        i = llen - 1;
        while (i < blen) {
            k = i;
            j = llen - 1;
            while (j >= 0 && big[k + bstart] == little[j + lstart]) {
                k--;
                j--;
            }
            if (j < 0) return k + 1;
            i += skip[big[i + bstart] & 0xFF];
        }
        return -1;
    }

//        if (sepArg != null) {
//            if (sepArg.isNil()) {
//                int bytesAvailable = data.internal.getByteList().getRealSize() - (int)data.pos;
//                int bytesToUse = (limit < 0 || limit >= bytesAvailable ? bytesAvailable : limit);
//
//                // add additional bytes to fix trailing broken character
//                bytesToUse += StringSupport.bytesToFixBrokenTrailingCharacter(data.internal.getByteList(), bytesToUse);
//
//                ByteList buf = data.internal.getByteList().makeShared(
//                    (int)data.pos, bytesToUse);
//                data.pos += buf.getRealSize();
//                return makeString(runtime, buf);
//            }
//
//            sep = sepArg.convertToString().getByteList();
//            if (sep.getRealSize() == 0) {
//                isParagraph = true;
//                sep = Stream.PARAGRAPH_SEPARATOR;
//            }
//        }
//
//        if (isEndOfString() || data.eof) return context.nil;
//
//        ByteList ss = data.internal.getByteList();
//
//        if (isParagraph) {
//            swallowLF(ss);
//            if (data.pos == ss.getRealSize()) {
//                return runtime.getNil();
//            }
//        }
//
//        int sepIndex = ss.indexOf(sep, (int)data.pos);
//
//        ByteList add;
//        if (-1 == sepIndex) {
//            sepIndex = data.internal.getByteList().getRealSize();
//            add = ByteList.EMPTY_BYTELIST;
//        } else {
//            add = sep;
//        }
//
//        int bytes = sepIndex - (int)data.pos;
//        int bytesToUse = (limit < 0 || limit >= bytes ? bytes : limit);
//
//        int bytesWithSep = sepIndex - (int)data.pos + add.getRealSize();
//        int bytesToUseWithSep = (limit < 0 || limit >= bytesWithSep ? bytesWithSep : limit);
//
//        ByteList line = new ByteList(bytesToUseWithSep);
//        if (is19) line.setEncoding(data.internal.getByteList().getEncoding());
//        line.append(data.internal.getByteList(), (int)data.pos, bytesToUse);
//        data.pos += bytesToUse;
//
//        if (is19) {
//            // add additional bytes to fix trailing broken character
//            int extraBytes = StringSupport.bytesToFixBrokenTrailingCharacter(line, line.length());
//            if (extraBytes != 0) {
//                line.append(data.internal.getByteList(), (int)data.pos, extraBytes);
//                data.pos += extraBytes;
//            }
//        }
//
//        int sepBytesToUse = bytesToUseWithSep - bytesToUse;
//        line.append(add, 0, sepBytesToUse);
//        data.pos += sepBytesToUse;
//
//        if (sepBytesToUse >= add.getRealSize()) {
//            data.lineno++;
//        }
//
//        return makeString(runtime, line);
//    }
//
//    private void swallowLF(ByteList list) {
//        while (ptr.pos < list.getRealSize()) {
//            if (list.get((int)ptr.pos) == '\n') {
//                ptr.pos++;
//            } else {
//                break;
//            }
//        }
//    }

    @JRubyMethod(name = "gets", optional = 2, writes = FrameField.LASTLINE)
    public IRubyObject gets(ThreadContext context, IRubyObject[] args) {
        checkReadable();

        IRubyObject str = getline(context, args);

        context.setLastLine(str);
        return str;
    }

    // strio_getline
    private IRubyObject getline(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        IRubyObject str = context.nil;;
        int n, limit = -1;

        switch (args.length) {
            case 0:
                str = runtime.getGlobalVariables().get("$/");
                break;

            case 1:
            {
                str = args[0];
                if (!str.isNil() && !(str instanceof RubyString)) {
                    IRubyObject tmp = str.checkStringType19();
                    if (tmp.isNil()) {
                        limit = RubyNumeric.num2int(str);
                        if (limit == 0) return runtime.newString();
                        str = runtime.getGlobalVariables().get("$/");
                    } else {
                        str = tmp;
                    }
                }
                break;
            }

            case 2:
                if (!args[0].isNil()) str = args[0].convertToString();
                // 2.0 ignores double nil, 1.9 raises
                if (runtime.is2_0()) {
                    if (!args[1].isNil()) {
                        limit = RubyNumeric.num2int(args[1]);
                    }
                } else {
                    limit = RubyNumeric.num2int(args[1]);
                }
                break;
        }

        if (isEndOfString()) {
            return context.nil;
        }

        ByteList sByteList = ptr.string.getByteList();
        byte[] sBytes = sByteList.getUnsafeBytes();
        int begin = sByteList.getBegin();
        int s = begin + ptr.pos;
        int e = begin + sByteList.getRealSize();
        int p;

        if (limit > 0 && s + limit < e) {
            e = sByteList.getEncoding().rightAdjustCharHead(sBytes, s, s + limit, e);
        }
        if (str.isNil()) {
            str = strioSubstr(runtime, ptr.pos, e - s);
        } else if ((n = ((RubyString)str).size()) == 0) {
            // this is not an exact port; the original confused me
            p = s;
            // remove leading \n
            while (sBytes[p] == '\n') {
                if (++p == e) {
                    return context.nil;
                }
            }
            s = p;
            // find next \n or end; if followed by \n, include it too
            p = StringSupport.memchr(sBytes, p, '\n', e - p);
            if (p != -1) {
                if (++p < e && sBytes[p] == '\n') {
                    e = p + 1;
                } else {
                    e = p;
                }
            }
            str = strioSubstr(runtime, s - begin, e - s);
        } else if (n == 1) {
            RubyString strStr = (RubyString)str;
            ByteList strByteList = strStr.getByteList();
            if ((p = StringSupport.memchr(sBytes, s, strByteList.get(0), e - s)) != -1) {
                e = p + 1;
            }
            str = strioSubstr(runtime, ptr.pos, e - s);
        } else {
            if (n < e - s) {
                RubyString strStr = (RubyString)str;
                ByteList strByteList = strStr.getByteList();
                byte[] strBytes = strByteList.getUnsafeBytes();

                int[] skip = new int[1 << CHAR_BIT];
                int pos;
                p = strByteList.getBegin();
                bm_init_skip(skip, strBytes, p, n);
                if ((pos = bm_search(strBytes, p, n, sBytes, s, e - s, skip)) >= 0) {
                    e = s + pos + n;
                }
            }
            str = strioSubstr(runtime, ptr.pos, e - s);
        }
        ptr.pos = e - begin;
        ptr.lineno++;
        return str;
    }

    @JRubyMethod(name = {"length", "size"})
    public IRubyObject length() {
        checkInitialized();
        checkFinalized();
        return getRuntime().newFixnum(ptr.string.size());
    }

    @JRubyMethod(name = "lineno")
    public IRubyObject lineno(ThreadContext context) {
        return context.runtime.newFixnum(ptr.lineno);
    }

    @JRubyMethod(name = "lineno=", required = 1)
    public IRubyObject set_lineno(ThreadContext context, IRubyObject arg) {
        ptr.lineno = RubyNumeric.fix2int(arg);

        return context.nil;
    }

    @JRubyMethod(name = {"pos", "tell"})
    public IRubyObject pos(ThreadContext context) {
        checkInitialized();

        return context.runtime.newFixnum(ptr.pos);
    }

    @JRubyMethod(name = "pos=", required = 1)
    public IRubyObject set_pos(IRubyObject arg) {
        checkInitialized();

        int p = RubyNumeric.fix2int(arg);
        
        if (p < 0) throw getRuntime().newErrnoEINVALError(arg.toString());

        ptr.pos = p;

        return arg;
    }

    @JRubyMethod(name = "print", rest = true)
    public IRubyObject print(ThreadContext context, IRubyObject[] args) {
        return RubyIO.print(context, this, args);
    }

    @JRubyMethod(name = "printf", required = 1, rest = true)
    public IRubyObject printf(ThreadContext context, IRubyObject[] args) {
        callMethod(context, "write", RubyKernel.sprintf(context, this, args));
        return getRuntime().getNil();
    }

    private void strioExtend(int pos, int len) {
        int olen;

        checkModifiable();
        olen = ptr.string.size();
        if (pos + len > olen) {
            ptr.string.resize(pos + len);
            if (pos > olen) {
                ByteList ptrByteList = ptr.string.getByteList();
                // zero the gap
                Arrays.fill(ptrByteList.getUnsafeBytes(),
                        ptrByteList.getBegin() + olen,
                        ptrByteList.getBegin() + pos,
                        (byte)0);
            }
        } else {
            ptr.string.modify19();
        }
    }

    // MRI: strio_putc
    @JRubyMethod(name = "putc")
    public IRubyObject putc(ThreadContext context, IRubyObject ch) {
        Ruby runtime = context.runtime;
        checkWritable();
        IRubyObject str;

        checkModifiable();
        if (ch instanceof RubyString) {
            str = ((RubyString)ch).substr19(runtime, 0, 1);
        }
        else {
            byte c = RubyNumeric.num2chr(ch);
            str = RubyString.newString(runtime, new byte[]{c});
        }
        write(context, str);
        return ch;
    }

    public static final ByteList NEWLINE = ByteList.create("\n");

    @JRubyMethod(name = "puts", rest = true)
    public IRubyObject puts(ThreadContext context, IRubyObject[] args) {
        checkModifiable();
        return puts(context, this, args);
    }

    private static IRubyObject puts(ThreadContext context, IRubyObject maybeIO, IRubyObject[] args) {
        // TODO: This should defer to RubyIO logic, but we don't have puts right there for 1.9
        Ruby runtime = context.runtime;
        if (args.length == 0) {
            RubyIO.write(context, maybeIO, RubyString.newStringShared(runtime, NEWLINE));
            return runtime.getNil();
        }

        for (int i = 0; i < args.length; i++) {
            RubyString line = null;

            if (!args[i].isNil()) {
                IRubyObject tmp = args[i].checkArrayType();
                if (!tmp.isNil()) {
                    RubyArray arr = (RubyArray) tmp;
                    if (runtime.isInspecting(arr)) {
                        line = runtime.newString("[...]");
                    } else {
                        inspectPuts(context, maybeIO, arr);
                        continue;
                    }
                } else {
                    if (args[i] instanceof RubyString) {
                        line = (RubyString) args[i];
                    } else {
                        line = args[i].asString();
                    }
                }
            }

            if (line != null) RubyIO.write(context, maybeIO, line);

            if (line == null || !line.getByteList().endsWith(NEWLINE)) {
                RubyIO.write(context, maybeIO, RubyString.newStringShared(runtime, NEWLINE));
            }
        }

        return runtime.getNil();
    }

    private static IRubyObject inspectPuts(ThreadContext context, IRubyObject maybeIO, RubyArray array) {
        Ruby runtime = context.runtime;
        try {
            runtime.registerInspecting(array);
            return puts(context, maybeIO, array.toJavaArray());
        } finally {
            runtime.unregisterInspecting(array);
        }
    }
    
    // Make string based on internal data encoding (which ironically is its
    // external encoding.  This seems messy and we should consider a more
    // uniform method for makeing strings (we have a slightly different variant
    // of this in RubyIO.
    private RubyString makeString(Ruby runtime, ByteList buf, boolean setEncoding) {
        if (runtime.is1_9() && setEncoding) buf.setEncoding(ptr.string.getEncoding());

        RubyString str = RubyString.newString(runtime, buf);
        str.setTaint(true);

        return str;        
    }
    
    private RubyString makeString(Ruby runtime, ByteList buf) {
        return makeString(runtime, buf, true);
    }

    @JRubyMethod(name = "read", optional = 2)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        checkReadable();

        Ruby runtime = context.runtime;
        IRubyObject str = runtime.getNil();
        int len;
        boolean binary = false;

        switch (args.length) {
        case 2:
            str = args[1];
            if (!str.isNil()) {
                str = str.convertToString();
                ((RubyString)str).modify();
            }
        case 1:
            if (!args[0].isNil()) {
                len = RubyNumeric.fix2int(args[0]);

                if (len < 0) {
                    throw getRuntime().newArgumentError("negative length " + len + " given");
                }
                if (len > 0 && isEndOfString()) {
                    if (!str.isNil()) ((RubyString)str).resize(0);
                    return getRuntime().getNil();
                }
                binary = true;
                break;
            }
        case 0:
            len = ptr.string.size();
            if (len <= ptr.pos) {
                if (str.isNil()) {
                    str = runtime.newString();
                } else {
                    ((RubyString)str).resize(0);
                }

                return str;
            } else {
                len -= ptr.pos;
            }
            break;
        default:
            throw getRuntime().newArgumentError(args.length, 0);
        }

        if (str.isNil()) {
            str = strioSubstr(runtime, ptr.pos, len);
            if (binary) ((RubyString)str).setEncoding(ASCIIEncoding.INSTANCE);
        } else {
            int rest = ptr.string.size() - ptr.pos;
            if (len > rest) len = rest;
            ((RubyString)str).resize(len);
            ByteList strByteList = ((RubyString)str).getByteList();
            byte[] strBytes = strByteList.getUnsafeBytes();
            ByteList dataByteList = ptr.string.getByteList();
            byte[] dataBytes = dataByteList.getUnsafeBytes();
            System.arraycopy(dataBytes, dataByteList.getBegin() + ptr.pos, strBytes, strByteList.getBegin(), len);
            if (binary) {
                ((RubyString)str).setEncoding(ASCIIEncoding.INSTANCE);
            } else {
                ((RubyString)str).setEncoding(ptr.string.getEncoding());
            }
        }
        ptr.pos += ((RubyString)str).size();
        return str;
    }

    @JRubyMethod(name="read_nonblock", optional = 2)
    public IRubyObject read_nonblock(ThreadContext context, IRubyObject[] args) {
        // TODO: nonblock exception option

        IRubyObject val = read(context, args);
        if (val.isNil()) {
            throw context.runtime.newEOFError();
        }

        return val;
    }

    @JRubyMethod(name = "readchar")
    public IRubyObject readchar(ThreadContext context) {
        IRubyObject c = callMethod(context, "getc");

        if (c.isNil()) throw getRuntime().newEOFError();

        return c;
    }

    @JRubyMethod(name = "readbyte")
    public IRubyObject readbyte(ThreadContext context) {
        IRubyObject c = callMethod(context, "getbyte");

        if (c.isNil()) throw getRuntime().newEOFError();

        return c;
    }

    @JRubyMethod(name = "readline", optional = 1, writes = FrameField.LASTLINE)
    public IRubyObject readline(ThreadContext context, IRubyObject[] args) {
        IRubyObject line = callMethod(context, "gets", args);

        if (line.isNil()) throw getRuntime().newEOFError();

        return line;
    }

    @JRubyMethod(name = "readlines", optional = 2)
    public IRubyObject readlines(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        
        if (args.length > 0 && !args[args.length - 1].isNil() && args[args.length - 1].checkStringType19().isNil() &&
                RubyNumeric.num2long(args[args.length - 1]) == 0) {
            throw runtime.newArgumentError("invalid limit: 0 for each_line");
        }

        RubyArray ary = runtime.newArray();
        IRubyObject line;

        checkReadable();

        while (!(line = getline(context, args)).isNil()) {
            ary.append(line);
        }
        return ary;
    }

    // MRI: strio_reopen
    @JRubyMethod(name = "reopen", required = 0, optional = 2)
    public IRubyObject reopen(ThreadContext context, IRubyObject[] args) {
        checkFrozen();
        
        if (args.length == 1 && !(args[0] instanceof RubyString)) {
            return initialize_copy(context, args[0]);
        }

        // reset the state
        strioInit(context, args);
        return this;
    }

    @JRubyMethod(name = "rewind")
    public IRubyObject rewind(ThreadContext context) {
        checkInitialized();

        this.ptr.pos = 0;
        this.ptr.lineno = 0;
        return RubyFixnum.zero(context.runtime);
    }

    @JRubyMethod(required = 1, optional = 1)
    public IRubyObject seek(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        checkFrozen();
        checkFinalized();
        
        int offset = RubyNumeric.num2int(args[0]);
        IRubyObject whence = context.nil;

        if (args.length > 1 && !args[0].isNil()) whence = args[1];
        
        checkOpen();

        switch (whence.isNil() ? 0 : RubyNumeric.num2int(whence)) {
            case 0:
                break;
            case 1:
                offset += ptr.pos;
                break;
            case 2:
                offset += ptr.string.size();
                break;
            default:
                throw runtime.newErrnoEINVALError("invalid whence");
        }
        
        if (offset < 0) throw runtime.newErrnoEINVALError("invalid seek value");

        ptr.pos = offset;

        return RubyFixnum.zero(runtime);
    }

    @JRubyMethod(name = "string=", required = 1)
    public IRubyObject set_string(IRubyObject arg) {
        checkFrozen();
        ptr.flags &= ~OpenFile.READWRITE;
        RubyString str = arg.convertToString();
        ptr.flags = str.isFrozen() ? OpenFile.READABLE : OpenFile.READWRITE;
        ptr.pos = 0;
        ptr.lineno = 0;
        return ptr.string = str;
    }

    @JRubyMethod(name = "string")
    public IRubyObject string(ThreadContext context) {
        if (ptr.string == null) return context.nil;

        return ptr.string;
    }

    @JRubyMethod(name = "sync")
    public IRubyObject sync(ThreadContext context) {
        checkInitialized();
        return context.runtime.getTrue();
    }
    
    @JRubyMethod(name = {"sysread", "readpartial"}, optional = 2)
    public IRubyObject sysread(ThreadContext context, IRubyObject[] args) {
        IRubyObject val = callMethod(context, "read", args);
        
        if (val.isNil()) throw getRuntime().newEOFError();
        
        return val;
    }
    
    // only here for the fake-out class in org.jruby
    public IRubyObject sysread(IRubyObject[] args) {
        return sysread(getRuntime().getCurrentContext(), args);
    }

    @JRubyMethod(name = "truncate", required = 1)
    public IRubyObject truncate(IRubyObject len) {
        checkWritable();

        int l = RubyFixnum.fix2int(len);
        int plen = ptr.string.size();
        if (l < 0) {
            throw getRuntime().newErrnoEINVALError("negative legnth");
        }
        ptr.string.resize(l);
        ByteList buf = ptr.string.getByteList();
        if (plen < l) {
            // zero the gap
            Arrays.fill(buf.getUnsafeBytes(), buf.getBegin() + plen, buf.getBegin() + l, (byte) 0);
        }
        return len;
    }

    @JRubyMethod(name = "ungetc")
    public IRubyObject ungetc(ThreadContext context, IRubyObject arg) {
        // TODO: Not a line-by-line port.
        checkReadable();
        return ungetbyte(context, arg);
    }

    private void ungetbyteCommon(int c) {
        ptr.string.modify();
        ptr.pos--;
        
        ByteList bytes = ptr.string.getByteList();

        if (isEndOfString()) bytes.length((int)ptr.pos + 1);

        if (ptr.pos == -1) {
            bytes.prepend((byte)c);
            ptr.pos = 0;
        } else {
            bytes.set((int) ptr.pos, c);
        }
    }

    private void ungetbyteCommon(RubyString ungetBytes) {
        ByteList ungetByteList = ungetBytes.getByteList();
        int len = ungetByteList.getRealSize();
        int start = ptr.pos;
        
        if (len == 0) return;
        
        ptr.string.modify();
        
        if (len > ptr.pos) {
            start = 0;
        } else {
            start = ptr.pos - len;
        }
        
        ByteList bytes = ptr.string.getByteList();
        
        if (isEndOfString()) bytes.length(Math.max(ptr.pos, len));

        bytes.replace(start, ptr.pos - start, ungetBytes.getByteList());
        
        ptr.pos = start;
    }
    
    @JRubyMethod
    public IRubyObject ungetbyte(ThreadContext context, IRubyObject arg) {
        // TODO: Not a line-by-line port.
        checkReadable();
        
        if (arg.isNil()) return arg;

        checkModifiable();

        if (arg instanceof RubyFixnum) {
            ungetbyteCommon(RubyNumeric.fix2int(arg));
        } else {
            ungetbyteCommon(arg.convertToString());
        }

        return context.nil;
    }

    @JRubyMethod(name = "syswrite", required = 1)
    public IRubyObject syswrite(ThreadContext context, IRubyObject arg) {
        return RubyIO.write(context, this, arg);
    }

    @JRubyMethod(name = "write_nonblock", required = 1, optional = 1)
    public IRubyObject syswrite_nonblock(ThreadContext context, IRubyObject[] args) {
        // TODO: handle opts?
        return syswrite(context, args[0]);
    }

    // MRI: strio_write
    @JRubyMethod(name = {"write"}, required = 1)
    public IRubyObject write(ThreadContext context, IRubyObject arg) {
        checkWritable();

        Ruby runtime = context.runtime;

        RubyString str = arg.asString();
        int len, olen;
        Encoding enc, enc2;

        enc = ptr.string.getEncoding();
        enc2 = str.getEncoding();
        if (enc != enc2 && enc != EncodingUtils.ascii8bitEncoding(runtime)
                // this is a hack because we don't seem to handle incoming ASCII-8BIT properly in transcoder
                && enc2 != ASCIIEncoding.INSTANCE) {
            str = runtime.newString(EncodingUtils.strConvEnc(context, str.getByteList(), enc2, enc));
        }
        len = str.size();
        if (len == 0) return RubyFixnum.zero(runtime);
        checkModifiable();
        olen = ptr.string.size();
        if ((ptr.flags & OpenFile.APPEND) != 0) {
            ptr.pos = olen;
        }
        if (ptr.pos == olen) {
            EncodingUtils.encStrBufCat(runtime, ptr.string, str.getByteList(), enc);
        } else {
            strioExtend(ptr.pos, len);
            ByteList ptrByteList = ptr.string.getByteList();
            System.arraycopy(str.getByteList().getUnsafeBytes(), str.getByteList().getBegin(), ptrByteList.getUnsafeBytes(), ptrByteList.begin + ptr.pos, len);
        }
        ptr.string.infectBy(str);
        ptr.string.infectBy(this);
        ptr.pos += len;
        return RubyFixnum.newFixnum(runtime, len);
    }
    
    @JRubyMethod
    public IRubyObject set_encoding(ThreadContext context, IRubyObject ext_enc) {
        Encoding enc;

        if (ext_enc.isNil()) {
            enc = EncodingUtils.defaultExternalEncoding(context.runtime);
        } else {
            enc = EncodingUtils.rbToEncoding(context, ext_enc);
        }
        if(ptr.string.getEncoding() != enc) {
            ptr.string.modify();
            ptr.string.setEncoding(enc);
        }
        return this;
    }
    
    @JRubyMethod
    public IRubyObject set_encoding(ThreadContext context, IRubyObject enc, IRubyObject ignored) {
        return set_encoding(context, enc);
    }
    
    @JRubyMethod
    public IRubyObject set_encoding(ThreadContext context, IRubyObject enc, IRubyObject ignored1, IRubyObject ignored2) {
        return set_encoding(context, enc);
    }
    
    @JRubyMethod
    public IRubyObject external_encoding(ThreadContext context) {
        return context.runtime.getEncodingService().convertEncodingToRubyEncoding(ptr.string.getEncoding());
    }
    
    @JRubyMethod
    public IRubyObject internal_encoding(ThreadContext context) {
        return context.nil;
    }
    
    @JRubyMethod(name = "each_codepoint")
    public IRubyObject each_codepoint(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;

        if (!block.isGiven()) return enumeratorize(runtime, this, "each_codepoint");

        checkReadable();
        
        Encoding enc = ptr.string.getEncoding();
        byte[] unsafeBytes = ptr.string.getByteList().getUnsafeBytes();
        int begin = ptr.string.getByteList().getBegin();
        for (;;) {
            if (ptr.pos >= ptr.string.size()) {
                return this;
            }
            
            int c = StringSupport.codePoint(runtime, enc, unsafeBytes, begin + ptr.pos, unsafeBytes.length);
            int n = StringSupport.codeLength(enc, c);
            block.yield(context, runtime.newFixnum(c));
            ptr.pos += n;
        }
    }

    @JRubyMethod(name = "codepoints")
    public IRubyObject codepoints(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        runtime.getWarnings().warn("StringIO#codepoints is deprecated; use #each_codepoint");

        if (!block.isGiven()) return enumeratorize(runtime, this, "each_codepoint");

        return each_codepoint(context, block);
    }

    /* rb: check_modifiable */
    public void checkFrozen() {
        super.checkFrozen();
        checkInitialized();
    }

    private boolean readable() {
        return (flags & STRIO_READABLE) != 0
                && (ptr.flags & OpenFile.READABLE) != 0;
    }

    private boolean writable() {
        return (flags & STRIO_WRITABLE) != 0
                && (ptr.flags & OpenFile.WRITABLE) != 0;
    }

    private boolean closed() {
        return !((flags & STRIO_READWRITE) != 0
                && (ptr.flags & OpenFile.READWRITE) != 0);
    }

    /* rb: readable */
    private void checkReadable() {
        checkInitialized();
        if (!readable()) {
            throw getRuntime().newIOError("not opened for reading");
        }
    }

    /* rb: writable */
    private void checkWritable() {
        checkInitialized();
        if (!writable()) {
            throw getRuntime().newIOError("not opened for writing");
        }

        // Tainting here if we ever want it. (secure 4)
    }

    private void checkModifiable() {
        checkFrozen();
        if (ptr.string.isFrozen()) throw getRuntime().newIOError("not modifiable string");
    }

    private void checkInitialized() {
        if (ptr == null) {
            throw getRuntime().newIOError("uninitialized stream");
        }
    }

    private void checkFinalized() {
        if (ptr.string == null) {
            throw getRuntime().newIOError("not opened");
        }
    }

    private void checkOpen() {
        if (closed()) {
            throw getRuntime().newIOError(RubyIO.CLOSED_STREAM_MSG);
        }
    }
}
