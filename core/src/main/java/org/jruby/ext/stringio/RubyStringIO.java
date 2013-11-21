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
import org.jruby.CompatVersion;
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
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.Stream;

import java.util.Arrays;

import static org.jruby.CompatVersion.RUBY1_8;
import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.runtime.Visibility.PRIVATE;

@JRubyClass(name="StringIO")
@SuppressWarnings("deprecation")
public class RubyStringIO extends RubyObject implements EncodingCapable {
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
        RubyString string;
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

    public Encoding getEncoding() {
        return ptr.string.getEncoding();
    }

    public void setEncoding(Encoding e) {
        ptr.string.setEncoding(e);
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

    private ModeFlags initializeModes(Object modeArgument) {
        Ruby runtime = getRuntime();

        if (modeArgument == null) {
            return RubyIO.newModeFlags(runtime, "r+");
        } else if (modeArgument instanceof Long) {
            return RubyIO.newModeFlags(runtime, ((Long) modeArgument).longValue());
        } else {
            return RubyIO.newModeFlags(runtime, (String) modeArgument);
        }
    }

    @JRubyMethod(optional = 2, visibility = PRIVATE)
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        Ruby runtime = getRuntime();
        ModeFlags flags;
        RubyString str;
        
        switch (args.length) {
            case 0:
                str = RubyString.newEmptyString(runtime);
                flags = initializeModes("r+");
                break;
            case 1:
                str = args[0].convertToString();
                flags = initializeModes(str.isFrozen() ? "r" : "r+");
                
                break;
            case 2:
                str = args[0].convertToString();
                Object modeArgument;
                if (args[1] instanceof RubyFixnum) {
                    modeArgument = RubyFixnum.fix2long(args[1]);
                } else {
                    modeArgument = args[1].convertToString().toString();
                }
                
                flags = initializeModes(modeArgument);
                if (flags.isWritable() && str.isFrozen()) {
                    throw runtime.newErrnoEACCESError("Permission denied");
                }
                break;
            default:
                // not reached
                throw runtime.newArgumentError(args.length, 2);
        }

        ptr.string = str;
        ptr.modes = flags;
        
        setupModes();

        if (ptr.modes.isTruncate()) {
            ptr.string.modifyCheck();
            ptr.string.empty();
        }

        return this;
    }

    @JRubyMethod(visibility = PRIVATE)
    public IRubyObject initialize_copy(IRubyObject other) {
        RubyStringIO otherIO = (RubyStringIO) TypeConverter.convertToType(other, 
                getRuntime().getClass("StringIO"), "to_strio");

        if (this == otherIO) return this;

        ptr = otherIO.ptr;
        if (otherIO.isTaint()) setTaint(true);

        return this;
    }

    @JRubyMethod(name = "<<", required = 1)
    public IRubyObject append(ThreadContext context, IRubyObject arg) {
        // Claims conversion is done via 'to_s' in docs.
        callMethod(context, "write", arg);
        
        return this; 
    }

    @JRubyMethod
    public IRubyObject binmode() {
        return this;
    }

    @JRubyMethod
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
        ptr.string = null;
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject closed_p() {
        checkInitialized();
        return getRuntime().newBoolean(ptr.closedRead && ptr.closedWrite);
    }

    @JRubyMethod
    public IRubyObject close_read() {
        checkReadable();
        ptr.closedRead = true;

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_read?")
    public IRubyObject closed_read_p() {
        checkInitialized();
        return getRuntime().newBoolean(ptr.closedRead);
    }

    @JRubyMethod
    public IRubyObject close_write() {
        checkWritable();
        ptr.closedWrite = true;

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_write?")
    public IRubyObject closed_write_p() {
        checkInitialized();
        return getRuntime().newBoolean(ptr.closedWrite);
    }

    public IRubyObject eachInternal(ThreadContext context, IRubyObject[] args, Block block) {
        IRubyObject line = getsOnly(context, args);

        while (!line.isNil()) {
            block.yield(context, line);
            line = getsOnly(context, args);
        }

        return this;
    }

    @JRubyMethod(name = "each", optional = 1, writes = FrameField.LASTLINE)
    public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? eachInternal(context, args, block) : enumeratorize(context.runtime, this, "each", args);
    }

    @JRubyMethod(optional = 1, compat = RUBY1_8)
    public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? eachInternal(context, args, block) : enumeratorize(context.runtime, this, "each_line", args);
    }

    @JRubyMethod(optional = 1, compat = RUBY1_8)
    public IRubyObject lines(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? each(context, args, block) : enumeratorize(context.runtime, this, "lines", args);
    }

    public IRubyObject each_byte(ThreadContext context, Block block) {
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

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte19(ThreadContext context, Block block) {
        return block.isGiven() ? each_byte(context, block) : enumeratorize(context.runtime, this, "each_byte");
    }

    @JRubyMethod
    public IRubyObject bytes(ThreadContext context, Block block) {
        return block.isGiven() ? each_byte(context, block) : enumeratorize(context.runtime, this, "bytes");
    }

    @JRubyMethod
    public IRubyObject each_char(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal(context, block) : enumeratorize(context.runtime, this, "each_char");
    }

    @JRubyMethod
    public IRubyObject chars(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal(context, block) : enumeratorize(context.runtime, this, "chars");
    }

    public IRubyObject each_charInternal(final ThreadContext context, final Block block) {
        checkReadable();

        Ruby runtime = context.runtime;
        ByteList bytes = ptr.string.getByteList();
        int len = bytes.getRealSize();
        int end = bytes.getBegin() + len;
        Encoding enc = runtime.is1_9() ? bytes.getEncoding() : runtime.getKCode().getEncoding();
        while (ptr.pos < len) {
            int pos = (int) ptr.pos;
            int n = StringSupport.length(enc, bytes.getUnsafeBytes(), pos, end);

            if (len < pos + n) n = len - pos;

            ptr.pos += n;

            block.yield(context, ptr.string.makeShared19(runtime, pos, n));
        }

        return this;
    }

    @JRubyMethod(name = {"eof", "eof?"})
    public IRubyObject eof() {
        return getRuntime().newBoolean(isEOF());
    }

    private boolean isEOF() {
        return isEndOfString() || (getRuntime().is1_8() && ptr.eof);
    }
    
    private boolean isEndOfString() {
        return ptr.pos >= ptr.string.getByteList().length();
    }    

    @JRubyMethod(name = "fcntl")
    public IRubyObject fcntl() {
        throw getRuntime().newNotImplementedError("fcntl not implemented");
    }

    @JRubyMethod(name = "fileno")
    public IRubyObject fileno() {
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "flush")
    public IRubyObject flush() {
        return this;
    }

    @JRubyMethod(name = "fsync")
    public IRubyObject fsync() {
        return RubyFixnum.zero(getRuntime());
    }

    @JRubyMethod(name = {"getc", "getbyte"})
    public IRubyObject getc() {
        checkReadable();
        if (isEndOfString()) return getRuntime().getNil();

        return getRuntime().newFixnum(ptr.string.getByteList().get((int)ptr.pos++) & 0xFF);
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
                    int bytesAvailable = ptr.string.getByteList().getRealSize() - (int)ptr.pos;
                    int bytesToUse = (limit < 0 || limit >= bytesAvailable ? bytesAvailable : limit);
                    
                    // add additional bytes to fix trailing broken character
                    bytesToUse += StringSupport.bytesToFixBrokenTrailingCharacter(ptr.string.getByteList(), bytesToUse);
                    
                    ByteList buf = ptr.string.getByteList().makeShared(
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

            ByteList ss = ptr.string.getByteList();

            if (isParagraph) {
                swallowLF(ss);
                if (ptr.pos == ss.getRealSize()) {
                    return runtime.getNil();
                }
            }

            int sepIndex = ss.indexOf(sep, (int)ptr.pos);

            ByteList add;
            if (-1 == sepIndex) {
                sepIndex = ptr.string.getByteList().getRealSize();
                add = ByteList.EMPTY_BYTELIST;
            } else {
                add = sep;
            }

            int bytes = sepIndex - (int)ptr.pos;
            int bytesToUse = (limit < 0 || limit >= bytes ? bytes : limit);

            int bytesWithSep = sepIndex - (int)ptr.pos + add.getRealSize();
            int bytesToUseWithSep = (limit < 0 || limit >= bytesWithSep ? bytesWithSep : limit);

            ByteList line = new ByteList(bytesToUseWithSep);
            line.append(ptr.string.getByteList(), (int)ptr.pos, bytesToUse);
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
    public IRubyObject gets(ThreadContext context, IRubyObject[] args) {
        IRubyObject result = getsOnly(context, args);
        context.setLastLine(result);

        return result;
    }

    public IRubyObject getsOnly(ThreadContext context, IRubyObject[] args) {
        checkReadable();

        return internalGets18(context, args);
    }

    @JRubyMethod(name = {"tty?", "isatty"})
    public IRubyObject isatty() {
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = {"length", "size"})
    public IRubyObject length() {
        checkFinalized();
        return getRuntime().newFixnum(ptr.string.getByteList().length());
    }

    @JRubyMethod(name = "lineno")
    public IRubyObject lineno() {
        return getRuntime().newFixnum(ptr.lineno);
    }

    @JRubyMethod(name = "lineno=", required = 1)
    public IRubyObject set_lineno(IRubyObject arg) {
        ptr.lineno = RubyNumeric.fix2int(arg);

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "path", compat = CompatVersion.RUBY1_8)
    public IRubyObject path() {
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "pid")
    public IRubyObject pid() {
        return getRuntime().getNil();
    }

    @JRubyMethod(name = {"pos", "tell"})
    public IRubyObject pos() {
        return getRuntime().newFixnum(ptr.pos);
    }

    @JRubyMethod(name = "pos=", required = 1)
    public IRubyObject set_pos(IRubyObject arg) {
        ptr.pos = RubyNumeric.fix2int(arg);
        
        if (ptr.pos < 0) throw getRuntime().newErrnoEINVALError("Invalid argument");

        if (getRuntime().is1_8() && !isEndOfString()) ptr.eof = false;

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "print", rest = true)
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

    @JRubyMethod(name = "printf", required = 1, rest = true)
    public IRubyObject printf(ThreadContext context, IRubyObject[] args) {
        append(context, RubyKernel.sprintf(context, this, args));
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "putc", required = 1)
    public IRubyObject putc(IRubyObject obj) {
        checkWritable();
        byte c = RubyNumeric.num2chr(obj);
        checkFrozen();

        ptr.string.modify();
        ByteList bytes = ptr.string.getByteList();
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
                line = getRuntime().newString("nil");
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
        RubyString str = RubyString.newString(runtime, buf);
        str.setTaint(true);

        return str;        
    }
    
    private RubyString makeString(Ruby runtime, ByteList buf) {
        return makeString(runtime, buf, true);
    }

    @JRubyMethod(name = "read", optional = 2)
    public IRubyObject read(ThreadContext context, IRubyObject[] args) {
        return read18(args);
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
            length = ptr.string.getByteList().length();

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
            int internalLength = ptr.string.getByteList().length();

            if (internalLength > 0) {
                if (internalLength >= ptr.pos + length) {
                    buf = new ByteList(ptr.string.getByteList(), (int) ptr.pos, length);
                } else {
                    int rest = (int) (ptr.string.getByteList().length() - ptr.pos);

                    if (length > rest) length = rest;
                    buf = new ByteList(ptr.string.getByteList(), (int) ptr.pos, length);
                }
            }
        } else {
            int rest = (int) (ptr.string.getByteList().length() - ptr.pos);

            if (length > rest) length = rest;

            // Yow...this is still ugly
            byte[] target = buf.getUnsafeBytes();
            if (target.length > length) {
                System.arraycopy(ptr.string.getByteList().getUnsafeBytes(), (int) ptr.pos, target, 0, length);
                buf.setBegin(0);
                buf.setRealSize(length);
            } else {
                target = new byte[length];
                System.arraycopy(ptr.string.getByteList().getUnsafeBytes(), (int) ptr.pos, target, 0, length);
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

    @JRubyMethod(name = {"readchar", "readbyte"}, compat = RUBY1_8)
    public IRubyObject readchar() {
        IRubyObject c = getc();

        if (c.isNil()) throw getRuntime().newEOFError();

        return c;
    }

    @JRubyMethod(name = "readline", optional = 1, writes = FrameField.LASTLINE, compat = RUBY1_8)
    public IRubyObject readline18(ThreadContext context, IRubyObject[] args) {
        IRubyObject line = gets(context, args);

        if (line.isNil()) throw getRuntime().newEOFError();

        return line;
    }

    @JRubyMethod(name = "readlines", optional = 1, compat = RUBY1_8)
    public IRubyObject readlines(ThreadContext context, IRubyObject[] args) {
        checkReadable();

        RubyArray ary = context.runtime.newArray();
        while (!(isEOF())) {
            IRubyObject line = internalGets18(context, args);
            if (line.isNil()) {
                break;
            }
            ary.append(line);
        }

        return ary;
    }

    @JRubyMethod(name = "reopen", required = 0, optional = 2)
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
                offset += ptr.string.size();
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
    public IRubyObject set_string(IRubyObject arg) {
        checkFrozen();
        RubyString str = arg.convertToString();
        ptr.modes = ModeFlags.createModeFlags(str.isFrozen() ? ModeFlags.RDONLY : ModeFlags.RDWR);
        ptr.pos = 0;
        ptr.lineno = 0;
        return ptr.string = str;
    }

    @JRubyMethod(name = "sync=", required = 1)
    public IRubyObject set_sync(IRubyObject args) {
        checkFrozen();
        
        return args;
    }

    @JRubyMethod(name = "string")
    public IRubyObject string() {
        if (ptr.string == null) return getRuntime().getNil();

        return ptr.string;
    }

    @JRubyMethod(name = "sync")
    public IRubyObject sync() {
        return getRuntime().getTrue();
    }

    @JRubyMethod(name = "sysread", optional = 2, compat = RUBY1_8)
    public IRubyObject sysread18(IRubyObject[] args) {
        return sysreadCommon(args);
    }
    
    // only here for the fake-out class in org.jruby
    public IRubyObject sysread(IRubyObject[] args) {
        return sysread18(args);
    }

    private IRubyObject sysreadCommon(IRubyObject[] args) {
        IRubyObject obj = read(args);

        if (isEOF() && obj.isNil()) throw getRuntime().newEOFError();

        return obj;        
    }

    @JRubyMethod(name = "truncate", required = 1)
    public IRubyObject truncate(IRubyObject arg) {
        checkWritable();

        int len = RubyFixnum.fix2int(arg);
        if (len < 0) {
            throw getRuntime().newErrnoEINVALError("negative legnth");
        }

        ptr.string.modify();
        ByteList buf = ptr.string.getByteList();
        if (len < buf.length()) {
            Arrays.fill(buf.getUnsafeBytes(), len, buf.length(), (byte) 0);
        }
        buf.length(len);
        return arg;
    }

    @JRubyMethod(name = "ungetc", required = 1, compat = RUBY1_8)
    public IRubyObject ungetc(IRubyObject arg) {
        checkReadable();

        int c = RubyNumeric.num2int(arg);
        if (ptr.pos == 0) return getRuntime().getNil();
        ungetbyteCommon(c);
        return getRuntime().getNil();
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

    @JRubyMethod(name = {"write", "write_nonblock", "syswrite"}, required = 1)
    public IRubyObject write(ThreadContext context, IRubyObject arg) {
        return context.runtime.newFixnum(writeInternal18(context, arg));
    }

    private int writeInternal18(ThreadContext context, IRubyObject arg) {
        checkWritable();

        RubyString val = arg.asString();
        ptr.string.modify();

        if (ptr.modes.isAppendable()) {
            ptr.string.getByteList().append(val.getByteList());
            ptr.pos = ptr.string.getByteList().length();
        } else {
            int left = ptr.string.getByteList().length()-(int)ptr.pos;
            ptr.string.getByteList().replace((int)ptr.pos,Math.min(val.getByteList().length(),left),val.getByteList());
            ptr.pos += val.getByteList().length();
        }

        if (val.isTaint()) {
            ptr.string.setTaint(true);
        }

        return val.getByteList().length();
    }

    /* rb: check_modifiable */
    public void checkFrozen() {
        super.checkFrozen();
        checkInitialized();
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
        if (ptr.string.isFrozen()) throw getRuntime().newIOError("not modifiable string");
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
        if (ptr.string == null) {
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

    
    @Deprecated
    public IRubyObject read(IRubyObject[] args) {
        return read18(args);
    }
}
