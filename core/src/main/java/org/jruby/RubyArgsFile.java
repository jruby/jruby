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

import jnr.posix.FileStat;
import jnr.posix.util.Platform;
import org.jruby.anno.JRubyMethod;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.JavaSites;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.StringSupport;
import org.jruby.util.TypeConverter;
import org.jruby.util.io.Getline;

import java.io.File;
import java.io.IOException;

import static org.jruby.RubyArgsFile.Next.NextFile;
import static org.jruby.RubyArgsFile.Next.Stream;
import static org.jruby.RubyEnumerator.enumeratorize;
import static org.jruby.anno.FrameField.LASTLINE;
import static org.jruby.api.Access.argsFile;
import static org.jruby.api.Access.fileClass;
import static org.jruby.api.Access.globalVariables;
import static org.jruby.api.Access.instanceConfig;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Access.stringClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.asSymbol;
import static org.jruby.api.Convert.toByte;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Convert.toLong;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newEmptyArray;
import static org.jruby.api.Create.newEmptyString;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Warn.warn;
import static org.jruby.runtime.ThreadContext.CALL_KEYWORD;
import static org.jruby.runtime.ThreadContext.resetCallInfo;
import static org.jruby.runtime.Visibility.PRIVATE;

public class RubyArgsFile extends RubyObject {

    public RubyArgsFile(Ruby runtime, RubyClass metaClass) {
        super(runtime, metaClass);
    }

    public static void initArgsFile(ThreadContext context, RubyModule Enumerable, GlobalVariables globals) {
        var Object = objectClass(context);
        RubyClass ARGF = defineClass(context, "ARGFClass", Object, RubyArgsFile::new).
                include(context, Enumerable).
                defineMethods(context, RubyArgsFile.class);

        IRubyObject argsFile = ARGF.newInstance(context, new IRubyObject[] { null }, null);

        context.runtime.setArgsFile(argsFile);
        globals.defineReadonly("$<", new ArgsFileAccessor(context.runtime), GlobalVariable.Scope.GLOBAL);
        Object.defineConstant(context, "ARGF", argsFile);
        globals.defineReadonly("$FILENAME", new ValueAccessor(newString(context, "-")), GlobalVariable.Scope.GLOBAL);
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
        final var argv = args.length == 1 && args[0] == null ?
                objectClass(context).getConstant(context, "ARGV").convertToArray() :
                newArray(context, args);

        // ARGF is intended to be a singleton from a Ruby perspective but it is still
        // possible for someone to ARGF.class.new.  We do still want a global view of
        // ARGF otherwise getline and rewind in IO would have to keep track of the n
        // instances in play.  So all instances will share
        var argsFile = argsFile(context);
        if (argsFile == null) {
            dataWrapStruct(new ArgsFileData(context.runtime, argv));
        } else {
            ArgsFileData data = (ArgsFileData) argsFile.dataGetStruct();
            dataWrapStruct(data);
            data.setArgs(argv);
        }
        return this;
    }

    public enum Next {
        Stream,   // We are only getting from $stdin
        SameFile, // We are continuing to interact with the same file
        NextFile  // Time to advance to the next file.
    }

    public static final class ArgsFileData {

        private final Ruby runtime;
        private RubyArray argv;
        public IRubyObject currentFile;
        private boolean inited = false;
        public Next next_p = NextFile;
        public boolean binmode = false;
        private IRubyObject inPlace; // false, nil, String

        public ArgsFileData(Ruby runtime, RubyArray argv) {
            this.runtime = runtime;
            setArgs(argv);
            this.currentFile = runtime.getNil();
        }

        @Deprecated
        public void setCurrentLineNumber(Ruby runtime, int linenumber) {
            runtime.setCurrentLine(linenumber);
        }

        // ARGF.class.new
        void setArgs(RubyArray argv) {
            inited = false;
            this.argv = argv;
            this.inPlace = runtime.getFalse();
        }

        public boolean next_argv(ThreadContext context) {
            if (!inited) {
                next_p = argv.getLength() > 0 ? NextFile : Stream;
                inited = true;
                runtime.setCurrentLine(0);
            } else {
                if (argv.isNil()) {
                    next_p = Stream;
                } else if (next_p == Stream && argv.getLength() > 0) {
                    next_p = NextFile;
                }
            }

            var globalVariables = globalVariables(context);
            GlobalVariable $FILENAME = globalVariables.getVariable("$FILENAME");

            if (next_p == NextFile) {
                if (argv.getLength() > 0) {
                    RubyString filename = TypeConverter.convertToType(argv.shift(context), stringClass(context), "to_path").convertToString();
                    StringSupport.checkStringSafety(runtime, filename);
                    if ( ! filename.op_equal(context, $FILENAME.getAccessor().getValue()).isTrue() ) {
                        $FILENAME.forceValue(filename);
                    }

                    if (filenameEqlDash(filename)) {
                        currentFile = globalVariables.get("$stdin");
                    } else {
                        RubyIO currentFileIO = (RubyIO) RubyFile.open(context, fileClass(context), new IRubyObject[]{ filename }, Block.NULL_BLOCK);
                        currentFile = currentFileIO;
                        String extension = null;
                        if (inPlace.isTrue()) extension = inPlace.asJavaString();
                        if (extension == null) extension = instanceConfig(context).getInPlaceBackupExtension();
                        if (extension != null) {
                            if (Platform.IS_WINDOWS) {
                                inplaceEditWindows(context, currentFileIO, filename.asJavaString(), extension);
                            } else {
                                inplaceEdit(context, currentFileIO, filename.asJavaString(), extension);
                            }
                        }
                        if (binmode) {
                            currentFileIO.binmode(context);
                        }
                    }
                    next_p = Next.SameFile;
                } else {
                    next_p = NextFile;
                    return false;
                }
            } else if (next_p == Stream) {
                currentFile = globalVariables.get("$stdin");
                if (!filenameEqlDash((RubyString) $FILENAME.getAccessor().getValue())) {
                    $FILENAME.forceValue(newString(context, "-"));
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
            return getArgsFileData(((RubyBasicObject) recv).getCurrentContext().runtime);
        }

        private void createNewFile(File file) {
            try {
                file.createNewFile();
            } catch (IOException ex) {
                throw runtime.newIOErrorFromException(ex);
            }
        }

        private void inplaceEditWindows(ThreadContext context, RubyIO argfIO, String filename, String extension) throws RaiseException {
            File file = new File(filename);

            if (!extension.isEmpty()) {
                String backup = filename + extension;
                File backupFile = new File(backup);

                ((RubyIO) currentFile).close(); // we can't rename a file while it's open in windows
                backupFile.delete();
                file.renameTo(backupFile);
                currentFile = RubyFile.open(context, fileClass(context), //reopen
                        new IRubyObject[]{newString(context, backup)}, Block.NULL_BLOCK);
            } else {
                throw runtime.newIOError("Windows doesn't support inplace editing without a backup");
            }

            createNewFile(file);

            IRubyObject writeIO = RubyFile.open(context, fileClass(context),
                    new IRubyObject[]{newString(context, filename), newString(context, "w")}, Block.NULL_BLOCK);
            argfIO.getOpenFile().tiedIOForWriting = (RubyIO) writeIO;
            globalVariables(context).set("$stdout", writeIO);
        }

        private void inplaceEdit(ThreadContext context, RubyIO argfIO, String filename, String extension) throws RaiseException {
            File file = new File(filename);
            var posix = context.runtime.getPosix();
            FileStat stat = posix.stat(filename);

            if (!extension.isEmpty()) {
                file.renameTo(new File(filename + extension));
            } else {
                file.delete();
            }

            createNewFile(file);

            posix.chmod(filename, stat.mode());
            posix.chown(filename, stat.uid(), stat.gid());
            IRubyObject writeIO = RubyFile.open(context, fileClass(context),
                    new IRubyObject[]{newString(context, filename), newString(context, "w")}, Block.NULL_BLOCK);
            argfIO.getOpenFile().tiedIOForWriting = (RubyIO) writeIO;
            globalVariables(context).set("$stdout", (RubyIO) writeIO);
        }

        public boolean isCurrentFile(RubyIO io) {
            return currentFile == io;
        }
    }

    @Deprecated
    public static void setCurrentLineNumber(IRubyObject recv, int newLineNumber) {
        ((RubyBasicObject) recv).getCurrentContext().runtime.setCurrentLine(newLineNumber);
    }

    @JRubyMethod
    public static IRubyObject inplace_mode(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (data.inPlace == null) return context.nil;
        if (data.inPlace.isNil()) return context.nil;

        return data.inPlace.dup();
    }

    @JRubyMethod(name = "inplace_mode=")
    public static IRubyObject inplace_mode_set(ThreadContext context, IRubyObject recv, IRubyObject test) {
        return setInplaceMode(context, recv, test);
    }

    private static IRubyObject setInplaceMode(ThreadContext context, IRubyObject recv, IRubyObject test) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (test.isNil()) {
            data.inPlace = context.nil;
        } else if (!test.isTrue()) {
            data.inPlace = context.fals;
        } else {
            test = TypeConverter.convertToType(test, stringClass(context), "to_str", false);
            if (test.isNil() || ((RubyString) test).isEmpty()) {
                data.inPlace = context.nil;
            } else {
                StringSupport.checkStringSafety(context.runtime, test);
                test.setFrozen(true);
                data.inPlace = test;
            }
        }

        return recv;
    }

    @JRubyMethod(name = "inplace_mode=")
    public IRubyObject inplace_mode_set(ThreadContext context, IRubyObject test) {
        return setInplaceMode(context, this, test);
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
        boolean keywords = (resetCallInfo(context) & CALL_KEYWORD) != 0;
        IRubyObject line;
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        while (true) {
            if (!data.next_argv(context)) return context.nil;

            RubyIO currentFile = (RubyIO) data.currentFile;

            if (isGenericInput(context, data)) {
                line = data.currentFile.callMethod(context, "gets", args);
            } else {
                if (args.length == 0 && context.runtime.getRecordSeparatorVar().get() == globalVariables(context).getDefaultSeparator()) {
                    line = (currentFile).gets(context);
                } else {
                    line = Getline.getlineCall(context, GETLINE, currentFile, currentFile.getReadEncoding(), keywords, args);
                }

                if (line.isNil() && data.next_p != Stream) {
                    argf_close(context, data.currentFile);
                    data.next_p = NextFile;
                    continue;
                }
            }

            return line;
        }
    }

    private static boolean isGenericInput(ThreadContext context, ArgsFileData data) {
        return data.currentFile == globalVariables(context).get("$stdin") && !(data.currentFile instanceof RubyFile);
    }

    private static final Getline.Callback<RubyIO, IRubyObject> GETLINE =
            (context, self, rs, limit, chomp, block) -> self.getlineImpl(context, rs, limit, chomp);

    // ARGF methods

    /** Read a line.
     *
     */
    @JRubyMethod(name = "gets", optional = 1, checkArity = false, writes = LASTLINE)
    public static IRubyObject gets(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        return context.setLastLine(argf_getline(context, recv, args));
    }

    /** Read a line.
     *
     */
    @JRubyMethod(name = "readline", optional = 1, checkArity = false, writes = LASTLINE)
    public static IRubyObject readline(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject line = gets(context, recv, args);

        if (line.isNil()) throw context.runtime.newEOFError();

        return line;
    }

    @JRubyMethod(optional = 1, checkArity = false)
    public static IRubyObject readlines(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (!data.next_argv(context)) return newEmptyArray(context);

        if (!(data.currentFile instanceof RubyIO)) return data.currentFile.callMethod(context, "readlines", args);

        var ary = newArray(context);
        IRubyObject line;
        while(!(line = argf_getline(context, recv, args)).isNil()) {
            ary.append(context, line);
        }
        return ary;
    }

    @JRubyMethod(optional = 1, checkArity = false)
    public static IRubyObject to_a(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 0, 1);

        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (!data.next_argv(context)) return newEmptyArray(context);
        if (!(data.currentFile instanceof RubyIO)) return data.currentFile.callMethod(context, "to_a", args);

        var ary = newArray(context);
        IRubyObject line;
        while ((line = argf_getline(context, recv, args)) != context.nil) {
            ary.append(context, line);
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

    @JRubyMethod(optional = 1, checkArity = false)
    public static IRubyObject bytes(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        Arity.checkArgumentCount(context, args, 0, 1);
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
                byte c = toByte(context, ch);
                int n = runtime.getKCode().getEncoding().length(c);
                IRubyObject file = data.currentFile;
                RubyString str = runtime.newString();
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
                    c = toByte(context, ch);
                    str.cat(c);
                }
                block.yield(context, str);
            }
        }
        return recv;
    }

    @JRubyMethod
    public static IRubyObject each_codepoint(ThreadContext context, IRubyObject recv, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, recv, "each_codepoint");
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        CallSite each_codepoint = sites(context).each_codepoint;
        while (data.next_argv(context)) {
            each_codepoint.call(context, recv, data.currentFile, block);
            data.next_p = NextFile;
        }

        return recv;
    }

    @Deprecated(since = "9.4-")
    public static IRubyObject codepoints(ThreadContext context, IRubyObject recv, Block block) {
        warn(context, "ARGF#codepoints is deprecated; use #each_codepoint instead");

        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, recv, "each_line");

        return each_codepoint(context, recv, block);
    }


    /** Invoke a block for each line.
     *
     */
    @JRubyMethod(name = "each_line", optional = 1, checkArity = false)
    public static IRubyObject each_line(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return enumeratorize(context.runtime, recv, "each_line", args);

        Arity.checkArgumentCount(context, args, 0, 1);

        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (!data.next_argv(context)) return context.nil;

        if (!(data.currentFile instanceof RubyIO)) {
            if (!data.next_argv(context)) return recv;

            data.currentFile.callMethod(context, "each", NULL_ARRAY, block);
            data.next_p = NextFile;
        }

        IRubyObject str;
        while ((str = argf_getline(context, recv, args)) != context.nil) {
        	block.yield(context, str);
        }

        return recv;
    }

    @Deprecated(since = "10.0")
    public static IRubyObject lines(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        if (!block.isGiven()) return RubyEnumerator.enumeratorize(context.runtime, recv, "each_line");
        return each_line(context, recv, args, block);
    }

    @JRubyMethod(name = "each", optional = 1, checkArity = false)
    public static IRubyObject each(final ThreadContext context, IRubyObject recv, IRubyObject[] args, final Block block) {
        return block.isGiven() ? each_line(context, recv, args, block) : enumeratorize(context.runtime, recv, "each", args);
    }

    @JRubyMethod(name = "file")
    public static IRubyObject file(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        data.next_argv(context);

        return data.currentFile;
    }

    @Deprecated(since = "10.0")
    public static IRubyObject skip(IRubyObject recv) {
        return skip(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(name = "skip")
    public static IRubyObject skip(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (data.inited && data.next_p == Next.SameFile) {
            argf_close(context, data.currentFile);
            data.next_p = NextFile;
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

        if (data.currentFile == globalVariables(context).get("$stdin")) return recv;

        argf_close(context, data.currentFile);

        if (data.next_p != Stream) data.next_p = NextFile;

        context.runtime.setCurrentLine(0);

        return recv;
    }

    @JRubyMethod(name = "closed?")
    public static IRubyObject closed_p(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        data.next_argv(context);

        return asBoolean(context, isClosed(context, data.currentFile));
    }

    private static boolean isClosed(ThreadContext context, IRubyObject currentFile) {
        if (!(currentFile instanceof RubyIO)) return currentFile.callMethod(context, "closed?").isTrue();

        return ((RubyIO)currentFile).closed_p(context).isTrue();
    }

    @JRubyMethod(name = "binmode")
    public static IRubyObject binmode(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        data.binmode = true;
        if (data.currentFile != context.nil) ((RubyIO) data.currentFile).binmode(context);

        return recv;
    }

    @JRubyMethod(name = "binmode?")
    public static IRubyObject op_binmode(ThreadContext context, IRubyObject recv) {
        return getCurrentDataFile(context, "no stream").op_binmode(context);
    }

    @JRubyMethod(name = "lineno")
    public static IRubyObject lineno(ThreadContext context, IRubyObject recv) {
        return asFixnum(context, context.runtime.getCurrentLine());
    }

    @JRubyMethod(name = "lineno=")
    public static IRubyObject lineno_set(ThreadContext context, IRubyObject recv, IRubyObject line) {
        context.runtime.setCurrentLine(toInt(context, line));

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
        currentFile.lineno_set(context, asFixnum(context, 0));

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

    @JRubyMethod(name = "pos=")
    public static IRubyObject set_pos(ThreadContext context, IRubyObject recv, IRubyObject offset) {
        return getCurrentDataFile(context, "no stream to set position").pos_set(context, offset);
    }

    @JRubyMethod(name = "seek", required = 1, optional = 1, checkArity = false)
    public static IRubyObject seek(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 2);

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
            data.next_p = NextFile;
        }
    }

    @JRubyMethod(required = 1, optional = 2, checkArity = false)
    public static IRubyObject read_nonblock(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 3);

        return getPartial(context, recv, args, true);
    }

    @JRubyMethod(required = 1, optional = 1, checkArity = false)
    public static IRubyObject readpartial(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        Arity.checkArgumentCount(context, args, 1, 2);

        return getPartial(context, recv, args, false);
    }

    private static IRubyObject getPartial(ThreadContext context, IRubyObject recv, IRubyObject[] args, boolean nonBlocking) {
        final Ruby runtime = context.runtime;
        boolean noException = false;
        RubyString str = null;
        if (args.length > 1) {
            IRubyObject opts = TypeConverter.checkHashType(runtime, args[args.length - 1]);
            if ( opts != context.nil &&
                context.fals == ((RubyHash) opts).op_aref(context, asSymbol(context, "exception")) ) {
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
            if (str != null) str.clear(context);
            
            return RubyIO.nonblockEOF(runtime, noException);
        }

        IRubyObject res = ((RubyIO) data.currentFile).getPartial(context, args, nonBlocking, noException);
        if (res != context.nil) return res;
        if (data.next_p == Stream) return RubyIO.nonblockEOF(runtime, noException);

        argf_close(context, data.currentFile);
        data.next_p = NextFile;

        if (data.argv.isEmpty()) return RubyIO.nonblockEOF(runtime, noException);

        return args.length > 1 && args[1] instanceof RubyString ? args[1] : newEmptyString(context);
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
            data.next_p = NextFile;
        }
    }

    @JRubyMethod(name = "read", optional = 2, checkArity = false)
    public static IRubyObject read(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 0, 2);
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);
        IRubyObject tmp, str, length;
        long len = 0;
        var nil = context.nil;

        if (argc > 0) {
            length = args[0];
            str = argc > 1 ? args[1] : nil;
        } else {
            str = length = nil;
        }

        if (length != nil) len = toLong(context, length);

        if (str != nil) {
            str = str.convertToString();
            ((RubyString) str).modify();
            ((RubyString) str).getByteList().length(0);
            args[1] = nil;
        }

        while (true) {
            if (!data.next_argv(context)) return str;

            tmp = data.currentFile instanceof RubyIO file ?
                    file.read(context, args) : data.currentFile.callMethod(context, "read", args);

            if (str == nil) {
                str = tmp;
            } else if (tmp != nil) {
                ((RubyString) str).append(tmp);
            }

            if (tmp == nil || length == nil) {
                if (data.next_p != Stream) {
                    argf_close(context, data.currentFile);
                    data.next_p = NextFile;
                    continue;
                }
            } else if (argc >= 1) {
                final int strLen = ((RubyString) str).getByteList().length();
                if (strLen < len) {
                    args[0] = asFixnum(context, len - strLen);
                    continue;
                }
            }
            return str;
        }
    }

    @JRubyMethod(name = "filename", alias = {"path"})
    public static IRubyObject filename(ThreadContext context, IRubyObject recv) {
        ArgsFileData.getArgsFileData(context.runtime).next_argv(context);

        return globalVariables(context).get("$FILENAME");
    }

    @Deprecated(since = "10.0")
    public static IRubyObject to_s(IRubyObject recv) {
        return to_s(((RubyBasicObject) recv).getCurrentContext(), recv);
    }

    @JRubyMethod(name = "to_s", alias = "inspect")
    public static IRubyObject to_s(ThreadContext context, IRubyObject recv) {
        return newString(context, "ARGF");
    }

    @JRubyMethod(name = "write", rest = true)
    public static IRubyObject write(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyIO.writev(context, argfWriteIO(context), args);
    }

    @JRubyMethod(name = "write")
    public static IRubyObject write(ThreadContext context, IRubyObject recv) {
        return RubyIO.writev(context, argfWriteIO(context));
    }

    @JRubyMethod(name = "write")
    public static IRubyObject write(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return RubyIO.writev(context, argfWriteIO(context), arg0);
    }

    @JRubyMethod(name = "write")
    public static IRubyObject write(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return RubyIO.writev(context, argfWriteIO(context), arg0, arg1);
    }

    @JRubyMethod(name = "write")
    public static IRubyObject write(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return RubyIO.writev(context, argfWriteIO(context), arg0, arg1, arg2);
    }

    @JRubyMethod(name = "print", rest = true)
    public static IRubyObject print(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyIO.print(context, recv, args);
    }

    @JRubyMethod(name = "print")
    public static IRubyObject print(ThreadContext context, IRubyObject recv) {
        return RubyIO.print0(context, recv);
    }

    @JRubyMethod(name = "print")
    public static IRubyObject print(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return RubyIO.print1(context, recv, arg0);
    }

    @JRubyMethod(name = "print")
    public static IRubyObject print(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return RubyIO.print2(context, recv, arg0, arg1);
    }

    @JRubyMethod(name = "print")
    public static IRubyObject print(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return RubyIO.print3(context, recv, arg0, arg1, arg2);
    }

    @JRubyMethod(name = "printf", rest = true)
    public static IRubyObject printf(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyIO.printf(context, recv, args);
    }

    @JRubyMethod(name = "putc")
    public static IRubyObject putc(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return RubyIO.putc(context, recv, arg0);
    }

    @JRubyMethod(name = "puts", rest = true)
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        return RubyIO.puts(context, recv, args);
    }

    @JRubyMethod(name = "puts")
    public static IRubyObject puts(ThreadContext context, IRubyObject recv) {
        return RubyIO.puts0(context, recv);
    }

    @JRubyMethod(name = "puts")
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject arg0) {
        return RubyIO.puts1(context, recv, arg0);
    }

    @JRubyMethod(name = "puts")
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1) {
        return RubyIO.puts2(context, recv, arg0, arg1);
    }

    @JRubyMethod(name = "puts")
    public static IRubyObject puts(ThreadContext context, IRubyObject recv, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        return RubyIO.puts3(context, recv, arg0, arg1, arg2);
    }

    private static RubyIO getCurrentDataFile(ThreadContext context, String errorMessage) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (!data.next_argv(context)) throw argumentError(context, errorMessage);

        return (RubyIO) data.currentFile;
    }

    private static RubyIO argfWriteIO(ThreadContext context) {
        ArgsFileData data = ArgsFileData.getArgsFileData(context.runtime);

        if (!data.next_argv(context)) throw argumentError(context, "not opened for writing");

        return ((RubyIO) data.currentFile).GetWriteIO();
    }

    private static JavaSites.ArgfSites sites(ThreadContext context) {
        return context.sites.Argf;
    }
}