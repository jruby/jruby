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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.util.io.STDIO;
import java.util.HashMap;
import java.util.Map;

import org.jruby.anno.JRubyMethod;
import org.jruby.common.IRubyWarnings.ID;
import org.jruby.environment.OSEnvironment;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.javasupport.JavaUtil;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.platform.Platform;
import org.jruby.runtime.Constants;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.IAccessor;
import org.jruby.runtime.ReadonlyGlobalVariable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.KCode;

/** This class initializes global variables and constants.
 * 
 * @author jpetersen
 */
public class RubyGlobal {
    
    /**
     * Obligate string-keyed and string-valued hash, used for ENV and ENV_JAVA
     * 
     */
    public static class StringOnlyRubyHash extends RubyHash {
        
        public StringOnlyRubyHash(Ruby runtime, Map valueMap, IRubyObject defaultValue) {
            super(runtime, valueMap, defaultValue);
        }

        @Override
        public RubyHash to_hash() {
            Ruby runtime = getRuntime();
            RubyHash hash = RubyHash.newHash(runtime);
            hash.replace(runtime.getCurrentContext(), this);
            return hash;
        }

        @Override
        public IRubyObject op_aref(ThreadContext context, IRubyObject key) {
            RubyString actualKey = getCorrectKey(key, context);
            return super.op_aref(context, actualKey);
        }

        @Override
        public IRubyObject op_aset(ThreadContext context, IRubyObject key, IRubyObject value) {
            if (!key.respondsTo("to_str")) {
                throw getRuntime().newTypeError("can't convert " + key.getMetaClass() + " into String");
            }
            if (!value.respondsTo("to_str") && !value.isNil()) {
                throw getRuntime().newTypeError("can't convert " + value.getMetaClass() + " into String");
            }
            
            RubyString actualKey = getCorrectKey(key, context);

            if (value.isNil()) {
                return super.delete(context, actualKey, org.jruby.runtime.Block.NULL_BLOCK);
            }
            
            //return super.aset(getRuntime().newString("sadfasdF"), getRuntime().newString("sadfasdF"));
            return super.op_aset(context, RuntimeHelpers.invoke(context, actualKey, "to_str"),
                    value.isNil() ? getRuntime().getNil() : RuntimeHelpers.invoke(context, value, "to_str"));
        }
        
        @JRubyMethod
        @Override
        public IRubyObject to_s(){
            return getRuntime().newString("ENV");
        }

        private RubyString getCorrectKey(IRubyObject key, ThreadContext context) {
            RubyString originalKey = key.convertToString();
            RubyString actualKey = originalKey;
            Ruby runtime = context.getRuntime();
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
    }
    
    public static void createGlobals(ThreadContext context, Ruby runtime) {
        runtime.defineGlobalConstant("TOPLEVEL_BINDING", runtime.newBinding());
        
        runtime.defineGlobalConstant("TRUE", runtime.getTrue());
        runtime.defineGlobalConstant("FALSE", runtime.getFalse());
        runtime.defineGlobalConstant("NIL", runtime.getNil());
        
        // define ARGV and $* for this runtime
        RubyArray argvArray = runtime.newArray();
        String[] argv = runtime.getInstanceConfig().getArgv();
        for (int i = 0; i < argv.length; i++) {
            argvArray.append(RubyString.newStringShared(runtime, argv[i].getBytes()));
        }
        runtime.defineGlobalConstant("ARGV", argvArray);
        runtime.getGlobalVariables().defineReadonly("$*", new ValueAccessor(argvArray));

        IAccessor d = new ValueAccessor(runtime.newString(
                runtime.getInstanceConfig().displayedFileName()));
        runtime.getGlobalVariables().define("$PROGRAM_NAME", d);
        runtime.getGlobalVariables().define("$0", d);

        // Version information:
        IRubyObject version = null;
        IRubyObject patchlevel = null;
        IRubyObject release = runtime.newString(Constants.COMPILE_DATE).freeze(context);
        IRubyObject platform = runtime.newString(Constants.PLATFORM).freeze(context);
        IRubyObject engine = runtime.newString(Constants.ENGINE).freeze(context);

        switch (runtime.getInstanceConfig().getCompatVersion()) {
        case RUBY1_8:
            version = runtime.newString(Constants.RUBY_VERSION).freeze(context);
            patchlevel = runtime.newFixnum(Constants.RUBY_PATCHLEVEL).freeze(context);
            break;
        case RUBY1_9:
            version = runtime.newString(Constants.RUBY1_9_VERSION).freeze(context);
            patchlevel = runtime.newFixnum(Constants.RUBY1_9_PATCHLEVEL).freeze(context);
            break;
        }
        runtime.defineGlobalConstant("RUBY_VERSION", version);
        runtime.defineGlobalConstant("RUBY_PATCHLEVEL", patchlevel);
        runtime.defineGlobalConstant("RUBY_RELEASE_DATE", release);
        runtime.defineGlobalConstant("RUBY_PLATFORM", platform);
        runtime.defineGlobalConstant("RUBY_ENGINE", engine);

        IRubyObject description = runtime.newString(runtime.getInstanceConfig().getVersionString()).freeze(context);
        runtime.defineGlobalConstant("RUBY_DESCRIPTION", description);

        IRubyObject copyright = runtime.newString(runtime.getInstanceConfig().getCopyrightString()).freeze(context);
        runtime.defineGlobalConstant("RUBY_COPYRIGHT", copyright);

        runtime.defineGlobalConstant("VERSION", version);
        runtime.defineGlobalConstant("RELEASE_DATE", release);
        runtime.defineGlobalConstant("PLATFORM", platform);
        
        IRubyObject jrubyVersion = runtime.newString(Constants.VERSION).freeze(context);
        runtime.defineGlobalConstant("JRUBY_VERSION", jrubyVersion);
		
        GlobalVariable kcodeGV = new KCodeGlobalVariable(runtime, "$KCODE", runtime.newString("NONE"));
        runtime.defineVariable(kcodeGV);
        runtime.defineVariable(new GlobalVariable.Copy(runtime, "$-K", kcodeGV));
        IRubyObject defaultRS = runtime.newString(runtime.getInstanceConfig().getRecordSeparator()).freeze(context);
        GlobalVariable rs = new StringGlobalVariable(runtime, "$/", defaultRS);
        runtime.defineVariable(rs);
        runtime.setRecordSeparatorVar(rs);
        runtime.getGlobalVariables().setDefaultSeparator(defaultRS);
        runtime.defineVariable(new StringGlobalVariable(runtime, "$\\", runtime.getNil()));
        runtime.defineVariable(new StringGlobalVariable(runtime, "$,", runtime.getNil()));

        runtime.defineVariable(new LineNumberGlobalVariable(runtime, "$.", RubyFixnum.one(runtime)));
        runtime.defineVariable(new LastlineGlobalVariable(runtime, "$_"));
        runtime.defineVariable(new LastExitStatusVariable(runtime, "$?"));

        runtime.defineVariable(new ErrorInfoGlobalVariable(runtime, "$!", runtime.getNil()));
        runtime.defineVariable(new NonEffectiveGlobalVariable(runtime, "$=", runtime.getFalse()));

        if(runtime.getInstanceConfig().getInputFieldSeparator() == null) {
            runtime.defineVariable(new GlobalVariable(runtime, "$;", runtime.getNil()));
        } else {
            runtime.defineVariable(new GlobalVariable(runtime, "$;", RubyRegexp.newRegexp(runtime, runtime.getInstanceConfig().getInputFieldSeparator(), 0)));
        }
        
        Boolean verbose = runtime.getInstanceConfig().getVerbose();
        IRubyObject verboseValue = null;
        if (verbose == null) {
            verboseValue = runtime.getNil();
        } else if(verbose == Boolean.TRUE) {
            verboseValue = runtime.getTrue();
        } else {
            verboseValue = runtime.getFalse();
        }
        runtime.defineVariable(new VerboseGlobalVariable(runtime, "$VERBOSE", verboseValue));
        
        IRubyObject debug = runtime.newBoolean(runtime.getInstanceConfig().isDebug());
        runtime.defineVariable(new DebugGlobalVariable(runtime, "$DEBUG", debug));
        runtime.defineVariable(new DebugGlobalVariable(runtime, "$-d", debug));

        runtime.defineVariable(new SafeGlobalVariable(runtime, "$SAFE"));

        runtime.defineVariable(new BacktraceGlobalVariable(runtime, "$@"));

        IRubyObject stdin = new RubyIO(runtime, STDIO.IN);
        IRubyObject stdout = new RubyIO(runtime, STDIO.OUT);
        IRubyObject stderr = new RubyIO(runtime, STDIO.ERR);

        runtime.defineVariable(new InputGlobalVariable(runtime, "$stdin", stdin));

        runtime.defineVariable(new OutputGlobalVariable(runtime, "$stdout", stdout));
        runtime.getGlobalVariables().alias("$>", "$stdout");
        runtime.getGlobalVariables().alias("$defout", "$stdout");

        runtime.defineVariable(new OutputGlobalVariable(runtime, "$stderr", stderr));
        runtime.getGlobalVariables().alias("$deferr", "$stderr");

        runtime.defineGlobalConstant("STDIN", stdin);
        runtime.defineGlobalConstant("STDOUT", stdout);
        runtime.defineGlobalConstant("STDERR", stderr);

        runtime.defineVariable(new LoadedFeatures(runtime, "$\""));
        runtime.defineVariable(new LoadedFeatures(runtime, "$LOADED_FEATURES"));

        runtime.defineVariable(new LoadPath(runtime, "$:"));
        runtime.defineVariable(new LoadPath(runtime, "$-I"));
        runtime.defineVariable(new LoadPath(runtime, "$LOAD_PATH"));
        
        runtime.defineVariable(new MatchMatchGlobalVariable(runtime, "$&"));
        runtime.defineVariable(new PreMatchGlobalVariable(runtime, "$`"));
        runtime.defineVariable(new PostMatchGlobalVariable(runtime, "$'"));
        runtime.defineVariable(new LastMatchGlobalVariable(runtime, "$+"));
        runtime.defineVariable(new BackRefGlobalVariable(runtime, "$~"));

        // On platforms without a c-library accessable through JNA, getpid will return hashCode 
        // as $$ used to. Using $$ to kill processes could take down many runtimes, but by basing
        // $$ on getpid() where available, we have the same semantics as MRI.
        runtime.getGlobalVariables().defineReadonly("$$", new ValueAccessor(runtime.newFixnum(runtime.getPosix().getpid())));

        // after defn of $stderr as the call may produce warnings
        defineGlobalEnvConstants(runtime);
        
        // Fixme: Do we need the check or does Main.java not call this...they should consolidate 
        if (runtime.getGlobalVariables().get("$*").isNil()) {
            runtime.getGlobalVariables().defineReadonly("$*", new ValueAccessor(runtime.newArray()));
        }
        
        runtime.getGlobalVariables().defineReadonly("$-p", 
                new ValueAccessor(runtime.getInstanceConfig().isAssumePrinting() ? runtime.getTrue() : runtime.getNil()));
        runtime.getGlobalVariables().defineReadonly("$-n", 
                new ValueAccessor(runtime.getInstanceConfig().isAssumeLoop() ? runtime.getTrue() : runtime.getNil()));
        runtime.getGlobalVariables().defineReadonly("$-a", 
                new ValueAccessor(runtime.getInstanceConfig().isSplit() ? runtime.getTrue() : runtime.getNil()));
        runtime.getGlobalVariables().defineReadonly("$-l", 
                new ValueAccessor(runtime.getInstanceConfig().isProcessLineEnds() ? runtime.getTrue() : runtime.getNil()));

        // ARGF, $< object
        RubyArgsFile.initArgsFile(runtime);
    }

    private static void defineGlobalEnvConstants(Ruby runtime) {
    	Map environmentVariableMap = null;
    	OSEnvironment environment = new OSEnvironment();
        environmentVariableMap = environment.getEnvironmentVariableMap(runtime);
		
    	if (environmentVariableMap == null) {
            // if the environment variables can't be obtained, define an empty ENV
    		environmentVariableMap = new HashMap();
    	}

        StringOnlyRubyHash h1 = new StringOnlyRubyHash(runtime,
                                                       environmentVariableMap, runtime.getNil());
        h1.getSingletonClass().defineAnnotatedMethods(StringOnlyRubyHash.class);
        runtime.defineGlobalConstant("ENV", h1);

        // Define System.getProperties() in ENV_JAVA
        Map systemProps = environment.getSystemPropertiesMap(runtime);
        runtime.defineGlobalConstant("ENV_JAVA", new StringOnlyRubyHash(
                runtime, systemProps, runtime.getNil()));
    }

    private static class NonEffectiveGlobalVariable extends GlobalVariable {
        public NonEffectiveGlobalVariable(Ruby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            runtime.getWarnings().warn(ID.INEFFECTIVE_GLOBAL, "warning: variable " + name + " is no longer effective; ignored", name);
            return value;
        }

        @Override
        public IRubyObject get() {
            runtime.getWarnings().warn(ID.INEFFECTIVE_GLOBAL, "warning: variable " + name + " is no longer effective", name);
            return runtime.getFalse();
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
            runtime.getCurrentContext().setLastExitStatus(lastExitStatus);
            
            return lastExitStatus;
        }
    }

    private static class MatchMatchGlobalVariable extends GlobalVariable {
        public MatchMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }
        
        @Override
        public IRubyObject get() {
            return RubyRegexp.last_match(runtime.getCurrentContext().getCurrentScope().getBackRef(runtime));
        }
    }

    private static class PreMatchGlobalVariable extends GlobalVariable {
        public PreMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }
        
        @Override
        public IRubyObject get() {
            return RubyRegexp.match_pre(runtime.getCurrentContext().getCurrentScope().getBackRef(runtime));
        }
    }

    private static class PostMatchGlobalVariable extends GlobalVariable {
        public PostMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }
        
        @Override
        public IRubyObject get() {
            return RubyRegexp.match_post(runtime.getCurrentContext().getCurrentScope().getBackRef(runtime));
        }
    }

    private static class LastMatchGlobalVariable extends GlobalVariable {
        public LastMatchGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }
        
        @Override
        public IRubyObject get() {
            return RubyRegexp.match_last(runtime.getCurrentContext().getCurrentScope().getBackRef(runtime));
        }
    }

    private static class BackRefGlobalVariable extends GlobalVariable {
        public BackRefGlobalVariable(Ruby runtime, String name) {
            super(runtime, name, runtime.getNil());
        }
        
        @Override
        public IRubyObject get() {
            return RuntimeHelpers.getBackref(runtime, runtime.getCurrentContext());
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            RuntimeHelpers.setBackref(runtime, runtime.getCurrentContext(), value);
            return value;
        }
    }

    // Accessor methods.

    private static class LineNumberGlobalVariable extends GlobalVariable {
        public LineNumberGlobalVariable(Ruby runtime, String name, RubyFixnum value) {
            super(runtime, name, value);
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            RubyArgsFile.setCurrentLineNumber(runtime.getGlobalVariables().get("$<"), value);
            return super.set(value);
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
            return runtime.newFixnum(runtime.getSafeLevel());
        }

        @Override
        public IRubyObject set(IRubyObject value) {
//            int level = RubyNumeric.fix2int(value);
//            if (level < runtime.getSafeLevel()) {
//            	throw runtime.newSecurityError("tried to downgrade safe level from " + 
//            			runtime.getSafeLevel() + " to " + level);
//            }
//            runtime.setSafeLevel(level);
            // thread.setSafeLevel(level);
            runtime.getWarnings().warn(ID.SAFE_NOT_SUPPORTED, "SAFE levels are not supported in JRuby");
            return RubyFixnum.newFixnum(runtime, runtime.getSafeLevel());
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
            return RuntimeHelpers.getLastLine(runtime, runtime.getCurrentContext());
        }

        @Override
        public IRubyObject set(IRubyObject value) {
            RuntimeHelpers.setLastLine(runtime, runtime.getCurrentContext(), value);
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
            if (value instanceof RubyIO) {
                RubyIO io = (RubyIO)value;
                
                // HACK: in order to have stdout/err act like ttys and flush always,
                // we set anything assigned to stdout/stderr to sync
                io.getHandler().setSync(true);
            }

            if (!value.respondsTo("write")) {
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
}
