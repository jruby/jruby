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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jruby.environment.OSEnvironmentReaderExcepton;
import org.jruby.environment.OSEnvironment;
import org.jruby.internal.runtime.ValueAccessor;
import org.jruby.runtime.Constants;
import org.jruby.runtime.GlobalVariable;
import org.jruby.runtime.ReadonlyGlobalVariable;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/** This class initializes global variables and constants.
 * 
 * @author jpetersen
 */
public class RubyGlobal {
    
    /**
     * Obligate string-keyed and string-valued hash, used for ENV and ENV_JAVA
     * 
     */
    private static class StringOnlyRubyHash extends RubyHash {
        
        public StringOnlyRubyHash(IRuby runtime, Map valueMap, IRubyObject defaultValue) {
            super(runtime, valueMap, defaultValue);
        }
        
        public IRubyObject aref(IRubyObject key) {
            if (!key.respondsTo("to_str")) {
                throw getRuntime().newTypeError("can't convert " + key.getMetaClass() + " into String");
            }

            return super.aref(key.callMethod(getRuntime().getCurrentContext(), "to_str"));
        }

        public IRubyObject aset(IRubyObject key, IRubyObject value) {
            if (!key.respondsTo("to_str")) {
                throw getRuntime().newTypeError("can't convert " + key.getMetaClass() + " into String");
            }
            if (!value.respondsTo("to_str") && !value.isNil()) {
                throw getRuntime().newTypeError("can't convert " + value.getMetaClass() + " into String");
            }

            ThreadContext context = getRuntime().getCurrentContext();
            
            return super.aset(key.callMethod(context, "to_str"),
                    value.isNil() ? getRuntime().getNil() : value.callMethod(context, "to_str"));
        }
    }
    
    public static void createGlobals(IRuby runtime) {

        // Version information:
        IRubyObject version = runtime.newString(Constants.RUBY_VERSION).freeze();
        IRubyObject release = runtime.newString(Constants.COMPILE_DATE).freeze();
        IRubyObject platform = runtime.newString(Constants.PLATFORM).freeze();

        runtime.defineGlobalConstant("RUBY_VERSION", version);
        runtime.defineGlobalConstant("RUBY_RELEASE_DATE", release);
        runtime.defineGlobalConstant("RUBY_PLATFORM", platform);

        runtime.defineGlobalConstant("VERSION", version);
        runtime.defineGlobalConstant("RELEASE_DATE", release);
        runtime.defineGlobalConstant("PLATFORM", platform);
		
        runtime.defineVariable(new StringGlobalVariable(runtime, "$KCODE", runtime.newString("UTF8")));
        runtime.defineVariable(new StringGlobalVariable(runtime, "$/", runtime.newString("\n")));
        runtime.defineVariable(new StringGlobalVariable(runtime, "$\\", runtime.getNil()));
        runtime.defineVariable(new StringGlobalVariable(runtime, "$,", runtime.getNil()));

        runtime.defineVariable(new LineNumberGlobalVariable(runtime, "$.", RubyFixnum.one(runtime)));
        runtime.defineVariable(new LastlineGlobalVariable(runtime, "$_"));

        runtime.defineVariable(new ErrorInfoGlobalVariable(runtime, "$!", runtime.getNil()));

        runtime.defineVariable(new SafeGlobalVariable(runtime, "$SAFE"));

        runtime.defineVariable(new BacktraceGlobalVariable(runtime, "$@"));

        IRubyObject stdin = RubyIO.fdOpen(runtime, RubyIO.STDIN);
        IRubyObject stdout = RubyIO.fdOpen(runtime, RubyIO.STDOUT);
        IRubyObject stderr = RubyIO.fdOpen(runtime, RubyIO.STDERR);

        runtime.defineVariable(new InputGlobalVariable(runtime, "$stdin", stdin));

        runtime.defineVariable(new OutputGlobalVariable(runtime, "$stdout", stdout));
        runtime.defineVariable(new OutputGlobalVariable(runtime, "$stderr", stderr));
        runtime.defineVariable(new OutputGlobalVariable(runtime, "$>", stdout));
        runtime.defineVariable(new OutputGlobalVariable(runtime, "$defout", stdout));
        runtime.defineVariable(new OutputGlobalVariable(runtime, "$deferr", stderr));

        runtime.defineGlobalConstant("STDIN", stdin);
        runtime.defineGlobalConstant("STDOUT", stdout);
        runtime.defineGlobalConstant("STDERR", stderr);

        runtime.defineVariable(new LoadedFeatures(runtime, "$\""));
        runtime.defineVariable(new LoadedFeatures(runtime, "$LOADED_FEATURES"));

        runtime.defineVariable(new LoadPath(runtime, "$:"));
        runtime.defineVariable(new LoadPath(runtime, "$-I"));
        runtime.defineVariable(new LoadPath(runtime, "$LOAD_PATH"));

        // after defn of $stderr as the call may produce warnings
        defineGlobalEnvConstants(runtime);
        
        // Fixme: Do we need the check or does Main.java not call this...they should consolidate 
        if (runtime.getGlobalVariables().get("$*").isNil()) {
            runtime.getGlobalVariables().defineReadonly("$*", new ValueAccessor(runtime.newArray()));
        }
        
        // ARGF, $< object
        RubyArgsFile argsFile = new RubyArgsFile(runtime);
        argsFile.initArgsFile();
    }

    private static void defineGlobalEnvConstants(IRuby runtime) {

    	Map environmentVariableMap = null;
    	OSEnvironment environment = new OSEnvironment();
    	try {
    		environmentVariableMap = environment.getEnvironmentVariableMap(runtime);
    	} catch (OSEnvironmentReaderExcepton e) {
    		// If the environment variables are not accessible shouldn't terminate 
    		runtime.getWarnings().warn(e.getMessage());
    	}
		
    	if (environmentVariableMap == null) {
            // if the environment variables can't be obtained, define an empty ENV
    		environmentVariableMap = new HashMap();
    	}
        runtime.defineGlobalConstant("ENV", new StringOnlyRubyHash(runtime,
                environmentVariableMap, runtime.getNil()));

        // Define System.getProperties() in ENV_JAVA
        Map systemProps = environment.getSystemPropertiesMap(runtime);
        runtime.defineGlobalConstant("ENV_JAVA", new StringOnlyRubyHash(
                runtime, systemProps, runtime.getNil()));
        
    }



    // Accessor methods.

    private static class LineNumberGlobalVariable extends GlobalVariable {
        public LineNumberGlobalVariable(IRuby runtime, String name, RubyFixnum value) {
            super(runtime, name, value);
        }

        public IRubyObject set(IRubyObject value) {
            ((RubyArgsFile) runtime.getGlobalVariables().get("$<")).setCurrentLineNumber(RubyNumeric.fix2int(value));
            return super.set(value);
        }
    }

    private static class ErrorInfoGlobalVariable extends GlobalVariable {
        public ErrorInfoGlobalVariable(IRuby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        public IRubyObject set(IRubyObject value) {
            if (!value.isNil() && ! value.isKindOf(runtime.getClass("Exception"))) {
                throw runtime.newTypeError("assigning non-exception to $!");
            }
            return super.set(value);
        }
    }

    // FIXME: move out of this class!
    public static class StringGlobalVariable extends GlobalVariable {
        public StringGlobalVariable(IRuby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        public IRubyObject set(IRubyObject value) {
            if (!value.isNil() && ! (value instanceof RubyString)) {
                throw runtime.newTypeError("value of " + name() + " must be a String");
            }
            return super.set(value);
        }
    }

    private static class SafeGlobalVariable extends GlobalVariable {
        public SafeGlobalVariable(IRuby runtime, String name) {
            super(runtime, name, null);
        }

        public IRubyObject get() {
            return runtime.newFixnum(runtime.getSafeLevel());
        }

        public IRubyObject set(IRubyObject value) {
            int level = RubyNumeric.fix2int(value);
            if (level < runtime.getSafeLevel()) {
            	throw runtime.newSecurityError("tried to downgrade safe level from " + 
            			runtime.getSafeLevel() + " to " + level);
            }
            runtime.setSafeLevel(level);
            // thread.setSafeLevel(level);
            return value;
        }
    }

    private static class BacktraceGlobalVariable extends GlobalVariable {
        public BacktraceGlobalVariable(IRuby runtime, String name) {
            super(runtime, name, null);
        }

        public IRubyObject get() {
            IRubyObject errorInfo = runtime.getGlobalVariables().get("$!");
            IRubyObject backtrace = errorInfo.isNil() ? runtime.getNil() : errorInfo.callMethod(errorInfo.getRuntime().getCurrentContext(), "backtrace");
            //$@ returns nil if $!.backtrace is not an array
            if (!(backtrace instanceof RubyArray)) {
                backtrace = runtime.getNil();
            }
            return backtrace;
        }

        public IRubyObject set(IRubyObject value) {
            if (runtime.getGlobalVariables().get("$!").isNil()) {
                throw runtime.newArgumentError("$! not set.");
            }
            runtime.getGlobalVariables().get("$!").callMethod(value.getRuntime().getCurrentContext(), "set_backtrace", value);
            return value;
        }
    }

    private static class LastlineGlobalVariable extends GlobalVariable {
        public LastlineGlobalVariable(IRuby runtime, String name) {
            super(runtime, name, null);
        }

        public IRubyObject get() {
            return runtime.getCurrentContext().getLastline();
        }

        public IRubyObject set(IRubyObject value) {
            runtime.getCurrentContext().setLastline(value);
            return value;
        }
    }

    private static class InputGlobalVariable extends GlobalVariable {
        public InputGlobalVariable(IRuby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        public IRubyObject set(IRubyObject value) {
            if (value == get()) {
                return value;
            }
            if (value instanceof RubyIO) {
                ((RubyIO) value).checkReadable();
            }
            return super.set(value);
        }
    }

    private static class OutputGlobalVariable extends GlobalVariable {
        public OutputGlobalVariable(IRuby runtime, String name, IRubyObject value) {
            super(runtime, name, value);
        }

        public IRubyObject set(IRubyObject value) {
            if (value == get()) {
                return value;
            }
            if (value instanceof RubyIO) {
                ((RubyIO) value).checkWriteable();
            }
            if (! value.respondsTo("write")) {
                throw runtime.newTypeError(name() + " must have write method, " +
                                    value.getType().getName() + " given");
            }
            
            if ("$stdout".equals(name())) {
                runtime.defineVariable(new OutputGlobalVariable(runtime, "$>", value));
            }

            return super.set(value);
        }
    }
    
    private static class LoadPath extends ReadonlyGlobalVariable {
        public LoadPath(IRuby runtime, String name) {
            super(runtime, name, null);
        }
        
        /**
         * @see org.jruby.runtime.GlobalVariable#get()
         */
        public IRubyObject get() {
            return runtime.newArray(runtime.getLoadService().getLoadPath());
        }
    }

    private static class LoadedFeatures extends ReadonlyGlobalVariable {
        public LoadedFeatures(IRuby runtime, String name) {
            super(runtime, name, null);
        }
        
        /**
         * @see org.jruby.runtime.GlobalVariable#get()
         */
        public IRubyObject get() {
            return runtime.newArray(new ArrayList(runtime.getLoadService().getLoadedFeatures()));
        }
    }
}
