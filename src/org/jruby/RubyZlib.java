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

import java.io.InputStream;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;

import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;

import org.jruby.util.IOInputStream;
import org.jruby.util.IOOutputStream;

public class RubyZlib {
    /** Create the Zlib module and add it to the Ruby runtime.
     * 
     */
    public static RubyModule createZlibModule(IRuby runtime) {
        RubyModule result = runtime.defineModule("Zlib");

        RubyClass gzfile = result.defineClassUnder("GzipFile", runtime.getObject());
        CallbackFactory callbackFactory = runtime.callbackFactory(RubyGzipFile.class);
        gzfile.defineSingletonMethod("wrap", callbackFactory.getSingletonMethod("wrap", RubyGzipFile.class, IRubyObject.class));
        gzfile.defineSingletonMethod("new", callbackFactory.getSingletonMethod("newCreate"));
        gzfile.defineMethod("os_code", callbackFactory.getMethod("os_code"));
        gzfile.defineMethod("closed?", callbackFactory.getMethod("closed_p"));
        gzfile.defineMethod("orig_name", callbackFactory.getMethod("orig_name"));
        gzfile.defineMethod("to_io", callbackFactory.getMethod("to_io"));
        gzfile.defineMethod("finish", callbackFactory.getMethod("finish"));
        gzfile.defineMethod("comment", callbackFactory.getMethod("comment"));
        gzfile.defineMethod("crc", callbackFactory.getMethod("crc"));
        gzfile.defineMethod("mtime", callbackFactory.getMethod("mtime"));
        gzfile.defineMethod("sync", callbackFactory.getMethod("sync"));
        gzfile.defineMethod("close", callbackFactory.getMethod("close"));
        gzfile.defineMethod("level", callbackFactory.getMethod("level"));
        gzfile.defineMethod("sync=", callbackFactory.getMethod("set_sync", IRubyObject.class));

        RubyClass gzreader = result.defineClassUnder("GzipReader", gzfile);
        gzreader.includeModule(runtime.getModule("Enumerable"));
        CallbackFactory callbackFactory2 = runtime.callbackFactory(RubyGzipReader.class);
        gzreader.defineSingletonMethod("open", callbackFactory2.getSingletonMethod("open", RubyString.class));
        gzreader.defineSingletonMethod("new", callbackFactory2.getOptSingletonMethod("newCreate"));
        gzreader.defineMethod("initialize", callbackFactory2.getMethod("initialize", IRubyObject.class));
        gzreader.defineMethod("rewind", callbackFactory2.getMethod("rewind"));
        gzreader.defineMethod("lineno", callbackFactory2.getMethod("lineno"));
        gzreader.defineMethod("readline", callbackFactory2.getMethod("readline"));
        gzreader.defineMethod("read", callbackFactory2.getOptMethod("read"));
        gzreader.defineMethod("lineno=", callbackFactory2.getMethod("set_lineno", RubyNumeric.class));
        gzreader.defineMethod("pos", callbackFactory2.getMethod("pos"));
        gzreader.defineMethod("readchar", callbackFactory2.getMethod("readchar"));
        gzreader.defineMethod("readlines", callbackFactory2.getOptMethod("readlines"));
        gzreader.defineMethod("each_byte", callbackFactory2.getMethod("each_byte"));
        gzreader.defineMethod("getc", callbackFactory2.getMethod("getc"));
        gzreader.defineMethod("eof", callbackFactory2.getMethod("eof"));
        gzreader.defineMethod("ungetc", callbackFactory2.getMethod("ungetc", RubyNumeric.class));
        gzreader.defineMethod("each", callbackFactory2.getOptMethod("each"));
        gzreader.defineMethod("unused", callbackFactory2.getMethod("unused"));
        gzreader.defineMethod("eof?", callbackFactory2.getMethod("eof_p"));
        gzreader.defineMethod("gets", callbackFactory2.getOptMethod("gets"));
        gzreader.defineMethod("tell", callbackFactory2.getMethod("tell"));

        RubyClass gzwriter = result.defineClassUnder("GzipWriter", gzfile);
        CallbackFactory callbackFactory3 = runtime.callbackFactory(RubyGzipWriter.class);
        gzwriter.defineSingletonMethod("open", callbackFactory3.getOptSingletonMethod("open"));
        gzwriter.defineSingletonMethod("new", callbackFactory3.getOptSingletonMethod("newCreate"));
        gzwriter.defineMethod("initialize", callbackFactory3.getOptMethod("initialize2"));
        gzwriter.defineMethod("<<", callbackFactory3.getMethod("append", IRubyObject.class));
        gzwriter.defineMethod("printf", callbackFactory3.getOptMethod("printf"));
        gzwriter.defineMethod("pos", callbackFactory3.getMethod("pos"));
        gzwriter.defineMethod("orig_name=", callbackFactory3.getMethod("set_orig_name", RubyString.class));
        gzwriter.defineMethod("putc", callbackFactory3.getMethod("putc", RubyNumeric.class));
        gzwriter.defineMethod("comment=", callbackFactory3.getMethod("set_comment", RubyString.class));
        gzwriter.defineMethod("puts", callbackFactory3.getOptMethod("puts"));
        gzwriter.defineMethod("flush", callbackFactory3.getOptMethod("flush"));
        gzwriter.defineMethod("mtime=", callbackFactory3.getMethod("set_mtime", IRubyObject.class));
        gzwriter.defineMethod("tell", callbackFactory3.getMethod("tell"));
        gzwriter.defineMethod("write", callbackFactory3.getMethod("write", IRubyObject.class));

        return result;
    }

    public static class RubyGzipFile extends RubyObject {
        public static IRubyObject wrap(IRubyObject recv, RubyGzipFile io, IRubyObject proc) throws IOException {
            if (!proc.isNil()) {
                try {
                    ((RubyProc)proc).call(new IRubyObject[]{io});
                } finally {
                    if (!io.isClosed()) {
                        io.close();
                    }
                }
                return recv.getRuntime().getNil();
            }
            
            return io;
        }

        public static RubyGzipFile newCreate(IRubyObject recv) {
            RubyGzipFile result = new RubyGzipFile(recv.getRuntime(), (RubyClass) recv);
            result.callInit(new IRubyObject[0]);
            return result;
        }

        protected boolean closed = false;
        protected boolean finished = false;
        private int os_code = 255;
        private int level = -1;
        private String orig_name;
        private String comment;
        protected IRubyObject realIo;
        private IRubyObject mtime;

        public RubyGzipFile(IRuby runtime, RubyClass type) {
            super(runtime, type);
            mtime = runtime.getNil();
        }
        
        public IRubyObject os_code() {
            return getRuntime().newFixnum(os_code);
        }
        
        public IRubyObject closed_p() {
            return closed ? getRuntime().getTrue() : getRuntime().getFalse();
        }
        
        protected boolean isClosed() {
            return closed;
        }
        
        public IRubyObject orig_name() {
            return orig_name == null ? getRuntime().getNil() : getRuntime().newString(orig_name);
        }
        
        public Object to_io() {
            return realIo;
        }
        
        public IRubyObject comment() {
            return comment == null ? getRuntime().getNil() : getRuntime().newString(comment);
        }
        
        public IRubyObject crc() {
            return RubyFixnum.zero(getRuntime());
        }
        
        public IRubyObject mtime() {
            return mtime;
        }
        
        public IRubyObject sync() {
            return getRuntime().getNil();
        }
        
        public IRubyObject finish() throws IOException {
            if (!finished) {
                //io.finish();
            }
            finished = true;
            return realIo;
        }

        public IRubyObject close() throws IOException {
            return null;
        }
        
        public IRubyObject level() {
            return getRuntime().newFixnum(level);
        }
        
        public IRubyObject set_sync(IRubyObject ignored) {
            return getRuntime().getNil();
        }
    }

    public static class RubyGzipReader extends RubyGzipFile {
        private static RubyGzipReader newInstance(IRubyObject recv, IRubyObject[] args) {
            RubyGzipReader result = new RubyGzipReader(recv.getRuntime(), recv.getRuntime().getModule("Zlib").getClass("GzipReader"));
            result.callInit(args);
            return result;
        }

        public static RubyGzipReader newCreate(IRubyObject recv, IRubyObject[] args) {
            RubyGzipReader result = new RubyGzipReader(recv.getRuntime(), (RubyClass)recv);
            result.callInit(args);
            return result;
        }

        public static IRubyObject open(IRubyObject recv, RubyString filename) throws IOException {
            RubyObject proc = (recv.getRuntime().getCurrentContext().isBlockGiven()) ? (RubyObject)recv.getRuntime().newProc() : (RubyObject)recv.getRuntime().getNil();
            RubyGzipReader io = newInstance(recv,new IRubyObject[]{recv.getRuntime().getClass("File").callMethod("open",new IRubyObject[]{filename,recv.getRuntime().newString("rb")})});
            
            return RubyGzipFile.wrap(recv, io, proc);
        }

        public RubyGzipReader(IRuby runtime, RubyClass type) {
            super(runtime, type);
        }
        
        private int line;
        private InputStream io;
        
        public IRubyObject initialize(IRubyObject io) throws IOException {
            realIo = io;
            this.io = new GZIPInputStream(new IOInputStream(io));
            line = 1;
            
            return this;
        }
        
        public IRubyObject rewind() {
            return getRuntime().getNil();
        }
        
        public IRubyObject lineno() {
            return getRuntime().newFixnum(line);
        }

        public IRubyObject readline() throws IOException {
            IRubyObject dst = gets(new IRubyObject[0]);
            if (dst.isNil()) {
                throw getRuntime().newEOFError();
            }
            return dst;
        }

        public IRubyObject internalGets(IRubyObject[] args) throws IOException {
            String sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getValue().toString();
            if (args.length > 0) {
                sep = args[0].toString();
            }
            return internalSepGets(sep);
        }

        private IRubyObject internalSepGets(String sep) throws IOException {
            StringBuffer result = new StringBuffer();
            char ce = (char) io.read();
            while (ce != -1 && sep.indexOf(ce) == -1) {
                result.append((char) ce);
                ce = (char) io.read();
            }
            line++;
            return getRuntime().newString(result.append(sep).toString());
        }

        public IRubyObject gets(IRubyObject[] args) throws IOException {
            IRubyObject result = internalGets(args);
            if (!result.isNil()) {
                getRuntime().getCurrentContext().setLastline(result);
            }
            return result;
        }

        private final static int BUFF_SIZE = 4096;
        public IRubyObject read(IRubyObject[] args) throws IOException {
            if (args.length == 0 || args[0].isNil()) {
                StringBuffer val = new StringBuffer();
                byte[] buffer = new byte[BUFF_SIZE];
                int read = io.read(buffer);
                while (read != -1) {
                    val.append(new String(buffer,0,read));
                    read = io.read(buffer);
                }
                return getRuntime().newString(val.toString());
            } 

            int len = RubyNumeric.fix2int(args[0]);
            if (len < 0) {
            	throw getRuntime().newArgumentError("negative length " + len + " given");
            } else if (len > 0) {
            	byte[] buffer = new byte[len];
            	int toRead = len;
            	int offset = 0;
            	int read = 0;
            	while (toRead > 0) {
            		read = io.read(buffer,offset,toRead);
            		if (read == -1) {
            			break;
            		}
            		toRead -= read;
            		offset += read;
            	}
            	return getRuntime().newString(new String(buffer,0,len-toRead, "PLAIN"));
            }
                
            return getRuntime().newString("");
        }

        public IRubyObject set_lineno(RubyNumeric lineArg) {
            line = RubyNumeric.fix2int(lineArg);
            return lineArg;
        }

        public IRubyObject pos() {
            return RubyFixnum.zero(getRuntime());
        }
        
        public IRubyObject readchar() throws IOException {
            int value = io.read();
            if (value == -1) {
                throw getRuntime().newEOFError();
            }
            return getRuntime().newFixnum(value);
        }

        public IRubyObject getc() throws IOException {
            int value = io.read();
            return value == -1 ? getRuntime().getNil() : getRuntime().newFixnum(value);
        }

        private boolean isEof() throws IOException {
            return ((GZIPInputStream)io).available() != 1;
        }

        public IRubyObject close() throws IOException {
            if (!closed) {
                io.close();
            }
            this.closed = true;
            return getRuntime().getNil();
        }
        
        public IRubyObject eof() throws IOException {
            return isEof() ? getRuntime().getTrue() : getRuntime().getFalse();
        }

        public IRubyObject eof_p() throws IOException {
            return eof();
        }

        public IRubyObject unused() {
            return getRuntime().getNil();
        }

        public IRubyObject tell() {
            return getRuntime().getNil();
        }

        public IRubyObject each(IRubyObject[] args) throws IOException {
            String sep = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getValue().toString();
            
            if (args.length > 0 && !args[0].isNil()) {
                sep = args[0].toString();
            }
            
            while (!isEof()) {
                getRuntime().getCurrentContext().yield(internalSepGets(sep));
            }
            
            return getRuntime().getNil();
        }
    
        public IRubyObject ungetc(RubyNumeric arg) {
            return getRuntime().getNil();
        }

        public IRubyObject readlines(IRubyObject[] args) throws IOException {
            List array = new ArrayList();
            
            if (args.length != 0 && args[0].isNil()) {
                array.add(read(new IRubyObject[0]));
            } else {
                String seperator = ((RubyString)getRuntime().getGlobalVariables().get("$/")).getValue().toString();
                if (args.length > 0) {
                    seperator = args[0].toString();
                }
                while (!isEof()) {
                    array.add(internalSepGets(seperator));
                }
            }
            return getRuntime().newArray(array);
        }

        public IRubyObject each_byte() throws IOException {
            int value = io.read();
            
            while (value != -1) {
                getRuntime().getCurrentContext().yield(getRuntime().newFixnum(value));
                value = io.read();
            }
            
            return getRuntime().getNil();
        }
    }

    public static class RubyGzipWriter extends RubyGzipFile {
        private static RubyGzipWriter newInstance(IRubyObject recv, IRubyObject[] args) {
            RubyGzipWriter result = new RubyGzipWriter(recv.getRuntime(), recv.getRuntime().getModule("Zlib").getClass("GzipWriter"));
            result.callInit(args);
            return result;
        }

        public static RubyGzipWriter newCreate(IRubyObject recv, IRubyObject[] args) {
            RubyGzipWriter result = new RubyGzipWriter(recv.getRuntime(), (RubyClass)recv);
            result.callInit(args);
            return result;
        }
        public static IRubyObject open(IRubyObject recv, IRubyObject[] args) throws IOException {
            IRubyObject level = recv.getRuntime().getNil();
            IRubyObject strategy = recv.getRuntime().getNil();
            if (args.length>1) {
                level = args[1];
                if (args.length>2) {
                    strategy = args[2];
                }
            }

            RubyObject proc = (recv.getRuntime().getCurrentContext().isBlockGiven()) ? (RubyObject)recv.getRuntime().newProc() : (RubyObject)recv.getRuntime().getNil();
            RubyGzipWriter io = newInstance(recv,new IRubyObject[]{recv.getRuntime().getClass("File").callMethod("open",new IRubyObject[]{args[0],recv.getRuntime().newString("wb")}),level,strategy});
            return RubyGzipFile.wrap(recv, io, proc);
        }

        public RubyGzipWriter(IRuby runtime, RubyClass type) {
            super(runtime, type);
        }

        private GZIPOutputStream io;
        public IRubyObject initialize2(IRubyObject[] args) throws IOException {
            realIo = (RubyObject)args[0];
            this.io = new GZIPOutputStream(new IOOutputStream(args[0]));
            
            return this;
        }

        public IRubyObject close() throws IOException {
            if (!closed) {
                io.close();
            }
            this.closed = true;
            
            return getRuntime().getNil();
        }

        public IRubyObject append(IRubyObject p1) throws IOException {
            this.write(p1);
            return this;
        }

        public IRubyObject printf(IRubyObject[] args) throws IOException {
            write(RubyKernel.sprintf(this, args));
            return getRuntime().getNil();
        }

        public IRubyObject print(IRubyObject[] args) throws IOException {
            if (args.length != 0) {
                for (int i = 0, j = args.length; i < j; i++) {
                    write(args[i]);
                }
            }
            
            IRubyObject sep = getRuntime().getGlobalVariables().get("$\\");
            if (!sep.isNil()) {
                write(sep);
            }
            
            return getRuntime().getNil();
        }

        public IRubyObject pos() {
            return getRuntime().getNil();
        }

        public IRubyObject set_orig_name(RubyString ignored) {
            return getRuntime().getNil();
        }

        public IRubyObject set_comment(RubyString ignored) {
            return getRuntime().getNil();
        }

        public IRubyObject putc(RubyNumeric p1) throws IOException {
            io.write(RubyNumeric.fix2int(p1));
            return p1;
        }
        
        public IRubyObject puts(IRubyObject[] args) throws IOException {
            RubyStringIO sio = (RubyStringIO)RubyStringIO.newInstance(this, new IRubyObject[0]);
            sio.puts(args);
            write(sio.string());
            
            return getRuntime().getNil();
        }

        public IRubyObject finish() throws IOException {
            if (!finished) {
                io.finish();
            }
            finished = true;
            return realIo;
        }

        public IRubyObject flush(IRubyObject[] args) throws IOException {
            if (args.length == 0 || args[0].isNil() || RubyNumeric.fix2int(args[0]) != 0) { // Zlib::NO_FLUSH
                io.flush();
            }
            return getRuntime().getNil();
        }

        public IRubyObject set_mtime(IRubyObject ignored) {
            return getRuntime().getNil();
        }

        public IRubyObject tell() {
            return getRuntime().getNil();
        }

        public IRubyObject write(IRubyObject p1) throws IOException {
            String str = p1.toString();
            io.write(str.getBytes("PLAIN"));
            return getRuntime().newFixnum(str.length());
        }
    }
}
