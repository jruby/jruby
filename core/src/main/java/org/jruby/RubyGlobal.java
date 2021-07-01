/*
 **** BEGIN LICENSE BLOCK *****
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
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.ir.Tuple;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Helpers;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.Constants;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ReadonlyGlobalVariable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.encoding.EncodingService;
import org.jruby.util.ByteList;
import org.jruby.util.ByteListHelper;
import org.jruby.util.KCode;
import org.jruby.util.OSEnvironment;
import org.jruby.util.RegexpOptions;
import org.jruby.util.cli.Options;
import org.jruby.util.cli.OutputStrings;
import org.jruby.util.io.ChannelHelper;
import org.jruby.util.io.FilenoUtil;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.STDIO;

import static org.jruby.internal.runtime.GlobalVariable.Scope.*;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.io.EncodingUtils.newExternalStringWithEncoding;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            runtime.getGlobalVariables().define("$*", new ValueAccessor(argvArray), GLOBAL);
        }
    }

    public static void createGlobals(Ruby runtime) {
        GlobalVariables globals = runtime.getGlobalVariables();

        runtime.defineGlobalConstant("TOPLEVEL_BINDING", runtime.newBinding());

        runtime.defineGlobalConstant("TRUE", runtime.getTrue());
        runtime.defineGlobalConstant("FALSE", runtime.getFalse());
        runtime.defineGlobalConstant("NIL", runtime.getNil());

        runtime.getObject().deprecateConstant(runtime, "TRUE");
        runtime.getObject().deprecateConstant(runtime, "FALSE");
        runtime.getObject().deprecateConstant(runtime, "NIL");

        initARGV(runtime);

        IAccessor d = new ValueAccessor(runtime.newString(
                runtime.getInstanceConfig().displayedFileName()));
        globals.define("$PROGRAM_NAME", d, GLOBAL);
        globals.define("$0", d, GLOBAL);

        // Version information:
        IRubyObject version;
        IRubyObject patchlevel;
        IRubyObject release = runtime.newString(Constants.COMPILE_DATE);
        release.setFrozen(true);
        IRubyObject platform = runtime.newString(Constants.PLATFORM);
        release.setFrozen(true);
        IRubyObject engine = runtime.newString(Constants.ENGINE);
        release.setFrozen(true);

        version = runtime.newString(Constants.RUBY_VERSION);
        release.setFrozen(true);
        patchlevel = runtime.newFixnum(0);
        runtime.defineGlobalConstant("RUBY_VERSION", version);
        runtime.defineGlobalConstant("RUBY_PATCHLEVEL", patchlevel);
        runtime.defineGlobalConstant("RUBY_RELEASE_DATE", release);
        runtime.defineGlobalConstant("RUBY_PLATFORM", platform);

        IRubyObject description = runtime.newString(OutputStrings.getVersionString());
        release.setFrozen(true);
        runtime.defineGlobalConstant("RUBY_DESCRIPTION", description);

        IRubyObject copyright = runtime.newString(OutputStrings.getCopyrightString());
        release.setFrozen(true);
        runtime.defineGlobalConstant("RUBY_COPYRIGHT", copyright);

        runtime.defineGlobalConstant("RELEASE_DATE", release);
        runtime.defineGlobalConstant("PLATFORM", platform);

        IRubyObject jrubyVersion = runtime.newString(Constants.VERSION);
        release.setFrozen(true);
        IRubyObject jrubyRevision = runtime.newString(Constants.REVISION);
        release.setFrozen(true);
        runtime.defineGlobalConstant("JRUBY_VERSION", jrubyVersion);
        runtime.defineGlobalConstant("JRUBY_REVISION", jrubyRevision);

        // needs to be a fixnum, but our revision is a sha1 hash from git
        runtime.defineGlobalConstant("RUBY_REVISION", runtime.newFixnum(Constants.RUBY_REVISION));
        runtime.defineGlobalConstant("RUBY_ENGINE", engine);
        runtime.defineGlobalConstant("RUBY_ENGINE_VERSION", jrubyVersion);

        RubyInstanceConfig.Verbosity verbosity = runtime.getInstanceConfig().getVerbosity();
        runtime.defineVariable(new WarningGlobalVariable(runtime, "$-W", verbosity), GLOBAL);

        final GlobalVariable kcodeGV;
        kcodeGV = new NonEffectiveGlobalVariable(runtime, "$KCODE", runtime.getNil());

        runtime.defineVariable(kcodeGV, GLOBAL);
        runtime.defineVariable(new GlobalVariable.Copy(runtime, "$-K", kcodeGV), GLOBAL);
        IRubyObject defaultRS = runtime.newString(runtime.getInstanceConfig().getRecordSeparator());
        release.setFrozen(true);
        GlobalVariable rs = new StringGlobalVariable(runtime, "$/", defaultRS);
        runtime.defineVariable(rs, GLOBAL);
        runtime.setRecordSeparatorVar(rs);
        globals.setDefaultSeparator(defaultRS);
        runtime.defineVariable(new StringGlobalVariable(runtime, "$\\", runtime.getNil()), GLOBAL);
        runtime.defineVariable(new StringGlobalVariable(runtime, "$,", runtime.getNil()), GLOBAL);

        runtime.defineVariable(new LineNumberGlobalVariable(runtime, "$."), GLOBAL);
        runtime.defineVariable(new LastlineGlobalVariable(runtime, "$_"), FRAME);
        runtime.defineVariable(new LastExitStatusVariable(runtime, "$?"), THREAD);

        runtime.defineVariable(new ErrorInfoGlobalVariable(runtime, "$!", runtime.getNil()), THREAD);
        runtime.defineVariable(new NonEffectiveGlobalVariable(runtime, "$=", runtime.getFalse()), GLOBAL);

        if(runtime.getInstanceConfig().getInputFieldSeparator() == null) {
            runtime.defineVariable(new StringOrRegexpGlobalVariable(runtime, "$;", runtime.getNil()), GLOBAL);
        } else {
            runtime.defineVariable(new StringOrRegexpGlobalVariable(runtime, "$;", RubyRegexp.newRegexp(runtime, runtime.getInstanceConfig().getInputFieldSeparator(), new RegexpOptions())), GLOBAL);
        }

        RubyInstanceConfig.Verbosity verbose = runtime.getInstanceConfig().getVerbosity();
        IRubyObject verboseValue;
        if (verbose == RubyInstanceConfig.Verbosity.NIL) {
            verboseValue = runtime.getNil();
        } else if(verbose == RubyInstanceConfig.Verbosity.TRUE) {
            verboseValue = runtime.getTrue();
        } else {
            verboseValue = runtime.getFalse();
        }
        runtime.defineVariable(new VerboseGlobalVariable(runtime, "$VERBOSE", verboseValue), GLOBAL);
        runtime.defineVariable(new VerboseGlobalVariable(runtime, "$-v", verboseValue), GLOBAL);
        runtime.defineVariable(new VerboseGlobalVariable(runtime, "$-w", verboseValue), GLOBAL);

        IRubyObject debug = runtime.newBoolean(runtime.getInstanceConfig().isDebug());
        runtime.defineVariable(new DebugGlobalVariable(runtime, "$DEBUG", debug), GLOBAL);
        runtime.defineVariable(new DebugGlobalVariable(runtime, "$-d", debug), GLOBAL);

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

        // On platforms without a c-library accessible through JNA, getpid will return hashCode
        // as $$ used to. Using $$ to kill processes could take down many runtimes, but by basing
        // $$ on getpid() where available, we have the same semantics as MRI.
        globals.defineReadonly("$$", new PidAccessor(runtime), GLOBAL);

        // after defn of $stderr as the call may produce warnings
        defineGlobalEnvConstants(runtime);

        // Fixme: Do we need the check or does Main.java not call this...they should consolidate
        if (globals.get("$*").isNil()) {
            globals.defineReadonly("$*", new ValueAccessor(runtime.newArray()), GLOBAL);
        }

        globals.defineReadonly("$-p",
                new ValueAccessor(runtime.newBoolean(runtime.getInstanceConfig().isAssumePrinting())),
                GLOBAL);
        globals.defineReadonly("$-a",
                new ValueAccessor(runtime.newBoolean(runtime.getInstanceConfig().isSplit())),
                GLOBAL);
        globals.defineReadonly("$-l",
                new ValueAccessor(runtime.newBoolean(runtime.getInstanceConfig().isProcessLineEnds())),
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
            Channel channel;

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

        @JRubyMethod(name = "[]", required = 1)
        public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
            IRubyObject key = arg.convertToString();
            IRubyObject value = internalGet(key);
            if (value == null) return context.nil;

            RubyString string = (RubyString) newName(context, key, value);

            string.freeze(context);

            return string;
        }

        private static final ByteList ENV = new ByteList(new byte[] {'E','N','V'}, USASCIIEncoding.INSTANCE, false);

        @JRubyMethod(name = "assoc")
        public IRubyObject assoc(final ThreadContext context, IRubyObject obj) {
            return super.assoc(context, verifyStringLike(context, obj).convertToString());
        }

        @JRubyMethod
        public IRubyObject delete(ThreadContext context, IRubyObject key, Block block) {
            return super.delete(context, verifyStringLike(context, key), block);
        }

        @JRubyMethod(name = {"each", "each_pair"})
        public IRubyObject each(final ThreadContext context, final Block block) {
            if (!block.isGiven()) return super.each(context, block);

            RubyArray ary = new RubyArray(context.runtime, size());

            visitAll(context, EachVisitor, ary);

            ary.eachSlice(context, 2, block);

            return this;
        }

        private static final VisitorWithState<RubyArray> EachVisitor = new VisitorWithState<RubyArray>() {
            @Override
            public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubyArray ary) {
                ary.append(newString(context, key));
                ary.append(newName(context, key, value));
            }
        };

        @JRubyMethod(name = "rassoc")
        public IRubyObject rassoc(final ThreadContext context, IRubyObject obj) {
            if (!isStringLike(obj)) return context.nil;

            return super.rassoc(context, obj.convertToString());
        }

        @JRubyMethod(name = {"has_key?", "key?", "include?", "member?"}, required = 1)
        public RubyBoolean has_key_p(ThreadContext context, IRubyObject key) {
            return internalGetEntry(verifyStringLike(context, key)) == NO_ENTRY ? context.fals : context.tru;
        }

        @JRubyMethod(name = {"has_value?", "value?"}, required = 1)
        public IRubyObject has_value_pp(ThreadContext context, IRubyObject expected) {
            if (!isStringLike(expected)) return context.nil;

            return super.has_value_p(context, expected.convertToString());
        }

        @JRubyMethod(name = "index")
        public IRubyObject index(ThreadContext context, IRubyObject expected) {
            context.runtime.getWarnings().warn(ID.DEPRECATED_METHOD, "ENV#index is deprecated; use ENV#key");

            return key(context, expected);
        }

        @JRubyMethod(name = "keys")
        public RubyArray keys(final ThreadContext context) {
            try {
                RubyArray keys = RubyArray.newBlankArrayInternal(context.runtime, size());

                visitAll(context, StoreKeyVisitor, keys);

                return keys;
            } catch (NegativeArraySizeException nase) {
                throw concurrentModification();
            }
        }

        private static final VisitorWithState<RubyArray> StoreKeyVisitor = new VisitorWithState<RubyArray>() {
            @Override
            public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubyArray keys) {
                keys.storeInternal(index, newString(context, key));
            }
        };

        @JRubyMethod
        public IRubyObject key(ThreadContext context, IRubyObject expected) {
            return super.key(context, verifyStringLike(context, expected));
        }

        @JRubyMethod(name = "rehash")
        public IRubyObject rehash1(ThreadContext context) {
            super.rehash(context);

            // In MRI they are interacting with the systems env at a C level so rehash does nothing.  We maintain
            // our hash as an actual Ruby Hash so we still rehash above.
            return context.nil;
        }

        @JRubyMethod(name = "replace", required = 1)
        public RubyHash replace(final ThreadContext context, IRubyObject other) {
            modify();

            final RubyHash otherHash = other.convertToHash();
            if (this == otherHash) return this;


            Set keys = new HashSet(directKeySet());
            Tuple<Set, RubyHash> tuple = new Tuple<>(keys, this);

            if (!isComparedByIdentity() && otherHash.isComparedByIdentity()) {
                setComparedByIdentity(true);
            }

            otherHash.visitAll(context, ReplaceVisitor, tuple);

            for (Object key: keys) {
                internalDelete((IRubyObject) key);
            }

            return this;
        }

        private static final VisitorWithState<Tuple<Set, RubyHash>> ReplaceVisitor = new VisitorWithState<Tuple<Set, RubyHash>>() {
            @Override
            public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, Tuple<Set, RubyHash> tuple) {
                tuple.a.remove(key);  // Remove from remove list since it is a valid key
                tuple.b.op_aset(context, key, value);
            }
        };

        @JRubyMethod(name = "shift")
        public IRubyObject shift(ThreadContext context) {
            // ENV dooes not support default_proc so we know 2 element Array.
            IRubyObject value = super.shift(context);

            if (value.isNil()) return value;
            // FIXME: If we cared we could reimplement this to not re-process the array elements.

            RubyArray pair = (RubyArray) value;
            IRubyObject key = pair.eltInternal(0);
            pair.eltInternalSet(0, newString(context, key));
            pair.eltInternalSet(1, newName(context, key, pair.eltInternal(1)));

            return pair;
        }

        @JRubyMethod(name = "to_s")
        public RubyString to_s(ThreadContext context) {
            return RubyString.newStringShared(context.runtime, ENV);
        }

        @Override
        public IRubyObject to_s() {
            return RubyString.newStringShared(getRuntime(), ENV);
        }

        @Deprecated
        public RubyHash to_h() {
            return to_h(getRuntime().getCurrentContext(), Block.NULL_BLOCK);
        }

        @JRubyMethod
        public RubyHash to_h(ThreadContext context, Block block){
            RubyHash h = to_hash(context);
            return block.isGiven() ? h.to_h_block(context, block) : h;
        }

        private final RaiseException concurrentModification() {
            return metaClass.runtime.newConcurrencyError(
                    "Detected invalid hash contents due to unsynchronized modifications with concurrent users");
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
        public RubyHash to_hash(ThreadContext context) {
            RubyHash hash = RubyHash.newHash(context.runtime);
            hash.replace(context, this);
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
            RubyString keyAsStr = verifyValidKey(context, verifyStringLike(context, key).convertToString(), value);

            if (!isCaseSensitive()) key = keyAsStr = getCorrectKey(keyAsStr);

            if (value == context.nil) {
                return super.delete(context, key, org.jruby.runtime.Block.NULL_BLOCK);
            }

            IRubyObject valueAsStr = newName(context, keyAsStr, verifyStringLike(context, value).convertToString());

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

            super.op_aset(context, keyAsStr, valueAsStr);

            return value;
        }

        protected static IRubyObject verifyStringLike(ThreadContext context, IRubyObject test) {
            if (!isStringLike(test)) {
                throw context.runtime.newTypeError("no implicit conversion of " + test.getMetaClass() + " into String");
            }

            return test;
        }

        private static RubyString verifyValidKey(ThreadContext context, RubyString key, IRubyObject value) {
            if (value.isNil()) return key;

            ByteList bytes = key.getByteList();
            int e = ByteListHelper.eachCodePointWhile(context.runtime, bytes, 0, (index, codepoint, enc) -> codepoint != '=');
            int length = bytes.length();

            if (e != length || length == 0) throw context.runtime.newErrnoEINVALError(str(context.runtime, "setenv(", key, ")"));

            return key;
        }

        protected static boolean isStringLike(final IRubyObject obj) {
            return obj instanceof RubyString || obj.respondsTo("to_str");
        }

        protected RubyString getCorrectKey(final RubyString key) {
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

        // MRI: env_name_new
        protected static IRubyObject newName(ThreadContext context, IRubyObject key, IRubyObject valueArg) {
            if (valueArg.isNil()) return context.nil;

            RubyString value = (RubyString) valueArg;
            EncodingService encodingService = context.runtime.getEncodingService();
            Encoding encoding = isPATH(context, (RubyString) key) && !value.isTaint() ?
                    encodingService.getFileSystemEncoding() :
                    encodingService.getLocaleEncoding();

            return newString(context, value, encoding);
        }

        protected static IRubyObject newString(ThreadContext context, RubyString value, Encoding encoding) {
            IRubyObject result = newExternalStringWithEncoding(context.runtime, value.getByteList(), encoding);

            result.setTaint(true);
            result.setFrozen(true);

            return result;
        }

        // MRI: env_str_new
        protected static IRubyObject newString(ThreadContext context, RubyString value) {
            return newString(context, value, context.runtime.getEncodingService().getLocaleEncoding());
        }

        // MRI: env_str_new2
        protected static IRubyObject newString(ThreadContext context, IRubyObject obj) {
            return obj.isNil() ? context.nil : newString(context, (RubyString) obj);
        }

        private static boolean isPATH(ThreadContext context, RubyString name) {
            return Platform.IS_WINDOWS ?
                    equalIgnoreCase(context, name, RubyString.newString(context.runtime, PATH_BYTES)) :
                    name.getByteList().equal(PATH_BYTES);
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

    private static class NonEffectiveGlobalVariable extends GlobalVariable {
        public NonEffectiveGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            runtime.getWarnings().warn(ID.INEFFECTIVE_GLOBAL, "warning: variable " + name + " is no longer effective; ignored");
            return value;
        }

        @Override
        public IRubyObject get() {
            runtime.getWarnings().warn(ID.INEFFECTIVE_GLOBAL, "warning: variable " + name + " is no longer effective");
            return value;
        }
    }

    private static class LastExitStatusVariable extends GlobalVariable {
        public LastExitStatusVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }

        @Override
        public IRubyObject get() {
            IRubyObject lastExitStatus = runtime.getCurrentContext().getLastExitStatus();
            return lastExitStatus == null ? runtime.getNil() : lastExitStatus;
        }

        @Override
        public IRubyObject set(IRubyObject lastExitStatus) {
            throw runtime.newNameError("$? is a read-only variable", "$?");
        }
    }

    private static class MatchMatchGlobalVariable extends GlobalVariable {
        public MatchMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }

        @Override
        public IRubyObject get() {
            return RubyRegexp.last_match(runtime.getCurrentContext().getBackRef());
        }
    }

    private static class PreMatchGlobalVariable extends GlobalVariable {
        public PreMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }

        @Override
        public IRubyObject get() {
            return RubyRegexp.match_pre(runtime.getCurrentContext().getBackRef());
        }
    }

    private static class PostMatchGlobalVariable extends GlobalVariable {
        public PostMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }

        @Override
        public IRubyObject get() {
            return RubyRegexp.match_post(runtime.getCurrentContext().getBackRef());
        }
    }

    private static class LastMatchGlobalVariable extends GlobalVariable {
        public LastMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }

        @Override
        public IRubyObject get() {
            return RubyRegexp.match_last(runtime.getCurrentContext().getBackRef());
        }
    }

    private static class BackRefGlobalVariable extends GlobalVariable {
        public BackRefGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }

        @Override
        public IRubyObject get() {
            return runtime.getCurrentContext().getBackRef();
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            ThreadContext context = runtime.getCurrentContext();
            if (value.isNil()) {
                context.clearBackRef();
            } else if (value instanceof RubyMatchData) {
                context.setBackRef((RubyMatchData) value);
            } else {
                throw runtime.newTypeError(value, runtime.getMatchData());
            }

            return value;
        }
    }

    // Accessor methods.

    private static class LineNumberGlobalVariable extends GlobalVariable {
        public LineNumberGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, null);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            int line = (int)value.convertToInteger().getLongValue();
            runtime.setCurrentLine(line);
            return value;
        }

        @Override
        public IRubyObject get() {
            return runtime.newFixnum(runtime.getCurrentLine());
        }
    }

    private static class ErrorInfoGlobalVariable extends GlobalVariable {
        public ErrorInfoGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, null);
            set(value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            if (!value.isNil() &&
                    !runtime.getException().isInstance(value) &&
                    !(JavaUtil.isJavaObject(value) && JavaUtil.unwrapJavaObject(value) instanceof Throwable)) {
                throw runtime.newTypeError("assigning non-exception to $!");
            }

            return runtime.getCurrentContext().setErrorInfo(value);
        }

        @Override
        public IRubyObject get() {
            return runtime.getCurrentContext().getErrorInfo();
        }
    }

    // FIXME: move out of this class!
    public static class StringGlobalVariable extends GlobalVariable {
        public StringGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            if (!value.isNil() && ! (value instanceof RubyString)) {
                throw runtime.newTypeError("value of " + name() + " must be a String");
            }
            return super.set(value);
        }
    }

    public static class StringOrRegexpGlobalVariable extends GlobalVariable {
        public StringOrRegexpGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            if (!value.isNil() && ! (value instanceof RubyString) && ! (value instanceof RubyRegexp)) {
                throw runtime.newTypeError("value of " + name() + " must be String or Regexp");
            }
            return super.set(value);
        }
    }

    public static class KCodeGlobalVariable extends GlobalVariable {
        public KCodeGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject get() {
            String kcode = runtime.getKCode().getKCode();
            return kcode == null ? runtime.getNil() : runtime.newString(kcode);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            runtime.setKCode(KCode.create(value.convertToString().toString()));
            return value;
        }
    }

    private static class SafeGlobalVariable extends GlobalVariable {
        public SafeGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, null);
        }

        @Override
        public IRubyObject get() {
            return RubyFixnum.zero(runtime);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            runtime.getWarnings().warnOnce(ID.SAFE_NOT_SUPPORTED, "SAFE levels are not supported in JRuby");
            return RubyFixnum.zero(runtime);
        }
    }

    private static class VerboseGlobalVariable extends GlobalVariable {
        public VerboseGlobalVariable(Ruby runtime, String name, IRubyObject initialValue) {
            super(runtime, name, initialValue);
            set(initialValue);
        }

        @Override
        public IRubyObject get() {
            return runtime.getVerbose();
        }

        @Override
        public IRubyObject set(IRubyObject newValue) {
            if (newValue.isNil()) {
                runtime.setVerbose(newValue);
            } else {
                runtime.setVerbose(runtime.newBoolean(newValue.isTrue()));
            }

            return newValue;
        }
    }

    private static class WarningGlobalVariable extends ReadonlyGlobalVariable {
        public WarningGlobalVariable(Ruby runtime, String name, RubyInstanceConfig.Verbosity verbosity) {
            super(runtime, name,
                    verbosity == RubyInstanceConfig.Verbosity.NIL   ? RubyFixnum.newFixnum(runtime, 0) :
                    verbosity == RubyInstanceConfig.Verbosity.FALSE ? RubyFixnum.newFixnum(runtime, 1) :
                    verbosity == RubyInstanceConfig.Verbosity.TRUE  ? RubyFixnum.newFixnum(runtime, 2) :
                    runtime.getNil()
                    );
        }
    }

    private static class DebugGlobalVariable extends GlobalVariable {
        public DebugGlobalVariable(Ruby runtime, String name, IRubyObject initialValue) {
            super(runtime, name, initialValue);
            set(initialValue);
        }

        @Override
        public IRubyObject get() {
            return runtime.getDebug();
        }

        @Override
        public IRubyObject set(IRubyObject newValue) {
            if (newValue.isNil()) {
                runtime.setDebug(newValue);
            } else {
                runtime.setDebug(runtime.newBoolean(newValue.isTrue()));
            }

            return newValue;
        }
    }

    private static class BacktraceGlobalVariable extends GlobalVariable {
        public BacktraceGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, null);
        }

        @Override
        public IRubyObject get() {
            IRubyObject errorInfo = runtime.getGlobalVariables().get("$!");
            IRubyObject backtrace = errorInfo.isNil() ? runtime.getNil() : errorInfo.callMethod(errorInfo.getRuntime().getCurrentContext(), "backtrace");
            //$@ returns nil if $!.backtrace is not an array
            if (!(backtrace instanceof RubyArray)) {
                backtrace = runtime.getNil();
            }
            return backtrace;
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            if (runtime.getGlobalVariables().get("$!").isNil()) {
                throw runtime.newArgumentError("$! not set");
            }
            runtime.getGlobalVariables().get("$!").callMethod(value.getRuntime().getCurrentContext(), "set_backtrace", value);
            return value;
        }
    }

    private static class LastlineGlobalVariable extends GlobalVariable {
        public LastlineGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, null);
        }

        @Override
        public IRubyObject get() {
            return runtime.getCurrentContext().getLastLine();
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            return runtime.getCurrentContext().setLastLine(value);
        }
    }

    public static class InputGlobalVariable extends GlobalVariable {
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

    public static class OutputGlobalVariable extends GlobalVariable {
        public OutputGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            if (value == get()) {
                return value;
            }

            if (!value.respondsTo("write") && !value.respondsToMissing("write")) {
                throw runtime.newTypeError(name() + " must have write method, " +
                                    value.getType().getName() + " given");
            }

            return super.set(value);
        }
    }

    private static class LoadPath extends ReadonlyGlobalVariable {
        public LoadPath(Ruby runtime, String name) {
            super(runtime, name, null);
        }

        /**
         * @see org.jruby.runtime.GlobalVariable#get()
         */
        @Override
        public IRubyObject get() {
            return runtime.getLoadService().getLoadPath();
        }
    }

    private static class LoadedFeatures extends ReadonlyGlobalVariable {
        public LoadedFeatures(Ruby runtime, String name) {
            super(runtime, name, null);
        }

        /**
         * @see org.jruby.runtime.GlobalVariable#get()
         */
        @Override
        public IRubyObject get() {
            return runtime.getLoadService().getLoadedFeatures();
        }
    }

    /**
     * A special accessor for getpid, to avoid loading the posix subsystem until
     * it is needed.
     */
    private static final class PidAccessor implements IAccessor {
        private final Ruby runtime;
        private volatile IRubyObject pid = null;

        public PidAccessor(Ruby runtime) {
            this.runtime = runtime;
        }

        public IRubyObject getValue() {
            return pid != null ? pid : (pid = runtime.newFixnum(runtime.getPosix().getpid()));
        }

        public IRubyObject setValue(IRubyObject newValue) {
            throw runtime.newRuntimeError("cannot assign to $$");
        }
    }

    /**
     * Globals that generally should not be cached, because they are expected to change frequently if used.
     *
     * See jruby/jruby#4508.
     */
    public static final List<String> UNCACHED_GLOBALS = Collections.unmodifiableList(Arrays.asList(
            "$.", "$INPUT_LINE_NUMBER", // ARGF current line number
            "$FILENAME" // ARGF current file name
    ));
}
