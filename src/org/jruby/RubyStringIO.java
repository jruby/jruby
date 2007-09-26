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
import org.jruby.runtime.Arity;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.IOHandler;
import org.jruby.util.ByteList;

public class RubyStringIO extends RubyObject {
    private static ObjectAllocator STRINGIO_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyStringIO(runtime, klass);
        }
    };
    
    public static RubyClass createStringIOClass(final Ruby runtime) {
        final RubyClass stringIOClass = runtime.defineClass("StringIO", runtime.getObject(), STRINGIO_ALLOCATOR);
        
        final CallbackFactory callbackFactory = runtime.callbackFactory(RubyStringIO.class);
        
        stringIOClass.getMetaClass().defineMethod("open", callbackFactory.getOptSingletonMethod("open"));
        stringIOClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        stringIOClass.defineFastMethod("<<", callbackFactory.getFastMethod("append", IRubyObject.class));
        stringIOClass.defineFastMethod("binmode", callbackFactory.getFastMethod("binmode"));
        stringIOClass.defineFastMethod("close", callbackFactory.getFastMethod("close"));
        stringIOClass.defineFastMethod("closed?", callbackFactory.getFastMethod("closed_p"));
        stringIOClass.defineFastMethod("close_read", callbackFactory.getFastMethod("close_read"));
        stringIOClass.defineFastMethod("closed_read?", callbackFactory.getFastMethod("closed_read_p"));
        stringIOClass.defineFastMethod("close_write", callbackFactory.getFastMethod("close_write"));
        stringIOClass.defineFastMethod("closed_write?", callbackFactory.getFastMethod("closed_write_p"));
        stringIOClass.defineMethod("each", callbackFactory.getOptMethod("each"));
        stringIOClass.defineMethod("each_byte", callbackFactory.getMethod("each_byte"));
        stringIOClass.defineMethod("each_line", callbackFactory.getMethod("each_line"));
        stringIOClass.defineFastMethod("eof", callbackFactory.getFastMethod("eof"));
        stringIOClass.defineFastMethod("eof?", callbackFactory.getFastMethod("eof_p"));
        stringIOClass.defineFastMethod("fcntl", callbackFactory.getFastMethod("fcntl"));
        stringIOClass.defineFastMethod("fileno", callbackFactory.getFastMethod("fileno"));
        stringIOClass.defineFastMethod("flush", callbackFactory.getFastMethod("flush"));
        stringIOClass.defineFastMethod("fsync", callbackFactory.getFastMethod("fsync"));
        stringIOClass.defineFastMethod("getc", callbackFactory.getFastMethod("getc"));
        stringIOClass.defineFastMethod("gets", callbackFactory.getFastOptMethod("gets"));
        stringIOClass.defineFastMethod("isatty", callbackFactory.getFastMethod("isatty"));
        // FIXME: this should probably be an alias?
        stringIOClass.defineFastMethod("tty?", callbackFactory.getFastMethod("tty_p"));
        stringIOClass.defineFastMethod("length", callbackFactory.getFastMethod("length"));
        stringIOClass.defineFastMethod("lineno", callbackFactory.getFastMethod("lineno"));
        stringIOClass.defineFastMethod("lineno=", callbackFactory.getFastMethod("set_lineno", IRubyObject.class));
        stringIOClass.defineFastMethod("path", callbackFactory.getFastMethod("path"));
        stringIOClass.defineFastMethod("pid", callbackFactory.getFastMethod("pid"));
        stringIOClass.defineFastMethod("pos", callbackFactory.getFastMethod("pos"));
        // FIXME: this should probably be an alias?
        stringIOClass.defineFastMethod("tell", callbackFactory.getFastMethod("tell"));
        stringIOClass.defineFastMethod("pos=", callbackFactory.getFastMethod("set_pos", IRubyObject.class));
        stringIOClass.defineFastMethod("print", callbackFactory.getFastOptMethod("print"));
        stringIOClass.defineFastMethod("printf", callbackFactory.getFastOptMethod("printf"));
        stringIOClass.defineFastMethod("putc", callbackFactory.getFastMethod("putc", IRubyObject.class));
        stringIOClass.defineFastMethod("puts", callbackFactory.getFastOptMethod("puts"));
        stringIOClass.defineFastMethod("read", callbackFactory.getFastOptMethod("read"));
        stringIOClass.defineFastMethod("readchar", callbackFactory.getFastMethod("readchar"));
        stringIOClass.defineFastMethod("readline", callbackFactory.getFastOptMethod("readline"));
        stringIOClass.defineFastMethod("readlines", callbackFactory.getFastOptMethod("readlines"));
        stringIOClass.defineFastMethod("reopen", callbackFactory.getFastOptMethod("reopen"));
        stringIOClass.defineFastMethod("rewind", callbackFactory.getFastMethod("rewind"));
        stringIOClass.defineFastMethod("seek", callbackFactory.getFastOptMethod("seek"));
        stringIOClass.defineFastMethod("size", callbackFactory.getFastMethod("size"));
        stringIOClass.defineFastMethod("string", callbackFactory.getFastMethod("string"));
        stringIOClass.defineFastMethod("string=", callbackFactory.getFastMethod("set_string", IRubyObject.class));
        stringIOClass.defineFastMethod("sync", callbackFactory.getFastMethod("sync"));
        stringIOClass.defineFastMethod("sync=", callbackFactory.getFastMethod("set_sync", IRubyObject.class));
        stringIOClass.defineFastMethod("sysread", callbackFactory.getFastOptMethod("sysread"));
        stringIOClass.defineFastMethod("syswrite", callbackFactory.getFastMethod("syswrite", IRubyObject.class));
        stringIOClass.defineFastMethod("truncate", callbackFactory.getFastMethod("truncate", IRubyObject.class));
        stringIOClass.defineFastMethod("ungetc", callbackFactory.getFastMethod("ungetc", IRubyObject.class));
        stringIOClass.defineFastMethod("write", callbackFactory.getFastMethod("write", IRubyObject.class));

        return stringIOClass;
    }

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

    @Override
    public IRubyObject initialize(IRubyObject[] args, Block block) {
        Arity.checkArgumentCount(getRuntime(), args, 0, 2);

        if (args.length > 0) {
            // Share bytelist since stringio is acting on this passed-in string.
            internal = args[0].convertToString();
            if (args.length > 1) {
                if (args[1] instanceof RubyFixnum) {
                    int numericModes = RubyNumeric.fix2int(args[1]);
                    
                }
                String modes = args[1].convertToString().toString();
                
                setupModes(modes);
            }
        } else {
            internal = getRuntime().newString("");
        }
        
        return this;
    }

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

    public IRubyObject binmode() {
        return this;
    }
    
    public IRubyObject close() {
        closedRead = true;
        closedWrite = true;
        
        return getRuntime().getNil();
    }

    public IRubyObject closed_p() {
        return getRuntime().newBoolean(closedRead && closedWrite);
    }

    public IRubyObject close_read() {
        closedRead = true;
        
        return getRuntime().getNil();
    }

    public IRubyObject closed_read_p() {
        return getRuntime().newBoolean(closedRead);
    }

    public IRubyObject close_write() {
        closedWrite = true;
        
        return getRuntime().getNil();
    }

    public IRubyObject closed_write_p() {
        return getRuntime().newBoolean(closedWrite);
    }

   public IRubyObject each(IRubyObject[] args, Block block) {
       IRubyObject line = gets(args);
       ThreadContext context = getRuntime().getCurrentContext();
       
       while (!line.isNil()) {
           block.yield(context, line);
           line = gets(args);
       }
       
       return this;
   }

    public IRubyObject each_byte(Block block) {
        checkReadable();
        
        RubyString.newString(getRuntime(),new ByteList(internal.getByteList(), (int)pos, internal.getByteList().length())).each_byte(block);
        
        return getRuntime().getNil();
    }

    public IRubyObject each_line(Block block) {
        return each(new RubyObject[0], block);
    }

    public IRubyObject eof() {
        return (pos >= internal.getByteList().length() || eof) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject eof_p() {
        return (pos >= internal.getByteList().length() || eof) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject fcntl() {
        throw getRuntime().newNotImplementedError("fcntl not implemented");
    }

    public IRubyObject fileno() {
        return getRuntime().getNil();
    }

    public IRubyObject flush() {
        return this;
    }

    public IRubyObject fsync() {
        return RubyFixnum.zero(getRuntime());
    }

    public IRubyObject getc() {
        if (pos >= internal.getByteList().length()) {
            return getRuntime().getNil();
        }
        return getRuntime().newFixnum(internal.getByteList().get((int)pos++) & 0xFF);
    }

    public IRubyObject internalGets(IRubyObject[] args) {
        if (pos < internal.getByteList().length() && !eof) {
            String sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getValue().toString();
            if (args.length>0) {
                if (args[0].isNil()) {
                    ByteList buf = new ByteList(internal.getByteList(), (int)pos, internal.getByteList().length()-(int)pos);
                    pos+=buf.length();
                    return RubyString.newString(getRuntime(),buf);
                }
                sep = args[0].toString();
            }
            String ss = internal.toString();
            int ix = ss.indexOf(sep,(int)pos);
            String add = sep;
            if (-1 == ix) {
                ix = internal.getByteList().length();
                add = "";
            }
            ByteList line = new ByteList(internal.getByteList(), (int)pos, ix-(int)pos);
            line.append(RubyString.stringToBytes(add));
            pos = ix + add.length();
            lineno++;
            return RubyString.newString(getRuntime(),line);
        }
        return getRuntime().getNil();
    }

    public IRubyObject gets(IRubyObject[] args) {
        checkReadable();

        IRubyObject result = internalGets(args);
        if (!result.isNil()) {
            getRuntime().getCurrentContext().getCurrentFrame().setLastLine(result);
        }
        
        return result;
    }

    public IRubyObject isatty() {
        return getRuntime().getFalse();
    }

    public IRubyObject tty_p() {
        return getRuntime().getFalse();
    }

    public IRubyObject length() {
        return getRuntime().newFixnum(internal.getByteList().length());
    }

    public IRubyObject lineno() {
        return getRuntime().newFixnum(lineno);
    }

    public IRubyObject set_lineno(IRubyObject arg) {
        lineno = RubyNumeric.fix2int(arg);
        
        return getRuntime().getNil();
    }

    public IRubyObject path() {
        return getRuntime().getNil();
    }

    public IRubyObject pid() {
        return getRuntime().getNil();
    }

    public IRubyObject pos() {
        return getRuntime().newFixnum(pos);
    }

    public IRubyObject tell() {
        return getRuntime().newFixnum(pos);
    }

    public IRubyObject set_pos(IRubyObject arg) {
        pos = RubyNumeric.fix2int(arg);
        
        return getRuntime().getNil();
    }

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
    
    public IRubyObject printf(IRubyObject[] args) {
        append(RubyKernel.sprintf(this,args));
        return getRuntime().getNil();
    }

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

    private static final ByteList NEWLINE_BL = new ByteList(new byte[]{10},false);

    public IRubyObject puts(IRubyObject[] obj) {
        checkWritable();

        if (obj.length == 0) append(RubyString.newString(getRuntime(),NEWLINE_BL));

        for (int i=0,j=obj.length;i<j;i++) {
            append(obj[i]);

            // Append a newline if there wasn't already one at the end of newest appended object.
            int lastPossibleNewlineIndex = (int) (pos - NEWLINE_BL.length());
            if (lastPossibleNewlineIndex == -1 ||
                !internal.getByteList().subSequence(lastPossibleNewlineIndex, (int) pos).equals(NEWLINE_BL)) {
                    // If we rewind/seek backwards write newline over existing, otherwise add
                    internal.getByteList().unsafeReplace((int) pos++, internal.getByteList().length() > pos ? 1 : 0, NEWLINE_BL);
            }
        }
        return getRuntime().getNil();
    }

    @SuppressWarnings("fallthrough")
    public IRubyObject read(IRubyObject[] args) {
        checkReadable();

        ByteList buf = null;
        int length = 0;
        int oldLength = 0;
        RubyString originalString = null;
        
        switch (args.length) {
        case 2:
            originalString = args[1].convertToString();
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

    public IRubyObject readchar() {
        IRubyObject c = getc();
        
        if (c.isNil()) throw getRuntime().newEOFError();
        
        return c;
    }

    public IRubyObject readline(IRubyObject[] args) {
        IRubyObject line = gets(args);
        
        if (line.isNil()) throw getRuntime().newEOFError();
        
        return line;
    }
    
    public IRubyObject readlines(IRubyObject[] arg) {
        checkReadable();
        
        List<IRubyObject> lns = new ArrayList<IRubyObject>();
        while (!(pos >= internal.getByteList().length() || eof)) {
            lns.add(gets(arg));
        }
        return getRuntime().newArray(lns);
    }
    
    public IRubyObject reopen(IRubyObject[] args) {
        Arity.checkArgumentCount(getRuntime(), args, 1, 2);
        
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
        }
        
        return this;
    }

    public IRubyObject rewind() {
        this.pos = 0L;
        this.lineno = 0;
        return RubyFixnum.zero(getRuntime());
    }

    public IRubyObject seek(IRubyObject[] args) {
        long amount = RubyNumeric.fix2long(args[0]);
        int whence = IOHandler.SEEK_SET;
        long newPosition = pos;

        if (args.length > 1 && !args[0].isNil()) whence = RubyNumeric.fix2int(args[1]);

        if (whence == IOHandler.SEEK_CUR) {
            newPosition += amount;
        } else if (whence == IOHandler.SEEK_END) {
            newPosition = internal.getByteList().length() + amount;
        } else {
            newPosition = amount;
        }

        if (newPosition < 0) throw getRuntime().newErrnoEINVALError();

        pos = newPosition;
        eof = false;
        
        return RubyFixnum.zero(getRuntime());
    }

    public IRubyObject set_string(IRubyObject arg) {
        return reopen(new IRubyObject[] { arg });
    }

    public IRubyObject set_sync(IRubyObject args) {
        return args;
    }
    
    public IRubyObject size() {
        return getRuntime().newFixnum(internal.getByteList().length());
    }

    public IRubyObject string() {
        return internal;
    }

    public IRubyObject sync() {
        return getRuntime().getTrue();
    }
    
    public IRubyObject sysread(IRubyObject[] args) {
        IRubyObject obj = read(args);
        
        if (obj.isNil() || ((RubyString) obj).getByteList().length() == 0) throw getRuntime().newEOFError();
        
        return obj; 
    }


    public IRubyObject syswrite(IRubyObject args) {
        return write(args);
    }

    public IRubyObject truncate(IRubyObject arg) {
        checkWritable();
        
        int len = RubyFixnum.fix2int(arg);
        internal.modify();
        internal.getByteList().length(len);
        return getRuntime().newFixnum(len);
    }

    public IRubyObject ungetc(IRubyObject arg) {
        checkReadable();
        
        int c = RubyNumeric.num2int(arg);
        if (pos == 0) return getRuntime().getNil();
        internal.modify();
        pos--;
        internal.getByteList().set((int) pos, c);
        
        return getRuntime().getNil();
    }

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
