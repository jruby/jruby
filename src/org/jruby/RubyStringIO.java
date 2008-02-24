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
import java.util.List;
import org.jruby.anno.JRubyMethod;

import org.jruby.runtime.Block;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.io.Stream;
import org.jruby.util.ByteList;

public class RubyStringIO extends RubyObject {
    private static ObjectAllocator STRINGIO_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyStringIO(runtime, klass);
        }
    };
    
    public static RubyClass createStringIOClass(final Ruby runtime) {
        RubyClass stringIOClass = runtime.defineClass("StringIO", runtime.getObject(), STRINGIO_ALLOCATOR);
        
        stringIOClass.defineAnnotatedMethods(RubyStringIO.class);

        return stringIOClass;
    }

    @JRubyMethod(name = "open", optional = 2, frame = true, meta = true)
    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyStringIO strio = (RubyStringIO)((RubyClass)recv).newInstance(args, Block.NULL_BLOCK);
        IRubyObject val = strio;
        ThreadContext tc = recv.getRuntime().getCurrentContext();
        
        if (block.isGiven()) {
            try {
                val = block.yield(tc, strio);
            } finally {
                strio.close();
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
    private RubyString internal;
    // FIXME: Replace these with common IOModes instance and make IOModes work for this and IO.
    private boolean closedRead = false;
    private boolean closedWrite = false;
    private boolean append = false;

    @JRubyMethod(name = "initialize", optional = 2, frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        if (args.length > 0) {
            // Share bytelist since stringio is acting on this passed-in string.
            internal = args[0].convertToString();
            if (args.length > 1) {
                if (args[1] instanceof RubyFixnum) {
                    // FIXME: We should use this somehow. yes?
                    //int numericModes = RubyNumeric.fix2int(args[1]);
                    
                }
                String modes = args[1].convertToString().toString();
                
                setupModes(modes);
            }
        } else {
            internal = getRuntime().newString("");
        }
        
        return this;
    }

    @JRubyMethod(name = "<<", required = 1)
    public IRubyObject append(IRubyObject obj) {
        checkWritable();
        checkFrozen();

        RubyString val = ((RubyString)obj.callMethod(obj.getRuntime().getCurrentContext(),MethodIndex.TO_S, "to_s"));
        internal.modify();
        if (append) {
            internal.getByteList().append(val.getByteList());
        } else {
            int left = internal.getByteList().length()-(int)pos;
            internal.getByteList().replace((int)pos,Math.min(val.getByteList().length(),left),val.getByteList());
            pos += val.getByteList().length();
        }

        return this;
    }

    @JRubyMethod(name = "binmode")
    public IRubyObject binmode() {
        return this;
    }
    
    @JRubyMethod(name = "close")
    public IRubyObject close() {
        closedRead = true;
        closedWrite = true;
        
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject closed_p() {
        return getRuntime().newBoolean(closedRead && closedWrite);
    }

    @JRubyMethod(name = "close_read")
    public IRubyObject close_read() {
        closedRead = true;
        
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_read?")
    public IRubyObject closed_read_p() {
        return getRuntime().newBoolean(closedRead);
    }

    @JRubyMethod(name = "close_write")
    public IRubyObject close_write() {
        closedWrite = true;
        
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "closed_write?")
    public IRubyObject closed_write_p() {
        return getRuntime().newBoolean(closedWrite);
    }

    @JRubyMethod(name = "each", optional = 1, frame = true)
    public IRubyObject each(IRubyObject[] args, Block block) {
        IRubyObject line = gets(args);
        ThreadContext context = getRuntime().getCurrentContext();
       
        while (!line.isNil()) {
            block.yield(context, line);
            line = gets(args);
        }
       
        return this;
    }

    @JRubyMethod(name = "each_byte", frame = true)
    public IRubyObject each_byte(Block block) {
        checkReadable();
        
        RubyString.newString(getRuntime(),new ByteList(internal.getByteList(), (int)pos, internal.getByteList().length())).each_byte(block);
        
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "each_line", optional = 1, frame = true)
    public IRubyObject each_line(IRubyObject[] args, Block block) {
        return each(args, block);
    }

    @JRubyMethod(name = "eof")
    public IRubyObject eof() {
        return (pos >= internal.getByteList().length() || eof) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "eof?")
    public IRubyObject eof_p() {
        return (pos >= internal.getByteList().length() || eof) ? getRuntime().getTrue() : getRuntime().getFalse();
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

    @JRubyMethod(name = "getc")
    public IRubyObject getc() {
        if (pos >= internal.getByteList().length()) {
            return getRuntime().getNil();
        }
        return getRuntime().newFixnum(internal.getByteList().get((int)pos++) & 0xFF);
    }

    public IRubyObject internalGets(IRubyObject[] args) {
        if (pos < internal.getByteList().realSize && !eof) {
            ByteList sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getByteList();
            if (args.length>0) {
                if (args[0].isNil()) {
                    ByteList buf = internal.getByteList().makeShared((int)pos, internal.getByteList().realSize-(int)pos);
                    pos+=buf.realSize;
                    return RubyString.newString(getRuntime(),buf);
                }
                sep = args[0].convertToString().getByteList();
            }
            ByteList ss = internal.getByteList();
            int ix = ss.indexOf(sep,(int)pos);
            ByteList add = sep;
            if (-1 == ix) {
                ix = internal.getByteList().realSize;
                add = new ByteList(new byte[0], false);
            }
            ByteList line = internal.getByteList().makeShared((int)pos, ix-(int)pos);
            line.unshare();
            line.append(add);
            line.invalidate();
            pos = ix + add.realSize;
            lineno++;
            return RubyString.newString(getRuntime(),line);
        }
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "gets", optional = 1)
    public IRubyObject gets(IRubyObject[] args) {
        checkReadable();

        IRubyObject result = internalGets(args);
        if (!result.isNil()) {
            getRuntime().getCurrentContext().getCurrentFrame().setLastLine(result);
        }
        
        return result;
    }

    @JRubyMethod(name = "isatty")
    public IRubyObject isatty() {
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = "tty?")
    public IRubyObject tty_p() {
        return getRuntime().getFalse();
    }

    @JRubyMethod(name = "length")
    public IRubyObject length() {
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

    @JRubyMethod(name = "pos")
    public IRubyObject pos() {
        return getRuntime().newFixnum(pos);
    }

    @JRubyMethod(name = "tell")
    public IRubyObject tell() {
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
    public IRubyObject print(IRubyObject[] args) {
        if (args.length != 0) {
            for (int i=0,j=args.length;i<j;i++) {
                append(args[i]);
            }
        }
        IRubyObject sep = getRuntime().getGlobalVariables().get("$\\");
        if (!sep.isNil()) {
            append(sep);
        }
        return getRuntime().getNil();
    }
    
    @JRubyMethod(name = "printf", required = 1, rest = true)
    public IRubyObject printf(IRubyObject[] args) {
        append(RubyKernel.sprintf(this,args));
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "putc", required = 1)
    public IRubyObject putc(IRubyObject obj) {
        checkWritable();
        byte c = RubyNumeric.num2chr(obj);
        checkFrozen();

        internal.modify();
        if (append) {
            pos = internal.getByteList().length();
            internal.getByteList().append(c);
        } else {
            if (pos >= internal.getByteList().length()) {
                internal.getByteList().append(c);
            } else {
                internal.getByteList().set((int) pos, c);
            }
            pos++;
        }

        return obj;
    }
    
    public static final ByteList NEWLINE = ByteList.create("\n");

    @JRubyMethod(name = "puts", rest = true)
    public IRubyObject puts(IRubyObject[] args) {
        checkWritable();
        
        // FIXME: the code below is a copy of RubyIO.puts,
        // and we should avoid copy-paste.
        ThreadContext context = getRuntime().getCurrentContext();
        
        if (args.length == 0) {
            callMethod(context, "write", getRuntime().newStringShared(NEWLINE));
            return getRuntime().getNil();
        }

        for (int i = 0; i < args.length; i++) {
            String line;
            
            if (args[i].isNil()) {
                line = "nil";
            } else if (getRuntime().isInspecting(args[i])) {
                line = "[...]";
            } else if (args[i] instanceof RubyArray) {
                inspectPuts((RubyArray) args[i]);
                continue;
            } else {
                line = args[i].toString();
            }
            
            callMethod(context, "write", getRuntime().newString(line));
            
            if (!line.endsWith("\n")) {
                callMethod(context, "write", getRuntime().newStringShared(NEWLINE));
            }
        }
        return getRuntime().getNil();
    }

    private IRubyObject inspectPuts(RubyArray array) {
        try {
            getRuntime().registerInspecting(array);
            return puts(array.toJavaArray());
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

            // Yow...this is ugly
            buf.realSize = length;
            buf.replace(0, length, internal.getByteList().bytes, (int) pos, length);
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

    @JRubyMethod(name = "readchar")
    public IRubyObject readchar() {
        IRubyObject c = getc();
        
        if (c.isNil()) throw getRuntime().newEOFError();
        
        return c;
    }

    @JRubyMethod(name = "readline", optional = 1)
    public IRubyObject readline(IRubyObject[] args) {
        IRubyObject line = gets(args);
        
        if (line.isNil()) throw getRuntime().newEOFError();
        
        return line;
    }
    
    @JRubyMethod(name = "readlines", optional = 1)
    public IRubyObject readlines(IRubyObject[] arg) {
        checkReadable();
        
        List<IRubyObject> lns = new ArrayList<IRubyObject>();
        while (!(pos >= internal.getByteList().length() || eof)) {
            lns.add(gets(arg));
        }
        return getRuntime().newArray(lns);
    }
    
    @JRubyMethod(name = "reopen", required = 1, optional = 1)
    public IRubyObject reopen(IRubyObject[] args) {
        IRubyObject str = args[0];
        if (str instanceof RubyStringIO) {
            pos = ((RubyStringIO)str).pos;
            lineno = ((RubyStringIO)str).lineno;
            eof = ((RubyStringIO)str).eof;
            closedRead = ((RubyStringIO)str).closedRead;
            closedWrite = ((RubyStringIO)str).closedWrite;
            internal = ((RubyStringIO)str).internal;
        } else {
            pos = 0L;
            lineno = 0;
            eof = false;
            closedRead = false;
            closedWrite = false;
            internal = str.convertToString();
        }
        
        if (args.length == 2) {
            setupModes(args[1].convertToString().toString());
        } else {
            // reset modes to default if none specified
            setupModes("");
        }
        
        if (internal.isFrozen() && (closedRead || append)) {
            throw getRuntime().newErrnoEACCESError("not opened for writing");
        }
        
        if (closedRead) {
            // if doing explicit write mode, truncate incoming string
            internal.modify();
            internal.setValue("");
        }
        
        return this;
    }

    @JRubyMethod(name = "rewind")
    public IRubyObject rewind() {
        this.pos = 0L;
        this.lineno = 0;
        return RubyFixnum.zero(getRuntime());
    }

    @JRubyMethod(name = "seek", required = 1, optional = 1)
    public IRubyObject seek(IRubyObject[] args) {
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
        return reopen(new IRubyObject[] { arg });
    }

    @JRubyMethod(name = "sync=", required = 1)
    public IRubyObject set_sync(IRubyObject args) {
        return args;
    }
    
    @JRubyMethod(name = "size")
    public IRubyObject size() {
        return getRuntime().newFixnum(internal.getByteList().length());
    }

    @JRubyMethod(name = "string")
    public IRubyObject string() {
        return internal;
    }

    @JRubyMethod(name = "sync")
    public IRubyObject sync() {
        return getRuntime().getTrue();
    }
    
    @JRubyMethod(name = "sysread", optional = 2)
    public IRubyObject sysread(IRubyObject[] args) {
        IRubyObject obj = read(args);
        
        if (obj.isNil() || ((RubyString) obj).getByteList().length() == 0) throw getRuntime().newEOFError();
        
        return obj; 
    }

    @JRubyMethod(name = "syswrite", required = 1)
    public IRubyObject syswrite(IRubyObject args) {
        return write(args);
    }

    @JRubyMethod(name = "truncate", required = 1)
    public IRubyObject truncate(IRubyObject arg) {
        checkWritable();
        
        int len = RubyFixnum.fix2int(arg);
        internal.modify();
        internal.getByteList().length(len);
        return getRuntime().newFixnum(len);
    }

    @JRubyMethod(name = "ungetc", required = 1)
    public IRubyObject ungetc(IRubyObject arg) {
        checkReadable();
        
        int c = RubyNumeric.num2int(arg);
        if (pos == 0) return getRuntime().getNil();
        internal.modify();
        pos--;
        internal.getByteList().set((int) pos, c);
        
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "write", required = 1)
    public IRubyObject write(IRubyObject arg) {
        checkWritable();
        String obj = arg.toString();
        append(arg);
        return getRuntime().newFixnum(obj.length());
    }

    /* rb: check_modifiable */
    @Override
    protected void checkFrozen() {
        if (internal.isFrozen()) throw getRuntime().newIOError("not modifiable string");
    }
    
    /* rb: readable */
    private void checkReadable() {
        if (closedRead) throw getRuntime().newIOError("not opened for reading");
    }

    /* rb: writable */
    private void checkWritable() {
        if (closedWrite) throw getRuntime().newIOError("not opened for writing");

        // Tainting here if we ever want it. (secure 4)
    }

    private void setupModes(String modes) {
        closedWrite = false;
        closedRead = false;
        append = false;
        
        if (modes.contains("r")) closedWrite = true;
        if (modes.contains("w")) closedRead = true;
        if (modes.contains("a")) append = true;
    }
}
