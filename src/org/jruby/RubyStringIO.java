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

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
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
        stringIOClass.defineFastMethod("<<", callbackFactory.getFastMethod("append",RubyKernel.IRUBY_OBJECT));
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
        stringIOClass.defineFastMethod("lineno=", callbackFactory.getFastMethod("set_lineno", RubyFixnum.class));
        stringIOClass.defineFastMethod("path", callbackFactory.getFastMethod("path"));
        stringIOClass.defineFastMethod("pid", callbackFactory.getFastMethod("pid"));
        stringIOClass.defineFastMethod("pos", callbackFactory.getFastMethod("pos"));
        // FIXME: this should probably be an alias?
        stringIOClass.defineFastMethod("tell", callbackFactory.getFastMethod("tell"));
        stringIOClass.defineFastMethod("pos=", callbackFactory.getFastMethod("set_pos", RubyFixnum.class));
        stringIOClass.defineFastMethod("print", callbackFactory.getFastOptMethod("print"));
        stringIOClass.defineFastMethod("printf", callbackFactory.getFastOptMethod("printf"));
        stringIOClass.defineFastMethod("putc", callbackFactory.getFastMethod("putc", RubyKernel.IRUBY_OBJECT));
        stringIOClass.defineFastMethod("puts", callbackFactory.getFastOptMethod("puts"));
        stringIOClass.defineFastMethod("read", callbackFactory.getFastOptMethod("read"));
        stringIOClass.defineFastMethod("readchar", callbackFactory.getFastMethod("readchar"));
        stringIOClass.defineFastMethod("readline", callbackFactory.getFastOptMethod("readline"));
        stringIOClass.defineFastMethod("readlines", callbackFactory.getFastOptMethod("readlines"));
        stringIOClass.defineFastMethod("reopen", callbackFactory.getFastMethod("reopen", RubyKernel.IRUBY_OBJECT));
        stringIOClass.defineFastMethod("rewind", callbackFactory.getFastMethod("rewind"));
        stringIOClass.defineFastMethod("seek", callbackFactory.getFastOptMethod("seek"));
        stringIOClass.defineFastMethod("size", callbackFactory.getFastMethod("size"));
        stringIOClass.defineFastMethod("string", callbackFactory.getFastMethod("string"));
        stringIOClass.defineFastMethod("string=", callbackFactory.getFastMethod("set_string",RubyString.class));
        stringIOClass.defineFastMethod("sync", callbackFactory.getFastMethod("sync"));
        stringIOClass.defineFastMethod("sync=", callbackFactory.getFastMethod("set_sync", RubyKernel.IRUBY_OBJECT));
        stringIOClass.defineFastMethod("syswrite", callbackFactory.getFastMethod("syswrite", RubyKernel.IRUBY_OBJECT));
        stringIOClass.defineFastMethod("truncate", callbackFactory.getFastMethod("truncate", RubyFixnum.class));
        stringIOClass.defineFastMethod("ungetc", callbackFactory.getFastMethod("ungetc", RubyFixnum.class));
        stringIOClass.defineFastMethod("write", callbackFactory.getFastMethod("write", RubyKernel.IRUBY_OBJECT));

        return stringIOClass;
    }

    public static IRubyObject open(IRubyObject recv, IRubyObject[] args, Block block) {
        RubyString str = recv.getRuntime().newString("");
        IRubyObject mode = recv.getRuntime().getNil();
        if (args.length > 0) {
            str = args[0].convertToString();
            if (args.length > 1) {
                mode = args[1];
            }
        }
        RubyStringIO strio = (RubyStringIO)((RubyClass)recv).newInstance(new IRubyObject[]{str,mode}, Block.NULL_BLOCK);
        IRubyObject val = strio;
        ThreadContext tc = recv.getRuntime().getCurrentContext();
        
        if (block.isGiven()) {
            try {
                val = tc.yield(strio, block);
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
    private ByteList internal;
    private boolean closedRead = false;
    private boolean closedWrite = false;

    public IRubyObject initialize(IRubyObject[] args, Block block) {
        if (checkArgumentCount(args, 0, 2) > 0) {
            // Share bytelist since stringio is acting on this passed-in string.
            internal = args[0].convertToString().getByteList();
        } else {
            internal = new ByteList();
        }
        return this;
    }

    public IRubyObject append(IRubyObject obj) {
        ByteList val = ((RubyString)obj.callMethod(obj.getRuntime().getCurrentContext(),"to_s")).getByteList();
        int left = internal.length()-(int)pos;
        internal.replace((int)pos,Math.min(val.length(),left),val);
        pos += val.length();
        return this;
    }

    public IRubyObject binmode() {
        return getRuntime().getTrue();
    }
    
    public IRubyObject close() {
        closedRead = true;
        closedWrite = true;
        return getRuntime().getNil();
    }

    public IRubyObject closed_p() {
        return (closedRead && closedWrite) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject close_read() {
        closedRead = true;
        return getRuntime().getNil();
    }

    public IRubyObject closed_read_p() {
        return closedRead ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject close_write() {
        closedWrite = true;
        return getRuntime().getNil();
    }

    public IRubyObject closed_write_p() {
        return closedWrite ? getRuntime().getTrue() : getRuntime().getFalse();
    }

   public IRubyObject each(IRubyObject[] args, Block block) {
       IRubyObject line = gets(args);
       ThreadContext context = getRuntime().getCurrentContext();
       while (!line.isNil()) {
           context.yield(line, block);
           line = gets(args);
       }
       return this;
   }

    public IRubyObject each_byte(Block block) {
        RubyString.newString(getRuntime(),new ByteList(internal, (int)pos, internal.length())).each_byte(block);
        return this;
    }

    public IRubyObject each_line(Block block) {
        return each(new RubyObject[0], block);
    }

    public IRubyObject eof() {
        return (pos >= internal.length() || eof) ? getRuntime().getTrue() : getRuntime().getFalse();
    }

    public IRubyObject eof_p() {
        return (pos >= internal.length() || eof) ? getRuntime().getTrue() : getRuntime().getFalse();
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
        return getRuntime().newFixnum( internal.get((int)pos++) & 0xFF);
    }

    public IRubyObject internalGets(IRubyObject[] args) {
        if (pos < internal.length() && !eof) {
            String sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getValue().toString();
            if (args.length>0) {
                if (args[0].isNil()) {
                    ByteList buf = new ByteList(internal, (int)pos, internal.length()-(int)pos);
                    pos+=buf.length();
                    return RubyString.newString(getRuntime(),buf);
                }
                sep = args[0].toString();
            }
            String ss = RubyString.byteListToString(internal);
            int ix = ss.indexOf(sep,(int)pos);
            String add = sep;
            if (-1 == ix) {
                ix = internal.length();
                add = "";
            }
            ByteList line = new ByteList(internal, (int)pos, ix-(int)pos);
            line.append(RubyString.stringToBytes(add));
            pos = ix + add.length();
            lineno++;
            return RubyString.newString(getRuntime(),line);
        }
        return getRuntime().getNil();
    }

    public IRubyObject gets(IRubyObject[] args) {
        IRubyObject result = internalGets(args);
        if (!result.isNil()) {
            getRuntime().getCurrentContext().setLastline(result);
        }
        return result;
    }

    public IRubyObject isatty() {
        return getRuntime().getNil();
    }

    public IRubyObject tty_p() {
        return getRuntime().getNil();
    }

    public IRubyObject length() {
        return getRuntime().newFixnum(internal.length());
    }

    public IRubyObject lineno() {
        return getRuntime().newFixnum(lineno);
    }

    public IRubyObject set_lineno(RubyFixnum val) {
        lineno = (int)val.getLongValue();
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

    public IRubyObject set_pos(RubyFixnum val) {
        pos = (int)val.getLongValue();
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
        append(obj);
        return obj;
    }

    private static final ByteList NEWLINE_BL = new ByteList(new byte[]{10},false);

    public IRubyObject puts(IRubyObject[] obj) {
        if (obj.length == 0) {
            append(RubyString.newString(getRuntime(),NEWLINE_BL));
        }
        
        for (int i=0,j=obj.length;i<j;i++) {
            append(obj[i]);
            internal.unsafeReplace((int)(pos++),0,NEWLINE_BL);
        }
        return getRuntime().getNil();
    }

    public IRubyObject read(IRubyObject[] args) {
        ByteList buf = null;
        if (!(pos >= internal.length() || eof)) {
            if (args.length > 0 && !args[0].isNil()) {
                int len = RubyNumeric.fix2int(args[0]);
                int end = ((int)pos) + len;
                if (end > internal.length()) {
                    buf = new ByteList(internal,(int)pos,internal.length()-(int)pos);
                } else {
                    buf = new ByteList(internal,(int)pos,len);
                }
            } else {
                buf = new ByteList(internal,(int)pos,internal.length()-(int)pos);
            }
            pos+= buf.length();
        }
        IRubyObject ret = null;
        if (buf == null) {
            if (args.length > 0) {
                return getRuntime().getNil();
            }
            return getRuntime().newString("");
        } else {
            if (args.length>1) {
                ret = args[1].convertToString();
								((RubyString)ret).setValue(buf);
            } else {
                ret = RubyString.newString(getRuntime(),buf);
            }
        }

        return ret;
    }

    public IRubyObject readchar() {
        return getc();
    }

    public IRubyObject readline(IRubyObject[] args) {
        return gets(args);
    }
    
    public IRubyObject readlines(IRubyObject[] arg) {
        List lns = new ArrayList();
        while (!(pos >= internal.length() || eof)) {
            lns.add(gets(arg));
        }
        return getRuntime().newArray(lns);
    }
    
    public IRubyObject reopen(IRubyObject str) {
        if (str instanceof RubyStringIO) {
            pos = ((RubyStringIO)str).pos;
            lineno = ((RubyStringIO)str).lineno;
            eof = ((RubyStringIO)str).eof;
            closedRead = ((RubyStringIO)str).closedRead;
            closedWrite = ((RubyStringIO)str).closedWrite;
            internal = new ByteList(((RubyStringIO)str).internal);
        } else {
            pos = 0L;
            lineno = 0;
            eof = false;
            closedRead = false;
            closedWrite = false;
            internal = new ByteList();
            internal.append(str.convertToString().getByteList());
        }
        return this;
    }

    public IRubyObject rewind() {
        this.pos = 0L;
        this.lineno = 0;
        return RubyFixnum.zero(getRuntime());
    }

    public IRubyObject seek(IRubyObject[] args) {
        long amount = ((RubyNumeric)args[0]).getLongValue();
        int whence = IOHandler.SEEK_SET;
        if (args.length > 1) {
            whence = (int)(((RubyNumeric)args[1]).getLongValue());
        }
        if (whence == IOHandler.SEEK_CUR) {
            pos += amount;
        } else if (whence == IOHandler.SEEK_END) {
            pos = internal.length() + amount;
        } else {
            pos = amount;
        }
        return RubyFixnum.zero(getRuntime());
    }

    public IRubyObject size() {
        return getRuntime().newFixnum(internal.length());
    }

    public IRubyObject string() {
        return RubyString.newString(getRuntime(),internal);
    }

    public IRubyObject set_string(RubyString arg) {
        return reopen(arg);
    }

    public IRubyObject sync() {
        return getRuntime().getTrue();
    }

    public IRubyObject set_sync(IRubyObject args) {
        return args;
    }

    public IRubyObject syswrite(IRubyObject args) {
        return write(args);
    }

    public IRubyObject truncate(RubyFixnum args) {
        int len = (int) args.getLongValue();
        internal.length(len);
        return RubyFixnum.zero(getRuntime());
    }

    public IRubyObject ungetc(RubyFixnum args) {
        internal.insert((int)pos,(int)args.getLongValue());
        return getRuntime().getNil();
    }

    public IRubyObject write(IRubyObject args) {
        String obj = args.toString();
        append(args);
        return getRuntime().newFixnum(obj.length());
    }
}
