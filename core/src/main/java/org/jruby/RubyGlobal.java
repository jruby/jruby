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
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Tim Azzopardi <tim@tigerfive.com>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * Copyright (C) 2006 Michael Studman <codehaus@michaelstudman.com>
 * Copyright (C) 2008 Joseph LaFata <joe@quibb.org>
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

import jnr.enxio.channels.NativeDeviceChannel;
import jnr.posix.POSIX;

import org.jcodings.Encoding;
import org.jcodings.specific.USASCIIEncoding;
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.GlobalSite;
import org.jruby.runtime.Helpers;
import org.jruby.platform.Platform;
import org.jruby.runtime.Constants;
import org.jruby.runtime.ReadonlyGlobalVariable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.OSEnvironment;
import org.jruby.util.RegexpOptions;
import org.jruby.util.cli.Options;
import org.jruby.util.cli.OutputStrings;
import org.jruby.util.io.ChannelHelper;
import org.jruby.util.io.EncodingUtils;
import org.jruby.util.io.FilenoUtil;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.STDIO;

import static org.jruby.internal.runtime.GlobalVariable.Scope.*;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.util.Map;

/** This class initializes global variables and constants.
 *
 * @author jpetersen
 */
public class RubyGlobal {
    public static void initARGV(Ruby runtime) {
        // define ARGV and $* for this runtime
        RubyArray argvArray = runtime.newArray();
        String[] argv = runtime.getInstanceConfig().getArgv();

        for (String arg : argv) {
            argvArray.append(RubyString.newInternalFromJavaExternal(runtime, arg).tainted());
        }

        if (runtime.getObject().getConstantNoConstMissing("ARGV") != null) {
            ((RubyArray)runtime.getObject().getConstant("ARGV")).replace(argvArray);
        } else {
            runtime.getObject().setConstantQuiet("ARGV", argvArray);
            runtime.getGlobalVariables().define("$*", new GlobalSite(runtime, "$*", argvArray) {

            }, GLOBAL);
        }
    }

    public static void createGlobals(ThreadContext context, Ruby runtime) {
        GlobalVariables globals = runtime.getGlobalVariables();

        runtime.defineGlobalConstant("TOPLEVEL_BINDING", runtime.newBinding());

        runtime.defineGlobalConstant("TRUE", runtime.getTrue());
        runtime.defineGlobalConstant("FALSE", runtime.getFalse());
        runtime.defineGlobalConstant("NIL", runtime.getNil());

        runtime.getObject().deprecateConstant(runtime, "TRUE");
        runtime.getObject().deprecateConstant(runtime, "FALSE");
        runtime.getObject().deprecateConstant(runtime, "NIL");

        initARGV(runtime);

        GlobalSite programName = new RubyGlobal.StringCoerceGlobalVariable(runtime, "$PROGRAM_NAME", runtime.newString(runtime.getInstanceConfig().displayedFileName()));
        runtime.getGlobalVariables().define(programName, GLOBAL);
        runtime.getGlobalVariables().define("$0", programName, GLOBAL);

        // Version information:
        IRubyObject version = null;
        IRubyObject patchlevel = null;
        IRubyObject release = runtime.newString(Constants.COMPILE_DATE).freeze(context);
        IRubyObject platform = runtime.newString(Constants.PLATFORM).freeze(context);
        IRubyObject engine = runtime.newString(Constants.ENGINE).freeze(context);

        version = runtime.newString(Constants.RUBY_VERSION).freeze(context);
        patchlevel = runtime.newFixnum(0);
        runtime.defineGlobalConstant("RUBY_VERSION", version);
        runtime.defineGlobalConstant("RUBY_PATCHLEVEL", patchlevel);
        runtime.defineGlobalConstant("RUBY_RELEASE_DATE", release);
        runtime.defineGlobalConstant("RUBY_PLATFORM", platform);

        IRubyObject description = runtime.newString(OutputStrings.getVersionString()).freeze(context);
        runtime.defineGlobalConstant("RUBY_DESCRIPTION", description);

        IRubyObject copyright = runtime.newString(OutputStrings.getCopyrightString()).freeze(context);
        runtime.defineGlobalConstant("RUBY_COPYRIGHT", copyright);

        runtime.defineGlobalConstant("RELEASE_DATE", release);
        runtime.defineGlobalConstant("PLATFORM", platform);

        IRubyObject jrubyVersion = runtime.newString(Constants.VERSION).freeze(context);
        IRubyObject jrubyRevision = runtime.newString(Constants.REVISION).freeze(context);
        runtime.defineGlobalConstant("JRUBY_VERSION", jrubyVersion);
        runtime.defineGlobalConstant("JRUBY_REVISION", jrubyRevision);

        // needs to be a fixnum, but our revision is a sha1 hash from git
        runtime.defineGlobalConstant("RUBY_REVISION", runtime.newFixnum(Constants.RUBY_REVISION));
        runtime.defineGlobalConstant("RUBY_ENGINE", engine);
        runtime.defineGlobalConstant("RUBY_ENGINE_VERSION", jrubyVersion);

        final GlobalSite kcodeGV;
        kcodeGV = new NonEffectiveGlobalVariable(runtime, "$KCODE", runtime.getNil());

        runtime.defineVariable(kcodeGV, GLOBAL);
        runtime.defineVariable(new GlobalSite.Copy(runtime, "$-K", kcodeGV), GLOBAL);
        IRubyObject defaultRS = runtime.newString(runtime.getInstanceConfig().getRecordSeparator()).freeze(context);
        GlobalSite rs = new StringGlobalVariable(runtime, "$/", defaultRS);
        runtime.defineVariable(rs, GLOBAL);
        runtime.setRecordSeparatorVar(rs);
        globals.setDefaultSeparator(defaultRS);
        runtime.defineVariable(new StringGlobalVariable(runtime, "$\\", runtime.getNil()), GLOBAL);
        runtime.defineVariable(new StringGlobalVariable(runtime, "$,", runtime.getNil()), GLOBAL);

        runtime.defineVariable(new LineNumberGlobalVariable(runtime, "$."), GLOBAL);
        runtime.defineVariable(new LastlineGlobalVariable(runtime, "$_"), FRAME);
        runtime.defineVariable(new LastExitStatusVariable(runtime, "$?"), THREAD);

        runtime.defineVariable(new ErrorInfoGlobalVariable(runtime, "$!"), THREAD);
        runtime.defineVariable(new NonEffectiveGlobalVariable(runtime, "$=", runtime.getFalse()), GLOBAL);

        if(runtime.getInstanceConfig().getInputFieldSeparator() == null) {
            runtime.defineVariable(new StringOrRegexpGlobalVariable(runtime, "$;", runtime.getNil()), GLOBAL);
        } else {
            runtime.defineVariable(new StringOrRegexpGlobalVariable(runtime, "$;", RubyRegexp.newRegexp(runtime, runtime.getInstanceConfig().getInputFieldSeparator(), new RegexpOptions())), GLOBAL);
        }

        runtime.defineVariable(new SafeGlobalVariable(runtime, "$SAFE"), THREAD);

        runtime.defineVariable(new BacktraceGlobalVariable(runtime, "$@"), THREAD);

        initSTDIO(runtime, globals);

        runtime.defineVariable(new LoadedFeatures(runtime, "$\""), GLOBAL);
        runtime.defineVariable(new LoadedFeatures(runtime, "$LOADED_FEATURES"), GLOBAL);

        runtime.defineVariable(new LoadPath(runtime, "$:"), GLOBAL);
        runtime.defineVariable(new LoadPath(runtime, "$-I"), GLOBAL);
        runtime.defineVariable(new LoadPath(runtime, "$LOAD_PATH"), GLOBAL);

        runtime.defineVariable(new MatchMatchGlobalVariable(runtime, "$&"), FRAME);
        runtime.defineVariable(new PreMatchGlobalVariable(runtime, "$`"), FRAME);
        runtime.defineVariable(new PostMatchGlobalVariable(runtime, "$'"), FRAME);
        runtime.defineVariable(new LastMatchGlobalVariable(runtime, "$+"), FRAME);
        runtime.defineVariable(new BackRefGlobalVariable(runtime, "$~"), FRAME);

        // On platforms without a c-library accessible through JNR, getpid will return hashCode
        // as $$ used to. Using $$ to kill processes could take down many runtimes, but by basing
        // $$ on getpid() where available, we have the same semantics as MRI.
        globals.defineReadonly("$$", runtime.newFixnum(runtime.getPosix().getpid()), GLOBAL);

        // after defn of $stderr as the call may produce warnings
        defineGlobalEnvConstants(runtime);

        // if $* is nil by this point, make it read-only
        if (globals.get("$*").isNil()) {
            globals.defineReadonly("$*", runtime.newArray(), GLOBAL);
        }

        globals.defineReadonly("$-p",
                () -> runtime.newBoolean(runtime.getInstanceConfig().isAssumePrinting()),
                GLOBAL);
        globals.defineReadonly("$-a",
                () -> runtime.newBoolean(runtime.getInstanceConfig().isSplit()),
                GLOBAL);
        globals.defineReadonly("$-l",
                () -> runtime.newBoolean(runtime.getInstanceConfig().isProcessLineEnds()),
                GLOBAL);

        // ARGF, $< object
        RubyArgsFile.initArgsFile(runtime);

        globals.alias("$-0", "$/");

        // Define aliases originally in the "English.rb" stdlib
        globals.alias("$ERROR_INFO", "$!");
        globals.alias("$ERROR_POSITION", "$@");
        globals.alias("$FS", "$;");
        globals.alias("$FIELD_SEPARATOR", "$;");
        globals.alias("$OFS", "$,");
        globals.alias("$OUTPUT_FIELD_SEPARATOR", "$,");
        globals.alias("$RS", "$/");
        globals.alias("$INPUT_RECORD_SEPARATOR", "$/");
        globals.alias("$ORS", "$\\");
        globals.alias("$OUTPUT_RECORD_SEPARATOR", "$\\");
        globals.alias("$NR", "$.");
        globals.alias("$INPUT_LINE_NUMBER", "$.");
        globals.alias("$LAST_READ_LINE", "$_");
        globals.alias("$DEFAULT_OUTPUT", "$>");
        globals.alias("$DEFAULT_INPUT", "$<");
        globals.alias("$PID", "$$");
        globals.alias("$PROCESS_ID", "$$");
        globals.alias("$CHILD_STATUS", "$?");
        globals.alias("$LAST_MATCH_INFO", "$~");
        globals.alias("$IGNORECASE", "$=");
        globals.alias("$ARGV", "$*");
        globals.alias("$MATCH", "$&");
        globals.alias("$PREMATCH", "$`");
        globals.alias("$POSTMATCH", "$'");
        globals.alias("$LAST_PAREN_MATCH", "$+");
    }

    public static void initSTDIO(Ruby runtime, GlobalVariables globals) {
        RubyIO stdin, stdout, stderr;

        // If we're the main for the process and native stdio is enabled, use default descriptors
        if (!Platform.IS_WINDOWS && // Windows does not do native IO yet
                runtime.getPosix().isNative() &&
                runtime.getInstanceConfig().isHardExit() && // main JRuby only
                Options.NATIVE_STDIO.load()) {
            stdin = RubyIO.prepStdio(
                    runtime, runtime.getIn(), new NativeDeviceChannel(0), OpenFile.READABLE, runtime.getIO(), "<STDIN>");
            stdout = RubyIO.prepStdio(
                    runtime, runtime.getOut(), new NativeDeviceChannel(1), OpenFile.WRITABLE, runtime.getIO(), "<STDOUT>");
            stderr = RubyIO.prepStdio(
                    runtime, runtime.getErr(), new NativeDeviceChannel(2), OpenFile.WRITABLE | OpenFile.SYNC, runtime.getIO(), "<STDERR>");
        } else {
            stdin = RubyIO.prepStdio(
                    runtime, runtime.getIn(), prepareStdioChannel(runtime, STDIO.IN, runtime.getIn()), OpenFile.READABLE, runtime.getIO(), "<STDIN>");
            stdout = RubyIO.prepStdio(
                    runtime, runtime.getOut(), prepareStdioChannel(runtime, STDIO.OUT, runtime.getOut()), OpenFile.WRITABLE, runtime.getIO(), "<STDOUT>");
            stderr = RubyIO.prepStdio(
                    runtime, runtime.getErr(), prepareStdioChannel(runtime, STDIO.ERR, runtime.getErr()), OpenFile.WRITABLE | OpenFile.SYNC, runtime.getIO(), "<STDERR>");
        }

        if (runtime.getObject().getConstantFromNoConstMissing("STDIN") == null) {
            runtime.defineVariable(new InputGlobalVariable(runtime, "$stdin", stdin), GLOBAL);
            runtime.defineVariable(new OutputGlobalVariable(runtime, "$stdout", stdout), GLOBAL);
            globals.alias("$>", "$stdout");
            runtime.defineVariable(new OutputGlobalVariable(runtime, "$stderr", stderr), GLOBAL);

            runtime.defineGlobalConstant("STDIN", stdin);
            runtime.defineGlobalConstant("STDOUT", stdout);
            runtime.defineGlobalConstant("STDERR", stderr);
        } else {
            ((RubyIO) runtime.getObject().getConstant("STDIN")).getOpenFile().setFD(stdin.getOpenFile().fd());
            ((RubyIO) runtime.getObject().getConstant("STDOUT")).getOpenFile().setFD(stdout.getOpenFile().fd());
            ((RubyIO) runtime.getObject().getConstant("STDERR")).getOpenFile().setFD(stderr.getOpenFile().fd());
        }
    }

    private static Channel prepareStdioChannel(Ruby runtime, STDIO stdio, Object stream) {
        if (runtime.getPosix().isNative() && stdio.isJVMDefault(stream) && !Platform.IS_WINDOWS) {
            // use real native fileno for stdio, if possible

            // try typical stdio stream and channel types
            int fileno = -1;
            Channel channel = null;

            if (stream instanceof Channel) {
                channel = (Channel) stream;
                fileno = FilenoUtil.filenoFrom(channel);
            } else if (stream instanceof InputStream) {
                InputStream unwrappedStream = ChannelHelper.unwrapFilterInputStream((InputStream) stream);
                if (unwrappedStream instanceof FileInputStream) {
                    fileno = FilenoUtil.filenoFrom(((FileInputStream) unwrappedStream).getChannel());
                }
            } else if (stream instanceof OutputStream) {
                OutputStream unwrappedStream = ChannelHelper.unwrapFilterOutputStream((OutputStream) stream);
                if (unwrappedStream instanceof FileOutputStream) {
                    fileno = FilenoUtil.filenoFrom(((FileOutputStream) unwrappedStream).getChannel());
                }
            }

            if (fileno >= 0) {
                // We got a real fileno, use it
                return new NativeDeviceChannel(fileno);
            }
        }

        // fall back on non-direct stdio
        // NOTE (CON): This affects interactivity in any case where we cannot determine the real fileno and use native.
        //             We do force flushing of stdout and stdout, but we can't provide all the interactive niceities
        //             without a proper native channel. See https://github.com/jruby/jruby/issues/2690
        switch (stdio) {
            case IN:
                return Channels.newChannel((InputStream)stream);
            case OUT:
            case ERR:
                return Channels.newChannel((OutputStream)stream);
            default: throw new RuntimeException("invalid stdio: " + stdio);
        }
    }

    // TODO: gross...see https://github.com/ninjudd/drip/issues/96
    private static int unwrapDripStream(Object stream) {
        if (stream.getClass().getName().startsWith("org.flatland.drip.Switchable")) {

            try {
                FileDescriptor fd = (FileDescriptor) stream.getClass().getMethod("getFD").invoke(stream);
                return FilenoUtil.filenoFrom(fd);
            } catch (NoSuchMethodException nsme) {
                nsme.printStackTrace(System.err);
            } catch (IllegalAccessException iae) {
                iae.printStackTrace(System.err);
            } catch (InvocationTargetException ite) {
                ite.printStackTrace(System.err);
            }
        }

        return -1;
    }


    @SuppressWarnings("unchecked")
    private static void defineGlobalEnvConstants(Ruby runtime) {
    	Map<RubyString, RubyString> environmentVariableMap = OSEnvironment.environmentVariableMap(runtime);
    	RubyHash env = new CaseInsensitiveStringOnlyRubyHash(
            runtime, environmentVariableMap, runtime.getNil(),
            runtime.getInstanceConfig().isNativeEnabled() && runtime.getInstanceConfig().isUpdateNativeENVEnabled()
        );
        env.getSingletonClass().defineAnnotatedMethods(CaseInsensitiveStringOnlyRubyHash.class);
        runtime.defineGlobalConstant("ENV", env);
        runtime.setENV(env);

        // Define System.getProperties() in ENV_JAVA
        Map<RubyString, RubyString> systemPropertiesMap = OSEnvironment.systemPropertiesMap(runtime);
        RubyHash envJava = new ReadOnlySystemPropertiesHash(
                runtime, systemPropertiesMap, runtime.getNil()
        );
        envJava.setFrozen(true);
        runtime.defineGlobalConstant("ENV_JAVA", envJava);
    }

    /**
     * Obligate string-keyed and string-valued hash, used for ENV.
     * On Windows, the keys are case-insensitive for ENV
     *
     */
    public static class CaseInsensitiveStringOnlyRubyHash extends StringOnlyRubyHash {

        public CaseInsensitiveStringOnlyRubyHash(Ruby runtime, Map<RubyString, RubyString> valueMap, IRubyObject defaultValue, boolean updateRealENV) {
            super(runtime, valueMap, defaultValue, updateRealENV);
        }

        protected final boolean isCaseSensitive() { return false; }

        @Override
        public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
            IRubyObject val = super.op_aref(context, key);
            val.setTaint(true);
            return val;
        }

        private static final ByteList ENV = new ByteList(new byte[] {'E','N','V'}, USASCIIEncoding.INSTANCE, false);

        @JRubyMethod(name = "to_s")
        public RubyString to_s(ThreadContext context) {
            return RubyString.newStringShared(context.runtime, ENV);
        }

        @Override
        public IRubyObject to_s() {
            return RubyString.newStringShared(getRuntime(), ENV);
        }

        @JRubyMethod
        public RubyHash to_h(){
            return to_hash();
        }

    }

    /**
     * A Pseudo-hash whose keys and values are required to be Strings.
     * On all platforms, the keys are case-sensitive.
     * Used for ENV_JAVA.
     */
    public static class StringOnlyRubyHash extends RubyHash {
        // This is an ugly hack.  Windows ENV map processing all happens in this
        // class and not in the caseinsensitive hash.  In order to not refactor
        // both of these maps we will pass in a flag to specify whether we want
        // the op_aset to also update the real ENV map via setenv/unsetenv.
        private final boolean updateRealENV;

        public StringOnlyRubyHash(Ruby runtime, Map<RubyString, RubyString> valueMap, IRubyObject defaultValue, boolean updateRealENV) {
            super(runtime, valueMap, defaultValue);
            this.updateRealENV = updateRealENV;
        }

        protected boolean isCaseSensitive() { return true; }

        public StringOnlyRubyHash(Ruby runtime, Map<RubyString, RubyString> valueMap, IRubyObject defaultValue) {
            this(runtime, valueMap, defaultValue, false);
        }

        @Override
        public RubyHash to_hash() {
            Ruby runtime = getRuntime();
            RubyHash hash = RubyHash.newHash(runtime);
            hash.replace(runtime.getCurrentContext(), this);
            return hash;
        }

        @Override
        protected IRubyObject internalGet(IRubyObject key) {
            if (size == 0) return null;

            if (!isCaseSensitive()) {
                key = getCorrectKey(key.convertToString());
            }

            return super.internalGet(key);
        }

        @Override
        public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject value) {
            return case_aware_op_aset(context, key, value);
        }

        @Override
        @Deprecated
        public IRubyObject op_aset19(ThreadContext context, IRubyObject key, IRubyObject value) {
            return op_aset(context, key, value);
        }

        private IRubyObject case_aware_op_aset(ThreadContext context, IRubyObject key, final IRubyObject value) {
            if (!isStringLike(key)) {
                throw context.runtime.newTypeError("can't convert " + key.getMetaClass() + " into String");
            }
            RubyString keyAsStr = key.convertToString();
            if (!isCaseSensitive()) key = keyAsStr = getCorrectKey(keyAsStr);

            if (value == context.nil) {
                return super.delete(context, key, org.jruby.runtime.Block.NULL_BLOCK);
            }
            if (!isStringLike(value)) {
                throw context.runtime.newTypeError("can't convert " + value.getMetaClass() + " into String");
            }

            RubyString valueAsStr = normalizeEnvString(context, keyAsStr, value.convertToString());

            if (updateRealENV) {
                final POSIX posix = context.runtime.getPosix();
                final String keyAsJava = keyAsStr.asJavaString();
                // libc (un)setenv is not reentrant, so we need to synchronize across the entire JVM (JRUBY-5933)
                if (valueAsStr == context.nil) {
                    synchronized (Object.class) { posix.unsetenv(keyAsJava); }
                } else {
                    final String valueAsJava = valueAsStr.asJavaString();
                    synchronized (Object.class) { posix.setenv(keyAsJava, valueAsJava, 1); }
                }
            }

            return super.op_aset(context, keyAsStr, valueAsStr);
        }

        private static boolean isStringLike(final IRubyObject obj) {
            return obj instanceof RubyString || obj.respondsTo("to_str");
        }

        private RubyString getCorrectKey(final RubyString key) {
            RubyString actualKey = key;
            if (Platform.IS_WINDOWS) {
                // this is a rather ugly hack, but similar to MRI. See hash.c:ruby_setenv and similar in MRI
                // we search all keys for a case-insensitive match, and use that
                final ThreadContext context = getRuntime().getCurrentContext();
                final RubyArray keys = super.keys(context);
                for (int i = 0; i < keys.size(); i++) {
                    RubyString candidateKey = keys.eltInternal(i).convertToString();
                    if (equalIgnoreCase(context, candidateKey, key)) {
                        actualKey = candidateKey;
                        break;
                    }
                }
            }
            return actualKey;
        }

        private static final ByteList PATH_BYTES = new ByteList(new byte[] {'P','A','T','H'}, USASCIIEncoding.INSTANCE, false);

        private static RubyString normalizeEnvString(ThreadContext context, RubyString key, RubyString value) {
            final Ruby runtime = context.runtime;

            RubyString valueStr;

            // Ensure PATH is encoded like filesystem
            if (Platform.IS_WINDOWS ?
                    equalIgnoreCase(context, key, RubyString.newString(context.runtime, PATH_BYTES)) :
                        key.getByteList().equal(PATH_BYTES)) {
                Encoding enc = runtime.getEncodingService().getFileSystemEncoding();
                valueStr = EncodingUtils.strConvEnc(context, value, value.getEncoding(), enc);
                if (value == valueStr) valueStr = (RubyString) value.dup();
            } else {
                valueStr = RubyString.newString(runtime, value.toString(), runtime.getEncodingService().getLocaleEncoding());
            }

            valueStr.setFrozen(true);
            return valueStr;
        }

        private static boolean equalIgnoreCase(ThreadContext context, final RubyString str1, final RubyString str2) {
            return ((RubyFixnum) str1.casecmp(context, str2)).isZero();
        }

    }

    private static class ReadOnlySystemPropertiesHash extends StringOnlyRubyHash {
        public ReadOnlySystemPropertiesHash(Ruby runtime, Map<RubyString, RubyString> valueMap, IRubyObject defaultValue, boolean updateRealENV) {
            super(runtime, valueMap, defaultValue, updateRealENV);
        }

        public ReadOnlySystemPropertiesHash(Ruby runtime, Map<RubyString, RubyString> valueMap, IRubyObject defaultValue) {
            this(runtime, valueMap, defaultValue, false);
        }

        public void modify() {
            if (isFrozen()) {
                throw getRuntime().newTypeError("ENV_JAVA is not writable until you require 'java'");
            }
        }
    }

    /**
     * A GlobalSite that never updates and always warns about ineffective variable.
     */
    private static class NonEffectiveGlobalVariable extends GlobalSite {
        public NonEffectiveGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public void getFold() {
            runtime.getWarnings().warn(ID.INEFFECTIVE_GLOBAL, "warning: variable " + name + " is no longer effective");
        }

        @Override
        public IRubyObject setFilter(IRubyObject value) {
            runtime.getWarnings().warn(ID.INEFFECTIVE_GLOBAL, "warning: variable " + name + " is no longer effective; ignored");

            return value;
        }

        @Override
        public void setTarget(MethodHandle target) {
            // ignored; keep initial value forever
        }
    }

    private static class LastExitStatusVariable extends GlobalSite {
        public LastExitStatusVariable(Ruby runtime, String name) {
            super(runtime, name, () -> runtime.getCurrentContext().getLastExitStatus());
        }

        @Override
        public IRubyObject getFilter(IRubyObject lastExitStatus) {
            return lastExitStatus == null ? runtime.getNil() : lastExitStatus;
        }

        @Override
        public IRubyObject setFilter(IRubyObject lastExitStatus) {
            throw runtime.newNameError("$? is a read-only variable", "$?");
        }
    }

    private static class BackRefGlobalVariable extends GlobalSite {
        public BackRefGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, () -> Helpers.backref(runtime.getCurrentContext()));
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            Helpers.setBackref(runtime, runtime.getCurrentContext(), value);
            return value;
        }
    }

    private static class MatchMatchGlobalVariable extends BackRefGlobalVariable {
        public MatchMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name);
        }

        @Override
        public IRubyObject getFilter(IRubyObject backref) {
            return RubyRegexp.last_match(backref);
        }
    }

    private static class PreMatchGlobalVariable extends BackRefGlobalVariable {
        public PreMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name);
        }

        @Override
        public IRubyObject getFilter(IRubyObject backref) {
            return RubyRegexp.match_pre(backref);
        }
    }

    private static class PostMatchGlobalVariable extends BackRefGlobalVariable {
        public PostMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name);
        }

        @Override
        public IRubyObject getFilter(IRubyObject backref) {
            return RubyRegexp.match_post(backref);
        }
    }

    private static class LastMatchGlobalVariable extends BackRefGlobalVariable {
        public LastMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name);
        }

        @Override
        public IRubyObject get() {
            return RubyRegexp.match_last(runtime.getCurrentContext().getBackRef());
        }
    }

    // Accessor methods.

    private static class LineNumberGlobalVariable extends GlobalSite {
        public LineNumberGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, () -> runtime.newFixnum(runtime.getCurrentLine()));
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            int line = (int)value.convertToInteger().getLongValue();
            runtime.setCurrentLine(line);
            return value;
        }
    }

    private static class ErrorInfoGlobalVariable extends GlobalSite {
        public ErrorInfoGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, () -> runtime.getCurrentContext().getErrorInfo());
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            if (!value.isNil() &&
                    !(value instanceof RubyException) &&
                    !(JavaUtil.isJavaObject(value) && JavaUtil.unwrapJavaObject(value) instanceof Throwable)) {
                throw runtime.newTypeError("assigning non-exception to $!");
            }

            return runtime.getCurrentContext().setErrorInfo(value);
        }
    }

    private static class BacktraceGlobalVariable extends GlobalSite {
        public BacktraceGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, () -> {
                ThreadContext context = runtime.getCurrentContext();
                IRubyObject errorInfo = context.getErrorInfo();
                IRubyObject nil = context.nil;

                IRubyObject backtrace = errorInfo.isNil() ? nil : errorInfo.callMethod(context, "backtrace");

                //$@ returns nil if $!.backtrace is not an array
                if (!(backtrace instanceof RubyArray)) {
                    backtrace = nil;
                }

                return backtrace;
            });
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            ThreadContext context = runtime.getCurrentContext();
            IRubyObject errorInfo = context.getErrorInfo();

            if (errorInfo.isNil()) {
                throw runtime.newArgumentError("$! not set");
            }

            errorInfo.callMethod(context, "set_backtrace", value);

            return value;
        }
    }

    // FIXME: move out of this class!
    public static class StringGlobalVariable extends GlobalSite {
        public StringGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject setFilter(IRubyObject value) {
            if (!value.isNil() && !(value instanceof RubyString)) {
                throw runtime.newTypeError("value of " + name() + " must be a String");
            }

            return value;
        }
    }

    public static class StringCoerceGlobalVariable extends GlobalSite {
        public StringCoerceGlobalVariable(Ruby runtime, String name, RubyString value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject setFilter(IRubyObject value) {
            return value.convertToString();
        }
    }

    public static class StringOrRegexpGlobalVariable extends GlobalSite {
        public StringOrRegexpGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject setFilter(IRubyObject value) {
            if (!value.isNil() && !(value instanceof RubyString) && !(value instanceof RubyRegexp)) {
                throw runtime.newTypeError("value of " + name() + " must be String or Regexp");
            }

            return value;
        }
    }

    private static class SafeGlobalVariable extends GlobalSite {
        public SafeGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, RubyFixnum.zero(runtime));
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            runtime.getWarnings().warnOnce(ID.SAFE_NOT_SUPPORTED, "SAFE levels are not supported in JRuby");

            return value;
        }
    }

    static class TristateGlobalVariable extends GlobalSite {
        public TristateGlobalVariable(Ruby runtime, String name, IRubyObject initial) {
            super(runtime, name, initial);
        }

        @Override
        public IRubyObject setFilter(IRubyObject newValue) {
            if (!newValue.isNil()) return runtime.newBoolean(newValue.isTrue());

            return newValue;
        }
    }

    public static class WarningGlobalVariable extends ReadonlyGlobalVariable {
        public WarningGlobalVariable(Ruby runtime, String name, MethodHandle reader) {
            super(runtime, name, reader);
        }

        @Override
        public IRubyObject getFilter(IRubyObject verbose) {
            if (verbose.isNil()) return RubyFixnum.zero(runtime);
            if (verbose.isTrue()) return RubyFixnum.two(runtime);
            else return RubyFixnum.one(runtime);
        }
    }

    private static class LastlineGlobalVariable extends GlobalSite {
        public LastlineGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, () -> Helpers.getLastLine(runtime, runtime.getCurrentContext()));
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            Helpers.setLastLine(runtime, runtime.getCurrentContext(), value);
            return value;
        }
    }

    public static class InputGlobalVariable extends GlobalSite {
        public InputGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            if (value == get()) {
                return value;
            }

            return super.set(value);
        }
    }

    public static class OutputGlobalVariable extends GlobalSite {
        public OutputGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject setFilter(IRubyObject value) {
            if (value == get()) {
                return value;
            }

            if (!value.respondsTo("write") && !value.respondsToMissing("write")) {
                throw runtime.newTypeError(name() + " must have write method, " +
                                    value.getType().getName() + " given");
            }

            return value;
        }
    }

    private static class LoadPath extends ReadonlyGlobalVariable {
        public LoadPath(Ruby runtime, String name) {
            super(runtime, name, () -> runtime.getLoadService().getLoadPath());
        }
    }

    private static class LoadedFeatures extends ReadonlyGlobalVariable {
        public LoadedFeatures(Ruby runtime, String name) {
            super(runtime, name, () -> runtime.getLoadService().getLoadedFeatures());
        }
    }

}
