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

import org.jruby.util.StringSupport;
import org.jcodings.Encoding;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jcodings.specific.ASCIIEncoding;
import org.joni.Regex;
import org.jruby.CompatVersion;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyIO;
import org.jruby.RubyKernel;
import org.jruby.RubyNumeric;
import org.jruby.RubyString;

import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.addons.IOJavaAddons;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import static org.jruby.CompatVersion.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.Stream;

import static org.jruby.RubyEnumerator.enumeratorize;
import org.jruby.runtime.encoding.EncodingCapable;

@JRubyClass(name="StringIO")
@SuppressWarnings("deprecation")
public class RubyStringIO extends org.jruby.RubyStringIO implements EncodingCapable {
    static class StringIOData {
        int pos = 0;
        int lineno = 0;
        boolean eof = false;
        boolean closedRead = false;
        boolean closedWrite = false;
        ModeFlags modes;
        /**
         * ATTN: the value of internal might be reset to null
         * (during StringIO.open with block), so watch out for that.
         */
        RubyString internal;
    }
    StringIOData ptr;

    private static ObjectAllocator STRINGIO_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyStringIO(runtime, klass);
        }
    };

    public static RubyClass createStringIOClass(final Ruby runtime) {
        RubyClass stringIOClass = runtime.defineClass(
                "StringIO", runtime.getClass("Data"), STRINGIO_ALLOCATOR);

        stringIOClass.defineAnnotatedMethods(RubyStringIO.class);
        stringIOClass.includeModule(runtime.getEnumerable());

        if (runtime.getObject().isConstantDefined("Java")) {
            stringIOClass.defineAnnotatedMethods(IOJavaAddons.AnyIO.class);
        }

        return stringIOClass;
    }

    @Override
    public Encoding getEncoding() {
        return ptr.internal.getEncoding();
    }

    @Override
    public void setEncoding(Encoding e) {
        ptr.internal.setEncoding(e);
    }

    @JRubyMethod(meta = true, rest = true)
    public static IRubyObject open(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        RubyStringIO strio = (RubyStringIO)((RubyClass)recv).newInstance(context, args, Block.NULL_BLOCK);
        IRubyObject val = strio;

        if (block.isGiven()) {
            try {
                val = block.yield(context, strio);
            } finally {
                strio.doFinalize();
            }
        }
        return val;
    }

    protected RubyStringIO(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        ptr = new StringIOData();
    }

    private void initializeModes(Object modeArgument) {
        Ruby runtime = getRuntime();

        if (modeArgument == null) {
            ptr.modes = RubyIO.newModeFlags(runtime, "r+");
        } else if (modeArgument instanceof Long) {
            ptr.modes = RubyIO.newModeFlags(runtime, ((Long) modeArgument).longValue());
        } else {
            ptr.modes = RubyIO.newModeFlags(runtime, (String) modeArgument);
        }

        setupModes();
    }

    @JRubyMethod(optional = 2, visibility = PRIVATE)
    @Override
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        Object modeArgument = null;
        Ruby runtime = getRuntime();
        
        switch (args.length) {
            case 0:
                ptr.internal = runtime.is1_9() ? RubyString.newEmptyString(runtime, runtime.getDefaultExternalEncoding()) : RubyString.newEmptyString(runtime);
                modeArgument = "r+";
                break;
            case 1:
                ptr.internal = args[0].convertToString();
                modeArgument = ptr.internal.isFrozen() ? "r" : "r+";
                break;
            case 2:
                ptr.internal = args[0].convertToString();
                if (args[1] instanceof RubyFixnum) {
                    modeArgument = RubyFixnum.fix2long(args[1]);
                } else {
                    modeArgument = args[1].convertToString().toString();
                }
                break;
        }

        initializeModes(modeArgument);

        if (ptr.modes.isWritable() && ptr.internal.isFrozen()) {
            throw runtime.newErrnoEACCESError("Permission denied");
        }

        if (ptr.modes.isTruncate()) {
            ptr.internal.modifyCheck();
            ptr.internal.empty();
        }

        return this;
    }

    @JRubyMethod(visibility = PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject other) {
        RubyStringIO otherIO = (RubyStringIO) TypeConverter.convertToType(other, 
                getRuntime().getClass("StringIO"), "to_strio");

        if (this == otherIO) return this;

        ptr = otherIO.ptr;
        if (otherIO.isTaint()) setTaint(true);

        return this;
    }

    @JRubyMethod(name = "<<", required = 1)
    @Override
    public IRubyObject append(ThreadContext context, IRubyObject arg) {
        // Claims conversion is done via 'to_s' in docs.
        callMethod(context, "write", arg);
        
        return this; 
    }

    @JRubyMethod
    @Override
    public IRubyObject binmode() {
        return this;
    }

    @JRubyMethod
    @Override
    public IRubyObject close() {
        checkInitialized();
        checkOpen();

        ptr.closedRead = true;
        ptr.closedWrite = true;

        return getRuntime().getNil();
    }

    private void doFinalize() {
        ptr.closedRead = true;
        ptr.closedWrite = true;
        ptr.internal = null;
    }

    @JRubyMethod(name = "closed?")
    @Override
    public IRubyObject closed_p() {
        checkInitialized();
        return getRuntime().newBoolean(ptr.closedRead && ptr.closedWrite);
    }

    @JRubyMethod
    @Override
    public IRubyObject close_read() {
        checkReadable();
        ptr.closedRead = true;

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_read?")
    @Override
    public IRubyObject closed_read_p() {
        checkInitialized();
        return getRuntime().newBoolean(ptr.closedRead);
    }

    @JRubyMethod
    @Override
    public IRubyObject close_write() {
        checkWritable();
        ptr.closedWrite = true;

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_write?")
    @Override
    public IRubyObject closed_write_p() {
        checkInitialized();
        return getRuntime().newBoolean(ptr.closedWrite);
    }

    @Override
    public IRubyObject eachInternal(ThreadContext context, IRubyObject[] args, Block block) {
        IRubyObject line = getsOnly(context, args);

        while (!line.isNil()) {
            block.yield(context, line);
            line = getsOnly(context, args);
        }

        return this;
    }

    @JRubyMethod(name = "each", optional = 1, writes = FrameField.LASTLINE)
    @Override
    public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? eachInternal(context, args, block) : enumeratorize(context.runtime, this, "each", args);
    }

    @JRubyMethod(name = "each", optional = 2, writes = FrameField.LASTLINE, compat = RUBY1_9)
    public IRubyObject each19(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each", args);
        
        if (args.length > 0 && !args[args.length - 1].isNil() && args[args.length - 1].checkStringType19().isNil() &&
                RubyNumeric.num2long(args[args.length - 1]) == 0) {
            throw context.runtime.newArgumentError("invalid limit: 0 for each_line");
        }
        
        return eachInternal(context, args, block);
    }

    @JRubyMethod(optional = 1, compat = RUBY1_8)
    @Override
    public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? eachInternal(context, args, block) : enumeratorize(context.runtime, this, "each_line", args);
    }

    @JRubyMethod(name = "each_line", optional = 2, compat = RUBY1_9)
    public IRubyObject each_line19(ThreadContext context, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, this, "each_line", args);
        
        return each19(context, args, block);
    }

    @JRubyMethod(optional = 1)
    @Override
    public IRubyObject lines(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? each(context, args, block) : enumeratorize(context.runtime, this, "lines", args);
    }

    @Override
    public IRubyObject each_byte(ThreadContext context, Block block) {
        checkReadable();
        Ruby runtime = context.runtime;
        ByteList bytes = ptr.internal.getByteList();

        // Check the length every iteration, since
        // the block can modify this string.
        while (ptr.pos < bytes.length()) {
            block.yield(context, runtime.newFixnum(bytes.get((int) ptr.pos++) & 0xFF));
        }
        return this;
    }

    @JRubyMethod(name = "each_byte")
    @Override
    public IRubyObject each_byte19(ThreadContext context, Block block) {
        return block.isGiven() ? each_byte(context, block) : enumeratorize(context.runtime, this, "each_byte");
    }

    @JRubyMethod
    @Override
    public IRubyObject bytes(ThreadContext context, Block block) {
        return block.isGiven() ? each_byte(context, block) : enumeratorize(context.runtime, this, "bytes");
    }

    @Override
    public IRubyObject each_charInternal(final ThreadContext context, final Block block) {
        checkReadable();

        Ruby runtime = context.runtime;
        ByteList bytes = ptr.internal.getByteList();
        int len = bytes.getRealSize();
        int end = bytes.getBegin() + len;
        Encoding enc = runtime.is1_9() ? bytes.getEncoding() : runtime.getKCode().getEncoding();        
        while (ptr.pos < len) {
            int pos = (int) ptr.pos;
            int n = StringSupport.length(enc, bytes.getUnsafeBytes(), pos, end);

            if(len < pos + n) n = len - pos;

            ptr.pos += n;

            block.yield(context, ptr.internal.makeShared19(runtime, pos, n));
        }

        return this;
    }

    @JRubyMethod
    @Override
    public IRubyObject each_char(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal(context, block) : enumeratorize(context.runtime, this, "each_char");
    }

    @JRubyMethod
    @Override
    public IRubyObject chars(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal(context, block) : enumeratorize(context.runtime, this, "chars");
    }

    @JRubyMethod(name = {"eof", "eof?"})
    @Override
    public IRubyObject eof() {
        return getRuntime().newBoolean(isEOF());
    }

    private boolean isEOF() {
        return isEndOfString() || (getRuntime().is1_8() && ptr.eof);
    }
    
    private boolean isEndOfString() {
        return ptr.pos >= ptr.internal.getByteList().length();
    }    

    @JRubyMethod(name = "fcntl")
    @Override
    public IRubyObject fcntl() {
        throw getRuntime().newNotImplementedError("fcntl not implemented");
    }

    @JRubyMethod(name = "fileno")
    @Override
    public IRubyObject fileno() {
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "flush")
    @Override
    public IRubyObject flush() {
        return this;
    }

    @JRubyMethod(name = "fsync")
    @Override
    public IRubyObject fsync() {
        return RubyFixnum.zero(getRuntime());
    }

    @JRubyMethod(name = {"getc", "getbyte"})
    @Override
    public IRubyObject getc() {
        checkReadable();
        if (isEndOfString()) return getRuntime().getNil();

        return getRuntime().newFixnum(ptr.internal.getByteList().get((int)ptr.pos++) & 0xFF);
    }

    @JRubyMethod(name = "getc", compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject getc19(ThreadContext context) {
        checkReadable();
        if (isEndOfString()) return context.runtime.getNil();

        return context.runtime.newString("" + (char) (ptr.internal.getByteList().get((int) ptr.pos++) & 0xFF));
    }
    
    private RubyString strioSubstr(Ruby runtime, int pos, int len) {
        RubyString str = ptr.internal;
        ByteList strByteList = str.getByteList();
        byte[] strBytes = strByteList.getUnsafeBytes();
        Encoding enc = str.getEncoding();
        int rlen = str.size() - pos;
        
        if (len > rlen) len = rlen;
        if (len < 0) len = 0;
        
        // This line is not in MRI; they may reference a bogus pointer, but never dereference it.
        // http://bugs.ruby-lang.org/issues/8728
        if (len == 0) return RubyString.newEmptyString(runtime, enc);
        
        return RubyString.newStringShared(runtime, strBytes, strByteList.getBegin() + pos, len, enc);
    }

    private IRubyObject internalGets18(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        if (!isEndOfString() && !ptr.eof) {
            boolean isParagraph = false;
            ByteList sep = ((RubyString)runtime.getGlobalVariables().get("$/")).getByteList();
            IRubyObject sepArg;
            int limit = -1;

            sepArg = (args.length > 0 ? args[0] : null);

            if (sepArg != null) {
                if (sepArg.isNil()) {
                    int bytesAvailable = ptr.internal.getByteList().getRealSize() - (int)ptr.pos;
                    int bytesToUse = (limit < 0 || limit >= bytesAvailable ? bytesAvailable : limit);
                    
                    // add additional bytes to fix trailing broken character
                    bytesToUse += StringSupport.bytesToFixBrokenTrailingCharacter(ptr.internal.getByteList(), bytesToUse);
                    
                    ByteList buf = ptr.internal.getByteList().makeShared(
                        (int)ptr.pos, bytesToUse);
                    ptr.pos += buf.getRealSize();
                    return makeString(runtime, buf);
                }

                sep = sepArg.convertToString().getByteList();
                if (sep.getRealSize() == 0) {
                    isParagraph = true;
                    sep = Stream.PARAGRAPH_SEPARATOR;
                }
            }

            ByteList ss = ptr.internal.getByteList();

            if (isParagraph) {
                swallowLF(ss);
                if (ptr.pos == ss.getRealSize()) {
                    return runtime.getNil();
                }
            }

            int sepIndex = ss.indexOf(sep, (int)ptr.pos);

            ByteList add;
            if (-1 == sepIndex) {
                sepIndex = ptr.internal.getByteList().getRealSize();
                add = ByteList.EMPTY_BYTELIST;
            } else {
                add = sep;
            }

            int bytes = sepIndex - (int)ptr.pos;
            int bytesToUse = (limit < 0 || limit >= bytes ? bytes : limit);

            int bytesWithSep = sepIndex - (int)ptr.pos + add.getRealSize();
            int bytesToUseWithSep = (limit < 0 || limit >= bytesWithSep ? bytesWithSep : limit);

            ByteList line = new ByteList(bytesToUseWithSep);
            line.append(ptr.internal.getByteList(), (int)ptr.pos, bytesToUse);
            ptr.pos += bytesToUse;

            int sepBytesToUse = bytesToUseWithSep - bytesToUse;
            line.append(add, 0, sepBytesToUse);
            ptr.pos += sepBytesToUse;

            if (sepBytesToUse >= add.getRealSize()) {
                ptr.lineno++;
            }

            return makeString(runtime, line);
        }
        return runtime.getNil();
    }
    
    private IRubyObject internalGets19(ThreadContext context, IRubyObject[] args) {
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
        
        if (ptr.pos >= (n = ptr.internal.size())) {
            return context.nil;
        }
        
        ByteList dataByteList = ptr.internal.getByteList();
        byte[] dataBytes = dataByteList.getUnsafeBytes();
        int begin = dataByteList.getBegin();
        int s = begin + ptr.pos;
        int e = begin + dataByteList.getRealSize();
        int p;
        
        if (limit > 0 && s + limit < e) {
            e = dataByteList.getEncoding().rightAdjustCharHead(dataBytes, s, s + limit, e);
        }
        
        if (str.isNil()) {
            str = strioSubstr(runtime, ptr.pos, e - s);
        } else if ((n = ((RubyString)str).size()) == 0) {
            // this is not an exact port; the original confused me
            p = s;
            // remove leading \n
            while (dataBytes[p] == '\n') {
                if (++p == e) {
                    return context.nil;
                }
            }
            s = p;
            // find next \n or end; if followed by \n, include it too
            p = memchr(dataBytes, p, '\n', e - p);
            if (p != -1) {
                if (++p < e && dataBytes[p] == '\n') {
                    e = p + 1;
                } else {
                    e = p;
                }
            }
            str = strioSubstr(runtime, s - begin, e - s);
        } else if (n == 1) {
            RubyString strStr = (RubyString)str;
            ByteList strByteList = strStr.getByteList();
            if ((p = memchr(dataBytes, s, strByteList.get(0), e - s)) != -1) {
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
                if ((pos = bm_search(strBytes, p, n, dataBytes, s, e - s, skip)) >= 0) {
                    e = s + pos + n;
                }
            }
            str = strioSubstr(runtime, ptr.pos, e - s);
        }
        ptr.pos = e - begin;
        ptr.lineno++;
        return str;
    }
    
    private static int memchr(byte[] ptr, int start, int find, int len) {
        for (int i = start; i < start + len; i++) {
            if (ptr[i] == find) return i;
        }
        return -1;
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
            i += skip[big[i + bstart]];
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

    private void swallowLF(ByteList list) {
        while (ptr.pos < list.getRealSize()) {
            if (list.get((int)ptr.pos) == '\n') {
                ptr.pos++;
            } else {
                break;
            }
        }
    }

    @JRubyMethod(name = "gets", optional = 1, writes = FrameField.LASTLINE, compat = CompatVersion.RUBY1_8)
    @Override
    public IRubyObject gets(ThreadContext context, IRubyObject[] args) {
        IRubyObject result = getsOnly(context, args);
        context.setLastLine(result);

        return result;
    }

    @JRubyMethod(name = "gets", optional = 2, writes = FrameField.LASTLINE, compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject gets19(ThreadContext context, IRubyObject[] args) {
        IRubyObject result = getsOnly(context, args);
        context.setLastLine(result);

        return result;
    }

    @Override
    public IRubyObject getsOnly(ThreadContext context, IRubyObject[] args) {
        checkReadable();

        return context.is19 ? internalGets19(context, args) : internalGets18(context, args);
    }

    @JRubyMethod(name = {"tty?", "isatty"})
    @Override
    public IRubyObject isatty() {
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = {"length", "size"})
    @Override
    public IRubyObject length() {
        checkFinalized();
        return getRuntime().newFixnum(ptr.internal.getByteList().length());
    }

    @JRubyMethod(name = "lineno")
    @Override
    public IRubyObject lineno() {
        return getRuntime().newFixnum(ptr.lineno);
    }

    @JRubyMethod(name = "lineno=", required = 1)
    @Override
    public IRubyObject set_lineno(IRubyObject arg) {
        ptr.lineno = RubyNumeric.fix2int(arg);

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "path", compat = CompatVersion.RUBY1_8)
    @Override
    public IRubyObject path() {
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "pid")
    @Override
    public IRubyObject pid() {
        return getRuntime().getNil();
    }

    @JRubyMethod(name = {"pos", "tell"})
    @Override
    public IRubyObject pos() {
        return getRuntime().newFixnum(ptr.pos);
    }

    @JRubyMethod(name = "pos=", required = 1)
    @Override
    public IRubyObject set_pos(IRubyObject arg) {
        ptr.pos = RubyNumeric.fix2int(arg);
        
        if (ptr.pos < 0) throw getRuntime().newErrnoEINVALError("Invalid argument");

        if (getRuntime().is1_8() && !isEndOfString()) ptr.eof = false;

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "print", rest = true)
    @Override
    public IRubyObject print(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        if (args.length != 0) {
            for (int i=0,j=args.length;i<j;i++) {
                append(context, args[i]);
            }
        } else {
            IRubyObject arg = runtime.getGlobalVariables().get("$_");
            append(context, arg.isNil() ? makeString(runtime, new ByteList(new byte[] {'n', 'i', 'l'})) : arg);
        }
        IRubyObject sep = runtime.getGlobalVariables().get("$\\");
        if (!sep.isNil()) append(context, sep);

        return runtime.getNil();
    }

    @JRubyMethod(name = "print", rest = true, compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject print19(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        if (args.length != 0) {
            for (int i=0,j=args.length;i<j;i++) {
                append(context, args[i]);
            }
        } else {
            IRubyObject arg = runtime.getGlobalVariables().get("$_");
            append(context, arg.isNil() ? RubyString.newEmptyString(getRuntime()) : arg);
        }
        IRubyObject sep = runtime.getGlobalVariables().get("$\\");
        if (!sep.isNil()) append(context, sep);

        return runtime.getNil();
    }

    @JRubyMethod(name = "printf", required = 1, rest = true)
    @Override
    public IRubyObject printf(ThreadContext context, IRubyObject[] args) {
        append(context, RubyKernel.sprintf(context, this, args));
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "putc", required = 1)
    @Override
    public IRubyObject putc(IRubyObject obj) {
        checkWritable();
        byte c = RubyNumeric.num2chr(obj);
        checkFrozen();

        ptr.internal.modify();
        ByteList bytes = ptr.internal.getByteList();
        if (ptr.modes.isAppendable()) {
            ptr.pos = bytes.length();
            bytes.append(c);
        } else {
            if (isEndOfString()) bytes.length((int)ptr.pos + 1);

            bytes.set((int) ptr.pos, c);
            ptr.pos++;
        }

        return obj;
    }

    public static final ByteList NEWLINE = ByteList.create("\n");

    @JRubyMethod(name = "puts", rest = true)
    @Override
    public IRubyObject puts(ThreadContext context, IRubyObject[] args) {
        checkWritable();

        // FIXME: the code below is a copy of RubyIO.puts,
        // and we should avoid copy-paste.

        if (args.length == 0) {
            callMethod(context, "write", RubyString.newStringShared(getRuntime(), NEWLINE));
            return getRuntime().getNil();
        }

        for (int i = 0; i < args.length; i++) {
            RubyString line = getRuntime().newString();

            if (args[i].isNil()) {
                if (!getRuntime().is1_9()) {
                    line = getRuntime().newString("nil");
                }
            } else {
                IRubyObject tmp = args[i].checkArrayType();
                if (!tmp.isNil()) {
                    RubyArray arr = (RubyArray) tmp;
                    if (getRuntime().isInspecting(arr)) {
                        line = getRuntime().newString("[...]");
                    } else {
                        inspectPuts(context, arr);
                        continue;
                    }
                } else {
                    if (args[i] instanceof RubyString) {
                        line = (RubyString)args[i];
                    } else {
                        line = args[i].asString();
                    }
                }
            }

            callMethod(context, "write", line);

            if (!line.getByteList().endsWith(NEWLINE)) {
                callMethod(context, "write", RubyString.newStringShared(getRuntime(), NEWLINE));
            }
        }
        return getRuntime().getNil();
    }

    private IRubyObject inspectPuts(ThreadContext context, RubyArray array) {
        try {
            getRuntime().registerInspecting(array);
            return puts(context, array.toJavaArray());
        } finally {
            getRuntime().unregisterInspecting(array);
        }
    }
    
    // Make string based on internal data encoding (which ironically is its
    // external encoding.  This seems messy and we should consider a more
    // uniform method for makeing strings (we have a slightly different variant
    // of this in RubyIO.
    private RubyString makeString(Ruby runtime, ByteList buf, boolean setEncoding) {
        if (runtime.is1_9() && setEncoding) buf.setEncoding(ptr.internal.getEncoding());

        RubyString str = RubyString.newString(runtime, buf);
        str.setTaint(true);

        return str;        
    }
    
    private RubyString makeString(Ruby runtime, ByteList buf) {
        return makeString(runtime, buf, true);
    }

    @JRubyMethod(name = "read", optional = 2)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        if (context.is19) {
            return read19(args);
        }
        return read18(args);
    }
    
    private IRubyObject read19(IRubyObject[] args) {
        checkReadable();

        Ruby runtime = getRuntime();
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
            len = ptr.internal.size();
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
            int rest = ptr.internal.size() - ptr.pos;
            if (len > rest) len = rest;
            ((RubyString)str).resize(len);
            ByteList strByteList = ((RubyString)str).getByteList();
            byte[] strBytes = strByteList.getUnsafeBytes();
            ByteList dataByteList = ptr.internal.getByteList();
            byte[] dataBytes = dataByteList.getUnsafeBytes();
            System.arraycopy(dataBytes, dataByteList.getBegin() + ptr.pos, strBytes, strByteList.getBegin(), len);
            if (binary) {
                ((RubyString)str).setEncoding(ASCIIEncoding.INSTANCE);
            } else {
                ((RubyString)str).setEncoding(ptr.internal.getEncoding());
            }
        }
        ptr.pos += ((RubyString)str).size();
        return str;
    }
    
    private IRubyObject read18(IRubyObject[] args) {
        checkReadable();

        ByteList buf = null;
        int length = 0;
        int oldLength = 0;
        RubyString originalString = null;

        switch (args.length) {
        case 2:
            originalString = args[1].convertToString();
            // must let original string know we're modifying, so shared buffers aren't damaged
            originalString.modify();
            buf = originalString.getByteList();
        case 1:
            if (!args[0].isNil()) {
                length = RubyNumeric.fix2int(args[0]);
                oldLength = length;

                if (length < 0) {
                    throw getRuntime().newArgumentError("negative length " + length + " given");
                }
                if (length > 0 && isEndOfString()) {
                    ptr.eof = true;
                    if (buf != null) buf.setRealSize(0);
                    return getRuntime().getNil();
                } else if (ptr.eof) {
                    if (buf != null) buf.setRealSize(0);
                    return getRuntime().getNil();
                }
                break;
            }
        case 0:
            oldLength = -1;
            length = ptr.internal.getByteList().length();

            if (length <= ptr.pos) {
                ptr.eof = true;
                if (buf == null) {
                    buf = new ByteList();
                } else {
                    buf.setRealSize(0);
                }

                return makeString(getRuntime(), buf);
            } else {
                length -= ptr.pos;
            }
            break;
        default:
            getRuntime().newArgumentError(args.length, 0);
        }

        if (buf == null) {
            int internalLength = ptr.internal.getByteList().length();

            if (internalLength > 0) {
                if (internalLength >= ptr.pos + length) {
                    buf = new ByteList(ptr.internal.getByteList(), (int) ptr.pos, length);
                } else {
                    int rest = (int) (ptr.internal.getByteList().length() - ptr.pos);

                    if (length > rest) length = rest;
                    buf = new ByteList(ptr.internal.getByteList(), (int) ptr.pos, length);
                }
            }
        } else {
            int rest = (int) (ptr.internal.getByteList().length() - ptr.pos);

            if (length > rest) length = rest;

            // Yow...this is still ugly
            byte[] target = buf.getUnsafeBytes();
            if (target.length > length) {
                System.arraycopy(ptr.internal.getByteList().getUnsafeBytes(), (int) ptr.pos, target, 0, length);
                buf.setBegin(0);
                buf.setRealSize(length);
            } else {
                target = new byte[length];
                System.arraycopy(ptr.internal.getByteList().getUnsafeBytes(), (int) ptr.pos, target, 0, length);
                buf.setBegin(0);
                buf.setRealSize(length);
                buf.setUnsafeBytes(target);
            }
        }

        if (buf == null) {
            if (!ptr.eof) buf = new ByteList();
            length = 0;
        } else {
            length = buf.length();
            ptr.pos += length;
        }

        if (oldLength < 0 || oldLength > length) ptr.eof = true;

        return originalString != null ? originalString : makeString(getRuntime(), buf);
    }

    @JRubyMethod(name="read_nonblock", compat = CompatVersion.RUBY1_9, optional = 2)
    @Override
    public IRubyObject read_nonblock(ThreadContext contet, IRubyObject[] args) {
        return sysreadCommon(args);
    }

    /**
     * readpartial(length, [buffer])
     *
     */
    @JRubyMethod(name ="readpartial", compat = CompatVersion.RUBY1_9, optional = 2)
    @Override
    public IRubyObject readpartial(ThreadContext context, IRubyObject[] args) {
        return sysreadCommon(args);
    }

    @JRubyMethod(name = {"readchar", "readbyte"})
    @Override
    public IRubyObject readchar() {
        IRubyObject c = getc();

        if (c.isNil()) throw getRuntime().newEOFError();

        return c;
    }

    @JRubyMethod(name = "readchar", compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject readchar19(ThreadContext context) {
        IRubyObject c = getc19(context);

        if (c.isNil()) throw getRuntime().newEOFError();

        return c;
    }

    @JRubyMethod(name = "readline", optional = 1, writes = FrameField.LASTLINE)
    @Override
    public IRubyObject readline(ThreadContext context, IRubyObject[] args) {
        IRubyObject line = gets(context, args);

        if (line.isNil()) throw getRuntime().newEOFError();

        return line;
    }

    @JRubyMethod(name = "readlines", optional = 1)
    public IRubyObject readlines(ThreadContext context, IRubyObject[] args) {
        checkReadable();
        
        if (context.is19) {
            if (args.length > 0 && !args[args.length - 1].isNil() && args[args.length - 1].checkStringType19().isNil() &&
                    RubyNumeric.num2long(args[args.length - 1]) == 0) {
                throw context.runtime.newArgumentError("invalid limit: 0 for each_line");
            }
        }

        List<IRubyObject> lns = new ArrayList<IRubyObject>();
        while (!(isEOF())) {
            IRubyObject line = context.is19 ? internalGets19(context, args) : internalGets18(context, args);
            if (line.isNil()) {
                break;
            }
            lns.add(line);
        }

        return getRuntime().newArray(lns);
    }

    @JRubyMethod(name = "reopen", required = 0, optional = 2)
    @Override
    public IRubyObject reopen(IRubyObject[] args) {
        checkFrozen();
        
        if (args.length == 1 && !(args[0] instanceof RubyString)) {
            return initialize_copy(args[0]);
        }

        // reset the state
        doRewind();
        ptr.closedRead = false;
        ptr.closedWrite = false;
        return initialize(args, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "rewind")
    @Override
    public IRubyObject rewind() {
        checkFrozen();
        
        doRewind();
        return RubyFixnum.zero(getRuntime());
    }

    private void doRewind() {
        this.ptr.pos = 0;
        // used in 1.8 mode only
        this.ptr.eof = false;
        this.ptr.lineno = 0;
    }
    
    @Override
    @Deprecated
    public IRubyObject seek(IRubyObject[] args) {
        return seek(getRuntime().getCurrentContext(), args);
    }

    @JRubyMethod(required = 1, optional = 1)
    public IRubyObject seek(ThreadContext context, IRubyObject[] args) {
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
                offset += ptr.internal.size();
                break;
            default:
                throw getRuntime().newErrnoEINVALError("invalid whence");
        }
        
        if (offset < 0) throw getRuntime().newErrnoEINVALError("invalid seek value");

        ptr.pos = offset;
        // used in 1.8 mode only
        ptr.eof = false;

        return RubyFixnum.zero(getRuntime());
    }

    @JRubyMethod(name = "string=", required = 1)
    @Override
    public IRubyObject set_string(IRubyObject arg) {
        return reopen(new IRubyObject[] { arg.convertToString() });
    }

    @JRubyMethod(name = "sync=", required = 1)
    @Override
    public IRubyObject set_sync(IRubyObject args) {
        checkFrozen();
        
        return args;
    }

    @JRubyMethod(name = "string")
    @Override
    public IRubyObject string() {
        if (ptr.internal == null) return getRuntime().getNil();

        return ptr.internal;
    }

    @JRubyMethod(name = "sync")
    @Override
    public IRubyObject sync() {
        return getRuntime().getTrue();
    }

    @JRubyMethod(name = "sysread", optional = 2)
    @Override
    public IRubyObject sysread(IRubyObject[] args) {
        return sysreadCommon(args);
    }
    

    private IRubyObject sysreadCommon(IRubyObject[] args) {
        IRubyObject obj = read(args);

        if (isEOF() && obj.isNil()) throw getRuntime().newEOFError();

        return obj;        
    }

    @JRubyMethod(name = "truncate", required = 1)
    @Override
    public IRubyObject truncate(IRubyObject arg) {
        checkWritable();

        int len = RubyFixnum.fix2int(arg);
        if (len < 0) {
            throw getRuntime().newErrnoEINVALError("negative legnth");
        }

        ptr.internal.modify();
        ByteList buf = ptr.internal.getByteList();
        if (len < buf.length()) {
            Arrays.fill(buf.getUnsafeBytes(), len, buf.length(), (byte) 0);
        }
        buf.length(len);
        return arg;
    }

    @JRubyMethod(name = "ungetc", required = 1)
    @Override
    public IRubyObject ungetc(IRubyObject arg) {
        checkReadable();

        int c = RubyNumeric.num2int(arg);
        if (ptr.pos == 0) return getRuntime().getNil();
        ungetbyteCommon(c);
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "ungetc", compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject ungetc19(ThreadContext context, IRubyObject arg) {
        return ungetbyte(context, arg);
    }

    private void ungetbyteCommon(int c) {
        ptr.internal.modify();
        ptr.pos--;
        
        ByteList bytes = ptr.internal.getByteList();

        if (isEndOfString()) bytes.length((int)ptr.pos + 1);

        if (ptr.pos == -1) {
            bytes.prepend((byte)c);
            ptr.pos = 0;
        } else {
            bytes.set((int) ptr.pos, c);
        }
    }

    private void ungetbyteCommon(RubyString ungetBytes) {
        ptr.internal.modify();
        ptr.pos--;
        
        ByteList bytes = ptr.internal.getByteList();

        if (isEndOfString()) bytes.length((int)ptr.pos + 1);

        if (ptr.pos == -1) {
            bytes.replace(0, 0, ungetBytes.getByteList());
            ptr.pos = 0;
        } else {
            bytes.replace(ptr.pos, 1, ungetBytes.getByteList());
        }
    }
    
    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject ungetbyte(ThreadContext context, IRubyObject arg) {
        checkReadable();

        if (!arg.isNil()) {
            int c;
            if (arg instanceof RubyFixnum) {
                ungetbyteCommon(RubyNumeric.fix2int(arg));
            } else {
                ungetbyteCommon(arg.convertToString());
            }
        }

        return context.nil;
    }

    @JRubyMethod(name = {"write", "write_nonblock", "syswrite"}, required = 1)
    @Override
    public IRubyObject write(ThreadContext context, IRubyObject arg) {
        return context.runtime.newFixnum(writeInternal(context, arg));
    }

    private int writeInternal(ThreadContext context, IRubyObject arg) {
        checkWritable();
        checkFrozen();

        RubyString val = arg.asString();
        ptr.internal.modify();

        if (ptr.modes.isAppendable()) {
            ptr.internal.getByteList().append(val.getByteList());
            ptr.pos = ptr.internal.getByteList().length();
        } else {
            int left = ptr.internal.getByteList().length()-(int)ptr.pos;
            ptr.internal.getByteList().replace((int)ptr.pos,Math.min(val.getByteList().length(),left),val.getByteList());
            ptr.pos += val.getByteList().length();
        }

        if (val.isTaint()) {
            ptr.internal.setTaint(true);
        }

        return val.getByteList().length();
    }
    
    @JRubyMethod(compat = RUBY1_9)
    @Override
    public IRubyObject set_encoding(ThreadContext context, IRubyObject enc) {
        if (enc.isNil()) {
            enc = context.runtime.getEncodingService().getDefaultExternal();
        }
        Encoding encoding = context.runtime.getEncodingService().getEncodingFromObject(enc);
        ptr.internal.setEncoding(encoding);
        return this;
    }
    
    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject set_encoding(ThreadContext context, IRubyObject enc, IRubyObject ignored) {
        return set_encoding(context, enc);
    }
    
    @JRubyMethod(compat = RUBY1_9)
    public IRubyObject set_encoding(ThreadContext context, IRubyObject enc, IRubyObject ignored1, IRubyObject ignored2) {
        return set_encoding(context, enc);
    }
    
    @JRubyMethod(compat = RUBY1_9)
    @Override
    public IRubyObject external_encoding(ThreadContext context) {
        return context.runtime.getEncodingService().convertEncodingToRubyEncoding(ptr.internal.getEncoding());
    }
    
    @JRubyMethod(compat = RUBY1_9)
    @Override
    public IRubyObject internal_encoding(ThreadContext context) {
        return context.nil;
    }
    
    @JRubyMethod(name = {"each_codepoint", "codepoints"}, compat = RUBY1_9)
    public IRubyObject each_codepoint(ThreadContext context, Block block) {
        return block.isGiven() ? eachCodepointInternal(context, block) : enumeratorize(context.runtime, this, "each_codepoint");
    }
    
    private IRubyObject eachCodepointInternal(ThreadContext context, Block block) {
        Ruby runtime = context.runtime;
        checkReadable();
        
        Encoding enc = ptr.internal.getEncoding();
        byte[] unsafeBytes = ptr.internal.getByteList().getUnsafeBytes();
        int begin = ptr.internal.getByteList().getBegin();
        for (;;) {
            if (ptr.pos >= ptr.internal.size()) {
                return this;
            }
            
            int c = StringSupport.codePoint(runtime, enc, unsafeBytes, begin + ptr.pos, unsafeBytes.length);
            int n = StringSupport.codeLength(runtime, enc, c);
            block.yield(context, runtime.newFixnum(c));
            ptr.pos += n;
        }
    }

    /* rb: check_modifiable */
    @Override
    public void checkFrozen() {
        super.checkFrozen();
        checkInitialized();
        if (ptr.internal.isFrozen()) throw getRuntime().newIOError("not modifiable string");
    }

    /* rb: readable */
    private void checkReadable() {
        checkFrozen();
        
        checkInitialized();
        if (ptr.closedRead || !ptr.modes.isReadable()) {
            throw getRuntime().newIOError("not opened for reading");
        }
    }

    /* rb: writable */
    private void checkWritable() {
        checkFrozen();
        checkInitialized();
        if (ptr.closedWrite || !ptr.modes.isWritable()) {
            throw getRuntime().newIOError("not opened for writing");
        }

        // Tainting here if we ever want it. (secure 4)
    }

    private void checkInitialized() {
        if (ptr.modes == null) {
            throw getRuntime().newIOError("uninitialized stream");
        }
    }

    private void checkFinalized() {
        if (ptr.internal == null) {
            throw getRuntime().newIOError("not opened");
        }
    }

    private void checkOpen() {
        if (ptr.closedRead && ptr.closedWrite) {
            throw getRuntime().newIOError("closed stream");
        }
    }

    private void setupModes() {
        ptr.closedWrite = false;
        ptr.closedRead = false;

        if (ptr.modes.isReadOnly()) ptr.closedWrite = true;
        if (!ptr.modes.isReadable()) ptr.closedRead = true;
    }
    
    @Override
    @Deprecated
    public IRubyObject read(IRubyObject[] args) {
        if (getRuntime().is1_9()) {
            return read19(args);
        }
        return read18(args);
    }
}
