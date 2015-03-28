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
import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.internal.runtime.GlobalVariables;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Helpers;
import org.jruby.platform.Platform;
import org.jruby.runtime.Constants;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ReadonlyGlobalVariable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;
import org.jruby.util.KCode;
import org.jruby.util.OSEnvironment;
import org.jruby.util.RegexpOptions;
import org.jruby.util.ShellLauncher;
import org.jruby.util.cli.OutputStrings;
import org.jruby.util.io.BadDescriptorException;
import org.jruby.util.io.OpenFile;
import org.jruby.util.io.STDIO;

import static org.jruby.internal.runtime.GlobalVariable.Scope.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.util.HashMap;
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
            argvArray.append(RubyString.newInternalFromJavaExternal(runtime, arg));
        }

        if (runtime.getObject().getConstantNoConstMissing("ARGV") != null) {
            ((RubyArray)runtime.getObject().getConstant("ARGV")).replace(argvArray);
        } else {
            runtime.getObject().setConstantQuiet("ARGV", argvArray);
            runtime.getGlobalVariables().define("$*", new ValueAccessor(argvArray), GLOBAL);
        }
    }

    public static void createGlobals(ThreadContext context, Ruby runtime) {
        GlobalVariables globals = runtime.getGlobalVariables();

        runtime.defineGlobalConstant("TOPLEVEL_BINDING", runtime.newBinding());
        
        runtime.defineGlobalConstant("TRUE", runtime.getTrue());
        runtime.defineGlobalConstant("FALSE", runtime.getFalse());
        runtime.defineGlobalConstant("NIL", runtime.getNil());

        initARGV(runtime);

        IAccessor d = new ValueAccessor(runtime.newString(
                runtime.getInstanceConfig().displayedFileName()));
        globals.define("$PROGRAM_NAME", d, GLOBAL);
        globals.define("$0", d, GLOBAL);

        // Version information:
        IRubyObject version = null;
        IRubyObject patchlevel = null;
        IRubyObject release = runtime.newString(Constants.COMPILE_DATE).freeze(context);
        IRubyObject platform = runtime.newString(Constants.PLATFORM).freeze(context);
        IRubyObject engine = runtime.newString(Constants.ENGINE).freeze(context);

        version = runtime.newString(Constants.RUBY_VERSION).freeze(context);
        patchlevel = runtime.newFixnum(Constants.RUBY_PATCHLEVEL);
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
        
        RubyInstanceConfig.Verbosity verbosity = runtime.getInstanceConfig().getVerbosity();
        runtime.defineVariable(new WarningGlobalVariable(runtime, "$-W", verbosity), GLOBAL);

        final GlobalVariable kcodeGV; 
        kcodeGV = new NonEffectiveGlobalVariable(runtime, "$KCODE", runtime.getNil());

        runtime.defineVariable(kcodeGV, GLOBAL);
        runtime.defineVariable(new GlobalVariable.Copy(runtime, "$-K", kcodeGV), GLOBAL);
        IRubyObject defaultRS = runtime.newString(runtime.getInstanceConfig().getRecordSeparator()).freeze(context);
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
            runtime.defineVariable(new GlobalVariable(runtime, "$;", runtime.getNil()), GLOBAL);
        } else {
            runtime.defineVariable(new GlobalVariable(runtime, "$;", RubyRegexp.newRegexp(runtime, runtime.getInstanceConfig().getInputFieldSeparator(), new RegexpOptions())), GLOBAL);
        }
        
        RubyInstanceConfig.Verbosity verbose = runtime.getInstanceConfig().getVerbosity();
        IRubyObject verboseValue = null;
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

        IRubyObject stdin = RubyIO.prepStdio(
                runtime, runtime.getIn(), prepareStdioChannel(runtime, STDIO.IN, runtime.getIn()), OpenFile.READABLE, runtime.getIO(), "<STDIN>");
        IRubyObject stdout = RubyIO.prepStdio(
                runtime, runtime.getOut(), prepareStdioChannel(runtime, STDIO.OUT, runtime.getOut()), OpenFile.WRITABLE, runtime.getIO(), "<STDOUT>");
        IRubyObject stderr = RubyIO.prepStdio(
                runtime, runtime.getErr(), prepareStdioChannel(runtime, STDIO.ERR, runtime.getErr()), OpenFile.WRITABLE | OpenFile.SYNC, runtime.getIO(), "<STDERR>");

        runtime.defineVariable(new InputGlobalVariable(runtime, "$stdin", stdin), GLOBAL);
        runtime.defineVariable(new OutputGlobalVariable(runtime, "$stdout", stdout), GLOBAL);
        globals.alias("$>", "$stdout");
        runtime.defineVariable(new OutputGlobalVariable(runtime, "$stderr", stderr), GLOBAL);

        runtime.defineGlobalConstant("STDIN", stdin);
        runtime.defineGlobalConstant("STDOUT", stdout);
        runtime.defineGlobalConstant("STDERR", stderr);

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

        // On platforms without a c-library accessable through JNA, getpid will return hashCode 
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

    private static Channel prepareStdioChannel(Ruby runtime, STDIO stdio, Object stream) {
        if (runtime.getPosix().isNative() && stdio.isJVMDefault(stream) && !Platform.IS_WINDOWS) {
            // use real native channel for stdio
            return new NativeDeviceChannel(stdio.fileno());
        } else {
            switch (stdio) {
                case IN:
                    stream = ShellLauncher.unwrapFilterInputStream((InputStream)stream);
                    if (stream instanceof FileInputStream) {
                        return ((FileInputStream)stream).getChannel();
                    }

                    return Channels.newChannel((InputStream)stream);
                case OUT:
                case ERR:
                    stream = ShellLauncher.unwrapFilterOutputStream((OutputStream)stream);
                    if (stream instanceof FileOutputStream) {
                        return ((FileOutputStream)stream).getChannel();
                    }

                    return Channels.newChannel((OutputStream)stream);
                default: throw new RuntimeException("invalid stdio: " + stdio);
            }
        }
    }

    private static void defineGlobalEnvConstants(Ruby runtime) {
    	Map environmentVariableMap = null;
    	OSEnvironment environment = new OSEnvironment();
        environmentVariableMap = environment.getEnvironmentVariableMap(runtime);
		
    	if (environmentVariableMap == null) {
            // if the environment variables can't be obtained, define an empty ENV
    		environmentVariableMap = new HashMap();
    	}

    	CaseInsensitiveStringOnlyRubyHash env = new CaseInsensitiveStringOnlyRubyHash(runtime,
                                                       environmentVariableMap, 
                                                       runtime.getNil(),
                                                       runtime.getInstanceConfig().isNativeEnabled() && 
                                                           runtime.getInstanceConfig().isUpdateNativeENVEnabled() );
        env.getSingletonClass().defineAnnotatedMethods(CaseInsensitiveStringOnlyRubyHash.class);
        runtime.defineGlobalConstant("ENV", env);
        runtime.setENV(env);

        // Define System.getProperties() in ENV_JAVA
        Map systemProps = environment.getSystemPropertiesMap(runtime);
        RubyHash systemPropsHash = new ReadOnlySystemPropertiesHash(
                runtime, systemProps, runtime.getNil());
        systemPropsHash.setFrozen(true);
        runtime.defineGlobalConstant("ENV_JAVA", systemPropsHash);
    }

    /**
     * Obligate string-keyed and string-valued hash, used for ENV.
     * On Windows, the keys are case-insensitive for ENV
     *
     */
    public static class CaseInsensitiveStringOnlyRubyHash extends StringOnlyRubyHash {

        public CaseInsensitiveStringOnlyRubyHash(Ruby runtime, Map valueMap, IRubyObject defaultValue, boolean updateRealENV) {
            super(runtime, valueMap, defaultValue, updateRealENV);
        }

        @Override
        public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
            return case_aware_op_aref(context, key, false);
        }

        @Override
        public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject value) {
            return case_aware_op_aset(context, key, value, false);
        }

        @JRubyMethod
        @Override
        public IRubyObject to_s(){
            return getRuntime().newString("ENV");
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
        private boolean updateRealENV;

        public StringOnlyRubyHash(Ruby runtime, Map valueMap, IRubyObject defaultValue, boolean updateRealENV) {
            super(runtime, valueMap, defaultValue);
            this.updateRealENV = updateRealENV;
        }

        public StringOnlyRubyHash(Ruby runtime, Map valueMap, IRubyObject defaultValue) {
            this(runtime, valueMap, defaultValue, false);
        }

        @Override
        public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
            return case_aware_op_aref(context, key, true);
        }

        @Override
        public RubyHash to_hash() {
            Ruby runtime = getRuntime();
            RubyHash hash = RubyHash.newHash(runtime);
            hash.replace(runtime.getCurrentContext(), this);
            return hash;
        }

        @Override
        public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject value) {
            return case_aware_op_aset(context, key, value, true);
        }

        @Override
        @Deprecated
        public IRubyObject op_aset19(ThreadContext context, IRubyObject key, IRubyObject value) {
            return op_aset(context, key, value);
        }

        protected IRubyObject case_aware_op_aref(ThreadContext context, IRubyObject key, boolean caseSensitive) {
            if (! caseSensitive) {
                key = getCorrectKey(key, context);
            }
            return super.op_aref(context, key);
        }

        protected IRubyObject case_aware_op_aset(ThreadContext context, IRubyObject key, IRubyObject value, boolean caseSensitive) {
            if (!key.respondsTo("to_str")) {
                throw getRuntime().newTypeError("can't convert " + key.getMetaClass() + " into String");
            }
            if (!value.respondsTo("to_str") && !value.isNil()) {
                throw getRuntime().newTypeError("can't convert " + value.getMetaClass() + " into String");
            }

            if (! caseSensitive) {
                key = getCorrectKey(key, context);
            }

            if (value.isNil()) {
                return super.delete(context, key, org.jruby.runtime.Block.NULL_BLOCK);
            }

            IRubyObject keyAsStr = normalizeEnvString(Helpers.invoke(context, key, "to_str"));
            IRubyObject valueAsStr = value.isNil() ? getRuntime().getNil() :
                    normalizeEnvString(Helpers.invoke(context, value, "to_str"));

            if (updateRealENV) {
                POSIX posix = getRuntime().getPosix();
                String keyAsJava = keyAsStr.asJavaString();
                // libc (un)setenv is not reentrant, so we need to synchronize across the entire JVM (JRUBY-5933)
                if (valueAsStr == getRuntime().getNil()) {
                    synchronized (Object.class) { posix.unsetenv(keyAsJava); }
                } else {
                    synchronized (Object.class) { posix.setenv(keyAsJava, valueAsStr.asJavaString(), 1); }
                }
            }

            return super.op_aset(context, keyAsStr, valueAsStr);

        }

        private RubyString getCorrectKey(IRubyObject key, ThreadContext context) {
            RubyString originalKey = key.convertToString();
            RubyString actualKey = originalKey;
            Ruby runtime = context.runtime;
            if (Platform.IS_WINDOWS) {
                // this is a rather ugly hack, but similar to MRI. See hash.c:ruby_setenv and similar in MRI
                // we search all keys for a case-insensitive match, and use that
                RubyArray keys = super.keys();
                for (int i = 0; i < keys.size(); i++) {
                    RubyString candidateKey = keys.eltInternal(i).convertToString();
                    if (candidateKey.casecmp(context, originalKey).op_equal(context, RubyFixnum.zero(runtime)).isTrue()) {
                        actualKey = candidateKey;
                        break;
                    }
                }
            }
            return actualKey;
        }

        private IRubyObject normalizeEnvString(IRubyObject str) {
            if (str instanceof RubyString) {
                Encoding enc = getRuntime().getEncodingService().getLocaleEncoding();
                RubyString newStr = getRuntime().newString(new ByteList(str.toString().getBytes(), enc));
                newStr.setFrozen(true);
                return newStr;
            } else {
                return str;
            }
        }
    }

    private static class ReadOnlySystemPropertiesHash extends StringOnlyRubyHash {
        public ReadOnlySystemPropertiesHash(Ruby runtime, Map valueMap, IRubyObject defaultValue, boolean updateRealENV) {
            super(runtime, valueMap, defaultValue, updateRealENV);
        }

        public ReadOnlySystemPropertiesHash(Ruby runtime, Map valueMap, IRubyObject defaultValue) {
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
            return Helpers.getBackref(runtime, runtime.getCurrentContext());
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            Helpers.setBackref(runtime, runtime.getCurrentContext(), value);
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
            RubyArgsFile.setCurrentLineNumber(runtime.getArgsFile(), line);
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

    public static class KCodeGlobalVariable extends GlobalVariable {
        public KCodeGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject get() {
            return runtime.getKCode().kcode(runtime);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            runtime.setKCode(KCode.create(runtime, value.convertToString().toString()));
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
                throw runtime.newArgumentError("$! not set.");
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
            return Helpers.getLastLine(runtime, runtime.getCurrentContext());
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            Helpers.setLastLine(runtime, runtime.getCurrentContext(), value);
            return value;
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
}
