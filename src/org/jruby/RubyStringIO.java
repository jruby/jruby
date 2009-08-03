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
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.InvalidValueException;
import org.jruby.util.io.ModeFlags;
import org.jruby.util.io.Stream;

import static org.jruby.RubyEnumerator.enumeratorize;

@JRubyClass(name="StringIO")
public class RubyStringIO extends RubyObject {
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

    @JRubyMethod(name = "open", optional = 2, frame = true, meta = true)
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
    }

    private long pos = 0L;
    private int lineno = 0;
    private boolean eof = false;

    /**
     * ATTN: the value of internal might be reset to null
     * (during StringIO.open with block), so watch out for that.
     */
    private RubyString internal;

    // Has read/write been closed or is it still open for business
    private boolean closedRead = false;
    private boolean closedWrite = false;

    // Support IO modes that this object was opened with
    ModeFlags modes;
    
    private void initializeModes(Object modeArgument) {
        try {        
            if (modeArgument == null) {
                modes = new ModeFlags(RubyIO.getIOModesIntFromString(getRuntime(), "r+"));            
            } else if (modeArgument instanceof Long) {
                modes = new ModeFlags(((Long)modeArgument).longValue());
            } else {
                modes = new ModeFlags(RubyIO.getIOModesIntFromString(getRuntime(), (String) modeArgument));            
            }
        } catch (InvalidValueException e) {
            throw getRuntime().newErrnoEINVALError();
        }
        setupModes();
    }

    @JRubyMethod(name = "initialize", optional = 2, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject[] args, Block unusedBlock) {
        Object modeArgument = null;
        switch (args.length) {
            case 0:
                internal = RubyString.newEmptyString(getRuntime());
                modeArgument = "r+";
                break;
            case 1:
                internal = args[0].convertToString();
                modeArgument = internal.isFrozen() ? "r" : "r+";
                break;
            case 2:
                internal = args[0].convertToString();
                if (args[1] instanceof RubyFixnum) {
                    modeArgument = RubyFixnum.fix2long(args[1]);
                } else {
                    modeArgument = args[1].convertToString().toString();
                }
                break;
        }

        initializeModes(modeArgument);

        if (modes.isWritable() && internal.isFrozen()) {
            throw getRuntime().newErrnoEACCESError("Permission denied");
        }

        if (modes.isTruncate()) {
            internal.modifyCheck();
            internal.empty();
        }

        return this;
    }

    @JRubyMethod(visibility = Visibility.PRIVATE)
    public IRubyObject initialize_copy(IRubyObject other) {

        RubyStringIO otherIO = (RubyStringIO) TypeConverter.convertToType(
                other, getRuntime().fastGetClass("StringIO"), "to_strio");

        if (this == otherIO) {
            return this;
        }

        pos = otherIO.pos;
        lineno = otherIO.lineno;
        eof = otherIO.eof;
        closedRead = otherIO.closedRead;
        closedWrite = otherIO.closedWrite;
        internal = otherIO.internal;
        modes = otherIO.modes;
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

    @JRubyMethod(name = "binmode")
    public IRubyObject binmode() {
        return this;
    }
    
    @JRubyMethod(name = "close", frame=true)
    public IRubyObject close() {
        checkInitialized();
        checkOpen();

        closedRead = true;
        closedWrite = true;
        
        return getRuntime().getNil();
    }

    private void doFinalize() {
        closedRead = true;
        closedWrite = true;
        internal = null;
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject closed_p() {
        checkInitialized();
        return getRuntime().newBoolean(closedRead && closedWrite);
    }

    @JRubyMethod(name = "close_read")
    public IRubyObject close_read() {
        checkReadable();
        closedRead = true;
        
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_read?")
    public IRubyObject closed_read_p() {
        checkInitialized();
        return getRuntime().newBoolean(closedRead);
    }

    @JRubyMethod(name = "close_write")
    public IRubyObject close_write() {
        checkWritable();
        closedWrite = true;
        
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_write?")
    public IRubyObject closed_write_p() {
        checkInitialized();
        return getRuntime().newBoolean(closedWrite);
    }

    public IRubyObject each(ThreadContext context, IRubyObject[] args, Block block) {
        IRubyObject line = getsOnly(context, args);
       
        while (!line.isNil()) {
            block.yield(context, line);
            line = getsOnly(context, args);
        }
       
        return this;
    }

    @JRubyMethod(name = "each", optional = 1, frame = true, writes = FrameField.LASTLINE)
    public IRubyObject each19(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? each(context, args, block) : enumeratorize(context.getRuntime(), this, "each", args);
    }

    @JRubyMethod(name = "each_line", optional = 1, frame = true)
    public IRubyObject each_line(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? each(context, args, block) : enumeratorize(context.getRuntime(), this, "each_line", args);
    }

    @JRubyMethod(name = "lines", optional = 1, frame = true)
    public IRubyObject lines(ThreadContext context, IRubyObject[] args, Block block) {
        return block.isGiven() ? each(context, args, block) : enumeratorize(context.getRuntime(), this, "lines", args);
    }

    public IRubyObject each_byte(ThreadContext context, Block block) {
        checkReadable();
        Ruby runtime = context.getRuntime();
        ByteList bytes = internal.getByteList();

        // Check the length every iteration, since
        // the block can modify this string.
        while (pos < bytes.length()) {
            block.yield(context, runtime.newFixnum(bytes.get((int) pos++) & 0xFF));
        }
        return this;
    }

    @JRubyMethod(name = "each_byte", frame = true)
    public IRubyObject each_byte19(ThreadContext context, Block block) {
        return block.isGiven() ? each_byte(context, block) : enumeratorize(context.getRuntime(), this, "each_byte");
    }

    @JRubyMethod(name = "bytes", frame = true)
    public IRubyObject bytes(ThreadContext context, Block block) {
        return block.isGiven() ? each_byte(context, block) : enumeratorize(context.getRuntime(), this, "bytes");
    }

    public IRubyObject each_char(final ThreadContext context, final Block block) {
        checkReadable();

        Ruby runtime = context.getRuntime();
        ByteList bytes = internal.getByteList();
        int len = bytes.realSize;
        while (pos < len) {
            int pos = (int)this.pos;
            byte c = bytes.bytes[bytes.begin + pos];
            int n = runtime.getKCode().getEncoding().length(c);
            if(len < pos + n) {
                n = len - pos;
            }
            this.pos += n;
            block.yield(context, internal.substr19(runtime, pos, n));
        }

        return this;
    }

    @JRubyMethod(name = "each_char", frame = true)
    public IRubyObject each_char19(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_char(context, block) : enumeratorize(context.getRuntime(), this, "each_char");
    }

    @JRubyMethod(name = "chars", frame = true)
    public IRubyObject chars19(final ThreadContext context, final Block block) {
        return block.isGiven() ? each_char(context, block) : enumeratorize(context.getRuntime(), this, "chars");
    }

    @JRubyMethod(name = {"eof", "eof?"})
    public IRubyObject eof() {
        return getRuntime().newBoolean(isEOF());
    }

    private boolean isEOF() {
        return (pos >= internal.getByteList().length()) || eof;
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
        if (pos >= internal.getByteList().length()) {
            return getRuntime().getNil();
        }
        return getRuntime().newFixnum(internal.getByteList().get((int)pos++) & 0xFF);
    }

    private IRubyObject internalGets(ThreadContext context, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();

        if (pos < internal.getByteList().realSize && !eof) {
            boolean isParagraph = false;

            ByteList sep;
            if (args.length > 0) {
                if (args[0].isNil()) {
                    ByteList buf = internal.getByteList().makeShared(
                            (int)pos, internal.getByteList().realSize - (int)pos);
                    pos += buf.realSize;
                    return RubyString.newString(runtime, buf);
                }
                sep = args[0].convertToString().getByteList();
                if (sep.realSize == 0) {
                    isParagraph = true;
                    sep = Stream.PARAGRAPH_SEPARATOR;
                }
            } else {
                sep = ((RubyString)runtime.getGlobalVariables().get("$/")).getByteList();
            }

            ByteList ss = internal.getByteList();

            if (isParagraph) {
                swallowLF(ss);
                if (pos == ss.realSize) {
                    return runtime.getNil();
                }
            }

            int ix = ss.indexOf(sep, (int)pos);

            ByteList add;
            if (-1 == ix) {
                ix = internal.getByteList().realSize;
                add = ByteList.EMPTY_BYTELIST;
            } else {
                add = isParagraph? NEWLINE : sep;
            }

            ByteList line = new ByteList(ix - (int)pos + add.length());
            line.append(internal.getByteList(), (int)pos, ix - (int)pos);
            line.append(add);
            pos = ix + add.realSize;
            lineno++;

            return RubyString.newString(runtime,line);
        }
        return runtime.getNil();
    }

    private void swallowLF(ByteList list) {
        while (pos < list.realSize) {
            if (list.get((int)pos) == '\n') {
                pos++;
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
        return getRuntime().newFixnum(internal.getByteList().length());
    }

    @JRubyMethod(name = "lineno")
    public IRubyObject lineno() {
        return getRuntime().newFixnum(lineno);
    }

    @JRubyMethod(name = "lineno=", required = 1)
    public IRubyObject set_lineno(IRubyObject arg) {
        lineno = RubyNumeric.fix2int(arg);
        
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "path")
    public IRubyObject path() {
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "pid")
    public IRubyObject pid() {
        return getRuntime().getNil();
    }

    @JRubyMethod(name = {"pos", "tell"})
    public IRubyObject pos() {
        return getRuntime().newFixnum(pos);
    }

    @JRubyMethod(name = "pos=", required = 1)
    public IRubyObject set_pos(IRubyObject arg) {
        pos = RubyNumeric.fix2int(arg);
        if (pos < 0) {
            throw getRuntime().newErrnoEINVALError("Invalid argument");
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
        if (!sep.isNil()) {
            append(context, sep);
        }
        return getRuntime().getNil();
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

        internal.modify();
        ByteList bytes = internal.getByteList();
        if (modes.isAppendable()) {
            pos = bytes.length();
            bytes.append(c);
        } else {
            if (pos >= bytes.length()) {
                bytes.length((int)pos + 1);
            }

            bytes.set((int) pos, c);
            pos++;
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
                if (length > 0 && pos >= internal.getByteList().length()) {
                    eof = true;
                    if (buf != null) buf.realSize = 0;
                    return getRuntime().getNil();
                } else if (eof) {
                    if (buf != null) buf.realSize = 0;
                    return getRuntime().getNil();
                }
                break;
            }
        case 0:
            oldLength = -1;
            length = internal.getByteList().length();
            
            if (length <= pos) {
                eof = true;
                if (buf == null) {
                    buf = new ByteList();
                } else {
                    buf.realSize = 0;
                }
                
                return getRuntime().newString(buf);
            } else {
                length -= pos;
            }
            break;
        default:
            getRuntime().newArgumentError(args.length, 0);
        }
         
        if (buf == null) {
            int internalLength = internal.getByteList().length();
         
            if (internalLength > 0) {
                if (internalLength >= pos + length) {
                    buf = new ByteList(internal.getByteList(), (int) pos, length);  
                } else {
                    int rest = (int) (internal.getByteList().length() - pos);
                    
                    if (length > rest) length = rest;
                    buf = new ByteList(internal.getByteList(), (int) pos, length);
                }
            }
        } else {
            int rest = (int) (internal.getByteList().length() - pos);
            
            if (length > rest) length = rest;

            // Yow...this is still ugly
            byte[] target = buf.bytes;
            if (target.length > length) {
                System.arraycopy(internal.getByteList().bytes, (int) pos, target, 0, length);
                buf.begin = 0;
                buf.realSize = length;
            } else {
                target = new byte[length];
                System.arraycopy(internal.getByteList().bytes, (int) pos, target, 0, length);
                buf.begin = 0;
                buf.realSize = length;
                buf.bytes = target;
            }
        }
        
        if (buf == null) {
            if (!eof) buf = new ByteList();
            length = 0;
        } else {
            length = buf.length();
            pos += length;
        }
        
        if (oldLength < 0 || oldLength > length) eof = true;
        
        return originalString != null ? originalString : getRuntime().newString(buf);
    }

    @JRubyMethod(name = {"readchar", "readbyte"})
    public IRubyObject readchar() {
        IRubyObject c = getc();
        
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
        closedRead = false;
        closedWrite = false;
        return initialize(args, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "rewind")
    public IRubyObject rewind() {
        doRewind();
        return RubyFixnum.zero(getRuntime());
    }

    private void doRewind() {
        this.pos = 0L;
        this.eof = false;
        this.lineno = 0;
    }

    @JRubyMethod(name = "seek", required = 1, optional = 1, frame=true)
    public IRubyObject seek(IRubyObject[] args) {
        checkOpen();
        checkFinalized();
        long amount = RubyNumeric.num2long(args[0]);
        int whence = Stream.SEEK_SET;
        long newPosition = pos;

        if (args.length > 1 && !args[0].isNil()) whence = RubyNumeric.fix2int(args[1]);

        if (whence == Stream.SEEK_CUR) {
            newPosition += amount;
        } else if (whence == Stream.SEEK_END) {
            newPosition = internal.getByteList().length() + amount;
        } else {
            newPosition = amount;
        }

        if (newPosition < 0) throw getRuntime().newErrnoEINVALError();

        pos = newPosition;
        eof = false;
        
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
        if (internal == null) {
            return getRuntime().getNil();
        } else {
            return internal;
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

        internal.modify();
        ByteList buf = internal.getByteList();
        if (len < buf.length()) {
            Arrays.fill(buf.unsafeBytes(), len, buf.length(), (byte) 0);
        }
        buf.length(len);
        return arg;
    }

    @JRubyMethod(name = "ungetc", required = 1)
    public IRubyObject ungetc(IRubyObject arg) {
        checkReadable();
        
        int c = RubyNumeric.num2int(arg);
        if (pos == 0) return getRuntime().getNil();
        internal.modify();
        pos--;
        
        ByteList bytes = internal.getByteList();

        if (bytes.length() <= pos) {
            bytes.length((int)pos + 1);
        }

        bytes.set((int) pos, c);
        return getRuntime().getNil();
    }

    @JRubyMethod(name = {"write", "syswrite"}, required = 1)
    public IRubyObject write(ThreadContext context, IRubyObject arg) {
        return context.getRuntime().newFixnum(writeInternal(context, arg));
    }

    private int writeInternal(ThreadContext context, IRubyObject arg) {
        checkWritable();
        checkFrozen();

        RubyString val = arg.asString();
        internal.modify();

        if (modes.isAppendable()) {
            internal.getByteList().append(val.getByteList());
            pos = internal.getByteList().length();
        } else {
            int left = internal.getByteList().length()-(int)pos;
            internal.getByteList().replace((int)pos,Math.min(val.getByteList().length(),left),val.getByteList());
            pos += val.getByteList().length();
        }

        if (val.isTaint()) {
            internal.setTaint(true);
        }

        return val.getByteList().length();
    }

    /* rb: check_modifiable */
    @Override
    protected void checkFrozen() {
        checkInitialized();
        if (internal.isFrozen()) throw getRuntime().newIOError("not modifiable string");
    }
    
    /* rb: readable */
    private void checkReadable() {
        checkInitialized();
        if (closedRead || !modes.isReadable()) {
            throw getRuntime().newIOError("not opened for reading");
        }
    }

    /* rb: writable */
    private void checkWritable() {
        checkInitialized();
        if (closedWrite || !modes.isWritable()) {
            throw getRuntime().newIOError("not opened for writing");
        }

        // Tainting here if we ever want it. (secure 4)
    }

    private void checkInitialized() {
        if (modes == null) {
            throw getRuntime().newIOError("uninitialized stream");
        }
    }
    
    private void checkFinalized() {
        if (internal == null) {
            throw getRuntime().newIOError("not opened");
        }
    }

    private void checkOpen() {
        if (closedRead && closedWrite) {
            throw getRuntime().newIOError("closed stream");
        }
    }

    private void setupModes() {
        closedWrite = false;
        closedRead = false;
        
        if (modes.isReadOnly()) closedWrite = true;
        if (!modes.isReadable()) closedRead = true;
    }
}
