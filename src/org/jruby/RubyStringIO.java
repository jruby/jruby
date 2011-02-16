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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.java.addons.IOJavaAddons;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import static org.jruby.runtime.Visibility.*;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.Stream;

import static org.jruby.RubyEnumerator.enumeratorize;

@JRubyClass(name="StringIO")
public class RubyStringIO extends RubyObject {
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
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyStringIO(runtime, klass);
        }
    };

    public static RubyClass createStringIOClass(final Ruby runtime) {
        RubyClass stringIOClass = runtime.defineClass(
                "StringIO", runtime.fastGetClass("Data"), STRINGIO_ALLOCATOR);

        stringIOClass.defineAnnotatedMethods(RubyStringIO.class);
        stringIOClass.includeModule(runtime.getEnumerable());

        if (runtime.getObject().isConstantDefined("Java")) {
            stringIOClass.defineAnnotatedMethods(IOJavaAddons.AnyIO.class);
        }

        return stringIOClass;
    }

    @JRubyMethod(optional = 2, meta = true)
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
        try {
            if (modeArgument == null) {
                data.modes = new ModeFlags(RubyIO.getIOModesIntFromString(getRuntime(), "r+"));
            } else if (modeArgument instanceof Long) {
                data.modes = new ModeFlags(((Long)modeArgument).longValue());
            } else {
                data.modes = new ModeFlags(RubyIO.getIOModesIntFromString(getRuntime(), (String) modeArgument));
            }
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
        setupModes();
    }

    @JRubyMethod(optional = 2, visibility = PRIVATE)
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        Object modeArgument = null;
        switch (args.length) {
            case 0:
                data.internal = RubyString.newEmptyString(getRuntime());
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

        RubyStringIO otherIO = (RubyStringIO) TypeConverter.convertToType(
                other, getRuntime().fastGetClass("StringIO"), "to_strio");

        if (this == otherIO) {
            return this;
        }

        data = otherIO.data;
        if (otherIO.isTaint()) {
            setTaint(true);
        }

        return this;
    }

    @JRubyMethod(name = "<<", required = 1)
    public IRubyObject append(ThreadContext context, IRubyObject arg) {
        writeInternal(context, arg);
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
    public IRubyObject closed_p() {
        checkInitialized();
        return getRuntime().newBoolean(data.closedRead && data.closedWrite);
    }

    @JRubyMethod
    public IRubyObject close_read() {
        checkReadable();
        data.closedRead = true;

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_read?")
    public IRubyObject closed_read_p() {
        checkInitialized();
        return getRuntime().newBoolean(data.closedRead);
    }

    @JRubyMethod
    public IRubyObject close_write() {
        checkWritable();
        data.closedWrite = true;

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_write?")
    public IRubyObject closed_write_p() {
        checkInitialized();
        return getRuntime().newBoolean(data.closedWrite);
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
        return block.isGiven() ? eachInternal(context, args, block) : enumeratorize(context.getRuntime(), this, "each", args);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? eachInternal(context, args, block) : enumeratorize(context.getRuntime(), this, "each_line", args);
    }

    @JRubyMethod(optional = 1)
    public IRubyObject lines(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? each(context, args, block) : enumeratorize(context.getRuntime(), this, "lines", args);
    }

    public IRubyObject each_byte(ThreadContext context, Block block) {
        checkReadable();
        Ruby runtime = context.getRuntime();
        ByteList bytes = data.internal.getByteList();

        // Check the length every iteration, since
        // the block can modify this string.
        while (data.pos < bytes.length()) {
            block.yield(context, runtime.newFixnum(bytes.get((int) data.pos++) & 0xFF));
        }
        return this;
    }

    @JRubyMethod(name = "each_byte")
    public IRubyObject each_byte19(ThreadContext context, Block block) {
        return block.isGiven() ? each_byte(context, block) : enumeratorize(context.getRuntime(), this, "each_byte");
    }

    @JRubyMethod
    public IRubyObject bytes(ThreadContext context, Block block) {
        return block.isGiven() ? each_byte(context, block) : enumeratorize(context.getRuntime(), this, "bytes");
    }

    public IRubyObject each_charInternal(final ThreadContext context, final Block block) {
        checkReadable();

        Ruby runtime = context.getRuntime();
        ByteList bytes = data.internal.getByteList();
        int len = bytes.getRealSize();
        while (data.pos < len) {
            int pos = (int)this.data.pos;
            byte c = bytes.getUnsafeBytes()[bytes.getBegin() + pos];
            int n = runtime.getKCode().getEncoding().length(c);
            if(len < pos + n) {
                n = len - pos;
            }
            this.data.pos += n;
            block.yield(context, data.internal.substr19(runtime, pos, n));
        }

        return this;
    }

    @JRubyMethod
    public IRubyObject each_char(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal(context, block) : enumeratorize(context.getRuntime(), this, "each_char");
    }

    @JRubyMethod
    public IRubyObject chars(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_charInternal(context, block) : enumeratorize(context.getRuntime(), this, "chars");
    }

    @JRubyMethod(name = {"eof", "eof?"})
    public IRubyObject eof() {
        return getRuntime().newBoolean(isEOF());
    }

    private boolean isEOF() {
        return (data.pos >= data.internal.getByteList().length()) || data.eof;
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
        if (data.pos >= data.internal.getByteList().length()) {
            return getRuntime().getNil();
        }
        return getRuntime().newFixnum(data.internal.getByteList().get((int)data.pos++) & 0xFF);
    }

    @JRubyMethod(name = "getc", compat = CompatVersion.RUBY1_9)
    public IRubyObject getc19(ThreadContext context) {
        checkReadable();
        if (data.pos >= data.internal.getByteList().length()) {
            return context.getRuntime().getNil();
        }
        return context.getRuntime().newString("" + (char)(data.internal.getByteList().get((int)data.pos++) & 0xFF));
    }

    private IRubyObject internalGets(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();

        if (data.pos < data.internal.getByteList().getRealSize() && !data.eof) {
            boolean isParagraph = false;

            ByteList sep;
            if (args.length > 0) {
                if (args[0].isNil()) {
                    ByteList buf = data.internal.getByteList().makeShared(
                            (int)data.pos, data.internal.getByteList().getRealSize() - (int)data.pos);
                    data.pos += buf.getRealSize();
                    return RubyString.newString(runtime, buf);
                }
                sep = args[0].convertToString().getByteList();
                if (sep.getRealSize() == 0) {
                    isParagraph = true;
                    sep = Stream.PARAGRAPH_SEPARATOR;
                }
            } else {
                sep = ((RubyString)runtime.getGlobalVariables().get("$/")).getByteList();
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

            ByteList line = new ByteList(ix - (int)data.pos + add.length());
            line.append(data.internal.getByteList(), (int)data.pos, ix - (int)data.pos);
            line.append(add);
            data.pos = ix + add.getRealSize();
            data.lineno++;

            return RubyString.newString(runtime,line);
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

    @JRubyMethod(name = "gets", optional = 1, writes = FrameField.LASTLINE)
    public IRubyObject gets(ThreadContext context, IRubyObject[] args) {
        IRubyObject result = getsOnly(context, args);
        context.getCurrentScope().setLastLine(result);

        return result;
    }

    public IRubyObject getsOnly(ThreadContext context, IRubyObject[] args) {
        checkReadable();

        return internalGets(context, args);
    }

    @JRubyMethod(name = {"tty?", "isatty"})
    public IRubyObject isatty() {
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = {"length", "size"})
    public IRubyObject length() {
        checkFinalized();
        return getRuntime().newFixnum(data.internal.getByteList().length());
    }

    @JRubyMethod(name = "lineno")
    public IRubyObject lineno() {
        return getRuntime().newFixnum(data.lineno);
    }

    @JRubyMethod(name = "lineno=", required = 1)
    public IRubyObject set_lineno(IRubyObject arg) {
        data.lineno = RubyNumeric.fix2int(arg);

        return getRuntime().getNil();
    }

    @JRubyMethod(name = "path")
    public IRubyObject path() {
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "path", compat = CompatVersion.RUBY1_9)
    public IRubyObject path19(ThreadContext context) {
        throw context.getRuntime().newNoMethodError("", "path", null);
    }

    @JRubyMethod(name = "pid")
    public IRubyObject pid() {
        return getRuntime().getNil();
    }

    @JRubyMethod(name = {"pos", "tell"})
    public IRubyObject pos() {
        return getRuntime().newFixnum(data.pos);
    }

    @JRubyMethod(name = "pos=", required = 1)
    public IRubyObject set_pos(IRubyObject arg) {
        data.pos = RubyNumeric.fix2int(arg);
        if (data.pos < 0) {
            throw getRuntime().newErrnoEINVALError("Invalid argument");
        }
        if (data.pos < data.internal.getByteList().length()) {
            data.eof = false;
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "print", rest = true)
    public IRubyObject print(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        if (args.length != 0) {
            for (int i=0,j=args.length;i<j;i++) {
                append(context, args[i]);
            }
        } else {
            IRubyObject arg = runtime.getGlobalVariables().get("$_");
            append(context, arg.isNil() ? runtime.newString("nil") : arg);
        }
        IRubyObject sep = runtime.getGlobalVariables().get("$\\");
        if (!sep.isNil()) append(context, sep);

        return runtime.getNil();
    }

    @JRubyMethod(name = "print", rest = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject print19(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
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
    public IRubyObject printf(ThreadContext context, IRubyObject[] args) {
        append(context, RubyKernel.sprintf(context, this, args));
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "putc", required = 1)
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
            if (data.pos >= bytes.length()) {
                bytes.length((int)data.pos + 1);
            }

            bytes.set((int) data.pos, c);
            data.pos++;
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

    @SuppressWarnings("fallthrough")
    @JRubyMethod(name = "read", optional = 2)
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
                if (length > 0 && data.pos >= data.internal.getByteList().length()) {
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

                return getRuntime().newString(buf);
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

        return originalString != null ? originalString : getRuntime().newString(buf);
    }

    /**
     * readpartial(length, [buffer])
     *  
     * @param context
     * @param args
     * @return
     */
    @JRubyMethod(name ="readpartial", compat = CompatVersion.RUBY1_9, required = 1, optional = 1)
    public IRubyObject readpartial(ThreadContext context, IRubyObject[] args) {
        IRubyObject result = this.read(args);

        if (data.eof && result.isNil()) throw context.getRuntime().newEOFError();

        return result;
    }

    @JRubyMethod(name = {"readchar", "readbyte"})
    public IRubyObject readchar() {
        IRubyObject c = getc();

        if (c.isNil()) throw getRuntime().newEOFError();

        return c;
    }

    @JRubyMethod(name = "readchar", compat = CompatVersion.RUBY1_9)
    public IRubyObject readchar19(ThreadContext context) {
        IRubyObject c = getc19(context);

        if (c.isNil()) throw getRuntime().newEOFError();

        return c;
    }

    @JRubyMethod(name = "readline", optional = 1, writes = FrameField.LASTLINE)
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
    public IRubyObject set_string(IRubyObject arg) {
        return reopen(new IRubyObject[] { arg.convertToString() });
    }

    @JRubyMethod(name = "sync=", required = 1)
    public IRubyObject set_sync(IRubyObject args) {
        return args;
    }

    @JRubyMethod(name = "string")
    public IRubyObject string() {
        if (data.internal == null) {
            return getRuntime().getNil();
        } else {
            return data.internal;
        }
    }

    @JRubyMethod(name = "sync")
    public IRubyObject sync() {
        return getRuntime().getTrue();
    }

    @JRubyMethod(name = "sysread", optional = 2)
    public IRubyObject sysread(IRubyObject[] args) {
        IRubyObject obj = read(args);

        if (isEOF()) {
            if (obj.isNil()) {
                throw getRuntime().newEOFError();
            }
        }

        return obj;
    }

    @JRubyMethod(name = "truncate", required = 1)
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
    public IRubyObject ungetc(IRubyObject arg) {
        checkReadable();

        int c = RubyNumeric.num2int(arg);
        if (data.pos == 0) return getRuntime().getNil();
        ungetcCommon(c);
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "ungetc", compat = CompatVersion.RUBY1_9)
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

        if (bytes.length() <= data.pos) {
            bytes.length((int)data.pos + 1);
        }

        bytes.set((int) data.pos, c);
    }

    @JRubyMethod(name = {"write", "syswrite"}, required = 1)
    public IRubyObject write(ThreadContext context, IRubyObject arg) {
        return context.getRuntime().newFixnum(writeInternal(context, arg));
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

    /* rb: check_modifiable */
    @Override
    protected void checkFrozen() {
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
