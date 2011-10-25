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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
 * Copyright (C) 2008-2009 Joseph LaFata <joe@quibb.org>
 * Copyright (C) 2007-2011 Charles Oliver Nutter <headius@headius.com>
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

import java.io.File;
import java.io.IOException;
import static org.jruby.RubyEnumerator.enumeratorize;

import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import jnr.posix.FileStat;
import jnr.posix.util.Platform;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.runtime.Block;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import static org.jruby.CompatVersion.*;

public class RubyArgsFile {
    public static void initArgsFile(final Ruby runtime) {
        RubyObject argsFile = new RubyObject(runtime, runtime.getObject());

        runtime.getEnumerable().extend_object(argsFile);

        runtime.setArgsFile(argsFile);
        runtime.getGlobalVariables().defineReadonly("$<", new IAccessor() {
            public IRubyObject getValue() {
                return runtime.getArgsFile();
            }

            public IRubyObject setValue(IRubyObject newValue) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
        runtime.defineGlobalConstant("ARGF", argsFile);
        
        RubyClass argfClass = argsFile.getMetaClass();
        argfClass.defineAnnotatedMethods(RubyArgsFile.class);
        runtime.defineReadonlyVariable("$FILENAME", runtime.newString("-"));
    }

    @JRubyMethod(name = {"fileno", "to_i"})
    public static IRubyObject fileno(ThreadContext context, IRubyObject recv) {
        return ((RubyIO) getData(context, recv, "no stream").currentFile).fileno(context);
    }

    @JRubyMethod(name = "to_io")
    public static IRubyObject to_io(ThreadContext context, IRubyObject recv) {
        return getData(context, recv, "no stream").currentFile;
    }

    private static IRubyObject argf_getline(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        boolean retry = true;
        IRubyObject line = null;
        while(retry) {
            retry = false;
            if (!data.nextArgv(context)) {
                return context.getRuntime().getNil();
            }

            line = data.currentFile.callMethod(context, "gets", args);

            if (line.isNil() && data.nextFlag != NextFlag.NO) {
                argf_close(context, data.currentFile);
                data.nextFlag = NextFlag.YES;
                retry = true;
            }
        }

        if (!line.isNil()) {
            context.getRuntime().setCurrentLine(data.currentLineNumber);
        }

        return line;
    }

    /** Read a line.
     * 
     */
    @JRubyMethod(name = "gets", optional = 1)
    public static IRubyObject gets(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if(!data.nextArgv(context)) return context.getRuntime().getNil();

        IRubyObject line;
        if (!(data.currentFile instanceof RubyIO)) {
            line = data.currentFile.callMethod(context, "gets", args);
        } else {
            line = argf_getline(context, recv, args);
        }

        context.getCurrentScope().setLastLine(line);
        context.getRuntime().getGlobalVariables().set("$_", line);
        
        return line;
    }
    
    /** Read a line.
     * 
     */
    @JRubyMethod(name = "readline", optional = 1)
    public static IRubyObject readline(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject line = gets(context, recv, args);

        if (line.isNil()) {
            throw context.getRuntime().newEOFError();
        }
        
        return line;
    }

    @JRubyMethod(optional = 1)
    public static IRubyObject readlines(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        Ruby runtime = context.getRuntime();
        
        if (!data.nextArgv(context)) {
            return runtime.is1_9() ? runtime.newEmptyArray() : runtime.getNil();
        }
        if (!(data.currentFile instanceof RubyIO)) {
            return data.currentFile.callMethod(context, "readlines", args);
        }
        
        RubyArray ary = runtime.newArray();
        IRubyObject line;
        while(!(line = argf_getline(context, recv, args)).isNil()) {
            ary.append(line);
        }
        return ary;
    }

    @JRubyMethod(optional = 1)
    public static IRubyObject to_a(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        Ruby runtime = context.getRuntime();
        
        if (!data.nextArgv(context)) {
            return runtime.is1_9() ? runtime.newEmptyArray() : runtime.getNil();
        }

        if (!(data.currentFile instanceof RubyIO)) {
            return data.currentFile.callMethod(context, "to_a", args);
        }
        
        RubyArray ary = runtime.newArray();
        IRubyObject line;
        while(!(line = argf_getline(context, recv, args)).isNil()) {
            ary.append(line);
        }
        return ary;
    }
    
    public static IRubyObject each_byte(ThreadContext context, IRubyObject recv, Block block) {
        IRubyObject bt;

        while(!(bt = getc(context, recv)).isNil()) {
            block.yield(context, bt);
        }

        return recv;
    }

    @JRubyMethod(optional = 1)
    public static IRubyObject each_byte(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_byte(context, recv, block) : enumeratorize(context.getRuntime(), recv, "each_byte");
    }

    @JRubyMethod(optional = 1)
    public static IRubyObject bytes(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_byte(context, recv, block) : enumeratorize(context.getRuntime(), recv, "bytes");
    }

    @JRubyMethod
    public static IRubyObject each_char(final ThreadContext context, IRubyObject recv, Block block) {
        return block.isGiven() ? each_charCommon(context, recv, block) : enumeratorize(context.getRuntime(), recv, "each_char");
    }

    @JRubyMethod
    public static IRubyObject chars(final ThreadContext context, IRubyObject recv, Block block) {
        return block.isGiven() ? each_charCommon(context, recv, block) : enumeratorize(context.getRuntime(), recv, "chars");
    }

    public static IRubyObject each_charCommon(ThreadContext context, IRubyObject recv, Block block) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        Ruby runtime = context.getRuntime();
        IRubyObject ch;
        while(!(ch = getc(context, recv)).isNil()) {
            boolean cont = true;
            while(cont) {
                cont = false;
                byte c = (byte)RubyNumeric.fix2int(ch);
                int n = runtime.getKCode().getEncoding().length(c);
                IRubyObject file = data.currentFile;
                RubyString str = runtime.newString();
                str.setTaint(true);
                str.cat(c);

                while(--n > 0) {
                    if((ch = getc(context, recv)).isNil()) {
                        block.yield(context, str);
                        return recv;
                    }
                    if (data.currentFile != file) {
                        block.yield(context, str);
                        cont = true;
                        continue;
                    }
                    c = (byte)RubyNumeric.fix2int(ch);
                    str.cat(c);
                }
                block.yield(context, str);
            }
        }
        return recv;
    }

    /** Invoke a block for each line.
     *
     */
    @JRubyMethod(name = {"each_line", "each"}, optional = 1)
    public static IRubyObject each_line(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, recv, "each_line");
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if (!data.nextArgv(context)) return context.getRuntime().getNil();

        if (!(data.currentFile instanceof RubyIO)) {
            if (!data.nextArgv(context)) return recv;

            data.currentFile.callMethod(context, "each", new IRubyObject[0], block);
            data.nextFlag = NextFlag.YES;
        }
        IRubyObject str;
        while(!(str = argf_getline(context, recv, args)).isNil()) {
        	block.yield(context, str);
        }
        
        return recv;
    }

    @JRubyMethod(name = "each_line", optional = 1, compat = RUBY1_9)
    public static IRubyObject each_line19(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_line(context, recv, args, block) : enumeratorize(context.getRuntime(), recv, "each_line", args);
    }

    @JRubyMethod(name = "each", optional = 1, compat = RUBY1_9)
    public static IRubyObject each19(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_line(context, recv, args, block) : enumeratorize(context.getRuntime(), recv, "each", args);
    }

    @JRubyMethod(name = "file")
    public static IRubyObject file(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        data.nextArgv(context);

        return data.currentFile;
    }

    @JRubyMethod(name = "skip")
    public static IRubyObject skip(IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if (data.nextFlag != NextFlag.NO) {
            argf_close(recv.getRuntime().getCurrentContext(), data.currentFile);
            data.nextFlag = NextFlag.YES;
        }

        return recv;
    }

    public static void argf_close(ThreadContext context, IRubyObject file) {
        if(file instanceof RubyIO) {
            ((RubyIO)file).close2(context.getRuntime());
        } else {
            file.callMethod(context, "close");
        }
    }

    @JRubyMethod(name = "close")
    public static IRubyObject close(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        data.nextArgv(context);
        if (isClosed(context, data.currentFile)) throw context.getRuntime().newIOError("closed stream");
        
        argf_close(context, data.currentFile);

        if (data.nextFlag != NextFlag.NO) data.nextFlag = NextFlag.YES;

        data.currentLineNumber = 0;
        return recv;
    }

    @JRubyMethod(name = "closed?")
    public static IRubyObject closed_p(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        data.nextArgv(context);

        return RubyBoolean.newBoolean(context.getRuntime(), isClosed(context, data.currentFile));
    }
    
    private static boolean isClosed(ThreadContext context, IRubyObject currentFile) {
        boolean closed = false;

        if (!(currentFile instanceof RubyIO)) {
            closed = currentFile.callMethod(context, "closed?").isTrue();
        } else {
            closed = ((RubyIO)currentFile).closed_p(context).isTrue();
        }
        return closed;
    }

    @JRubyMethod(name = "binmode")
    public static IRubyObject binmode(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = getData(context, recv, "no stream");
        
        ((RubyIO)data.currentFile).binmode();
        return recv;
    }
    
    @JRubyMethod(name = "binmode?", compat = CompatVersion.RUBY1_9)
    public static IRubyObject op_binmode(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = getData(context, recv, "no stream");
        
        return ((RubyIO)data.currentFile).op_binmode(context);
    }

    @JRubyMethod(name = "lineno")
    public static IRubyObject lineno(ThreadContext context, IRubyObject recv) {
        return recv.getRuntime().newFixnum(ArgsFileData.getDataFrom(recv).currentLineNumber);
    }

    @JRubyMethod(name = "lineno=")
    public static IRubyObject lineno_set(ThreadContext context, IRubyObject recv, IRubyObject line) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        data.currentLineNumber = RubyNumeric.fix2int(line);
        context.getRuntime().setCurrentLine(data.currentLineNumber);
        return recv.getRuntime().getNil();
    }
    
    public static void setCurrentLineNumber(IRubyObject recv, int newLineNumber) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if (data != null) {
            data.currentLineNumber = newLineNumber;
        }
    }

    @JRubyMethod(name = "tell", alias = {"pos"})
    public static IRubyObject tell(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if(!data.nextArgv(context)) throw context.getRuntime().newArgumentError("no stream to tell");

        return ((RubyIO)data.currentFile).pos(context);
    }

    @JRubyMethod(name = "rewind")
    public static IRubyObject rewind(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = getData(context, recv, "no stream to rewind");
        
        RubyFixnum retVal = ((RubyIO)data.currentFile).rewind(context);
        ((RubyIO)data.currentFile).lineno_set(context, context.getRuntime().newFixnum(0));
        
        data.minLineNumber = 0;
        data.currentLineNumber = 0;
        return retVal;
    }

    @JRubyMethod(name = {"eof"})
    public static IRubyObject eof(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if(!data.initialized) return context.getRuntime().getTrue();
        if(!(data.currentFile instanceof RubyIO)) return data.currentFile.callMethod(context, "eof");

        return ((RubyIO) data.currentFile).eof_p(context);
    }

    @JRubyMethod(name = {"eof?"})
    public static IRubyObject eof_p(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if (!data.initialized) return context.getRuntime().getTrue();
        if (!(data.currentFile instanceof RubyIO)) return data.currentFile.callMethod(context, "eof?");

        return ((RubyIO) data.currentFile).eof_p(context);
    }

    @JRubyMethod(name = "pos=", required = 1)
    public static IRubyObject set_pos(ThreadContext context, IRubyObject recv, IRubyObject offset) {
        ArgsFileData data = getData(context, recv, "no stream to set position");

        return ((RubyIO)data.currentFile).pos_set(context, offset);
    }

    @JRubyMethod(name = "seek", required = 1, optional = 1)
    public static IRubyObject seek(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        ArgsFileData data = getData(context, recv, "no stream to seek");

        return ((RubyIO)data.currentFile).seek(context, args);
    }

    @JRubyMethod(name = "readchar")
    public static IRubyObject readchar(ThreadContext context, IRubyObject recv) {
        IRubyObject c = getc(context, recv);
        
        if (c.isNil()) throw context.getRuntime().newEOFError();

        return c;
    }

    @JRubyMethod(name = "getc")
    public static IRubyObject getc(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        while(true) {
            IRubyObject bt;

            if (!data.nextArgv(context)) return context.getRuntime().getNil();

            if (!(data.currentFile instanceof RubyFile)) {
                bt = data.currentFile.callMethod(context,"getc");
            } else {
                bt = ((RubyIO)data.currentFile).getc();
            }

            if (bt.isNil()) {
                data.nextFlag = NextFlag.YES;
                continue;
            }
            return bt;
        }
    }

    @JRubyMethod(name = "read", optional = 2)
    public static IRubyObject read(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.getRuntime();
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        IRubyObject tmp, str, length;
        long len = 0;

        if (args.length > 0) {
            length = args[0];
            str = args.length > 1 ? args[1] : runtime.getNil();
        } else {
            length = runtime.getNil();
            str = runtime.getNil();
        }

        if (!length.isNil()) len = RubyNumeric.num2long(length);

        if (!str.isNil()) {
            str = str.convertToString();
            ((RubyString)str).modify();
            ((RubyString)str).getByteList().length(0);
            args[1] = runtime.getNil();
        }

        while(true) {
            if (!data.nextArgv(context)) return str;

            if (!(data.currentFile instanceof RubyIO)) {
                tmp = data.currentFile.callMethod(context, "read", args);
            } else {
                tmp = ((RubyIO)data.currentFile).read(args);
            }

            if (str.isNil()) {
                str = tmp;
            } else if (!tmp.isNil()) {
                ((RubyString)str).append(tmp);
            }

            if (tmp.isNil() || length.isNil()) {
                if(data.nextFlag != NextFlag.NO) {
                    argf_close(context, data.currentFile);
                    data.nextFlag = NextFlag.YES;
                    continue;
                }
            } else if(args.length >= 1) {
                if (((RubyString)str).getByteList().length() < len) {
                    len -= ((RubyString)str).getByteList().length();
                    args[0] = runtime.newFixnum(len);
                    continue;
                }
            }
            return str;
        }
    }

    @JRubyMethod(name = "filename", alias = {"path"})
    public static IRubyObject filename(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        data.nextArgv(context);
        return context.getRuntime().getGlobalVariables().get("$FILENAME");
    }

    @JRubyMethod(name = "to_s") 
    public static IRubyObject to_s(IRubyObject recv) {
        return recv.getRuntime().newString("ARGF");
    }
    
    private static ArgsFileData getData(ThreadContext context, IRubyObject recv, String errorMessage) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        
        if (!data.nextArgv(context)) throw context.getRuntime().newArgumentError(errorMessage);

        return data;
    }
    public enum NextFlag { NO, MAYBE, YES }
    
    private static final class ArgsFileData {
        private final Ruby runtime;
        public ArgsFileData(Ruby runtime) {
            this.runtime = runtime;
            this.currentFile = runtime.getNil();
        }

        public boolean nextArgv(ThreadContext context) {
            GlobalVariables gvars = runtime.getGlobalVariables();
            RubyArray args = (RubyArray)gvars.get("$*");
            RubyString mainFilename = (RubyString)gvars.get("$FILENAME");
            
            if (!initialized) initialize(args);

            switch (nextFlag) {
                case YES:
                    nextFlag = NextFlag.MAYBE;
                    if (args.getLength() > 0) {
                        nextArgvFile(context, args.shift(context), mainFilename, gvars);
                    } else {
                        nextFlag = NextFlag.YES;
                        return false;
                    }
                case NO:
                    currentFile = gvars.get("$stdin");
                    if(!mainFilename.asJavaString().equals("-")) {
                        runtime.defineReadonlyVariable("$FILENAME", runtime.newString("-"));
                    }
            }

            return true;
        }

        private void initialize(RubyArray args) {
            if (args.getLength() > 0) {
                nextFlag = NextFlag.YES;
            } else {
                nextFlag = NextFlag.NO;
            }
            initialized = true;
            currentLineNumber = 0;
        }

        private void nextArgvFile(ThreadContext context, IRubyObject nextArg, RubyString mainFilename, GlobalVariables gvars) throws RaiseException {
            RubyString filename = (RubyString)((RubyObject)nextArg).to_s();
            ByteList filenameBytes = filename.getByteList();
            if (!filename.op_equal(context, mainFilename).isTrue()) {
                runtime.defineReadonlyVariable("$FILENAME", filename);
            }

            if (filenameBytes.length() == 1 && filenameBytes.get(0) == '-') {
                currentFile = gvars.get("$stdin");
            } else {
                currentFile = RubyFile.open(context, filename);
                String extension = runtime.getInstanceConfig().getInPlaceBackupExtension();
                if (extension != null) {
                    if (Platform.IS_WINDOWS) {
                        inplaceEditWindows(context, filename.asJavaString(), extension);
                    } else {
                        inplaceEdit(context, filename.asJavaString(), extension);
                    }
                }
                minLineNumber = currentLineNumber;
                currentFile.callMethod(context, "lineno=", runtime.newFixnum(currentLineNumber));
            }
        }

        public static ArgsFileData getDataFrom(IRubyObject recv) {
            ArgsFileData data = (ArgsFileData)recv.dataGetStruct();

            if (data == null) {
                data = new ArgsFileData(recv.getRuntime());
                recv.dataWrapStruct(data);
            }

            return data;
        }

        private void createNewFile(File file) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                throw runtime.newIOErrorFromException(ex);
            }
        }

        private void inplaceEditWindows(ThreadContext context, String filename, String extension) throws RaiseException {
            File file = new File(filename);

            if (!extension.equals("")) {
                String backup = filename + extension;
                File backupFile = new File(backup);

                ((RubyIO) currentFile).close(); // we can't rename a file while it's open in windows
                backupFile.delete();
                file.renameTo(backupFile);
                currentFile = RubyFile.open(context, runtime.newString(backup));
            } else {
                throw runtime.newIOError("Windows doesn't support inplace editing without a backup");
            }

            createNewFile(file);

            runtime.getGlobalVariables().set("$stdout", RubyFile.open(context, runtime.newString(filename), runtime.newString("w")));
        }

        private void inplaceEdit(ThreadContext context, String filename, String extension) throws RaiseException {
            File file = new File(filename);
            FileStat stat = runtime.getPosix().stat(filename);

            if (!extension.equals("")) {
                file.renameTo(new File(filename + extension));
            } else {
                file.delete();
            }

            createNewFile(file);

            runtime.getPosix().chmod(filename, stat.mode());
            runtime.getPosix().chown(filename, stat.uid(), stat.gid());
            runtime.getGlobalVariables().set("$stdout", (RubyIO) RubyFile.open(context, runtime.newString(filename), runtime.newString("w")));
        }

        public IRubyObject currentFile;
        public int currentLineNumber;
        public int minLineNumber;
        private boolean initialized = false;
        
        public NextFlag nextFlag = NextFlag.MAYBE;
    }
}
