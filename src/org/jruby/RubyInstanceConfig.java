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
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jruby.exceptions.MainExitException;
import org.jruby.runtime.Constants;
import org.jruby.runtime.load.LoadService;
import org.jruby.util.JRubyFile;
import org.jruby.util.JRubyClassLoader;
import org.jruby.util.KCode;
import org.jruby.util.SafePropertyAccessor;

public class RubyInstanceConfig {
    
    public enum CompileMode {
        JIT, FORCE, OFF;
        
        public boolean shouldPrecompileCLI() {
            switch (this) {
            case JIT: case FORCE:
                return true;
            }
            return false;
        }
        
        public boolean shouldJIT() {
            switch (this) {
            case JIT: case FORCE:
                return true;
            }
            return false;
        }
        
        public boolean shouldPrecompileAll() {
            return this == FORCE;
        }
    }
    private InputStream input          = System.in;
    private PrintStream output         = System.out;
    private PrintStream error          = System.err;
    private Profile profile            = Profile.DEFAULT;
    private boolean objectSpaceEnabled
            = SafePropertyAccessor.getBoolean("jruby.objectspace.enabled", false);
    private CompileMode compileMode = CompileMode.JIT;
    private boolean runRubyInProcess   = true;
    private String currentDirectory;
    private Map environment;
    private String[] argv = {};

    private final boolean jitLogging;
    private final boolean jitLoggingVerbose;
    private final int jitThreshold;
    private final boolean samplingEnabled;
    private CompatVersion compatVersion;

    private final String defaultRegexpEngine;
    private final JRubyClassLoader defaultJRubyClassLoader;

    private ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    // from CommandlineParser
    private List<String> loadPaths = new ArrayList<String>();
    private StringBuffer inlineScript = new StringBuffer();
    private boolean hasInlineScript = false;
    private String scriptFileName = null;
    private List<String> requiredLibraries = new ArrayList<String>();
    private boolean benchmarking = false;
    private boolean assumeLoop = false;
    private boolean assumePrinting = false;
    private boolean processLineEnds = false;
    private boolean split = false;
    private boolean verbose = false;
    private boolean debug = false;
    private boolean showVersion = false;
    private boolean endOfArguments = false;
    private boolean shouldRunInterpreter = true;
    private boolean shouldPrintUsage = false;
    private boolean shouldPrintProperties=false;
    private boolean yarv = false;
    private boolean rubinius = false;
    private boolean yarvCompile = false;
    private KCode kcode = KCode.NONE;
    
    public static final boolean FRAMELESS_COMPILE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.compile.frameless");
    public static boolean nativeEnabled = true;
    
    static {
        try {
            if (System.getProperty("jruby.native.enabled") != null) {
                nativeEnabled = Boolean.getBoolean("jruby.native.enabled");
            }
        } catch (SecurityException se) {
            nativeEnabled = false;
        }
    }

    public int characterIndex = 0;
    
    public static interface LoadServiceCreator {
        LoadService create(Ruby runtime);

        LoadServiceCreator DEFAULT = new LoadServiceCreator() {
            public LoadService create(Ruby runtime) {
                return new LoadService(runtime);
            }
        };
    }

    private LoadServiceCreator creator = LoadServiceCreator.DEFAULT;

    public RubyInstanceConfig() {
        if (Ruby.isSecurityRestricted())
            currentDirectory = "/";
        else {
            currentDirectory = JRubyFile.getFileProperty("user.dir");
        }

        samplingEnabled = SafePropertyAccessor.getBoolean("jruby.sampling.enabled", false);
        String compatString = SafePropertyAccessor.getProperty("jruby.compat.version", "RUBY1_8");
        if (compatString.equalsIgnoreCase("RUBY1_8")) {
            compatVersion = CompatVersion.RUBY1_8;
        } else if (compatString.equalsIgnoreCase("RUBY1_9")) {
            compatVersion = CompatVersion.RUBY1_9;
        } else {
            System.err.println("Compatibility version `" + compatString + "' invalid; use RUBY1_8 or RUBY1_9. Using RUBY1_8.");
            compatVersion = CompatVersion.RUBY1_8;
        }
        
        if (Ruby.isSecurityRestricted()) {
            compileMode = CompileMode.OFF;
            jitLogging = false;
            jitLoggingVerbose = false;
            jitThreshold = -1;
        } else {
            String threshold = SafePropertyAccessor.getProperty("jruby.jit.threshold");

            runRubyInProcess = SafePropertyAccessor.getBoolean("jruby.launch.inproc", true);
            boolean jitProperty = SafePropertyAccessor.getProperty("jruby.jit.enabled") != null;
            if (jitProperty) {
                error.print("jruby.jit.enabled property is deprecated; use jruby.compile.mode=(OFF|JIT|FORCE) for -C, default, and +C flags");
                compileMode = SafePropertyAccessor.getBoolean("jruby.jit.enabled") ? CompileMode.JIT : CompileMode.OFF;
            } else {
                String jitModeProperty = SafePropertyAccessor.getProperty("jruby.compile.mode", "JIT");
                
                if (jitModeProperty.equals("OFF")) {
                    compileMode = CompileMode.OFF;
                } else if (jitModeProperty.equals("JIT")) {
                    compileMode = CompileMode.JIT;
                } else if (jitModeProperty.equals("FORCE")) {
                    compileMode = CompileMode.FORCE;
                } else {
                    error.print("jruby.jit.mode property must be OFF, JIT, FORCE, or unset; defaulting to JIT");
                    compileMode = CompileMode.JIT;
                }
            }
            jitLogging = SafePropertyAccessor.getBoolean("jruby.jit.logging");
            jitLoggingVerbose = SafePropertyAccessor.getBoolean("jruby.jit.logging.verbose");
            jitThreshold = threshold == null ? 20 : Integer.parseInt(threshold); 
        }

        defaultRegexpEngine = SafePropertyAccessor.getProperty("jruby.regexp","jregex");
        defaultJRubyClassLoader = null;
    }
    
    public String getBasicUsageHelp() {
        StringBuffer sb = new StringBuffer();
        sb
                .append("Usage: jruby [switches] [--] [rubyfile.rb] [arguments]").append("\n")
                .append("    -e 'command'    one line of script. Several -e's allowed. Omit [programfile]").append("\n")
                .append("    -b              benchmark mode, times the script execution").append("\n")
                .append("    -Jjava option   pass an option on to the JVM (e.g. -J-Xmx512m)").append("\n")
                .append("    -Idirectory     specify $LOAD_PATH directory (may be used more than once)").append("\n")
                .append("    --              optional -- before rubyfile.rb for compatibility with ruby").append("\n")
                .append("    -d              set debugging flags (set $DEBUG to true)").append("\n")
                .append("    -v              print version number, then turn on verbose mode").append("\n")
                .append("    -O              run with ObjectSpace disabled (default; improves performance)").append("\n")
                .append("    +O              run with ObjectSpace enabled (reduces performance)").append("\n")
                .append("    -S cmd          run the specified command in JRuby's bin dir").append("\n")
                .append("    -C              disable all compilation").append("\n")
                .append("    +C              force compilation of all scripts before they are run (except eval)").append("\n")
                .append("    -y              read a YARV-compiled Ruby script and run that (EXPERIMENTAL)").append("\n")
                .append("    -Y              compile a Ruby script into YARV bytecodes and run this (EXPERIMENTAL)").append("\n")
                .append("    -R              read a Rubinius-compiled Ruby script and run that (EXPERIMENTAL)").append("\n")
                .append("    --properties    List all configuration properties (specify with -J-Dproperty=value)").append("\n");
        
        return sb.toString();
    }
    
    public String getPropertyHelp() {
        StringBuffer sb = new StringBuffer();
        sb
                .append("These properties can be used to alter runtime behavior for perf or compatibility.").append("\n")
                .append("Specify them by passing -J-D<property>=<value>").append("\n")
                .append("    jruby.objectspace.enabled=true|false").append("\n")
                .append("       Enable or disable ObjectSpace.each_object (default is disabled)").append("\n")
                .append("    jruby.compile.mode=JIT|FORCE|OFF").append("\n")
                .append("       Set compilation mode. JIT is default; FORCE compiles all, OFF disables").append("\n")
                .append("    jruby.compile.boxed=true|false").append("\n")
                .append("       Use boxed variables; this can speed up some methods. Default is false").append("\n")
                .append("    jruby.compile.frameless=true|false").append("\n")
                .append("       (EXPERIMENTAL) Turn on frameless compilation where possible").append("\n")
                .append("    jruby.jit.threshold=<invocation count>").append("\n")
                .append("       Set the JIT threshold to the specified method invocation count. Default is 20").append("\n")
                .append("    jruby.jit.logging=true|false").append("\n")
                .append("       Enable JIT logging (reports successful compilation). Default is false").append("\n")
                .append("    jruby.jit.logging.verbose=true|false").append("\n")
                .append("       Enable verbose JIT logging (reports failed compilation). Default is false").append("\n")
                .append("    jruby.launch.inproc=true|false").append("\n")
                .append("       Set in-process launching of e.g. system('ruby ...'). Default is true").append("\n")
                .append("    jruby.native.enabled=true|false").append("\n")
                .append("       Enable/disable native extensions (like JNA for non-Java APIs; Default is true").append("\n")
                .append("       (This affects all JRuby instances in a given JVM)").append("\n");
        
        return sb.toString();
    }
    
    public String getVersionString() {
        StringBuffer buf = new StringBuffer("ruby ");
        switch (compatVersion) {
        case RUBY1_8:
            buf.append(Constants.RUBY_VERSION);
            break;
        case RUBY1_9:
            buf.append(Constants.RUBY1_9_VERSION);
            break;
        }
        buf
                .append(" (")
                .append(Constants.COMPILE_DATE + " rev " + Constants.REVISION)
                .append(") [")
                .append(SafePropertyAccessor.getProperty("os.arch", "unknown") + "-jruby" + Constants.VERSION)
                .append("]")
                .append("\n");
        
        return buf.toString();
    }
    
    public void processArguments(String[] arguments) {
        new ArgumentProcessor(arguments).processArguments();
    }

    public LoadServiceCreator getLoadServiceCreator() {
        return creator;
    }

    public void setLoadServiceCreator(LoadServiceCreator creator) {
        this.creator = creator;
    }

    public LoadService createLoadService(Ruby runtime) {
        return this.creator.create(runtime);
    }

    public CompileMode getCompileMode() {
        return compileMode;
    }
    
    public void setCompileMode(CompileMode compileMode) {
        this.compileMode = compileMode;
    }

    public boolean isJitLogging() {
        return jitLogging;
    }

    public boolean isJitLoggingVerbose() {
        return jitLoggingVerbose;
    }

    public boolean isSamplingEnabled() {
        return samplingEnabled;
    }
    
    public int getJitThreshold() {
        return jitThreshold;
    }
    
    public boolean isRunRubyInProcess() {
        return runRubyInProcess;
    }
    
    public void setRunRubyInProcess(boolean flag) {
        this.runRubyInProcess = flag;
    }

    public void setInput(InputStream newInput) {
        input = newInput;
    }

    public InputStream getInput() {
        return input;
    }

    public CompatVersion getCompatVersion() {
        return compatVersion;
    }

    public void setOutput(PrintStream newOutput) {
        output = newOutput;
    }

    public PrintStream getOutput() {
        return output;
    }

    public void setError(PrintStream newError) {
        error = newError;
    }

    public PrintStream getError() {
        return error;
    }

    public void setCurrentDirectory(String newCurrentDirectory) {
        currentDirectory = newCurrentDirectory;
    }

    public String getCurrentDirectory() {
        return currentDirectory;
    }

    public void setProfile(Profile newProfile) {
        profile = newProfile;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setObjectSpaceEnabled(boolean newObjectSpaceEnabled) {
        objectSpaceEnabled = newObjectSpaceEnabled;
    }

    public boolean isObjectSpaceEnabled() {
        return objectSpaceEnabled;
    }

    public void setEnvironment(Map newEnvironment) {
        environment = newEnvironment;
    }

    public Map getEnvironment() {
        return environment;
    }

    public String getDefaultRegexpEngine() {
        return defaultRegexpEngine;
    }
    
    public JRubyClassLoader getJRubyClassLoader() {
        return defaultJRubyClassLoader;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public void setLoader(ClassLoader loader) {
        this.loader = loader;
    }
    
    public String[] getArgv() {
        return argv;
    }
    
    public void setArgv(String[] argv) {
        this.argv = argv;
    }

    private class ArgumentProcessor {
        private String[] arguments;
        private int argumentIndex = 0;
        
        public ArgumentProcessor(String[] arguments) {
            this.arguments = arguments;
        }
        
        public void processArguments() {
            while (argumentIndex < arguments.length && isInterpreterArgument(arguments[argumentIndex])) {
                processArgument();
                argumentIndex++;
            }

            if (!hasInlineScript) {
                if (argumentIndex < arguments.length) {
                    setScriptFileName(arguments[argumentIndex]); //consume the file name
                    argumentIndex++;
                }
            }


            // Remaining arguments are for the script itself
            argv = new String[arguments.length - argumentIndex];
            System.arraycopy(arguments, argumentIndex, argv, 0, argv.length);
        }

        private boolean isInterpreterArgument(String argument) {
            return (argument.charAt(0) == '-' || argument.charAt(0) == '+') && !endOfArguments;
        }

        private void processArgument() {
            String argument = arguments[argumentIndex];
            FOR : for (characterIndex = 1; characterIndex < argument.length(); characterIndex++) {
                switch (argument.charAt(characterIndex)) {
                    case 'h' :
                        shouldPrintUsage = true;
                        shouldRunInterpreter = false;
                        break;
                    case 'I' :
                        String s = grabValue(" -I must be followed by a directory name to add to lib path");
                        String[] ls = s.split(java.io.File.pathSeparator);
                        for(int i=0;i<ls.length;i++) {
                            loadPaths.add(ls[i]);
                        }
                        break FOR;
                    case 'r' :
                        requiredLibraries.add(grabValue("-r must be followed by a package to require"));
                        break FOR;
                    case 'e' :
                        inlineScript.append(grabValue(" -e must be followed by an expression to evaluate"));
                        inlineScript.append('\n');
                        hasInlineScript = true;
                        break FOR;
                    case 'b' :
                        benchmarking = true;
                        break;
                    case 'p' :
                        assumePrinting = true;
                        assumeLoop = true;
                        break;
                    case 'O' :
                        if (argument.charAt(0) == '-') {
                            objectSpaceEnabled = false;
                        } else if (argument.charAt(0) == '+') {
                            objectSpaceEnabled = true;
                        }
                        break;
                    case 'C' :
                        if (argument.charAt(0) == '-') {
                            compileMode = CompileMode.OFF;
                        } else if (argument.charAt(0) == '+') {
                            compileMode = CompileMode.FORCE;
                        }
                        break;
                    case 'y' :
                        yarv = true;
                        break;
                    case 'Y' :
                        yarvCompile = true;
                        break;
                    case 'R' :
                        rubinius = true;
                        break;
                    case 'n' :
                        assumeLoop = true;
                        break;
                    case 'a' :
                        split = true;
                        break;
                    case 'd' :
                        debug = true;
                        verbose = true;
                        break;
                    case 'l' :
                        processLineEnds = true;
                        break;
                    case 'v' :
                        verbose = true;
                        setShowVersion(true);
                        break;
                    case 'w' :
                        verbose = true;
                        break;
                    case 'K':
                        // FIXME: No argument seems to work for -K in MRI plus this should not
                        // siphon off additional args 'jruby -K ~/scripts/foo'.  Also better error
                        // processing.
                        String eArg = grabValue("provide a value for -K");
                        kcode = KCode.create(null, eArg);
                        break;
                    case 'S':
                        runBinScript();
                        break FOR;
                    case '-' :
                        if (argument.equals("--version")) {
                            setShowVersion(true);
                            break FOR;
                        } else if(argument.equals("--debug")) {
                            debug = true;
                            verbose = true;
                            break;
                        } else if (argument.equals("--help")) {
                            shouldPrintUsage = true;
                            shouldRunInterpreter = false;
                            break;
                        } else if (argument.equals("--command") || argument.equals("--bin")) {
                            characterIndex = argument.length();
                            runBinScript();
                            break;
                        } else if (argument.equals("--compat")) {
                            characterIndex = argument.length();
                            compatVersion = CompatVersion.getVersionFromString(grabValue("--compat must be RUBY1_8 or RUBY1_9"));
                            if (compatVersion == null) {
                                compatVersion = CompatVersion.RUBY1_8;
                            }
                            break FOR;
                        } else if (argument.equals("--properties")) {
                            shouldPrintProperties = true;
                            shouldRunInterpreter = false;
                            break;
                        } else {
                            if (argument.equals("--")) {
                                // ruby interpreter compatibilty 
                                // Usage: ruby [switches] [--] [programfile] [arguments])
                                endOfArguments = true;
                                break;
                            }
                        }
                    default :
                        throw new MainExitException(1, "unknown option " + argument.charAt(characterIndex));
                }
            }
        }

        private void runBinScript() {
            requiredLibraries.add("jruby/commands");
            inlineScript.append("JRuby::Commands." + grabValue("provide a bin script to execute"));
            inlineScript.append("\n");
            hasInlineScript = true;
            endOfArguments = true;
        }

        private String grabValue(String errorMessage) {
            characterIndex++;
            if (characterIndex < arguments[argumentIndex].length()) {
                return arguments[argumentIndex].substring(characterIndex);
            }
            argumentIndex++;
            if (argumentIndex < arguments.length) {
                return arguments[argumentIndex];
            }

            MainExitException mee = new MainExitException(1, "invalid argument " + argumentIndex + "\n" + errorMessage);
            mee.setUsageError(true);

            throw mee;
        }
    }

    public byte[] inlineScript() {
        try {
            return inlineScript.toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return inlineScript.toString().getBytes();
        }
    }

    public List<String> requiredLibraries() {
        return requiredLibraries;
    }

    public List<String> loadPaths() {
        return loadPaths;
    }

    public boolean shouldRunInterpreter() {
        if(isShowVersion() && (hasInlineScript || scriptFileName != null)) {
            return true;
        }
        return isShouldRunInterpreter();
    }
    
    public boolean shouldPrintUsage() {
        return shouldPrintUsage;
    }
    
    public boolean shouldPrintProperties() {
        return shouldPrintProperties;
    }
    
    private boolean isSourceFromStdin() {
        return getScriptFileName() == null;
    }
    
    public boolean isInlineScript() {
        return hasInlineScript;
    }

    public InputStream getScriptSource() {
        try {
            // KCode.NONE is used because KCODE does not affect parse in Ruby 1.8
            // if Ruby 2.0 encoding pragmas are implemented, this will need to change
            if (hasInlineScript) {
                if (scriptFileName != null) {
                    File file = JRubyFile.create(getCurrentDirectory(), getScriptFileName());
                    return new FileInputStream(file);
                }
                
                return new ByteArrayInputStream(inlineScript());
            } else if (isSourceFromStdin()) {
                // can't use -v and stdin
                if (isShowVersion()) {
                    return null;
                }
                return getInput();
            } else {
                File file = JRubyFile.create(getCurrentDirectory(), getScriptFileName());
                return new FileInputStream(file);
            }
        } catch (IOException e) {
            throw new MainExitException(1, "Error opening script file: " + e.getMessage());
        }
    }

    public String displayedFileName() {
        if (hasInlineScript) {
            if (scriptFileName != null) {
                return scriptFileName;
            } else {
                return "-e";
            }
        } else if (isSourceFromStdin()) {
            return "-";
        } else {
            return getScriptFileName();
        }
    }

    private void setScriptFileName(String scriptFileName) {
        this.scriptFileName = scriptFileName;
    }

    public String getScriptFileName() {
        return scriptFileName;
    }

    public boolean isBenchmarking() {
        return benchmarking;
    }

    public boolean isAssumeLoop() {
        return assumeLoop;
    }

    public boolean isAssumePrinting() {
        return assumePrinting;
    }

    public boolean isProcessLineEnds() {
        return processLineEnds;
    }

    public boolean isSplit() {
        return split;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    protected void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }

    public boolean isShouldRunInterpreter() {
        return shouldRunInterpreter;
    }

    public boolean isYARVEnabled() {
        return yarv;
    }

    public boolean isRubiniusEnabled() {
        return rubinius;
    }

    public boolean isYARVCompileEnabled() {
        return yarvCompile;
    }
    
    public KCode getKCode() {
        return kcode;
    }
}
