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

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.IOHandler;

public class RubyStringIO extends RubyObject {
    public static RubyClass createStringIOClass(final IRuby runtime) {
        final RubyClass stringIOClass = runtime.defineClass("StringIO",runtime.getObject());
        final CallbackFactory callbackFactory = runtime.callbackFactory(RubyStringIO.class);
        stringIOClass.defineSingletonMethod("open", callbackFactory.getOptSingletonMethod("open"));
        stringIOClass.defineSingletonMethod("new", callbackFactory.getOptSingletonMethod("newInstance"));
        stringIOClass.defineMethod("initialize", callbackFactory.getOptMethod("initialize"));
        stringIOClass.defineMethod("<<", callbackFactory.getMethod("append",IRubyObject.class));
        stringIOClass.defineMethod("binmode", callbackFactory.getMethod("binmode"));
        stringIOClass.defineMethod("close", callbackFactory.getMethod("close"));
        stringIOClass.defineMethod("closed?", callbackFactory.getMethod("closed_p"));
        stringIOClass.defineMethod("close_read", callbackFactory.getMethod("close_read"));
        stringIOClass.defineMethod("closed_read?", callbackFactory.getMethod("closed_read_p"));
        stringIOClass.defineMethod("close_write", callbackFactory.getMethod("close_write"));
        stringIOClass.defineMethod("closed_write?", callbackFactory.getMethod("closed_write_p"));
        stringIOClass.defineMethod("each", callbackFactory.getOptMethod("each"));
        stringIOClass.defineMethod("each_byte", callbackFactory.getMethod("each_byte"));
        stringIOClass.defineMethod("each_line", callbackFactory.getMethod("each_line"));
        stringIOClass.defineMethod("eof", callbackFactory.getMethod("eof"));
        stringIOClass.defineMethod("eof?", callbackFactory.getMethod("eof_p"));
        stringIOClass.defineMethod("fcntl", callbackFactory.getMethod("fcntl"));
        stringIOClass.defineMethod("fileno", callbackFactory.getMethod("fileno"));
        stringIOClass.defineMethod("flush", callbackFactory.getMethod("flush"));
        stringIOClass.defineMethod("fsync", callbackFactory.getMethod("fsync"));
        stringIOClass.defineMethod("getc", callbackFactory.getMethod("getc"));
        stringIOClass.defineMethod("gets", callbackFactory.getOptMethod("gets"));
        stringIOClass.defineMethod("isatty", callbackFactory.getMethod("isatty"));
        stringIOClass.defineMethod("tty?", callbackFactory.getMethod("tty_p"));
        stringIOClass.defineMethod("length", callbackFactory.getMethod("length"));
        stringIOClass.defineMethod("lineno", callbackFactory.getMethod("lineno"));
        stringIOClass.defineMethod("lineno=", callbackFactory.getMethod("set_lineno", RubyFixnum.class));
        stringIOClass.defineMethod("path", callbackFactory.getMethod("path"));
        stringIOClass.defineMethod("pid", callbackFactory.getMethod("pid"));
        stringIOClass.defineMethod("pos", callbackFactory.getMethod("pos"));
        stringIOClass.defineMethod("tell", callbackFactory.getMethod("tell"));
        stringIOClass.defineMethod("pos=", callbackFactory.getMethod("set_pos", RubyFixnum.class));
        stringIOClass.defineMethod("print", callbackFactory.getOptMethod("print"));
        stringIOClass.defineMethod("printf", callbackFactory.getOptMethod("printf"));
        stringIOClass.defineMethod("putc", callbackFactory.getMethod("putc", IRubyObject.class));
        stringIOClass.defineMethod("puts", callbackFactory.getOptMethod("puts"));
        stringIOClass.defineMethod("read", callbackFactory.getOptMethod("read"));
        stringIOClass.defineMethod("readchar", callbackFactory.getMethod("readchar"));
        stringIOClass.defineMethod("readline", callbackFactory.getOptMethod("readline"));
        stringIOClass.defineMethod("readlines", callbackFactory.getOptMethod("readlines"));
        stringIOClass.defineMethod("reopen", callbackFactory.getMethod("reopen", IRubyObject.class));
        stringIOClass.defineMethod("rewind", callbackFactory.getMethod("rewind"));
        stringIOClass.defineMethod("seek", callbackFactory.getOptMethod("seek"));
        stringIOClass.defineMethod("size", callbackFactory.getMethod("size"));
        stringIOClass.defineMethod("string", callbackFactory.getMethod("string"));
        stringIOClass.defineMethod("string=", callbackFactory.getMethod("set_string",RubyString.class));
        stringIOClass.defineMethod("sync", callbackFactory.getMethod("sync"));
        stringIOClass.defineMethod("sync=", callbackFactory.getMethod("set_sync", IRubyObject.class));
        stringIOClass.defineMethod("syswrite", callbackFactory.getMethod("syswrite", IRubyObject.class));
        stringIOClass.defineMethod("truncate", callbackFactory.getMethod("truncate", RubyFixnum.class));
        stringIOClass.defineMethod("ungetc", callbackFactory.getMethod("ungetc", RubyFixnum.class));
        stringIOClass.defineMethod("write", callbackFactory.getMethod("write", IRubyObject.class));

        return stringIOClass;
    }

    public static IRubyObject newInstance(IRubyObject recv, IRubyObject[] args) {
        RubyStringIO result = new RubyStringIO(recv.getRuntime());
        result.callInit(args);
        return result;
    }

    public static IRubyObject open(IRubyObject recv, IRubyObject[] args) {
        RubyString str = recv.getRuntime().newString("");
        IRubyObject mode = recv.getRuntime().getNil();
        if (args.length > 0) {
            str = (RubyString)args[0];
            if (args.length > 1) {
                mode = args[1];
            }
        }
        RubyStringIO strio = (RubyStringIO)newInstance(recv,new IRubyObject[]{str,mode});
        IRubyObject val = strio;
        ThreadContext tc = recv.getRuntime().getCurrentContext();
        
        if (tc.isBlockGiven()) {
            try {
                val = tc.yield(strio);
            } finally {
                strio.close();
            }
        }
        return val;
    }


    protected RubyStringIO(IRuby runtime) {
        super(runtime, runtime.getClass("StringIO"));
    }

    private long pos = 0L;
    private int lineno = 0;
    private boolean eof = false;
    private StringBuffer internal;
    private boolean closedRead = false;
    private boolean closedWrite = false;

    public IRubyObject initialize(IRubyObject[] args) {
        internal = new StringBuffer();
        if (checkArgumentCount(args, 0, 2) > 0) {
            internal.append(((RubyString)args[0]).getValue());
        }
        return this;
    }

    public IRubyObject append(IRubyObject obj) {
        String val = obj.toString();
        internal.replace((int)pos,(int)(pos+val.length()),val);
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

   public IRubyObject each(IRubyObject[] args) {
       IRubyObject line = gets(args);
       while (!line.isNil()) {
           getRuntime().getCurrentContext().yield(line);
           line = gets(args);
       }
       return this;
   }

    public IRubyObject each_byte() {
        getRuntime().newString(internal.substring((int)pos)).each_byte();
        return this;
    }

    public IRubyObject each_line() {
        return each(new RubyObject[0]);
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
        return getRuntime().newFixnum(internal.charAt((int)pos++));
    }

    public IRubyObject internalGets(IRubyObject[] args) {
        if (pos < internal.length() && !eof) {
            String sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getValue().toString();
            if (args.length>0) {
                if (args[0].isNil()) {
                    String buf = internal.substring((int)pos);
                    pos+=buf.length();
                    return getRuntime().newString(buf);
                }
                sep = args[0].toString();
            }
            int ix = internal.indexOf(sep,(int)pos);
            String add = sep;
            if (-1 == ix) {
                ix = internal.length();
                add = "";
            }
            String line = internal.substring((int)pos,ix)+add;
            pos = ix + add.length();
            lineno++;
            return getRuntime().newString(line);
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

    public IRubyObject puts(IRubyObject[] obj) {
        for (int i=0,j=obj.length;i<j;i++) {
            append(obj[i]);
            internal.replace((int)pos,(int)(++pos),("\n"));
        }
        return getRuntime().getNil();
    }

    public IRubyObject read(IRubyObject[] args) {
        String buf = null;
        if (!(pos >= internal.length() || eof)) {
            if (args.length > 0 && !args[0].isNil()) {
                int end = ((int)pos) + RubyNumeric.fix2int(args[0]);
                if (end > internal.length()) {
                    buf = internal.substring((int)pos);
                } else {
                    buf = internal.substring((int)pos,end);
                }
            } else {
                buf = internal.substring((int)pos);
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
                ((RubyString)args[1]).cat(buf);
                ret = args[1];
            } else {
                ret = getRuntime().newString(buf);
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
            internal = new StringBuffer(((RubyStringIO)str).internal.toString());
        } else {
            pos = 0L;
            lineno = 0;
            eof = false;
            internal = new StringBuffer();
            internal.append(((RubyString)str).getValue());
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
        return getRuntime().newString(internal.toString());
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
        internal.delete(len,internal.length());
        return RubyFixnum.zero(getRuntime());
    }

    public IRubyObject ungetc(RubyFixnum args) {
        char val = (char) args.getLongValue();
        internal.insert((int)pos,val);
        return getRuntime().getNil();
    }

    public IRubyObject write(IRubyObject args) {
        String obj = args.toString();
        append(args);
        return getRuntime().newFixnum(obj.length());
    }
}
