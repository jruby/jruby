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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
 * Copyright (C) 2008-2009 Joseph LaFata <joe@quibb.org>
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
package org.jruby;

import java.io.File;
import java.io.IOException;

import jnr.posix.FileStat;
import jnr.posix.util.Platform;

import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.runtime.Visibility.PRIVATE;

import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.runtime.Block;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

public class RubyArgsFile extends RubyObject {

    public RubyArgsFile(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public static void initArgsFile(final Ruby runtime) {
        RubyClass argfClass = runtime.defineClass("ARGFClass", runtime.getObject(), ARGF_ALLOCATOR);
        argfClass.includeModule(runtime.getEnumerable());

        argfClass.defineAnnotatedMethods(RubyArgsFile.class);

        IRubyObject argsFile = argfClass.newInstance(runtime.getCurrentContext(), new IRubyObject[] { null }, (Block) null);

        runtime.setArgsFile(argsFile);
        runtime.getGlobalVariables().defineReadonly("$<", new ArgsFileAccessor(runtime), GlobalVariable.Scope.GLOBAL);
        runtime.defineGlobalConstant("ARGF", argsFile);
        runtime.defineReadonlyVariable("$FILENAME", runtime.newString("-"), GlobalVariable.Scope.GLOBAL);
    }


    private static class ArgsFileAccessor implements IAccessor {

        private final Ruby runtime;
        ArgsFileAccessor(Ruby runtime) { this.runtime = runtime; }

        public IRubyObject getValue() {
            return runtime.getArgsFile();
        }

        public IRubyObject setValue(IRubyObject newValue) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static final ObjectAllocator ARGF_ALLOCATOR = new ObjectAllocator() {
        @Override
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new RubyArgsFile(runtime, klass);
        }
    };

    @JRubyMethod(name = "initialize", visibility = PRIVATE, rest = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        final RubyArray argv;
        if (args.length == 1 && args[0] == null) {
            argv = runtime.getObject().getConstant("ARGV").convertToArray();
        } else {
            argv = runtime.newArray(args);
        }

        ArgsFileData data = new ArgsFileData(runtime, argv);
        this.dataWrapStruct(data);
        return this;
    }

    static final class ArgsFileData {

        private final Ruby runtime;
        private final RubyArray argv;
        public IRubyObject currentFile;
        public int currentLineNumber;
        public int minLineNumber;
        private boolean inited = false;
        public int next_p = 0;

        public ArgsFileData(Ruby runtime, RubyArray argv) {
            this.runtime = runtime;
            this.argv = argv;
            this.currentFile = runtime.getNil();
        }

        public boolean next_argv(ThreadContext context) {
            if (!inited) {
                if (argv.getLength() > 0) {
                    next_p = 1;
                } else {
                    next_p = -1;
                }
                inited = true;
                currentLineNumber = 0;
            }

            IRubyObject $FILENAME = runtime.getGlobalVariables().get("$FILENAME");

            if (next_p == 1) {
                next_p = 0;
                if (argv.getLength() > 0) {
                    final RubyString filename = argv.shift(context).convertToString();
                    if ( ! filename.op_equal(context, $FILENAME).isTrue() ) {
                        runtime.defineReadonlyVariable("$FILENAME", filename, GlobalVariable.Scope.GLOBAL);
                    }

                    if ( filenameEqlDash(filename) ) {
                        currentFile = runtime.getGlobalVariables().get("$stdin");
                    }
                    else {
                        currentFile = RubyFile.open(context, runtime.getFile(), new IRubyObject[]{ filename }, Block.NULL_BLOCK);
                        String extension = runtime.getInstanceConfig().getInPlaceBackupExtension();
                        if (extension != null) {
                            if (Platform.IS_WINDOWS) {
                                inplaceEditWindows(context, filename.asJavaString(), extension);
                            } else {
                                inplaceEdit(context, filename.asJavaString(), extension);
                            }
                        }
                        minLineNumber = currentLineNumber;
                        currentFile.callMethod(context, "lineno=", context.runtime.newFixnum(currentLineNumber));
                    }
                } else {
                    next_p = 1;
                    return false;
                }
            }
            else if (next_p == -1) {
                currentFile = runtime.getGlobalVariables().get("$stdin");
                if ( ! filenameEqlDash((RubyString) $FILENAME) ) {
                    runtime.defineReadonlyVariable("$FILENAME", runtime.newString("-"), GlobalVariable.Scope.GLOBAL);
                }
            }

            return true;
        }

        private static boolean filenameEqlDash(final RubyString filename) {
            final ByteList filenameBytes = filename.getByteList();
            return filenameBytes.length() == 1 && filenameBytes.get(0) == '-';
        }

        public static ArgsFileData getDataFrom(IRubyObject recv) {
            ArgsFileData data = (ArgsFileData) recv.dataGetStruct();

            if (data == null) {
                final Ruby runtime = recv.getRuntime();
                data = new ArgsFileData(runtime, runtime.newEmptyArray());
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

            if (extension.length() > 0) {
                String backup = filename + extension;
                File backupFile = new File(backup);

                ((RubyIO) currentFile).close(); // we can't rename a file while it's open in windows
                backupFile.delete();
                file.renameTo(backupFile);
                currentFile = RubyFile.open(context, runtime.getFile(), //reopen
                        new IRubyObject[]{runtime.newString(backup)}, Block.NULL_BLOCK);
            } else {
                throw runtime.newIOError("Windows doesn't support inplace editing without a backup");
            }

            createNewFile(file);

            runtime.getGlobalVariables().set("$stdout", RubyFile.open(context, runtime.getFile(),
                    new IRubyObject[]{runtime.newString(filename), runtime.newString("w")}, Block.NULL_BLOCK));
        }

        private void inplaceEdit(ThreadContext context, String filename, String extension) throws RaiseException {
            File file = new File(filename);
            FileStat stat = runtime.getPosix().stat(filename);

            if (extension.length() > 0) {
                file.renameTo(new File(filename + extension));
            } else {
                file.delete();
            }

            createNewFile(file);

            runtime.getPosix().chmod(filename, stat.mode());
            runtime.getPosix().chown(filename, stat.uid(), stat.gid());
            runtime.getGlobalVariables().set("$stdout", (RubyIO) RubyFile.open(context, runtime.getFile(),
                    new IRubyObject[]{runtime.newString(filename), runtime.newString("w")}, Block.NULL_BLOCK));
        }
    }

    public static void setCurrentLineNumber(IRubyObject recv, int newLineNumber) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if (data != null) {
            data.currentLineNumber = newLineNumber;
        }
    }

    @JRubyMethod(name = "argv")
    public static IRubyObject argv(ThreadContext context, IRubyObject recv) {
        return ArgsFileData.getDataFrom(recv).argv;
    }

    @JRubyMethod(name = {"fileno", "to_i"})
    public static IRubyObject fileno(ThreadContext context, IRubyObject recv) {
        return ((RubyIO) getData(context, recv, "no stream").currentFile).fileno(context);
    }

    @JRubyMethod(name = "to_io")
    public static IRubyObject to_io(ThreadContext context, IRubyObject recv) {
        return getData(context, recv, "no stream").currentFile;
    }

    @JRubyMethod
    public static IRubyObject set_encoding(ThreadContext context, IRubyObject recv, IRubyObject encoding) {
        return ((RubyIO) getData(context, recv, "no stream to set encoding").currentFile).set_encoding(context, encoding);
    }

    @JRubyMethod
    public static IRubyObject set_encoding(ThreadContext context, IRubyObject recv, IRubyObject encoding, IRubyObject internalEncoding) {
        return ((RubyIO) getData(context, recv, "no stream to set encoding").currentFile).set_encoding(context, encoding, internalEncoding);
    }

    @JRubyMethod
    public static IRubyObject set_encoding(ThreadContext context, IRubyObject recv, IRubyObject encoding, IRubyObject internalEncoding, IRubyObject options) {
        return ((RubyIO) getData(context, recv, "no stream to set encoding").currentFile).set_encoding(context, encoding, internalEncoding, options);
    }

    @JRubyMethod
    public static IRubyObject internal_encoding(ThreadContext context, IRubyObject recv) {
        return ((RubyIO) getData(context, recv, "no stream to set encoding").currentFile).internal_encoding(context);
    }

    @JRubyMethod
    public static IRubyObject external_encoding(ThreadContext context, IRubyObject recv) {
        return ((RubyIO) getData(context, recv, "no stream to set encoding").currentFile).external_encoding(context);
    }

    // MRI: argf_getline
    private static IRubyObject argf_getline(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        boolean retry = true;
        IRubyObject line = null;
        while (retry) {
            retry = false;
            if ( ! data.next_argv(context) ) return context.nil;

            line = data.currentFile.callMethod(context, "gets", args);

            if ( line.isNil() && data.next_p != -1 ) {
                argf_close(context, data.currentFile);
                data.next_p = 1;
                retry = true;
            }
        }

        if ( line != null && ! line.isNil() ) {
            context.runtime.setCurrentLine(data.currentLineNumber);
        }

        return line;
    }

    // ARGF methods

    /** Read a line.
     *
     */
    @JRubyMethod(name = "gets", optional = 1, writes = FrameField.LASTLINE)
    public static IRubyObject gets(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject line = argf_getline(context, recv, args);
        context.setLastLine(line);

        return line;
    }

    /** Read a line.
     *
     */
    @JRubyMethod(name = "readline", optional = 1)
    public static IRubyObject readline(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject line = gets(context, recv, args);

        if (line.isNil()) throw context.runtime.newEOFError();

        return line;
    }

    @JRubyMethod(optional = 1)
    public static IRubyObject readlines(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        Ruby runtime = context.runtime;

        if (!data.next_argv(context)) {
            return runtime.newEmptyArray();
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
        Ruby runtime = context.runtime;

        if (!data.next_argv(context)) {
            return runtime.newEmptyArray();
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
        return block.isGiven() ? each_byte(context, recv, block) : enumeratorize(context.runtime, recv, "each_byte");
    }

    @JRubyMethod(optional = 1)
    public static IRubyObject bytes(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_byte(context, recv, block) : enumeratorize(context.runtime, recv, "bytes");
    }

    @JRubyMethod
    public static IRubyObject each_char(final ThreadContext context, IRubyObject recv, Block block) {
        return block.isGiven() ? each_charCommon(context, recv, block) : enumeratorize(context.runtime, recv, "each_char");
    }

    @JRubyMethod
    public static IRubyObject chars(final ThreadContext context, IRubyObject recv, Block block) {
        return block.isGiven() ? each_charCommon(context, recv, block) : enumeratorize(context.runtime, recv, "chars");
    }

    public static IRubyObject each_charCommon(ThreadContext context, IRubyObject recv, Block block) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        Ruby runtime = context.runtime;
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
    public static IRubyObject each_line(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, recv, "each_line");
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if (!data.next_argv(context)) {
            return context.runtime.getNil();
        }

        if (!(data.currentFile instanceof RubyIO)) {
            if (!data.next_argv(context)) return recv;

            data.currentFile.callMethod(context, "each", NULL_ARRAY, block);
            data.next_p = 1;
        }
        IRubyObject str;
        while(!(str = argf_getline(context, recv, args)).isNil()) {
        	block.yield(context, str);
        }

        return recv;
    }

    @JRubyMethod(optional = 1)
    public static IRubyObject lines(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        return each_line(context, recv, args, block);
    }

    @JRubyMethod(name = "each_line", optional = 1)
    public static IRubyObject each_line19(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_line(context, recv, args, block) : enumeratorize(context.runtime, recv, "each_line", args);
    }

    @JRubyMethod(name = "each", optional = 1)
    public static IRubyObject each19(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_line(context, recv, args, block) : enumeratorize(context.runtime, recv, "each", args);
    }

    @JRubyMethod(name = "file")
    public static IRubyObject file(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        data.next_argv(context);

        return data.currentFile;
    }

    @JRubyMethod(name = "skip")
    public static IRubyObject skip(IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if (data.next_p != -1) {
            argf_close(recv.getRuntime().getCurrentContext(), data.currentFile);
            data.next_p = 1;
        }

        return recv;
    }

    public static void argf_close(ThreadContext context, IRubyObject file) {
        if(file instanceof RubyIO) {
            ((RubyIO)file).rbIoClose(context.runtime);
        } else {
            file.callMethod(context, "close");
        }
    }

    @JRubyMethod(name = "close")
    public static IRubyObject close(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        data.next_argv(context);

        argf_close(context, data.currentFile);

        if (data.next_p != -1) data.next_p = 1;

        data.currentLineNumber = 0;
        return recv;
    }

    @JRubyMethod(name = "closed?")
    public static IRubyObject closed_p(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        data.next_argv(context);

        return RubyBoolean.newBoolean(context.runtime, isClosed(context, data.currentFile));
    }

    private static boolean isClosed(ThreadContext context, IRubyObject currentFile) {
        if (!(currentFile instanceof RubyIO)) return currentFile.callMethod(context, "closed?").isTrue();

        return ((RubyIO)currentFile).closed_p(context).isTrue();
    }

    @JRubyMethod(name = "binmode")
    public static IRubyObject binmode(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = getData(context, recv, "no stream");

        ((RubyIO)data.currentFile).binmode();
        return recv;
    }

    @JRubyMethod(name = "binmode?")
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
        context.runtime.setCurrentLine(data.currentLineNumber);
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "tell", alias = {"pos"})
    public static IRubyObject tell(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if(!data.next_argv(context)) {
            throw context.runtime.newArgumentError("no stream to tell");
        }

        return ((RubyIO)data.currentFile).pos(context);
    }

    @JRubyMethod(name = "rewind")
    public static IRubyObject rewind(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = getData(context, recv, "no stream to rewind");

        RubyFixnum retVal = ((RubyIO)data.currentFile).rewind(context);
        ((RubyIO)data.currentFile).lineno_set(context, context.runtime.newFixnum(0));

        data.minLineNumber = 0;
        data.currentLineNumber = 0;
        return retVal;
    }

    @JRubyMethod(name = {"eof"})
    public static IRubyObject eof(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if(!data.inited) {
            return context.runtime.getTrue();
        }
        if(!(data.currentFile instanceof RubyIO)) return data.currentFile.callMethod(context, "eof");

        return ((RubyIO) data.currentFile).eof_p(context);
    }

    @JRubyMethod(name = {"eof?"})
    public static IRubyObject eof_p(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if (!data.inited) {
            return context.runtime.getTrue();
        }
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

        if (c.isNil()) {
            throw context.runtime.newEOFError();
        }

        return c;
    }

    @JRubyMethod
    public static IRubyObject getbyte(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        while(true) {
            if (!data.next_argv(context)) return context.runtime.getNil();

            IRubyObject bt;
            if (!(data.currentFile instanceof RubyFile)) {
                bt = data.currentFile.callMethod(context, "getbyte");
            } else {
                bt = ((RubyIO)data.currentFile).getbyte(context);
            }

            if (!bt.isNil()) return bt;

            data.next_p = 1;
        }
    }

    @JRubyMethod(required = 1, optional = 2)
    public static IRubyObject read_nonblock(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return getPartial(context, recv, args, true);
    }

    @JRubyMethod(required = 1, optional = 1)
    public static IRubyObject readpartial(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return getPartial(context, recv, args, false);
    }

    private static IRubyObject getPartial(ThreadContext context, IRubyObject recv, IRubyObject[] args, boolean nonBlocking) {
        final Ruby runtime = context.runtime;
        boolean noException = false;
        if ( args.length > 1 ) {
            IRubyObject opts = TypeConverter.checkHashType(runtime, args[args.length - 1]);
            if ( ! opts.isNil() &&
                runtime.getFalse() == ((RubyHash) opts).op_aref(context, runtime.newSymbol("exception")) ) {
                noException = true;
            }
            if ( args.length > 2 || opts.isNil() ) args[1] = args[1].convertToString();
        }

        final ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if ( ! data.next_argv(context) ) {
            return RubyIO.nonblockEOF(runtime, noException);
        }

        IRubyObject res;
        // NOTE: this seems no longer relevant, please review :
        //if ( ! (data.currentFile instanceof RubyFile) ) { // In MRI this is stdin && !FILE
        //    res = data.currentFile.callMethod(context, "getpartial", args);
        //} else {
        //    res = ((RubyIO) data.currentFile).getPartial(context, args, nonBlocking, noException);
        //}
        res = ((RubyIO) data.currentFile).getPartial(context, args, nonBlocking, noException);

        if ( res.isNil() ) {
            if ( data.next_p == -1 ) {
                return RubyIO.nonblockEOF(runtime, noException);
            }

            argf_close(context, data.currentFile);
            data.next_p = 1;

            if ( data.argv.size() == 0 ) {
                return RubyIO.nonblockEOF(runtime, noException);
            }

            if ( args.length > 1 && args[1] instanceof RubyString ) return args[1];
            return RubyString.newEmptyString(runtime);
        }

        return res;
    }

    @JRubyMethod
    public static IRubyObject readbyte(ThreadContext context, IRubyObject recv) {
        IRubyObject c = getbyte(context, recv);

        if (c.isNil()) throw context.runtime.newEOFError();

        return c;
    }

    @JRubyMethod(name = "getc")
    public static IRubyObject getc(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        while(true) {
            if (!data.next_argv(context)) return context.runtime.getNil();

            IRubyObject bt;
            if (!(data.currentFile instanceof RubyFile)) {
                bt = data.currentFile.callMethod(context,"getc");
            } else {
                bt = ((RubyIO)data.currentFile).getc();
            }

            if (!bt.isNil()) return bt;

            data.next_p = 1;
        }
    }

    @JRubyMethod(name = "read", optional = 2)
    public static IRubyObject read(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
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
            if (!data.next_argv(context)) return str;

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
                if(data.next_p != -1) {
                    argf_close(context, data.currentFile);
                    data.next_p = 1;
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
        data.next_argv(context);
        return context.runtime.getGlobalVariables().get("$FILENAME");
    }

    @JRubyMethod(name = "to_s", alias = "inspect")
    public static IRubyObject to_s(IRubyObject recv) {
        return recv.getRuntime().newString("ARGF");
    }

    private static ArgsFileData getData(ThreadContext context, IRubyObject recv, String errorMessage) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if (!data.next_argv(context)) {
            throw context.runtime.newArgumentError(errorMessage);
        }

        return data;
    }
}
