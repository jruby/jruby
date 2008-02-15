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
import org.jruby.ast.executable.Script;
import org.jruby.exceptions.MainExitException;
import org.jruby.runtime.Constants;
import org.jruby.util.ClassCache;
import org.jruby.util.JRubyFile;
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
    private final int jitLogEvery;
    private final int jitThreshold;
    private final int jitMax;
    private final boolean samplingEnabled;
    private CompatVersion compatVersion;

    private ClassLoader loader = Thread.currentThread().getContextClassLoader();
    
    private ClassCache<Script> classCache;
    
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
    // This property is a Boolean, to allow three values, so it can match MRI's nil, false and true
    private Boolean verbose = Boolean.FALSE;
    private boolean debug = false;
    private boolean showVersion = false;
    private boolean showCopyright = false;
    private boolean endOfArguments = false;
    private boolean shouldRunInterpreter = true;
    private boolean shouldPrintUsage = false;
    private boolean shouldPrintProperties=false;
    private boolean yarv = false;
    private boolean rubinius = false;
    private boolean yarvCompile = false;
    private KCode kcode = KCode.NONE;
    private String recordSeparator = "\n";
    private boolean shouldCheckSyntax = false;
    private String inputFieldSeparator = null;
    
    private int safeLevel = 0;

    public static final boolean FASTEST_COMPILE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.compile.fastest");
    public static final boolean FRAMELESS_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED
            || SafePropertyAccessor.getBoolean("jruby.compile.frameless");
    public static final boolean POSITIONLESS_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED
            || SafePropertyAccessor.getBoolean("jruby.compile.positionless");
    public static final boolean THREADLESS_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED
            || SafePropertyAccessor.getBoolean("jruby.compile.threadless");
    public static final boolean INDEXED_METHODS
            = SafePropertyAccessor.getBoolean("jruby.indexed.methods");
    public static final boolean FORK_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.fork.enabled");
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
            jitLogEvery = 0;
            jitThreshold = -1;
            jitMax = 0;
        } else {
            String threshold = SafePropertyAccessor.getProperty("jruby.jit.threshold");
            String max = SafePropertyAccessor.getProperty("jruby.jit.max");

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
                    error.print("jruby.compile.mode property must be OFF, JIT, FORCE, or unset; defaulting to JIT");
                    compileMode = CompileMode.JIT;
                }
            }
            jitLogging = SafePropertyAccessor.getBoolean("jruby.jit.logging");
            jitLoggingVerbose = SafePropertyAccessor.getBoolean("jruby.jit.logging.verbose");
            String logEvery = SafePropertyAccessor.getProperty("jruby.jit.logEvery");
            jitLogEvery = logEvery == null ? 0 : Integer.parseInt(logEvery);
            jitThreshold = threshold == null ? 20 : Integer.parseInt(threshold); 
            jitMax = max == null ? 2048 : Integer.parseInt(max);
        }
        
        // default ClassCache using jitMax as a soft upper bound
        classCache = new ClassCache<Script>(loader, jitMax);
        
        if (FORK_ENABLED) {
            error.print("WARNING: fork is highly unlikely to be safe or stable on the JVM. Have fun!\n");
        }
    }
    
    public String getBasicUsageHelp() {
        StringBuffer sb = new StringBuffer();
        sb
                .append("Usage: jruby [switches] [--] [programfile] [arguments]\n")
                .append("  -0[octal]       specify record separator (\0, if no argument)\n")
                .append("  -a              autosplit mode with -n or -p (splits $_ into $F)\n")
                .append("  -b              benchmark mode, times the script execution\n")
                .append("  -c              check syntax only\n")
                .append("  -Cdirectory     cd to directory, before executing your script\n")
                .append("  -d              set debugging flags (set $DEBUG to true)\n")
                .append("  -e 'command'    one line of script. Several -e's allowed. Omit [programfile]\n")
                .append("  -Fpattern       split() pattern for autosplit (-a)\n")
                //.append("  -i[extension]   edit ARGV files in place (make backup if extension supplied)\n")
                .append("  -Idirectory     specify $LOAD_PATH directory (may be used more than once)\n")
                .append("  -J[java option] pass an option on to the JVM (e.g. -J-Xmx512m)\n")
                .append("                    use --properties to list JRuby properties\n")
                .append("  -Kkcode         specifies KANJI (Japanese) code-set\n")
                .append("  -l              enable line ending processing\n")
                .append("  -n              assume 'while gets(); ... end' loop around your script\n")
                .append("  -p              assume loop like -n but print line also like sed\n")
                .append("  -rlibrary       require the library, before executing your script\n")
                //.append("  -s              enable some switch parsing for switches after script name\n")
                .append("  -S              look for the script in bin or using PATH environment variable\n")
                .append("  -T[level]       turn on tainting checks\n")
                .append("  -v              print version number, then turn on verbose mode\n")
                .append("  -w              turn warnings on for your script\n")
                .append("  -W[level]       set warning level; 0=silence, 1=medium, 2=verbose (default)\n")
                //.append("  -x[directory]   strip off text before #!ruby line and perhaps cd to directory\n")
                .append("  -X[option]      enable extended option (omit option to list)\n")
                .append("  --copyright     print the copyright\n")
                .append("  --properties    List all configuration Java properties (pass -J-Dproperty=value)\n")
                .append("  --version       print the version\n");
        
        return sb.toString();
    }
    
    public String getExtendedHelp() {
        StringBuffer sb = new StringBuffer();
        sb
                .append("These flags are for extended JRuby options.\n")
                .append("Specify them by passing -X<option>\n")
                .append("  -O              run with ObjectSpace disabled (default; improves performance)\n")
                .append("  +O              run with ObjectSpace enabled (reduces performance)\n")
                .append("  -C              disable all compilation\n")
                .append("  +C              force compilation of all scripts before they are run (except eval)\n")
                .append("  -y              read a YARV-compiled Ruby script and run that (EXPERIMENTAL)\n")
                .append("  -Y              compile a Ruby script into YARV bytecodes and run this (EXPERIMENTAL)\n")
                .append("  -R              read a Rubinius-compiled Ruby script and run that (EXPERIMENTAL)");
        
        return sb.toString();
    }
    
    public String getPropertyHelp() {
        StringBuffer sb = new StringBuffer();
        sb
                .append("These properties can be used to alter runtime behavior for perf or compatibility.\n")
                .append("Specify them by passing -J-D<property>=<value>\n")
                .append("\nCOMPILER SETTINGS:\n")
                .append("    jruby.compile.mode=JIT|FORCE|OFF\n")
                .append("       Set compilation mode. JIT is default; FORCE compiles all, OFF disables\n")
                .append("    jruby.compile.boxed=true|false\n")
                .append("       (EXPERIMENTAL) Use boxed variables; this can speed up some methods. Default is false\n")
                .append("    jruby.compile.frameless=true|false\n")
                .append("       (EXPERIMENTAL) Turn on frameless compilation where possible\n")
                .append("    jruby.compile.positionless=true|false\n")
                .append("       (EXPERIMENTAL) Turn on compilation that avoids updating Ruby position info. Default is false\n")
                .append("    jruby.compile.threadless=true|false\n")
                .append("       (EXPERIMENTAL) Turn on compilation without polling for \"unsafe\" thread events. Default is false\n")
                .append("\nJIT SETTINGS:\n")
                .append("    jruby.jit.threshold=<invocation count>\n")
                .append("       Set the JIT threshold to the specified method invocation count. Default is 20\n")
                .append("    jruby.jit.max=<method count>\n")
                .append("       Set the max count of active methods eligible for JIT-compilation.\n")
                .append("       Default is 2048 per runtime. A value of 0 disables JIT, -1 disables max.\n")
                .append("    jruby.jit.logging=true|false\n")
                .append("       Enable JIT logging (reports successful compilation). Default is false\n")
                .append("    jruby.jit.logging.verbose=true|false\n")
                .append("       Enable verbose JIT logging (reports failed compilation). Default is false\n")
                .append("    jruby.jit.logEvery=<method count>\n")
                .append("       Log a message every n methods JIT compiled. Default is 0 (off).\n")
                .append("\nNATIVE SUPPORT:\n")
                .append("    jruby.native.enabled=true|false\n")
                .append("       Enable/disable native extensions (like JNA for non-Java APIs; Default is true\n")
                .append("       (This affects all JRuby instances in a given JVM)\n")
                .append("    jruby.native.verbose=true|false\n")
                .append("       Enable verbose logging of native extension loading. Default is false.\n")
                .append("    jruby.fork.enabled=true|false\n")
                .append("       (EXPERIMENTAL, maybe dangerous) Enable fork(2) on platforms that support it.\n")
                .append("\nMISCELLANY:\n")
                .append("    jruby.compat.version=RUBY1_8|RUBY1_9\n")
                .append("       Specify the major Ruby version to be compatible with; Default is RUBY1_8\n")
                .append("    jruby.indexed.methods=true|false\n")
                .append("       Generate \"invokers\" for core classes using a single indexed class\n")
                .append("    jruby.objectspace.enabled=true|false\n")
                .append("       Enable or disable ObjectSpace.each_object (default is disabled)\n")
                .append("    jruby.launch.inproc=true|false\n")
                .append("       Set in-process launching of e.g. system('ruby ...'). Default is true\n");
        
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
    
    public String getCopyrightString() {
        return "JRuby - Copyright (C) 2001-2008 The JRuby Community (and contribs)\n";
    }
    
    public void processArguments(String[] arguments) {
        new ArgumentProcessor(arguments).processArguments();
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

    public int getJitLogEvery() {
        return jitLogEvery;
    }

    public boolean isSamplingEnabled() {
        return samplingEnabled;
    }
    
    public int getJitThreshold() {
        return jitThreshold;
    }
    
    public int getJitMax() {
        return jitMax;
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
        
        private String getArgumentError(String argument, String additionalError) {
            return "jruby: invalid argument " + argument + "\n" + additionalError + "\n";
        }
        
        private String getArgumentError(String additionalError) {
            return "jruby: invalid argument\n" + additionalError + "\n";
        }

        private void processArgument() {
            String argument = arguments[argumentIndex];
            FOR : for (characterIndex = 1; characterIndex < argument.length(); characterIndex++) {
                switch (argument.charAt(characterIndex)) {
                case '0': {
                    String temp = grabOptionalValue();
                    if (null == temp) {
                        recordSeparator = "\u0000";
                    } else if (temp.equals("0")) {
                        recordSeparator = "\n\n";
                    } else if (temp.equals("777")) {
                        recordSeparator = "\uFFFF"; // Specify something that can't separate
                    } else {
                        try {
                            int val = Integer.parseInt(temp, 8);
                            recordSeparator = "" + (char) val;
                        } catch (Exception e) {
                            MainExitException mee = new MainExitException(1, getArgumentError(" -0 must be followed by either 0, 777, or a valid octal value"));
                            mee.setUsageError(true);
                            throw mee;
                        }
                    }
                    break FOR;
                }
                case 'a':
                    split = true;
                    break;
                case 'b':
                    benchmarking = true;
                    break;
                case 'c':
                    shouldCheckSyntax = true;
                    break;
                case 'C':
                    try {
                        String saved = grabValue(getArgumentError(" -C must be followed by a directory expression"));
                        File base = new File(currentDirectory);
                        File newDir = new File(saved);
                        if (newDir.isAbsolute()) {
                            currentDirectory = newDir.getCanonicalPath();
                        } else {
                            currentDirectory = new File(base, newDir.getPath()).getCanonicalPath();
                        }
                        if (!(new File(currentDirectory).isDirectory())) {
                            MainExitException mee = new MainExitException(1, "jruby: Can't chdir to " + saved + " (fatal)");
                            mee.setUsageError(true);
                            throw mee;
                        }
                    } catch (IOException e) {
                        MainExitException mee = new MainExitException(1, getArgumentError(" -C must be followed by a valid directory"));
                        mee.setUsageError(true);
                        throw mee;
                    }
                    break;
                case 'd':
                    debug = true;
                    verbose = Boolean.TRUE;
                    break;
                case 'e':
                    inlineScript.append(grabValue(getArgumentError(" -e must be followed by an expression to evaluate")));
                    inlineScript.append('\n');
                    hasInlineScript = true;
                    break FOR;
                case 'F':
                    inputFieldSeparator = grabValue(getArgumentError(" -F must be followed by a pattern for input field separation"));
                    break;
                case 'h':
                    shouldPrintUsage = true;
                    shouldRunInterpreter = false;
                    break;
                // FIXME: -i flag not supported
//                    case 'i' :
//                        break;
                case 'I':
                    String s = grabValue(getArgumentError("-I must be followed by a directory name to add to lib path"));
                    String[] ls = s.split(java.io.File.pathSeparator);
                    for (int i = 0; i < ls.length; i++) {
                        loadPaths.add(ls[i]);
                    }
                    break FOR;
                case 'K':
                    // FIXME: No argument seems to work for -K in MRI plus this should not
                    // siphon off additional args 'jruby -K ~/scripts/foo'.  Also better error
                    // processing.
                    String eArg = grabValue(getArgumentError("provide a value for -K"));
                    kcode = KCode.create(null, eArg);
                    break;
                case 'l':
                    processLineEnds = true;
                    break;
                case 'n':
                    assumeLoop = true;
                    break;
                case 'p':
                    assumePrinting = true;
                    assumeLoop = true;
                    break;
                case 'r':
                    requiredLibraries.add(grabValue(getArgumentError("-r must be followed by a package to require")));
                    break FOR;
                // FIXME: -s flag not supported
//                    case 's' :
//                        break;
                case 'S':
                    runBinScript();
                    break FOR;
                case 'T' :{
                    String temp = grabOptionalValue();
                    int value = 1;
                    
                    if(temp!=null) {
                        try {
                            value = Integer.parseInt(temp, 8);
                        } catch(Exception e) {
                            value = 1;
                        }
                    }

                    safeLevel = value;

                    break FOR;
                }
                case 'v':
                    verbose = Boolean.TRUE;
                    setShowVersion(true);
                    break;
                case 'w':
                    verbose = Boolean.TRUE;
                    break;
                case 'W': {
                    String temp = grabOptionalValue();
                    int value = 2;
                    if (null != temp) {
                        if (temp.equals("2")) {
                            value = 2;
                        } else if (temp.equals("1")) {
                            value = 1;
                        } else if (temp.equals("0")) {
                            value = 0;
                        } else {
                            MainExitException mee = new MainExitException(1, getArgumentError(" -W must be followed by either 0, 1, 2 or nothing"));
                            mee.setUsageError(true);
                            throw mee;
                        }
                    }
                    switch (value) {
                    case 0:
                        verbose = null;
                        break;
                    case 1:
                        verbose = Boolean.FALSE;
                        break;
                    case 2:
                        verbose = Boolean.TRUE;
                        break;
                    }


                    break FOR;
                }
                // FIXME: -x flag not supported
//                    case 'x' :
//                        break;
                case 'X':
                    String extendedOption = grabValue("jruby: missing extended option, listing available options\n" + getExtendedHelp());

                    if (extendedOption.equals("-O")) {
                        objectSpaceEnabled = false;
                    } else if (extendedOption.equals("+O")) {
                        objectSpaceEnabled = true;
                    } else if (extendedOption.equals("-C")) {
                        compileMode = CompileMode.OFF;
                    } else if (extendedOption.equals("+C")) {
                        compileMode = CompileMode.FORCE;
                    } else if (extendedOption.equals("y")) {
                        yarv = true;
                    } else if (extendedOption.equals("Y")) {
                        yarvCompile = true;
                    } else if (extendedOption.equals("R")) {
                        rubinius = true;
                    } else {
                        MainExitException mee =
                                new MainExitException(1, "jruby: invalid extended option " + extendedOption + " (-X will list valid options)\n");
                        mee.setUsageError(true);

                        throw mee;
                    }
                    break FOR;
                case '-':
                    if (argument.equals("--command") || argument.equals("--bin")) {
                        characterIndex = argument.length();
                        runBinScript();
                        break;
                    } else if (argument.equals("--compat")) {
                        characterIndex = argument.length();
                        compatVersion = CompatVersion.getVersionFromString(grabValue(getArgumentError("--compat must be RUBY1_8 or RUBY1_9")));
                        if (compatVersion == null) {
                            compatVersion = CompatVersion.RUBY1_8;
                        }
                        break FOR;
                    } else if (argument.equals("--copyright")) {
                        setShowCopyright(true);
                        shouldRunInterpreter = false;
                        break FOR;
                    } else if (argument.equals("--debug")) {
                        debug = true;
                        verbose = Boolean.TRUE;
                        break;
                    } else if (argument.equals("--help")) {
                        shouldPrintUsage = true;
                        shouldRunInterpreter = false;
                        break;
                    } else if (argument.equals("--properties")) {
                        shouldPrintProperties = true;
                        shouldRunInterpreter = false;
                        break;
                    } else if (argument.equals("--version")) {
                        setShowVersion(true);
                        break FOR;
                    } else {
                        if (argument.equals("--")) {
                            // ruby interpreter compatibilty 
                            // Usage: ruby [switches] [--] [programfile] [arguments])
                            endOfArguments = true;
                            break;
                        }
                    }
                default:
                    throw new MainExitException(1, "jruby: unknown option " + argument);
                }
            }
        }

        private void runBinScript() {
            requiredLibraries.add("jruby/commands");
            String scriptName = grabValue("jruby: provide a bin script to execute");
            if (scriptName.equals("irb")) {
                scriptName = "jirb";
            }
            inlineScript.append("JRuby::Commands." + scriptName);
            inlineScript.append("\n");
            hasInlineScript = true;
            String jrubyHome = JRubyFile.create(System.getProperty("user.dir"), JRubyFile.getFileProperty("jruby.home")).getAbsolutePath();
            scriptFileName = JRubyFile.create(jrubyHome + JRubyFile.separator + "bin", scriptName).getAbsolutePath();
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

            MainExitException mee = new MainExitException(1, errorMessage);
            mee.setUsageError(true);

            throw mee;
        }

        private String grabOptionalValue() {
            characterIndex++;
            if (characterIndex < arguments[argumentIndex].length()) {
                return arguments[argumentIndex].substring(characterIndex);
            }
            return null;
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
        return verbose == Boolean.TRUE;
    }

    public Boolean getVerbose() {
        return verbose;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    public boolean isShowCopyright() {
        return showCopyright;
    }

    protected void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }

    protected void setShowCopyright(boolean showCopyright) {
        this.showCopyright = showCopyright;
    }

    public boolean isShouldRunInterpreter() {
        return shouldRunInterpreter;
    }

    public boolean isShouldCheckSyntax() {
        return shouldCheckSyntax;
    }

    public boolean isYARVEnabled() {
        return yarv;
    }

    public String getInputFieldSeparator() {
        return inputFieldSeparator;
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
    
    public String getRecordSeparator() {
        return recordSeparator;
    }

    public int getSafeLevel() {
        return safeLevel;
    }
    
    public void setRecordSeparator(String recordSeparator) {
        this.recordSeparator = recordSeparator;
    }
    
    public ClassCache getClassCache() {
        return classCache;
    }
    
    public void setClassCache(ClassCache classCache) {
        this.classCache = classCache;
    }
}
