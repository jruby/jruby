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
import org.jruby.api.Create;
import org.jruby.ast.util.ArgsUtil;
import org.jruby.common.IRubyWarnings;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.ir.Tuple;
import org.jruby.javasupport.JavaUtil;
import org.jruby.platform.Platform;
import org.jruby.runtime.Block;
import org.jruby.runtime.Constants;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ReadonlyGlobalVariable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
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

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jruby.api.Access.argsFile;
import static org.jruby.api.Access.encodingService;
import static org.jruby.api.Access.enumerableModule;
import static org.jruby.api.Access.exceptionClass;
import static org.jruby.api.Access.globalVariables;
import static org.jruby.api.Access.instanceConfig;
import static org.jruby.api.Access.objectClass;
import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Convert.toInt;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newFrozenString;
import static org.jruby.api.Create.newSharedString;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.argumentError;
import static org.jruby.api.Error.nameError;
import static org.jruby.api.Error.runtimeError;
import static org.jruby.api.Error.typeError;
import static org.jruby.api.Warn.warn;
import static org.jruby.api.Warn.warnDeprecated;
import static org.jruby.internal.runtime.GlobalVariable.Scope.FRAME;
import static org.jruby.internal.runtime.GlobalVariable.Scope.GLOBAL;
import static org.jruby.internal.runtime.GlobalVariable.Scope.THREAD;
import static org.jruby.util.RubyStringBuilder.str;
import static org.jruby.util.io.EncodingUtils.newExternalStringWithEncoding;

/** This class initializes global variables and constants.
 *
 * @author jpetersen
 */
public class RubyGlobal {
    public static void initARGV(Ruby runtime) {
        var context = runtime.getCurrentContext();
        var objectClass = objectClass(context);
        // define ARGV and $* for this runtime
        String[] argv = instanceConfig(context).getArgv();
        var argvArray = Create.allocArray(context, argv.length);

        for (String arg : argv) {
            argvArray.append(context, RubyString.newInternalFromJavaExternal(runtime, arg));
        }

        if (objectClass.getConstantNoConstMissing(context, "ARGV") != null) {
            ((RubyArray<?>) objectClass.getConstant(context, "ARGV")).replace(context, argvArray);
        } else {
            objectClass.setConstantQuiet(context, "ARGV", argvArray);
            globalVariables(context).define("$*", new ValueAccessor(argvArray), GLOBAL);
        }
    }

    @Deprecated(since = "10.0.0.0")
    public static void createGlobals(Ruby runtime) {
        var context = runtime.getCurrentContext();
        createGlobalsAndENV(context, globalVariables(context), instanceConfig(context));
    }

    public static RubyHash createGlobalsAndENV(ThreadContext context, GlobalVariables globals, RubyInstanceConfig instanceConfig) {
        var runtime = context.runtime;
        var Object = objectClass(context);
        var topLevelBinding = runtime.newBinding();
        runtime.setTopLevelBinding(topLevelBinding);
        Object.defineConstant(context, "TOPLEVEL_BINDING", topLevelBinding);

        initARGV(runtime);

        // initially set to config's script name, if any
        defineArgv0Global(context, globals, instanceConfig.displayedFileName());

        // Version information:
        IRubyObject release = RubyString.newFString(runtime, Constants.COMPILE_DATE);
        IRubyObject platform = RubyString.newFString(runtime, Constants.PLATFORM);
        IRubyObject engine = RubyString.newFString(runtime, Constants.ENGINE);
        IRubyObject version = RubyString.newFString(runtime, Constants.RUBY_VERSION);
        IRubyObject patchlevel = asFixnum(context, 0);

        Object.defineConstant(context, "RUBY_VERSION", version);
        Object.defineConstant(context, "RUBY_PATCHLEVEL", patchlevel);
        Object.defineConstant(context, "RUBY_RELEASE_DATE", release);
        Object.defineConstant(context, "RUBY_PLATFORM", platform);

        IRubyObject description = RubyString.newFString(runtime, OutputStrings.getVersionString(instanceConfig));
        Object.defineConstant(context, "RUBY_DESCRIPTION", description);

        IRubyObject copyright = RubyString.newFString(runtime, OutputStrings.getCopyrightString());
        Object.defineConstant(context, "RUBY_COPYRIGHT", copyright);

        Object.defineConstant(context, "RELEASE_DATE", release);
        Object.defineConstant(context, "PLATFORM", platform);

        IRubyObject jrubyVersion = RubyString.newFString(runtime, Constants.VERSION);
        IRubyObject jrubyRevision = RubyString.newFString(runtime, Constants.REVISION);
        Object.defineConstant(context, "JRUBY_VERSION", jrubyVersion);
        Object.defineConstant(context, "JRUBY_REVISION", jrubyRevision);
        Object.defineConstant(context, "RUBY_REVISION", RubyString.newFString(runtime, Constants.REVISION));
        Object.defineConstant(context, "RUBY_ENGINE", engine);
        Object.defineConstant(context, "RUBY_ENGINE_VERSION", jrubyVersion);

        RubyInstanceConfig.Verbosity verbosity = instanceConfig.getVerbosity();
        runtime.defineVariable(new WarningGlobalVariable(context, "$-W", verbosity), GLOBAL);

        IRubyObject defaultRS = RubyString.newFString(runtime, instanceConfig.getRecordSeparator());
        GlobalVariable rs = new StringGlobalVariable(runtime, "$/", defaultRS);
        runtime.defineVariable(rs, GLOBAL);
        runtime.setRecordSeparatorVar(rs);
        globals.setDefaultSeparator(defaultRS);
        runtime.defineVariable(new StringGlobalVariable(runtime, "$\\", context.nil), GLOBAL);
        runtime.defineVariable(new DeprecatedStringGlobalVariable(runtime, "$,", context.nil), GLOBAL);

        runtime.defineVariable(new LineNumberGlobalVariable(runtime, "$."), GLOBAL);
        runtime.defineVariable(new LastlineGlobalVariable(runtime, "$_"), FRAME);
        runtime.defineVariable(new LastExitStatusVariable(runtime, "$?"), THREAD);

        if (runtime.getProfile().allowClass("Thread")) {
            runtime.defineVariable(new ErrorInfoGlobalVariable(runtime, "$!", context.nil), THREAD);
        }
        runtime.defineVariable(new NonEffectiveGlobalVariable(runtime, "$=", context.fals), GLOBAL);

        if(instanceConfig.getInputFieldSeparator() == null) {
            runtime.defineVariable(new DeprecatedStringOrRegexpGlobalVariable(runtime, "$;", context.nil), GLOBAL);
        } else {
            runtime.defineVariable(new DeprecatedStringOrRegexpGlobalVariable(runtime, "$;", RubyRegexp.newRegexp(runtime, instanceConfig.getInputFieldSeparator(), new RegexpOptions())), GLOBAL);
        }

        RubyInstanceConfig.Verbosity verbose = instanceConfig.getVerbosity();
        IRubyObject verboseValue;
        if (verbose == RubyInstanceConfig.Verbosity.NIL) {
            verboseValue = context.nil;
        } else if(verbose == RubyInstanceConfig.Verbosity.TRUE) {
            verboseValue = context.tru;
        } else {
            verboseValue = context.fals;
        }
        runtime.setVerbose(verboseValue);
        runtime.defineVariable(new VerboseGlobalVariable(runtime, "$VERBOSE"), GLOBAL);
        runtime.defineVariable(new VerboseGlobalVariable(runtime, "$-v"), GLOBAL);
        runtime.defineVariable(new VerboseGlobalVariable(runtime, "$-w"), GLOBAL);

        runtime.setDebug(runtime.newBoolean(instanceConfig.isDebug()));
        runtime.defineVariable(new DebugGlobalVariable(runtime, "$DEBUG"), GLOBAL);
        runtime.defineVariable(new DebugGlobalVariable(runtime, "$-d"), GLOBAL);

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
        RubyHash env = defineGlobalEnvConstants(context);

        // Fixme: Do we need the check or does Main.java not call this...they should consolidate
        if (globals.get("$*").isNil()) {
            globals.defineReadonly("$*", new ValueAccessor(newArray(context)), GLOBAL);
        }

        globals.defineReadonly("$-p", new ValueAccessor(asBoolean(context, instanceConfig.isAssumePrinting())), GLOBAL);
        globals.defineReadonly("$-a", new ValueAccessor(asBoolean(context, instanceConfig.isSplit())), GLOBAL);
        globals.defineReadonly("$-l", new ValueAccessor(asBoolean(context, instanceConfig.isProcessLineEnds())), GLOBAL);

        // ARGF, $< object
        RubyArgsFile.initArgsFile(context, enumerableModule(context), globals);

        String inplace = instanceConfig.getInPlaceBackupExtension();
        runtime.defineVariable(new ArgfGlobalVariable(runtime, "$-i",
                inplace != null ? newString(context, inplace) : context.nil), GLOBAL);

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

        return env;
    }

    static void defineArgv0Global(ThreadContext context, GlobalVariables globals, String scriptName) {
        IAccessor d = new IAccessor() {
            RubyString value = newFrozenString(context, scriptName);

            @Override
            public IRubyObject getValue() {
                return value;
            }

            @Override
            public IRubyObject setValue(IRubyObject newValue) {
                RubyString stringValue = newValue.convertToString();
                if (!stringValue.isFrozen()) {
                    stringValue = stringValue.newFrozen();
                }
                value = stringValue;
                return stringValue;
            }
        };
        globals.define("$PROGRAM_NAME", d, GLOBAL);
        globals.define("$0", d, GLOBAL);
    }

    public static void initSTDIO(Ruby runtime, GlobalVariables globals) {
        var context = runtime.getCurrentContext();
        var Object = objectClass(context);
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

        var object = runtime.getObject();
        if (object.getConstantFromNoConstMissing(context, "STDIN") == null) {
            runtime.defineVariable(new InputGlobalVariable(runtime, "$stdin", stdin), GLOBAL);
            runtime.defineVariable(new OutputGlobalVariable(runtime, "$stdout", stdout), GLOBAL);
            globals.alias("$>", "$stdout");
            runtime.defineVariable(new OutputGlobalVariable(runtime, "$stderr", stderr), GLOBAL);

            Object.defineConstant(context, "STDIN", stdin);
            Object.defineConstant(context, "STDOUT", stdout);
            Object.defineConstant(context, "STDERR", stderr);

            runtime.setOriginalStderr(stderr);
        } else {
            ((RubyIO) object.getConstant(context, "STDIN")).getOpenFile().setFD(stdin.getOpenFile().fd());
            ((RubyIO) object.getConstant(context, "STDOUT")).getOpenFile().setFD(stdout.getOpenFile().fd());
            ((RubyIO) object.getConstant(context, "STDERR")).getOpenFile().setFD(stderr.getOpenFile().fd());
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
    private static RubyHash defineGlobalEnvConstants(ThreadContext context) {
        var runtime = context.runtime;
        var Object = objectClass(context);
    	Map<RubyString, RubyString> environmentVariableMap = OSEnvironment.environmentVariableMap(runtime);
        var instanceConfig = instanceConfig(context);
    	RubyHash env = new CaseInsensitiveStringOnlyRubyHash(
            runtime, environmentVariableMap, context.nil,
            instanceConfig.isNativeEnabled() && instanceConfig.isUpdateNativeENVEnabled()
        );
        env.singletonClass(context).defineMethods(context, CaseInsensitiveStringOnlyRubyHash.class);
        Object.defineConstant(context, "ENV", env);

        // Define System.getProperties() in ENV_JAVA
        Map<RubyString, RubyString> systemPropertiesMap = OSEnvironment.systemPropertiesMap(runtime);
        RubyHash envJava = new ReadOnlySystemPropertiesHash(runtime, systemPropertiesMap, context.nil);
        envJava.setFrozen(true);
        Object.defineConstant(context, "ENV_JAVA", envJava);

        return env;
    }

    private static void warnDeprecatedGlobal(ThreadContext context, final String name) {
        warnDeprecated(context, "'" + name + "' is deprecated");
    }

    /**
     * Obligate string-keyed and string-valued hash, used for ENV.
     * On Windows, the keys are case-insensitive for ENV
     *
     */
    public static class CaseInsensitiveStringOnlyRubyHash extends StringOnlyRubyHash {

        private static final byte[] HASHROCKET_WITH_SPACES = " => ".getBytes(StandardCharsets.UTF_8);

        public CaseInsensitiveStringOnlyRubyHash(Ruby runtime, Map<RubyString, RubyString> valueMap, IRubyObject defaultValue, boolean updateRealENV) {
            super(runtime, valueMap, defaultValue, updateRealENV);
        }

        protected final boolean isCaseSensitive() { return false; }

        @JRubyMethod(name = "[]")
        public IRubyObject op_aref(ThreadContext context, IRubyObject arg) {
            IRubyObject key = arg.convertToString();
            IRubyObject value = internalGet(key);

            EnvStringValidation.ensureValidEnvString(context, key, "key");

            if (value == null) return context.nil;

            RubyString string = (RubyString) newName(context, key, value);

            string.freeze(context);

            return string;
        }

        private static final ByteList ENV = new ByteList(new byte[] {'E','N','V'}, USASCIIEncoding.INSTANCE, false);

        @JRubyMethod
        public IRubyObject freeze(ThreadContext context) {
            // FIXME: So far I can see this is only used by ENV so I put it here but perhaps we need to differentiate case
            throw typeError(context, "cannot freeze ENV");
        }

        @JRubyMethod(name = "assoc")
        public IRubyObject assoc(final ThreadContext context, IRubyObject obj) {
            RubyString expected = verifyStringLike(context, obj).convertToString();
            EnvStringValidation.ensureValidEnvString(context, obj, "value");

            return super.assoc(context, expected);
        }

        @JRubyMethod(name = "fetch", required = 1, optional = 2)
        public IRubyObject fetch(ThreadContext context, IRubyObject[] args, Block block) {
            EnvStringValidation.ensureValidEnvString(context, args[0], "key");

            return switch (args.length) {
                case 1 -> super.fetch(context, args[0], block);
                case 2 -> super.fetch(context, args[0], args[1], block);
                default -> null;
            };

        }

        @JRubyMethod(name = "fetch", required = 1)
        public IRubyObject fetch(ThreadContext context, IRubyObject key, Block block) {
            EnvStringValidation.ensureValidEnvString(context, key, "key");

            return super.fetch(context, verifyStringLike(context, key.convertToString()), block);
        }

        @JRubyMethod
        public IRubyObject delete(ThreadContext context, IRubyObject key, Block block) {
            EnvStringValidation.ensureValidEnvString(context, key, "key");

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

        private static final VisitorWithState<RubyArray<?>> EachVisitor = new VisitorWithState<>() {
            @Override
            public void visit(ThreadContext context, RubyHash self, IRubyObject key, IRubyObject value, int index, RubyArray<?> ary) {
                ary.append(context, newString(context, key));
                ary.append(context, newName(context, key, value));
            }
        };

        @JRubyMethod(name = "rassoc")
        public IRubyObject rassoc(final ThreadContext context, IRubyObject obj) {
            if (!isStringLike(obj)) return context.nil;

            return super.rassoc(context, obj.convertToString());
        }

        @JRubyMethod(name = {"has_key?", "key?", "include?", "member?"})
        public RubyBoolean has_key_p(ThreadContext context, IRubyObject key) {
            IRubyObject expected = verifyStringLike(context, key);
            
            EnvStringValidation.ensureValidEnvString(context, key, "key");

            return internalGetEntry(expected) == NO_ENTRY ? context.fals : context.tru;
        }

        @JRubyMethod(name = {"has_value?", "value?"})
        public IRubyObject has_value_pp(ThreadContext context, IRubyObject expected) {
            if (!isStringLike(expected)) return context.nil;

            EnvStringValidation.ensureValidEnvString(context, expected, "value");
            
            return super.has_value_p(context, expected.convertToString());
        }

        @Deprecated(since = "10.0.0.0")
        public IRubyObject index(ThreadContext context, IRubyObject expected) {
            warn(context, "ENV#index is deprecated; use ENV#key");

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
                keys.storeInternal(context, index, newString(context, key));
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

        @JRubyMethod(name = "replace")
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

        @Override
        @JRubyMethod(name = "to_s")
        public RubyString to_s(ThreadContext context) {
            return newSharedString(context, ENV);
        }

        @Deprecated(since = "9.3.0.0")
        public RubyHash to_h() {
            return to_h(getCurrentContext(), Block.NULL_BLOCK);
        }

        @JRubyMethod
        public RubyHash to_h(ThreadContext context, Block block){
            RubyHash h = to_hash(context);
            return block.isGiven() ? h.to_h_block(context, block) : h;
        }

        @JRubyMethod(name = "clone")
        @Override
        public IRubyObject rbClone(ThreadContext context) {
            throw typeError(context, "Cannot clone ENV, use ENV.to_h to get a copy of ENV as a hash");
        }

        @JRubyMethod(name = "clone")
        public IRubyObject rbClone(ThreadContext context, IRubyObject _opts) {
            IRubyObject opts = ArgsUtil.getOptionsArg(context.runtime, _opts, true);
            if (opts.isNil()) throw argumentError(context, 1, 0);

            IRubyObject freeze = ArgsUtil.getFreezeOpt(context, opts);
            if (freeze != null && freeze.isTrue()) throw typeError(context, "Cannot clone ENV");

            return rbClone(context);
        }

        @JRubyMethod()
        @Override
        public IRubyObject dup(ThreadContext context) {
            throw typeError(context, "Cannot dup ENV, use ENV.to_h to get a copy of ENV as a hash");
        }

        private final RaiseException concurrentModification() {
            return metaClass.runtime.newConcurrencyError(
                    "Detected invalid hash contents due to unsynchronized modifications with concurrent users");
        }

        @Override
        protected void replaceWith(ThreadContext context, RubyHash otherHash) {
            replaceExternally(context, otherHash);
        }

        @Override
        protected void appendAssociation(boolean keyIsSymbol, ByteList bytes) {
            if (keyIsSymbol) {
                // key should never be Symbol for ENV
                bytes.append(':');
            } else {
                bytes.append(HASHROCKET_WITH_SPACES);
            }
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
            RubyHash hash = Create.newHash(context);
            hash.replace(context, this);
            return hash;
        }

        @Override
        protected IRubyObject internalGet(IRubyObject key) {
            if (size == 0) return null;

            if (!isCaseSensitive()) key = getCorrectKey(key.convertToString());

            return super.internalGet(key);
        }

        @Override
        public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject value) {
            return case_aware_op_aset(context, key, value);
        }

        private IRubyObject case_aware_op_aset(ThreadContext context, IRubyObject key, final IRubyObject value) {
            RubyString keyAsStr = verifyValidKey(context, verifyStringLike(context, key).convertToString(), value);
            if (!isCaseSensitive()) key = keyAsStr = getCorrectKey(keyAsStr);

            if (value == context.nil) return super.delete(context, key, org.jruby.runtime.Block.NULL_BLOCK);

            IRubyObject valueAsStr = newName(context, keyAsStr, verifyStringLike(context, value).convertToString());

            if (updateRealENV) {
                final POSIX posix = context.runtime.getPosix();
                final String keyAsJava = keyAsStr.asJavaString();
                // libc (un)setenv is not reentrant, so we need to synchronize across the entire JVM (JRUBY-5933)
                if (valueAsStr == context.nil) {
                    synchronized (Object.class) { posix.unsetenv(keyAsJava); }
                } else {
                    EnvStringValidation.ensureValidEnvString(context, value, "value");
                    EnvStringValidation.ensureValidEnvString(context, key, "key");

                    final String valueAsJava = valueAsStr.asJavaString();
                    synchronized (Object.class) { posix.setenv(keyAsJava, valueAsJava, 1); }
                }
            }

            super.op_aset(context, keyAsStr, valueAsStr);

            return value;
        }

        protected static IRubyObject verifyStringLike(ThreadContext context, IRubyObject test) {
            IRubyObject string = test.checkStringType();
            if (string.isNil()) throw typeError(context, "no implicit conversion of ", test, " into String");

            return string;
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

        // MRI: env_name_new
        // TODO: mri 3.1 does not use env_name_new
        protected static IRubyObject newName(ThreadContext context, IRubyObject key, IRubyObject valueArg) {
            return valueArg.isNil() ? context.nil : newString(context, (RubyString) valueArg);
        }

        protected static IRubyObject newString(ThreadContext context, RubyString value, Encoding encoding) {
            IRubyObject result = newExternalStringWithEncoding(context.runtime, value.getByteList().dup(), encoding);
            result.setFrozen(true);
            return result;
        }

        // MRI: env_str_new
        protected static IRubyObject newString(ThreadContext context, RubyString value) {
            return newString(context, value, encodingService(context).getEnvEncoding());
        }

        // MRI: env_str_new2
        protected static IRubyObject newString(ThreadContext context, IRubyObject obj) {
            return obj.isNil() ? context.nil : newString(context, (RubyString) obj);
        }

        private static boolean equalIgnoreCase(ThreadContext context, final RubyString str1, final RubyString str2) {
            return ((RubyFixnum) str1.casecmp(context, str2)).isZero(context);
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
            if (isFrozen()) throw typeError(getRuntime().getCurrentContext(), "ENV_JAVA is not writable until you require 'java'");
        }
    }

    private static class NonEffectiveGlobalVariable extends GlobalVariable {
        public NonEffectiveGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            warnDeprecated(runtime.getCurrentContext(), "warning: variable " + name + " is no longer effective; ignored");
            return value;
        }

        @Override
        public IRubyObject get() {
            warnDeprecated(runtime.getCurrentContext(), "warning: variable " + name + " is no longer effective");
            return value;
        }
    }

    private static class LastExitStatusVariable extends GlobalVariable {
        public LastExitStatusVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }

        @Override
        public IRubyObject get() {
            var context = runtime.getCurrentContext();
            IRubyObject lastExitStatus = context.getLastExitStatus();
            return lastExitStatus == null ? context.nil : lastExitStatus;
        }

        @Override
        public IRubyObject set(IRubyObject lastExitStatus) {
            throw nameError(runtime.getCurrentContext(), "$? is a read-only variable", "$?");
        }
    }

    private static class MatchMatchGlobalVariable extends GlobalVariable {
        public MatchMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }

        @Override
        public IRubyObject get() {
            var context = runtime.getCurrentContext();
            return RubyRegexp.last_match(context, context.getBackRef());
        }
    }

    private static class PreMatchGlobalVariable extends GlobalVariable {
        public PreMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }

        @Override
        public IRubyObject get() {
            var context = runtime.getCurrentContext();
            return RubyRegexp.match_pre(context, context.getBackRef());
        }
    }

    private static class PostMatchGlobalVariable extends GlobalVariable {
        public PostMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }

        @Override
        public IRubyObject get() {
            var context = runtime.getCurrentContext();
            return RubyRegexp.match_post(context, context.getBackRef());
        }
    }

    private static class LastMatchGlobalVariable extends GlobalVariable {
        public LastMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }

        @Override
        public IRubyObject get() {
            var context = runtime.getCurrentContext();
            return RubyRegexp.match_last(context, context.getBackRef());
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
            } else if (value instanceof RubyMatchData match) {
                context.setBackRef(match);
            } else {
                throw typeError(context, value, "MatchData");
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
            int line = toInt(runtime.getCurrentContext(), value);
            runtime.setCurrentLine(line);
            return value;
        }

        @Override
        public IRubyObject get() {
            return runtime.newFixnum(runtime.getCurrentLine());
        }
    }

    private static class ErrorInfoGlobalVariable extends ReadonlyGlobalVariable {
        public ErrorInfoGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, null);
            runtime.getCurrentContext().setErrorInfo(value);
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
                throw typeError(value.getRuntime().getCurrentContext(), "value of " + name() + " must be String");
            }
            return super.set(value);
        }
    }

    public static class ArgfGlobalVariable extends GlobalVariable {
        public ArgfGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
            set(value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            var context = runtime.getCurrentContext();
            RubyArgsFile.inplace_mode_set(context, argsFile(context), value);
            if (value.isNil() || !value.isTrue()) instanceConfig(context).setInPlaceBackupExtension(null);

            return super.set(value);
        }
    }

    public static class DeprecatedStringGlobalVariable extends StringGlobalVariable {
        public DeprecatedStringGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            IRubyObject result = super.set(value);

            if (!value.isNil()) warnDeprecatedGlobal(runtime.getCurrentContext(), name);

            return result;
        }
    }

    public static class DeprecatedStringOrRegexpGlobalVariable extends StringOrRegexpGlobalVariable {
        public DeprecatedStringOrRegexpGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            IRubyObject result = super.set(value);

            if (!result.isNil()) warnDeprecatedGlobal(runtime.getCurrentContext(), name);

            return result;
        }
    }

    public static class StringOrRegexpGlobalVariable extends GlobalVariable {
        public StringOrRegexpGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            if (!value.isNil() && ! (value instanceof RubyString) && ! (value instanceof RubyRegexp)) {
                throw typeError(value.getRuntime().getCurrentContext(), "value of " + name() + " must be String or Regexp");
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

    private static class VerboseGlobalVariable extends GlobalVariable {
        public VerboseGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, null); // this.value not used
        }

        @Override
        public IRubyObject get() {
            return runtime.getVerbose();
        }

        @Override
        public IRubyObject set(IRubyObject newValue) {
            runtime.setVerbose(newValue.isNil() ? null : newValue.isTrue());

            return newValue;
        }
    }

    private static class WarningGlobalVariable extends ReadonlyGlobalVariable {
        public WarningGlobalVariable(ThreadContext context, String name, RubyInstanceConfig.Verbosity verbosity) {
            super(context.runtime, name,
                    verbosity == RubyInstanceConfig.Verbosity.NIL   ? asFixnum(context, 0) :
                    verbosity == RubyInstanceConfig.Verbosity.FALSE ? asFixnum(context, 1) :
                    verbosity == RubyInstanceConfig.Verbosity.TRUE  ? asFixnum(context, 2) :
                    context.nil
                    );
        }
    }

    private static class DebugGlobalVariable extends GlobalVariable {
        public DebugGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, null); // this.value not used
        }

        @Override
        public IRubyObject get() {
            return runtime.getDebug();
        }

        @Override
        public IRubyObject set(IRubyObject newValue) {
            runtime.setDebug(newValue.isTrue());

            return newValue;
        }
    }

    private static class BacktraceGlobalVariable extends GlobalVariable {
        public BacktraceGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, null);
        }

        @Override
        public IRubyObject get() {
            var context = runtime.getCurrentContext();
            IRubyObject errorInfo = globalVariables(context).get("$!");
            IRubyObject backtrace = errorInfo.isNil() ? context.nil : errorInfo.callMethod(context, "backtrace");

            //$@ returns nil if $!.backtrace is not an array
            return backtrace instanceof RubyArray ? backtrace : context.nil;
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            var context = runtime.getCurrentContext();
            if (globalVariables(context).get("$!").isNil()) throw argumentError(context, "$! not set");

            globalVariables(context).get("$!").callMethod(context, "set_backtrace", value);
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
                throw typeError(value.getRuntime().getCurrentContext(), name() + " must have write method, ", value, " given");
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
            throw runtimeError(runtime.getCurrentContext(), "cannot assign to $$");
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

    private static class EnvStringValidation {
        public static void ensureValidEnvString(ThreadContext context, IRubyObject str, String type) {
            RubyString value = str.asString();

            if (!value.getByteList().getEncoding().isAsciiCompatible()) {
                throw argumentError(context, "bad environment variable " + type + ": ASCII incompatible encoding: " + value.getByteList().getEncoding().toString());
            }

            if (value.toString().contains("\0")) {
                throw argumentError(context, "bad environment variable " + type + ": contains null byte");
            }
        }
    }
}
