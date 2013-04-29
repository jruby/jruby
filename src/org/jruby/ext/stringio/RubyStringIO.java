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

@JRubyClass(name="StringIO")
@SuppressWarnings("deprecation")
public class RubyStringIO extends org.jruby.RubyStringIO {
    static class StringIOData {
        long pos = 0L;
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
    StringIOData data;

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
        data = new StringIOData();
    }

    private void initializeModes(Object modeArgument) {
        Ruby runtime = getRuntime();

        if (modeArgument == null) {
            data.modes = RubyIO.newModeFlags(runtime, "r+");
        } else if (modeArgument instanceof Long) {
            data.modes = RubyIO.newModeFlags(runtime, ((Long) modeArgument).longValue());
        } else {
            data.modes = RubyIO.newModeFlags(runtime, (String) modeArgument);
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
                data.internal = runtime.is1_9() ? RubyString.newEmptyString(runtime, runtime.getDefaultExternalEncoding()) : RubyString.newEmptyString(getRuntime());
                modeArgument = "r+";
                break;
            case 1:
                data.internal = args[0].convertToString();
                modeArgument = data.internal.isFrozen() ? "r" : "r+";
                break;
            case 2:
                data.internal = args[0].convertToString();
                if (args[1] instanceof RubyFixnum) {
                    modeArgument = RubyFixnum.fix2long(args[1]);
                } else {
                    modeArgument = args[1].convertToString().toString();
                }
                break;
        }

        initializeModes(modeArgument);

        if (data.modes.isWritable() && data.internal.isFrozen()) {
            throw getRuntime().newErrnoEACCESError("Permission denied");
        }

        if (data.modes.isTruncate()) {
            data.internal.modifyCheck();
            data.internal.empty();
        }

        return this;
    }

    @JRubyMethod(visibility = PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject other) {
        RubyStringIO otherIO = (RubyStringIO) TypeConverter.convertToType(other, 
                getRuntime().getClass("StringIO"), "to_strio");

        if (this == otherIO) return this;

        data = otherIO.data;
        if (otherIO.isTaint()) setTaint(true);

        return this;
    }

    @JRubyMethod(name = "<<", required = 1)
    @Override
    public IRubyObject append(ThreadContext context, IRubyObject arg) {
        writeInternal(context, arg);
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

        data.closedRead = true;
        data.closedWrite = true;

        return getRuntime().getNil();
    }

    private void doFinalize() {
        data.closedRead = true;
        data.closedWrite = true;
        data.internal = null;
    }

    @JRubyMethod(name = "closed?")
    @Override
    public IRubyObject closed_p() {
        checkInitialized();
        return getRuntime().newBoolean(data.closedRead && data.closedWrite);
    }

    @JRubyMethod
    @Override
    public IRubyObject close_read() {
        checkReadable();
        data.closedRead = true;

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_read?")
    @Override
    public IRubyObject closed_read_p() {
        checkInitialized();
        return getRuntime().newBoolean(data.closedRead);
    }

    @JRubyMethod
    @Override
    public IRubyObject close_write() {
        checkWritable();
        data.closedWrite = true;

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_write?")
    @Override
    public IRubyObject closed_write_p() {
        checkInitialized();
        return getRuntime().newBoolean(data.closedWrite);
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

    @JRubyMethod(optional = 1)
    @Override
    public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? eachInternal(context, args, block) : enumeratorize(context.runtime, this, "each_line", args);
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
        ByteList bytes = data.internal.getByteList();

        // Check the length every iteration, since
        // the block can modify this string.
        while (data.pos < bytes.length()) {
            block.yield(context, runtime.newFixnum(bytes.get((int) data.pos++) & 0xFF));
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
        ByteList bytes = data.internal.getByteList();
        int len = bytes.getRealSize();
        int end = bytes.getBegin() + len;
        Encoding enc = runtime.is1_9() ? bytes.getEncoding() : runtime.getKCode().getEncoding();        
        while (data.pos < len) {
            int pos = (int) data.pos;
            int n = StringSupport.length(enc, bytes.getUnsafeBytes(), pos, end);

            if(len < pos + n) n = len - pos;

            data.pos += n;

            block.yield(context, data.internal.makeShared19(runtime, pos, n));
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
        return isEndOfString() || data.eof;
    }
    
    private boolean isEndOfString() {
        return data.pos >= data.internal.getByteList().length();
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

        return getRuntime().newFixnum(data.internal.getByteList().get((int)data.pos++) & 0xFF);
    }

    @JRubyMethod(name = "getc", compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject getc19(ThreadContext context) {
        checkReadable();
        if (isEndOfString()) return context.runtime.getNil();

        return context.runtime.newString("" + (char) (data.internal.getByteList().get((int) data.pos++) & 0xFF));
    }

    private IRubyObject internalGets(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.runtime;

        if (!isEndOfString() && !data.eof) {
            boolean isParagraph = false;
            boolean is19 = runtime.is1_9();
            ByteList sep = ((RubyString)runtime.getGlobalVariables().get("$/")).getByteList();
            IRubyObject sepArg;
            int limit = -1;

            if (is19) {
                IRubyObject limitArg = (args.length > 1 ? args[1] :
                                        (args.length > 0 && args[0] instanceof RubyFixnum ? args[0] :
                                         null));
                if (limitArg != null) {
                    limit = RubyNumeric.fix2int(limitArg);
                }

                sepArg = (args.length > 0 && !(args[0] instanceof RubyFixnum) ? args[0] : null);
            } else {
                sepArg = (args.length > 0 ? args[0] : null);
            }

            if (sepArg != null) {
                if (sepArg.isNil()) {
                    int bytesAvailable = data.internal.getByteList().getRealSize() - (int)data.pos;
                    int bytesToUse = (limit < 0 || limit >= bytesAvailable ? bytesAvailable : limit);
                    ByteList buf = data.internal.getByteList().makeShared(
                        (int)data.pos, bytesToUse);
                    data.pos += buf.getRealSize();
                    return makeString(runtime, buf);
                }

                sep = sepArg.convertToString().getByteList();
                if (sep.getRealSize() == 0) {
                    isParagraph = true;
                    sep = Stream.PARAGRAPH_SEPARATOR;
                }
            }

            ByteList ss = data.internal.getByteList();

            if (isParagraph) {
                swallowLF(ss);
                if (data.pos == ss.getRealSize()) {
                    return runtime.getNil();
                }
            }

            int ix = ss.indexOf(sep, (int)data.pos);

            ByteList add;
            if (-1 == ix) {
                ix = data.internal.getByteList().getRealSize();
                add = ByteList.EMPTY_BYTELIST;
            } else {
                add = sep;
            }

            int bytes = ix - (int)data.pos;
            int bytesToUse = (limit < 0 || limit >= bytes ? bytes : limit);

            int bytesWithSep = ix - (int)data.pos + add.getRealSize();
            int bytesToUseWithSep = (limit < 0 || limit >= bytesWithSep ? bytesWithSep : limit);

            ByteList line = new ByteList(bytesToUseWithSep);
            if (is19) line.setEncoding(data.internal.getByteList().getEncoding());
            line.append(data.internal.getByteList(), (int)data.pos, bytesToUse);
            data.pos += bytesToUse;

            int sepBytesToUse = bytesToUseWithSep - bytesToUse;
            line.append(add, 0, sepBytesToUse);
            data.pos += sepBytesToUse;

            if (sepBytesToUse >= add.getRealSize()) {
                data.lineno++;
            }

            return makeString(runtime, line);
        }
        return runtime.getNil();
    }

    private void swallowLF(ByteList list) {
        while (data.pos < list.getRealSize()) {
            if (list.get((int)data.pos) == '\n') {
                data.pos++;
            } else {
                break;
            }
        }
    }

    @JRubyMethod(name = "gets", optional = 1, writes = FrameField.LASTLINE, compat = CompatVersion.RUBY1_8)
    @Override
    public IRubyObject gets(ThreadContext context, IRubyObject[] args) {
        IRubyObject result = getsOnly(context, args);
        context.getCurrentScope().setLastLine(result);

        return result;
    }

    @JRubyMethod(name = "gets", optional = 2, writes = FrameField.LASTLINE, compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject gets19(ThreadContext context, IRubyObject[] args) {
        IRubyObject result = getsOnly(context, args);
        context.getCurrentScope().setLastLine(result);

        return result;
    }

    @Override
    public IRubyObject getsOnly(ThreadContext context, IRubyObject[] args) {
        checkReadable();

        return internalGets(context, args);
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
        return getRuntime().newFixnum(data.internal.getByteList().length());
    }

    @JRubyMethod(name = "lineno")
    @Override
    public IRubyObject lineno() {
        return getRuntime().newFixnum(data.lineno);
    }

    @JRubyMethod(name = "lineno=", required = 1)
    @Override
    public IRubyObject set_lineno(IRubyObject arg) {
        data.lineno = RubyNumeric.fix2int(arg);

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
        return getRuntime().newFixnum(data.pos);
    }

    @JRubyMethod(name = "pos=", required = 1)
    @Override
    public IRubyObject set_pos(IRubyObject arg) {
        data.pos = RubyNumeric.fix2int(arg);
        
        if (data.pos < 0) throw getRuntime().newErrnoEINVALError("Invalid argument");

        if (!isEndOfString()) data.eof = false;

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

        data.internal.modify();
        ByteList bytes = data.internal.getByteList();
        if (data.modes.isAppendable()) {
            data.pos = bytes.length();
            bytes.append(c);
        } else {
            if (isEndOfString()) bytes.length((int)data.pos + 1);

            bytes.set((int) data.pos, c);
            data.pos++;
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
            RubyString line;

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
    private RubyString makeString(Ruby runtime, ByteList buf) {
        if (runtime.is1_9()) buf.setEncoding(data.internal.getEncoding());

        RubyString str = RubyString.newString(runtime, buf);
        str.setTaint(true);

        return str;        
    }

    @SuppressWarnings("fallthrough")
    @JRubyMethod(name = "read", optional = 2)
    @Override
    public IRubyObject read(IRubyObject[] args) {
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
                    data.eof = true;
                    if (buf != null) buf.setRealSize(0);
                    return getRuntime().getNil();
                } else if (data.eof) {
                    if (buf != null) buf.setRealSize(0);
                    return getRuntime().getNil();
                }
                break;
            }
        case 0:
            oldLength = -1;
            length = data.internal.getByteList().length();

            if (length <= data.pos) {
                data.eof = true;
                if (buf == null) {
                    buf = new ByteList();
                } else {
                    buf.setRealSize(0);
                }

                return makeString(getRuntime(), buf);
            } else {
                length -= data.pos;
            }
            break;
        default:
            getRuntime().newArgumentError(args.length, 0);
        }

        if (buf == null) {
            int internalLength = data.internal.getByteList().length();

            if (internalLength > 0) {
                if (internalLength >= data.pos + length) {
                    buf = new ByteList(data.internal.getByteList(), (int) data.pos, length);
                } else {
                    int rest = (int) (data.internal.getByteList().length() - data.pos);

                    if (length > rest) length = rest;
                    buf = new ByteList(data.internal.getByteList(), (int) data.pos, length);
                }
            }
        } else {
            int rest = (int) (data.internal.getByteList().length() - data.pos);

            if (length > rest) length = rest;

            // Yow...this is still ugly
            byte[] target = buf.getUnsafeBytes();
            if (target.length > length) {
                System.arraycopy(data.internal.getByteList().getUnsafeBytes(), (int) data.pos, target, 0, length);
                buf.setBegin(0);
                buf.setRealSize(length);
            } else {
                target = new byte[length];
                System.arraycopy(data.internal.getByteList().getUnsafeBytes(), (int) data.pos, target, 0, length);
                buf.setBegin(0);
                buf.setRealSize(length);
                buf.setUnsafeBytes(target);
            }
        }

        if (buf == null) {
            if (!data.eof) buf = new ByteList();
            length = 0;
        } else {
            length = buf.length();
            data.pos += length;
        }

        if (oldLength < 0 || oldLength > length) data.eof = true;

        return originalString != null ? originalString : makeString(getRuntime(), buf);
    }

    @JRubyMethod(name="read_nonblock", compat = CompatVersion.RUBY1_9, required = 1, optional = 1)
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
    public IRubyObject readlines(ThreadContext context, IRubyObject[] arg) {
        checkReadable();

        List<IRubyObject> lns = new ArrayList<IRubyObject>();
        while (!(isEOF())) {
            IRubyObject line = internalGets(context, arg);
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
        if (args.length == 1 && !(args[0] instanceof RubyString)) {
            return initialize_copy(args[0]);
        }

        // reset the state
        doRewind();
        data.closedRead = false;
        data.closedWrite = false;
        return initialize(args, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "rewind")
    @Override
    public IRubyObject rewind() {
        doRewind();
        return RubyFixnum.zero(getRuntime());
    }

    private void doRewind() {
        this.data.pos = 0L;
        this.data.eof = false;
        this.data.lineno = 0;
    }

    @JRubyMethod(required = 1, optional = 1)
    @Override
    public IRubyObject seek(IRubyObject[] args) {
        checkOpen();
        checkFinalized();
        long amount = RubyNumeric.num2long(args[0]);
        int whence = Stream.SEEK_SET;
        long newPosition = data.pos;

        if (args.length > 1 && !args[0].isNil()) whence = RubyNumeric.fix2int(args[1]);

        if (whence == Stream.SEEK_CUR) {
            newPosition += amount;
        } else if (whence == Stream.SEEK_END) {
            newPosition = data.internal.getByteList().length() + amount;
        } else if (whence == Stream.SEEK_SET) {
            newPosition = amount;
        } else {
            throw getRuntime().newErrnoEINVALError("invalid whence");
        }

        if (newPosition < 0) throw getRuntime().newErrnoEINVALError("invalid seek value");

        data.pos = newPosition;
        data.eof = false;

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
        return args;
    }

    @JRubyMethod(name = "string")
    @Override
    public IRubyObject string() {
        if (data.internal == null) return getRuntime().getNil();

        return data.internal;
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

        data.internal.modify();
        ByteList buf = data.internal.getByteList();
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
        if (data.pos == 0) return getRuntime().getNil();
        ungetcCommon(c);
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "ungetc", compat = CompatVersion.RUBY1_9)
    @Override
    public IRubyObject ungetc19(ThreadContext context, IRubyObject arg) {
        checkReadable();

        if (!arg.isNil()) {
            int c;
            if (arg instanceof RubyFixnum) {
                c = RubyNumeric.fix2int(arg);
            } else {
                RubyString str = arg.convertToString();
                c = str.getEncoding().mbcToCode(str.getBytes(), 0, 1);
            }

            ungetcCommon(c);
        }

        return getRuntime().getNil();
    }

    private void ungetcCommon(int c) {
        data.internal.modify();
        data.pos--;

        ByteList bytes = data.internal.getByteList();

        if (isEndOfString()) bytes.length((int)data.pos + 1);

        bytes.set((int) data.pos, c);
    }

    @JRubyMethod(name = {"write", "syswrite"}, required = 1)
    @Override
    public IRubyObject write(ThreadContext context, IRubyObject arg) {
        return context.runtime.newFixnum(writeInternal(context, arg));
    }

    private int writeInternal(ThreadContext context, IRubyObject arg) {
        checkWritable();
        checkFrozen();

        RubyString val = arg.asString();
        data.internal.modify();

        if (data.modes.isAppendable()) {
            data.internal.getByteList().append(val.getByteList());
            data.pos = data.internal.getByteList().length();
        } else {
            int left = data.internal.getByteList().length()-(int)data.pos;
            data.internal.getByteList().replace((int)data.pos,Math.min(val.getByteList().length(),left),val.getByteList());
            data.pos += val.getByteList().length();
        }

        if (val.isTaint()) {
            data.internal.setTaint(true);
        }

        return val.getByteList().length();
    }
    
    @JRubyMethod(compat = RUBY1_9)
    @Override
    public IRubyObject set_encoding(ThreadContext context, IRubyObject enc) {
        Encoding encoding = context.runtime.getEncodingService().getEncodingFromObject(enc);
        data.internal.setEncoding(encoding);
        return this;
    }
    
    @JRubyMethod(compat = RUBY1_9)
    @Override
    public IRubyObject external_encoding(ThreadContext context) {
        return context.runtime.getEncodingService().convertEncodingToRubyEncoding(data.internal.getEncoding());
    }
    
    @JRubyMethod(compat = RUBY1_9)
    @Override
    public IRubyObject internal_encoding(ThreadContext context) {
        return context.nil;
    }

    /* rb: check_modifiable */
    @Override
    public void checkFrozen() {
        checkInitialized();
        if (data.internal.isFrozen()) throw getRuntime().newIOError("not modifiable string");
    }

    /* rb: readable */
    private void checkReadable() {
        checkInitialized();
        if (data.closedRead || !data.modes.isReadable()) {
            throw getRuntime().newIOError("not opened for reading");
        }
    }

    /* rb: writable */
    private void checkWritable() {
        checkInitialized();
        if (data.closedWrite || !data.modes.isWritable()) {
            throw getRuntime().newIOError("not opened for writing");
        }

        // Tainting here if we ever want it. (secure 4)
    }

    private void checkInitialized() {
        if (data.modes == null) {
            throw getRuntime().newIOError("uninitialized stream");
        }
    }

    private void checkFinalized() {
        if (data.internal == null) {
            throw getRuntime().newIOError("not opened");
        }
    }

    private void checkOpen() {
        if (data.closedRead && data.closedWrite) {
            throw getRuntime().newIOError("closed stream");
        }
    }

    private void setupModes() {
        data.closedWrite = false;
        data.closedRead = false;

        if (data.modes.isReadOnly()) data.closedWrite = true;
        if (!data.modes.isReadable()) data.closedRead = true;
    }
}
