/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import static org.jruby.anno.FrameField.LASTLINE;
import static org.jruby.runtime.Visibility.PRIVATE;

import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.TypeConverter;

public class RubyArgsFile extends RubyObject {

    public RubyArgsFile(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public static void initArgsFile(final Ruby runtime) {
        RubyClass argfClass = runtime.defineClass("ARGFClass", runtime.getObject(), RubyArgsFile::new);
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

    @JRubyMethod(name = "initialize", visibility = PRIVATE, rest = true)
    public IRubyObject initialize(ThreadContext context, IRubyObject[] args) {
        final Ruby runtime = context.runtime;
        final RubyArray argv;
        if (args.length == 1 && args[0] == null) {
            argv = runtime.getObject().getConstant("ARGV").convertToArray();
        } else {
            argv = runtime.newArray(args);
        }

        // ARGF is intended to be a singleton from a Ruby perspective but it is still
        // possible for someone to ARGF.class.new.  We do still want a global view of
        // ARGF otherwise getline and rewind in IO would have to keep track of the n
        // instances in play.  So all instances will share
        if (runtime.getArgsFile() == null) {
            dataWrapStruct(new ArgsFileData(runtime, argv));
        } else {
            ArgsFileData data = (ArgsFileData) runtime.getArgsFile().dataGetStruct();
            dataWrapStruct(data);
            data.setArgs(argv);
        }
        return this;
    }

    public static final class ArgsFileData {

        private final Ruby runtime;
        private RubyArray argv;
        public IRubyObject currentFile;
        private boolean inited = false;
        public int next_p = 0;
        public boolean binmode = false;

        public ArgsFileData(Ruby runtime, RubyArray argv) {
            this.runtime = runtime;
            this.argv = argv;
            this.currentFile = runtime.getNil();
        }

        @Deprecated
        public void setCurrentLineNumber(Ruby runtime, int linenumber) {
            runtime.setCurrentLine(linenumber);
        }

        void setArgs(RubyArray argv) {
            inited = false;
            this.argv = argv;
        }

        public boolean next_argv(ThreadContext context) {
            if (!inited) {
                next_p = argv.getLength() > 0 ? 1 : -1;
                inited = true;
                runtime.setCurrentLine(0);
            } else {
                if (argv.isNil()) {
                    next_p = -1;
                } else if (next_p == -1 && argv.getLength() > 0) {
                    next_p = 1;
                }
            }

            IRubyObject $FILENAME = runtime.getGlobalVariables().get("$FILENAME");

            if (next_p == 1) {
                if (argv.getLength() > 0) {
                    final RubyString filename = argv.shift(context).convertToString();
                    if ( ! filename.op_equal(context, $FILENAME).isTrue() ) {
                        runtime.defineReadonlyVariable("$FILENAME", filename, GlobalVariable.Scope.GLOBAL);
                    }

                    if (filenameEqlDash(filename)) {
                        currentFile = runtime.getGlobalVariables().get("$stdin");
                    } else {
                        currentFile = RubyFile.open(context, runtime.getFile(), new IRubyObject[]{ filename }, Block.NULL_BLOCK);
                        String extension = runtime.getInstanceConfig().getInPlaceBackupExtension();
                        if (extension != null) {
                            if (Platform.IS_WINDOWS) {
                                inplaceEditWindows(context, filename.asJavaString(), extension);
                            } else {
                                inplaceEdit(context, filename.asJavaString(), extension);
                            }
                        }
                        if (binmode) ((RubyIO) currentFile).binmode();
                    }
                    next_p = 0;
                } else {
                    next_p = 1;
                    return false;
                }
            } else if (next_p == -1) {
                currentFile = runtime.getGlobalVariables().get("$stdin");
                if (!filenameEqlDash((RubyString) $FILENAME)) {
                    runtime.defineReadonlyVariable("$FILENAME", runtime.newString("-"), GlobalVariable.Scope.GLOBAL);
                }
            }
            return true;
        }

        private static boolean filenameEqlDash(final RubyString filename) {
            final ByteList filenameBytes = filename.getByteList();
            return filenameBytes.length() == 1 && filenameBytes.get(0) == '-';
        }

        public static ArgsFileData getArgsFileData(Ruby runtime) {
            return (ArgsFileData) runtime.getArgsFile().dataGetStruct();
        }

        @Deprecated
        public static ArgsFileData getDataFrom(IRubyObject recv) {
            return getArgsFileData(recv.getRuntime());
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

        public boolean isCurrentFile(RubyIO io) {
            return currentFile == io;
        }
    }

    @Deprecated
    public static void setCurrentLineNumber(IRubyObject recv, int newLineNumber) {
        recv.getRuntime().setCurrentLine(newLineNumber);
    }

    @JRubyMethod(name = "argv")
    public static IRubyObject argv(ThreadContext context, IRubyObject recv) {
        return ArgsFileData.getArgsFileData(context.runtime).argv;
    }

    @JRubyMethod(name = {"fileno", "to_i"})
    public static IRubyObject fileno(ThreadContext context, IRubyObject recv) {
        return getCurrentDataFile(context, "no stream").fileno(context);
    }

    @JRubyMethod(name = "to_io")
    public static IRubyObject to_io(ThreadContext context, IRubyObject recv) {
        return getCurrentDataFile(context, "no stream");
    }

    @JRubyMethod
    public static IRubyObject set_encoding(ThreadContext context, IRubyObject recv, IRubyObject encoding) {
        return getCurrentDataFile(context, "no stream to set encoding").set_encoding(context, encoding);
    }

    @JRubyMethod
    public static IRubyObject set_encoding(ThreadContext context, IRubyObject recv, IRubyObject encoding, IRubyObject internalEncoding) {
        return getCurrentDataFile(context, "no stream to set encoding").set_encoding(context, encoding, internalEncoding);
    }

    @JRubyMethod
    public static IRubyObject set_encoding(ThreadContext context, IRubyObject recv, IRubyObject encoding, IRubyObject internalEncoding, IRubyObject options) {
        return getCurrentDataFile(context, "no stream to set encoding").set_encoding(context, encoding, internalEncoding, options);
    }

    @JRubyMethod
    public static IRubyObject internal_encoding(ThreadContext context, IRubyObject recv) {
        return getCurrentDataFile(context, "no stream to set encoding").internal_encoding(context);
    }

    @JRubyMethod
    public static IRubyObject external_encoding(ThreadContext context, IRubyObject recv) {
        return getCurrentDataFile(context, "no stream to set encoding").external_encoding(context);
    }

    // MRI: argf_getline
    private static IRubyObject argf_getline(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        while (true) {
            if (!data.next_argv(context)) return context.nil;

            IRubyObject line = data.currentFile.callMethod(context, "gets", args);

            if (line.isNil() && data.next_p != -1) {
                argf_close(context, data.currentFile);
                data.next_p = 1;
                continue;
            }

            return line;
        }
    }

    // ARGF methods

    /** Read a line.
     *
     */
    @JRubyMethod(name = "gets", optional = 1, writes = LASTLINE)
    public static IRubyObject gets(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return context.setLastLine(argf_getline(context, recv, args));
    }

    /** Read a line.
     *
     */
    @JRubyMethod(name = "readline", optional = 1, writes = LASTLINE)
    public static IRubyObject readline(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject line = gets(context, recv, args);

        if (line.isNil()) throw context.runtime.newEOFError();

        return line;
    }

    @JRubyMethod(optional = 1)
    public static IRubyObject readlines(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        ArgsFileData data = ArgsFileData.getArgsFileData(runtime);

        if (!data.next_argv(context)) return runtime.newEmptyArray();

        if (!(data.currentFile instanceof RubyIO)) return data.currentFile.callMethod(context, "readlines", args);

        RubyArray ary = runtime.newArray();
        IRubyObject line;
        while(!(line = argf_getline(context, recv, args)).isNil()) {
            ary.append(line);
        }
        return ary;
    }

    @JRubyMethod(optional = 1)
    public static IRubyObject to_a(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        ArgsFileData data = ArgsFileData.getArgsFileData(runtime);

        if (!data.next_argv(context)) return runtime.newEmptyArray();
        if (!(data.currentFile instanceof RubyIO)) return data.currentFile.callMethod(context, "to_a", args);

        RubyArray ary = runtime.newArray();
        IRubyObject line;
        while ((line = argf_getline(context, recv, args)) != context.nil) {
            ary.append(line);
        }
        return ary;
    }

    @JRubyMethod
    public static IRubyObject each_byte(final ThreadContext context, IRubyObject recv, final Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, recv, "each_byte");

        IRubyObject bt;
        while ((bt = getc(context, recv)) != context.nil) {
            block.yield(context, bt);
        }

        return recv;
    }

    @JRubyMethod
    public static IRubyObject each_byte(final ThreadContext context, IRubyObject recv, IRubyObject arg, final Block block) {
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
        final Ruby runtime = context.runtime;

        ArgsFileData data = ArgsFileData.getArgsFileData(runtime);

        IRubyObject ch;
        while ((ch = getc(context, recv)) != context.nil) {
            boolean cont = true;
            while (cont) {
                cont = false;
                byte c = (byte) RubyNumeric.fix2int(ch);
                int n = runtime.getKCode().getEncoding().length(c);
                IRubyObject file = data.currentFile;
                RubyString str = runtime.newString();
                str.setTaint(true);
                str.cat(c);

                while(--n > 0) {
                    if ((ch = getc(context, recv)) == context.nil) {
                        block.yield(context, str);
                        return recv;
                    }
                    if (data.currentFile != file) {
                        block.yield(context, str);
                        cont = true;
                        continue;
                    }
                    c = (byte) RubyNumeric.fix2int(ch);
                    str.cat(c);
                }
                block.yield(context, str);
            }
        }
        return recv;
    }

    @JRubyMethod
    public static IRubyObject each_codepoint(ThreadContext context, IRubyObject recv, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, recv, "each_line");
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        CallSite each_codepoint = sites(context).each_codepoint;
        while (data.next_argv(context)) {
            each_codepoint.call(context, recv, data.currentFile, block);
        }

        return context.nil;
    }

    @JRubyMethod
    public static IRubyObject codepoints(ThreadContext context, IRubyObject recv, Block block) {
        context.runtime.getWarnings().warn("ARGF#codepoints is deprecated; use #each_codepoint instead");

        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, recv, "each_line");

        return each_codepoint(context, recv, block);
    }

    /** Invoke a block for each line.
     *
     */
    @JRubyMethod(name = "each_line", optional = 1)
    public static IRubyObject each_line(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, recv, "each_line", args);

        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (!data.next_argv(context)) return context.nil;

        if (!(data.currentFile instanceof RubyIO)) {
            if (!data.next_argv(context)) return recv;

            data.currentFile.callMethod(context, "each", NULL_ARRAY, block);
            data.next_p = 1;
        }

        IRubyObject str;
        while ((str = argf_getline(context, recv, args)) != context.nil) {
        	block.yield(context, str);
        }

        return recv;
    }

    @Deprecated // TODO "warning: ARGF#lines is deprecated; use #each_line instead"
    @JRubyMethod(optional = 1)
    public static IRubyObject lines(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, recv, "each_line");
        return each_line(context, recv, args, block);
    }

    @Deprecated
    public static IRubyObject each_line19(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        return each_line(context, recv, args, block);
    }

    @JRubyMethod(name = "each", optional = 1)
    public static IRubyObject each(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_line(context, recv, args, block) : enumeratorize(context.runtime, recv, "each", args);
    }

    @Deprecated
    public static IRubyObject each19(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        return each(context, recv, args, block);
    }

    @JRubyMethod(name = "file")
    public static IRubyObject file(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        data.next_argv(context);

        return data.currentFile;
    }

    @JRubyMethod(name = "skip")
    public static IRubyObject skip(IRubyObject recv) {
        Ruby runtime = recv.getRuntime();
        ArgsFileData data = ArgsFileData.getArgsFileData(runtime);

        if (data.inited && data.next_p == 0) {
            argf_close(runtime.getCurrentContext(), data.currentFile);
            data.next_p = 1;
        }

        return recv;
    }

    public static void argf_close(ThreadContext context, IRubyObject file) {
        if (file instanceof RubyIO) {
            ((RubyIO) file).rbIoClose(context);
        } else {
            file.callMethod(context, "close");
        }
    }

    @JRubyMethod(name = "close")
    public static IRubyObject close(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        data.next_argv(context);

        if (data.currentFile == context.runtime.getGlobalVariables().get("$stdin")) return recv;

        argf_close(context, data.currentFile);

        if (data.next_p != -1) data.next_p = 1;

        context.runtime.setCurrentLine(0);

        return recv;
    }

    @JRubyMethod(name = "closed?")
    public static IRubyObject closed_p(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        data.next_argv(context);

        return RubyBoolean.newBoolean(context, isClosed(context, data.currentFile));
    }

    private static boolean isClosed(ThreadContext context, IRubyObject currentFile) {
        if (!(currentFile instanceof RubyIO)) return currentFile.callMethod(context, "closed?").isTrue();

        return ((RubyIO)currentFile).closed_p(context).isTrue();
    }

    @JRubyMethod(name = "binmode")
    public static IRubyObject binmode(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        data.binmode = true;
        if (data.currentFile != context.nil) ((RubyIO) data.currentFile).binmode();

        return recv;
    }

    @JRubyMethod(name = "binmode?")
    public static IRubyObject op_binmode(ThreadContext context, IRubyObject recv) {
        return getCurrentDataFile(context, "no stream").op_binmode(context);
    }

    @JRubyMethod(name = "lineno")
    public static IRubyObject lineno(ThreadContext context, IRubyObject recv) {
        return context.runtime.newFixnum(context.runtime.getCurrentLine());
    }

    @JRubyMethod(name = "lineno=")
    public static IRubyObject lineno_set(ThreadContext context, IRubyObject recv, IRubyObject line) {
        context.runtime.setCurrentLine(RubyNumeric.fix2int(line));

        return context.nil;
    }

    @JRubyMethod(name = "tell", alias = {"pos"})
    public static IRubyObject tell(ThreadContext context, IRubyObject recv) {
        return getCurrentDataFile(context, "no stream to tell").pos(context);
    }

    @JRubyMethod(name = "rewind")
    public static IRubyObject rewind(ThreadContext context, IRubyObject recv) {
        RubyIO currentFile = getCurrentDataFile(context, "no stream to rewind");

        RubyFixnum retVal = currentFile.rewind(context);
        currentFile.lineno_set(context, context.runtime.newFixnum(0));

        return retVal;
    }

    @JRubyMethod(name = {"eof"})
    public static IRubyObject eof(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (!data.inited) return context.tru;

        if (!(data.currentFile instanceof RubyIO)) {
            return data.currentFile.callMethod(context, "eof");
        } else {
            return ((RubyIO) data.currentFile).eof_p(context);
        }
    }

    @JRubyMethod(name = {"eof?"})
    public static IRubyObject eof_p(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (!data.inited) return context.tru;

        if (!(data.currentFile instanceof RubyIO)) {
            return data.currentFile.callMethod(context, "eof?");
        } else {
            return ((RubyIO) data.currentFile).eof_p(context);
        }
    }

    @JRubyMethod(name = "pos=", required = 1)
    public static IRubyObject set_pos(ThreadContext context, IRubyObject recv, IRubyObject offset) {
        return getCurrentDataFile(context, "no stream to set position").pos_set(context, offset);
    }

    @JRubyMethod(name = "seek", required = 1, optional = 1)
    public static IRubyObject seek(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return getCurrentDataFile(context, "no stream to seek").seek(context, args);
    }

    @JRubyMethod(name = "readchar")
    public static IRubyObject readchar(ThreadContext context, IRubyObject recv) {
        IRubyObject c = getc(context, recv);

        if (c == context.nil) throw context.runtime.newEOFError();

        return c;
    }

    @JRubyMethod
    public static IRubyObject getbyte(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        while (true) {
            if (!data.next_argv(context)) return context.nil;

            IRubyObject bt;
            if (data.currentFile instanceof RubyFile) {
                bt = ((RubyIO) data.currentFile).getbyte(context);
            } else {
                bt = data.currentFile.callMethod(context, "getbyte");
            }

            if (bt != context.nil) return bt;

            argf_close(context, data.currentFile);
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
        RubyString str = null;
        if ( args.length > 1 ) {
            IRubyObject opts = TypeConverter.checkHashType(runtime, args[args.length - 1]);
            if ( opts != context.nil &&
                context.fals == ((RubyHash) opts).op_aref(context, runtime.newSymbol("exception")) ) {
                noException = true;
            }
            if (args.length > 2 || opts == context.nil) {
                if (args[1] != context.nil) {
                    args[1] = args[1].convertToString();
                    str = (RubyString) args[1];
                }
            }
        }

        final ArgsFileData data = ArgsFileData.getArgsFileData(runtime);

        if (!data.next_argv(context)) {
            if (str != null) str.clear();
            
            return RubyIO.nonblockEOF(runtime, noException);
        }

        IRubyObject res = ((RubyIO) data.currentFile).getPartial(context, args, nonBlocking, noException);
        if (res == context.nil) {
            if (data.next_p == -1) return RubyIO.nonblockEOF(runtime, noException);

            argf_close(context, data.currentFile);
            data.next_p = 1;

            if (data.argv.isEmpty()) return RubyIO.nonblockEOF(runtime, noException);

            if (args.length > 1 && args[1] instanceof RubyString ) return args[1];
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
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        while (true) {
            if (!data.next_argv(context)) return context.nil;

            IRubyObject bt;
            if (data.currentFile instanceof RubyFile) {
                bt = ((RubyIO) data.currentFile).getbyte(context);
            } else {
                bt = data.currentFile.callMethod(context,"getc");
            }

            if (bt != context.nil) return bt;

            argf_close(context, data.currentFile);
            data.next_p = 1;
        }
    }

    @JRubyMethod(name = "read", optional = 2)
    public static IRubyObject read(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Ruby runtime = context.runtime;
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);
        IRubyObject tmp, str, length;
        long len = 0;

        if (args.length > 0) {
            length = args[0];
            str = args.length > 1 ? args[1] : context.nil;
        } else {
            str = length = context.nil;
        }

        if (length != context.nil) len = RubyNumeric.num2long(length);

        if (str != context.nil) {
            str = str.convertToString();
            ((RubyString) str).modify();
            ((RubyString) str).getByteList().length(0);
            args[1] = context.nil;
        }

        while (true) {
            if (!data.next_argv(context)) return str;

            if (data.currentFile instanceof RubyIO) {
                tmp = ((RubyIO) data.currentFile).read(args);
            } else {
                tmp = data.currentFile.callMethod(context, "read", args);
            }

            if (str == context.nil) {
                str = tmp;
            } else if (tmp != context.nil) {
                ((RubyString) str).append(tmp);
            }

            if (tmp == context.nil || length == context.nil) {
                if(data.next_p != -1) {
                    argf_close(context, data.currentFile);
                    data.next_p = 1;
                    continue;
                }
            } else if(args.length >= 1) {
                final int strLen = ((RubyString) str).getByteList().length();
                if (strLen < len) {
                    len -= strLen;
                    args[0] = runtime.newFixnum(len);
                    continue;
                }
            }
            return str;
        }
    }

    @JRubyMethod(name = "filename", alias = {"path"})
    public static IRubyObject filename(ThreadContext context, IRubyObject recv) {
        ArgsFileData.getArgsFileData(context.runtime).next_argv(context);

        return context.runtime.getGlobalVariables().get("$FILENAME");
    }

    @JRubyMethod(name = "to_s", alias = "inspect")
    public static IRubyObject to_s(IRubyObject recv) {
        return recv.getRuntime().newString("ARGF");
    }

    private static RubyIO getCurrentDataFile(ThreadContext context, String errorMessage) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (!data.next_argv(context)) throw context.runtime.newArgumentError(errorMessage);

        return (RubyIO) data.currentFile;
    }

    private static JavaSites.ArgfSites sites(ThreadContext context) {
        return context.sites.Argf;
    }
}