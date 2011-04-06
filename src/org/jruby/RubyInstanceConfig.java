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
 * Copyright (C) 2007-2011 Nick Sieger <nicksieger@gmail.com>
 * Copyright (C) 2009 Joseph LaFata <joe@quibb.org>
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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jruby.ast.executable.Script;
import org.jruby.compiler.ASTCompiler;
import org.jruby.compiler.ASTCompiler19;
import org.jruby.exceptions.MainExitException;
import org.jruby.embed.util.SystemPropertyCatcher;
import org.jruby.ext.posix.util.Platform;
import org.jruby.runtime.Constants;
import org.jruby.runtime.backtrace.TraceType;
import org.jruby.runtime.profile.IProfileData;
import org.jruby.runtime.profile.AbstractProfilePrinter;
import org.jruby.runtime.profile.FlatProfilePrinter;
import org.jruby.runtime.profile.GraphProfilePrinter;
import org.jruby.runtime.load.LoadService;
import org.jruby.runtime.load.LoadService19;
import org.jruby.util.ClassCache;
import org.jruby.util.JRubyFile;
import org.jruby.util.KCode;
import org.jruby.util.NormalizedFile;
import org.jruby.util.SafePropertyAccessor;
import org.objectweb.asm.Opcodes;

public class RubyInstanceConfig {

    /**
     * The max count of active methods eligible for JIT-compilation.
     */
    public static final int JIT_MAX_METHODS_LIMIT = 4096;

    /**
     * The max size of JIT-compiled methods (full class size) allowed.
     */
    public static final int JIT_MAX_SIZE_LIMIT = 30000;

    /**
     * The JIT threshold to the specified method invocation count.
     */
    public static final int JIT_THRESHOLD = 50;
    
    /** The version to use for generated classes. Set to current JVM version by default */
    public static final int JAVA_VERSION;
    
    /**
     * Default size for chained compilation.
     */
    public static final int CHAINED_COMPILE_LINE_COUNT_DEFAULT = 500;
    
    /**
     * The number of lines at which a method, class, or block body is split into
     * chained methods (to dodge 64k method-size limit in JVM).
     */
    public static final int CHAINED_COMPILE_LINE_COUNT
            = SafePropertyAccessor.getInt("jruby.compile.chainsize", CHAINED_COMPILE_LINE_COUNT_DEFAULT);

    /**
     * Indicates whether the script must be extracted from script source
     */
    private boolean xFlag;

    public boolean hasShebangLine() {
        return hasShebangLine;
    }

    public void setHasShebangLine(boolean hasShebangLine) {
        this.hasShebangLine = hasShebangLine;
    }

    /**
     * Indicates whether the script has a shebang line or not
     */
    private boolean hasShebangLine;

    public boolean isxFlag() {
        return xFlag;
    }

    public enum CompileMode {
        JIT, FORCE, OFF, OFFIR;

        public boolean shouldPrecompileCLI() {
            switch (this) {
            case JIT: case FORCE:
                if (DYNOPT_COMPILE_ENABLED) {
                    // don't precompile the CLI script in dynopt mode
                    return false;
                }
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

    /** Environment variables; defaults to System.getenv() in constructor */
    private Map environment;
    private String[] argv = {};

    private final boolean jitLogging;
    private final boolean jitDumping;
    private final boolean jitLoggingVerbose;
    private int jitLogEvery;
    private int jitThreshold;
    private int jitMax;
    private int jitMaxSize;
    private final boolean samplingEnabled;
    private CompatVersion compatVersion;

    private String internalEncoding = null;
    private String externalEncoding = null;

    public enum ProfilingMode {
		OFF, API, FLAT, GRAPH
	}
		
    private ProfilingMode profilingMode = ProfilingMode.OFF;
    
    private ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
    private ClassLoader loader = contextLoader == null ? RubyInstanceConfig.class.getClassLoader() : contextLoader;

    private ClassCache<Script> classCache;

    // from CommandlineParser
    private List<String> loadPaths = new ArrayList<String>();
    private Set<String> excludedMethods = new HashSet<String>();
    private StringBuffer inlineScript = new StringBuffer();
    private boolean hasInlineScript = false;
    private String scriptFileName = null;
    private Collection<String> requiredLibraries = new LinkedHashSet<String>();
    private boolean benchmarking = false;
    private boolean argvGlobalsOn = false;
    private boolean assumeLoop = false;
    private boolean assumePrinting = false;
    private Map optionGlobals = new HashMap();
    private boolean processLineEnds = false;
    private boolean split = false;
    // This property is a Boolean, to allow three values, so it can match MRI's nil, false and true
    private Boolean verbose = Boolean.FALSE;
    private boolean debug = false;
    private boolean showVersion = false;
    private boolean showBytecode = false;
    private boolean showCopyright = false;
    private boolean endOfArguments = false;
    private boolean shouldRunInterpreter = true;
    private boolean shouldPrintUsage = false;
    private boolean shouldPrintProperties=false;
    private KCode kcode = KCode.NONE;
    private String recordSeparator = "\n";
    private boolean shouldCheckSyntax = false;
    private String inputFieldSeparator = null;
    private boolean managementEnabled = false;
    private String inPlaceBackupExtension = null;
    private boolean parserDebug = false;
    private String threadDumpSignal = null;
    private boolean hardExit = false;
    private boolean disableGems = false;

    private int safeLevel = 0;

    private String jrubyHome;
    
    private static volatile boolean loadedNativeExtensions = false;

    public static final boolean PEEPHOLE_OPTZ
            = SafePropertyAccessor.getBoolean("jruby.compile.peephole", true);
    public static boolean DYNOPT_COMPILE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.compile.dynopt", false);
    public static boolean NOGUARDS_COMPILE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.compile.noguards");
    public static boolean FASTEST_COMPILE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.compile.fastest");
    public static boolean FASTOPS_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED
            || SafePropertyAccessor.getBoolean("jruby.compile.fastops", true);
    public static boolean THREADLESS_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED
            || SafePropertyAccessor.getBoolean("jruby.compile.threadless");
    public static boolean FASTSEND_COMPILE_ENABLED
            = FASTEST_COMPILE_ENABLED
            || SafePropertyAccessor.getBoolean("jruby.compile.fastsend");
    public static boolean LAZYHANDLES_COMPILE = SafePropertyAccessor.getBoolean("jruby.compile.lazyHandles", false);
    public static boolean INLINE_DYNCALL_ENABLED
            = FASTEST_COMPILE_ENABLED
            || SafePropertyAccessor.getBoolean("jruby.compile.inlineDyncalls");
    public static final boolean POOLING_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.thread.pool.enabled");
    public static final int POOL_MAX
            = SafePropertyAccessor.getInt("jruby.thread.pool.max", Integer.MAX_VALUE);
    public static final int POOL_MIN
            = SafePropertyAccessor.getInt("jruby.thread.pool.min", 0);
    public static final int POOL_TTL
            = SafePropertyAccessor.getInt("jruby.thread.pool.ttl", 60);

    public static final boolean NATIVE_NET_PROTOCOL
            = SafePropertyAccessor.getBoolean("jruby.native.net.protocol", false);

    public static boolean FULL_TRACE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.debug.fullTrace", false);

    public static final String COMPILE_EXCLUDE
            = SafePropertyAccessor.getProperty("jruby.jit.exclude");
    public static boolean nativeEnabled = true;

    public static final boolean REIFY_RUBY_CLASSES
            = SafePropertyAccessor.getBoolean("jruby.reify.classes", false);

    public static final boolean REIFY_LOG_ERRORS
            = SafePropertyAccessor.getBoolean("jruby.reify.logErrors", false);

    public static final boolean USE_GENERATED_HANDLES
            = SafePropertyAccessor.getBoolean("jruby.java.handles", false);

    public static final boolean DEBUG_LOAD_SERVICE
            = SafePropertyAccessor.getBoolean("jruby.debug.loadService", false);

    public static final boolean DEBUG_LOAD_TIMINGS
            = SafePropertyAccessor.getBoolean("jruby.debug.loadService.timing", false);

    public static final boolean DEBUG_LAUNCHING
            = SafePropertyAccessor.getBoolean("jruby.debug.launch", false);

    public static final boolean DEBUG_SCRIPT_RESOLUTION
            = SafePropertyAccessor.getBoolean("jruby.debug.scriptResolution", false);

    public static final boolean JUMPS_HAVE_BACKTRACE
            = SafePropertyAccessor.getBoolean("jruby.jump.backtrace", false);

    public static final boolean JIT_CACHE_ENABLED
            = SafePropertyAccessor.getBoolean("jruby.jit.cache", true);

    public static final String JIT_CODE_CACHE
            = SafePropertyAccessor.getProperty("jruby.jit.codeCache", null);

    public static final boolean REFLECTED_HANDLES
            = SafePropertyAccessor.getBoolean("jruby.reflected.handles", false)
            || SafePropertyAccessor.getBoolean("jruby.reflection", false);

    public static final boolean NO_UNWRAP_PROCESS_STREAMS
            = SafePropertyAccessor.getBoolean("jruby.process.noUnwrap", false);

    public static final boolean INTERFACES_USE_PROXY
            = SafePropertyAccessor.getBoolean("jruby.interfaces.useProxy");

    public static final boolean JIT_LOADING_DEBUG = SafePropertyAccessor.getBoolean("jruby.jit.debug", false);

    public static final boolean CAN_SET_ACCESSIBLE = SafePropertyAccessor.getBoolean("jruby.ji.setAccessible", true);

    private TraceType traceType =
            TraceType.traceTypeFor(SafePropertyAccessor.getProperty("jruby.backtrace.style", "ruby_framed"));
    
    public static final boolean ERRNO_BACKTRACE
            = SafePropertyAccessor.getBoolean("jruby.errno.backtrace", false);

    public static interface LoadServiceCreator {
        LoadService create(Ruby runtime);

        LoadServiceCreator DEFAULT = new LoadServiceCreator() {
                public LoadService create(Ruby runtime) {
                    if (runtime.is1_9()) {
                        return new LoadService19(runtime);
                    }
                    return new LoadService(runtime);
                }
            };
    }

    private LoadServiceCreator creator = LoadServiceCreator.DEFAULT;


    static {
        String specVersion = null;
        try {
            specVersion = System.getProperty("jruby.bytecode.version");
            if (specVersion == null) {
                specVersion = System.getProperty("java.specification.version");
            }
            if (System.getProperty("jruby.native.enabled") != null) {
                nativeEnabled = Boolean.getBoolean("jruby.native.enabled");
            }
        } catch (SecurityException se) {
            nativeEnabled = false;
            specVersion = "1.5";
        }
        
        if (specVersion.equals("1.5")) {
            JAVA_VERSION = Opcodes.V1_5;
        } else if (specVersion.equals("1.6")) {
            JAVA_VERSION = Opcodes.V1_6;
        } else if (specVersion.equals("1.7")) {
            JAVA_VERSION = Opcodes.V1_7;
        } else {
            throw new RuntimeException("unsupported Java version: " + specVersion);
        }
    }

    public int characterIndex = 0;

    public RubyInstanceConfig(RubyInstanceConfig parentConfig) {
        setCurrentDirectory(parentConfig.getCurrentDirectory());
        samplingEnabled = parentConfig.samplingEnabled;
        compatVersion = parentConfig.compatVersion;
        compileMode = parentConfig.getCompileMode();
        jitLogging = parentConfig.jitLogging;
        jitDumping = parentConfig.jitDumping;
        jitLoggingVerbose = parentConfig.jitLoggingVerbose;
        jitLogEvery = parentConfig.jitLogEvery;
        jitThreshold = parentConfig.jitThreshold;
        jitMax = parentConfig.jitMax;
        jitMaxSize = parentConfig.jitMaxSize;
        managementEnabled = parentConfig.managementEnabled;
        runRubyInProcess = parentConfig.runRubyInProcess;
        excludedMethods = parentConfig.excludedMethods;
        threadDumpSignal = parentConfig.threadDumpSignal;
        
        classCache = new ClassCache<Script>(loader, jitMax);

        try {
            environment = System.getenv();
        } catch (SecurityException se) {
            environment = new HashMap();
        }
    }

    public RubyInstanceConfig() {
        setCurrentDirectory(Ruby.isSecurityRestricted() ? "/" : JRubyFile.getFileProperty("user.dir"));
        samplingEnabled = SafePropertyAccessor.getBoolean("jruby.sampling.enabled", false);

        String compatString = SafePropertyAccessor.getProperty("jruby.compat.version", "RUBY1_8");
        if (compatString.equalsIgnoreCase("RUBY1_8")) {
            setCompatVersion(CompatVersion.RUBY1_8);
        } else if (compatString.equalsIgnoreCase("RUBY1_9")) {
            setCompatVersion(CompatVersion.RUBY1_9);
        } else {
            error.println("Compatibility version `" + compatString + "' invalid; use RUBY1_8 or RUBY1_9. Using RUBY1_8.");
            setCompatVersion(CompatVersion.RUBY1_8);
        }

        if (Ruby.isSecurityRestricted()) {
            compileMode = CompileMode.OFF;
            jitLogging = false;
            jitDumping = false;
            jitLoggingVerbose = false;
            jitLogEvery = 0;
            jitThreshold = -1;
            jitMax = 0;
            jitMaxSize = -1;
            managementEnabled = false;
        } else {
            String threshold = SafePropertyAccessor.getProperty("jruby.jit.threshold");
            String max = SafePropertyAccessor.getProperty("jruby.jit.max");
            String maxSize = SafePropertyAccessor.getProperty("jruby.jit.maxsize");
            
            if (COMPILE_EXCLUDE != null) {
                String[] elements = COMPILE_EXCLUDE.split(",");
                excludedMethods.addAll(Arrays.asList(elements));
            }
            
            managementEnabled = SafePropertyAccessor.getBoolean("jruby.management.enabled", false);
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
            jitDumping = SafePropertyAccessor.getBoolean("jruby.jit.dumping");
            jitLoggingVerbose = SafePropertyAccessor.getBoolean("jruby.jit.logging.verbose");
            String logEvery = SafePropertyAccessor.getProperty("jruby.jit.logEvery");
            jitLogEvery = logEvery == null ? 0 : Integer.parseInt(logEvery);
            jitThreshold = threshold == null ?
                    JIT_THRESHOLD : Integer.parseInt(threshold);
            jitMax = max == null ?
                    JIT_MAX_METHODS_LIMIT : Integer.parseInt(max);
            jitMaxSize = maxSize == null ?
                    JIT_MAX_SIZE_LIMIT : Integer.parseInt(maxSize);
        }

        // default ClassCache using jitMax as a soft upper bound
        classCache = new ClassCache<Script>(loader, jitMax);
        threadDumpSignal = SafePropertyAccessor.getProperty("jruby.thread.dump.signal", "USR2");

        try {
            environment = System.getenv();
        } catch (SecurityException se) {
            environment = new HashMap();
        }
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

    public String getBasicUsageHelp() {
        StringBuilder sb = new StringBuilder();
        sb
                .append("Usage: jruby [switches] [--] [programfile] [arguments]\n")
                .append("  -0[octal]       specify record separator (\\0, if no argument)\n")
                .append("  -a              autosplit mode with -n or -p (splits $_ into $F)\n")
                .append("  -b              benchmark mode, times the script execution\n")
                .append("  -c              check syntax only\n")
                .append("  -Cdirectory     cd to directory, before executing your script\n")
                .append("  -d              set debugging flags (set $DEBUG to true)\n")
                .append("  -e 'command'    one line of script. Several -e's allowed. Omit [programfile]\n")
                .append("  -Eex[:in]       specify the default external and internal character encodings\n")
                .append("  -Fpattern       split() pattern for autosplit (-a)\n")
                .append("  -i[extension]   edit ARGV files in place (make backup if extension supplied)\n")
                .append("  -Idirectory     specify $LOAD_PATH directory (may be used more than once)\n")
                .append("  -J[java option] pass an option on to the JVM (e.g. -J-Xmx512m)\n")
                .append("                    use --properties to list JRuby properties\n")
                .append("                    run 'java -help' for a list of other Java options\n")
                .append("  -Kkcode         specifies code-set (e.g. -Ku for Unicode, -Ke for EUC and -Ks\n")
                .append("                    for SJIS)\n")
                .append("  -l              enable line ending processing\n")
                .append("  -n              assume 'while gets(); ... end' loop around your script\n")
                .append("  -p              assume loop like -n but print line also like sed\n")
                .append("  -rlibrary       require the library, before executing your script\n")
                .append("  -s              enable some switch parsing for switches after script name\n")
                .append("  -S              look for the script in bin or using PATH environment variable\n")
                .append("  -T[level]       turn on tainting checks\n")
                .append("  -U              use UTF-8 as default internal encoding\n")
                .append("  -v              print version number, then turn on verbose mode\n")
                .append("  -w              turn warnings on for your script\n")
                .append("  -W[level]       set warning level; 0=silence, 1=medium, 2=verbose (default)\n")
                .append("  -x[directory]   strip off text before #!ruby line and perhaps cd to directory\n")
                .append("  -X[option]      enable extended option (omit option to list)\n")
                .append("  -y              enable parsing debug output\n")
                .append("  --copyright     print the copyright\n")
                .append("  --debug         sets the execution mode most suitable for debugger\n")
                .append("                    functionality\n")
                .append("  --jdb           runs JRuby process under JDB\n")
                .append("  --properties    List all configuration Java properties\n")
                .append("                    (pass -X<property without \"jruby.\">=value to set them)\n")
                .append("  --sample        run with profiling using the JVM's sampling profiler\n")
                .append("  --profile       run with instrumented (timed) profiling, flat format\n")
                .append("  --profile.api   activate Ruby profiler API\n")
                .append("  --profile.flat  synonym for --profile\n")
                .append("  --profile.graph run with instrumented (timed) profiling, graph format\n")
                .append("  --client        use the non-optimizing \"client\" JVM\n")
                .append("                    (improves startup; default)\n")
                .append("  --server        use the optimizing \"server\" JVM (improves perf)\n")
                .append("  --manage        enable remote JMX management and monitoring of the VM\n")
                .append("                    and JRuby\n")
                .append("  --headless      do not launch a GUI window, no matter what\n")
                .append("  --1.8           specify Ruby 1.8.x compatibility (default)\n")
                .append("  --1.9           specify Ruby 1.9.x compatibility\n")
                .append("  --bytecode      show the JVM bytecode produced by compiling specified code\n")
                .append("  --version       print the version\n");

        return sb.toString();
    }

    public String getExtendedHelp() {
        StringBuilder sb = new StringBuilder();
        sb
                .append("Extended options:\n")
                .append("  -X-O        run with ObjectSpace disabled (default; improves performance)\n")
                .append("  -X+O        run with ObjectSpace enabled (reduces performance)\n")
                .append("  -X-C        disable all compilation\n")
                .append("  -X+C        force compilation of all scripts before they are run (except eval)\n");

        return sb.toString();
    }

    public String getPropertyHelp() {
        StringBuilder sb = new StringBuilder();
        sb
                .append("These properties can be used to alter runtime behavior for perf or compatibility.\n")
                .append("Specify them by passing -J-D<property>=<value>\n")
                .append("\nCOMPILER SETTINGS:\n")
                .append("    jruby.compile.mode=JIT|FORCE|OFF\n")
                .append("       Set compilation mode. JIT is default; FORCE compiles all, OFF disables\n")
                .append("    jruby.compile.threadless=true|false\n")
                .append("       (EXPERIMENTAL) Turn on compilation without polling for \"unsafe\" thread events. Default is false\n")
                .append("    jruby.compile.dynopt=true|false\n")
                .append("       (EXPERIMENTAL) Use interpreter to help compiler make direct calls. Default is false\n")
                .append("    jruby.compile.fastops=true|false\n")
                .append("       Turn on fast operators for Fixnum and Float. Default is true\n")
                .append("    jruby.compile.chainsize=<line count>\n")
                .append("       Set the number of lines at which compiled bodies are \"chained\". Default is ").append(CHAINED_COMPILE_LINE_COUNT_DEFAULT).append("\n")
                .append("    jruby.compile.lazyHandles=true|false\n")
                .append("       Generate method bindings (handles) for compiled methods lazily. Default is false.\n")
                .append("    jruby.compile.peephole=true|false\n")
                .append("       Enable or disable peephole optimizations. Default is true (on).\n")
                .append("\nJIT SETTINGS:\n")
                .append("    jruby.jit.threshold=<invocation count>\n")
                .append("       Set the JIT threshold to the specified method invocation count. Default is ").append(JIT_THRESHOLD).append(".\n")
                .append("    jruby.jit.max=<method count>\n")
                .append("       Set the max count of active methods eligible for JIT-compilation.\n")
                .append("       Default is ").append(JIT_MAX_METHODS_LIMIT).append(" per runtime. A value of 0 disables JIT, -1 disables max.\n")
                .append("    jruby.jit.maxsize=<jitted method size (full .class)>\n")
                .append("       Set the maximum full-class byte size allowed for jitted methods. Default is ").append(JIT_MAX_SIZE_LIMIT).append(".\n")
                .append("    jruby.jit.logging=true|false\n")
                .append("       Enable JIT logging (reports successful compilation). Default is false\n")
                .append("    jruby.jit.logging.verbose=true|false\n")
                .append("       Enable verbose JIT logging (reports failed compilation). Default is false\n")
                .append("    jruby.jit.logEvery=<method count>\n")
                .append("       Log a message every n methods JIT compiled. Default is 0 (off).\n")
                .append("    jruby.jit.exclude=<ClsOrMod,ClsOrMod::method_name,-::method_name>\n")
                .append("       Exclude methods from JIT by class/module short name, c/m::method_name,\n")
                .append("       or -::method_name for anon/singleton classes/modules. Comma-delimited.\n")
                .append("    jruby.jit.cache=true|false\n")
                .append("       Cache jitted method in-memory bodies across runtimes and loads. Default is true.\n")
                .append("    jruby.jit.codeCache=<dir>\n")
                .append("       Save jitted methods to <dir> as they're compiled, for future runs.\n")
                .append("\nNATIVE SUPPORT:\n")
                .append("    jruby.native.enabled=true|false\n")
                .append("       Enable/disable native extensions (like JNA for non-Java APIs; Default is true\n")
                .append("       (This affects all JRuby instances in a given JVM)\n")
                .append("    jruby.native.verbose=true|false\n")
                .append("       Enable verbose logging of native extension loading. Default is false.\n")
                .append("\nTHREAD POOLING:\n")
                .append("    jruby.thread.pool.enabled=true|false\n")
                .append("       Enable reuse of native backing threads via a thread pool. Default is false.\n")
                .append("    jruby.thread.pool.min=<min thread count>\n")
                .append("       The minimum number of threads to keep alive in the pool. Default is 0.\n")
                .append("    jruby.thread.pool.max=<max thread count>\n")
                .append("       The maximum number of threads to allow in the pool. Default is unlimited.\n")
                .append("    jruby.thread.pool.ttl=<time to live, in seconds>\n")
                .append("       The maximum number of seconds to keep alive an idle thread. Default is 60.\n")
                .append("\nMISCELLANY:\n")
                .append("    jruby.compat.version=RUBY1_8|RUBY1_9\n")
                .append("       Specify the major Ruby version to be compatible with; Default is RUBY1_8\n")
                .append("    jruby.objectspace.enabled=true|false\n")
                .append("       Enable or disable ObjectSpace.each_object (default is disabled)\n")
                .append("    jruby.launch.inproc=true|false\n")
                .append("       Set in-process launching of e.g. system('ruby ...'). Default is true\n")
                .append("    jruby.bytecode.version=1.5|1.6\n")
                .append("       Set bytecode version for JRuby to generate. Default is current JVM version.\n")
                .append("    jruby.management.enabled=true|false\n")
                .append("       Set whether JMX management is enabled. Default is false.\n")
                .append("    jruby.jump.backtrace=true|false\n")
                .append("       Make non-local flow jumps generate backtraces. Default is false.\n")
                .append("    jruby.process.noUnwrap=true|false\n")
                .append("       Do not unwrap process streams (IBM Java 6 issue). Default is false.\n")
                .append("    jruby.reify.classes=true|false\n")
                .append("       Before instantiation, stand up a real Java class for ever Ruby class. Default is false. \n")
                .append("    jruby.reify.logErrors=true|false\n")
                .append("       Log errors during reification (reify.classes=true). Default is false. \n")
                .append("\nDEBUGGING/LOGGING:\n")
                .append("    jruby.debug.loadService=true|false\n")
                .append("       LoadService logging\n")
                .append("    jruby.debug.loadService.timing=true|false\n")
                .append("       Print load timings for each require'd library. Default is false.\n")
                .append("    jruby.debug.launch=true|false\n")
                .append("       ShellLauncher logging\n")
                .append("    jruby.debug.fullTrace=true|false\n")
                .append("       Set whether full traces are enabled (c-call/c-return). Default is false.\n")
                .append("    jruby.debug.scriptResolution=true|false\n")
                .append("       Print which script is executed by '-S' flag. Default is false.\n")
                .append("    jruby.reflected.handles=true|false\n")
                .append("       Use reflection for binding methods, not generated bytecode. Default is false.\n")
                .append("    jruby.errno.backtrace=true|false\n")
                .append("       Generate backtraces for heavily-used Errno exceptions (EAGAIN). Default is false.\n")
                .append("\nJAVA INTEGRATION:\n")
                .append("    jruby.ji.setAccessible=true|false\n")
                .append("       Try to set inaccessible Java methods to be accessible. Default is true.\n")
                .append("    jruby.interfaces.useProxy=true|false\n")
                .append("       Use java.lang.reflect.Proxy for interface impl. Default is false.\n");

        return sb.toString();
    }

    public String getVersionString() {
        String ver = null;
        String patchDelimeter = "-p";
        int patchlevel = 0;
        switch (getCompatVersion()) {
        case RUBY1_8:
            ver = Constants.RUBY_VERSION;
            patchlevel = Constants.RUBY_PATCHLEVEL;
            break;
        case RUBY1_9:
            ver = Constants.RUBY1_9_VERSION;
            patchlevel = Constants.RUBY1_9_PATCHLEVEL;
            break;
        }

        String fullVersion = String.format(
                "jruby %s (ruby-%s%s%d) (%s %s) (%s %s) [%s-%s-java]",
                Constants.VERSION, ver, patchDelimeter, patchlevel,
                Constants.COMPILE_DATE, Constants.REVISION,
                System.getProperty("java.vm.name"), System.getProperty("java.version"),
                Platform.getOSName(),
                SafePropertyAccessor.getProperty("os.arch", "unknown")
                );

        return fullVersion;
    }

    public String getCopyrightString() {
        return "JRuby - Copyright (C) 2001-2011 The JRuby Community (and contribs)";
    }

    public void processArguments(String[] arguments) {
        new ArgumentProcessor(arguments).processArguments();
        tryProcessArgumentsWithRubyopts();
    }

    public void tryProcessArgumentsWithRubyopts() {
        try {
            // environment defaults to System.getenv normally
            Object rubyoptObj = environment.get("RUBYOPT");
            String rubyopt = rubyoptObj == null ? null : rubyoptObj.toString();
            
            if (rubyopt == null || "".equals(rubyopt)) return;

            if (rubyopt.split("\\s").length != 0) {
                String[] rubyoptArgs = rubyopt.split("\\s+");
                endOfArguments = false;
                new ArgumentProcessor(rubyoptArgs, false, true).processArguments();
            }
        } catch (SecurityException se) {
            // ignore and do nothing
        }
    }

    /**
     * The intent here is to gather up any options that might have
     * been specified in the shebang line and return them so they can
     * be merged into the ones specified on the commandline.  This is
     * kind of a hopeless task because it's impossible to figure out
     * where the command invocation stops and the parameters start.
     * We try to work with the common scenarios where /usr/bin/env is
     * used to invoke the jruby shell script, and skip any parameters
     * it might have.  Then we look for the interpreter invokation and
     * assume that the binary will have the word "ruby" in the name.
     * This is error prone but should cover more cases than the
     * previous code.
     */
    public String[] parseShebangOptions(InputStream in) {
        BufferedReader reader = null;
        String[] result = new String[0];
        if (in == null) return result;
        try {
            in.mark(1024);
            reader = new BufferedReader(new InputStreamReader(in, "iso-8859-1"), 8192);
            String firstLine = reader.readLine();

            // Search for the shebang line in the given stream
            // if it wasn't found on the first line and the -x option
            // was specified
            if (isxFlag()) {
                while (firstLine != null && !isShebangLine(firstLine)) {
                    firstLine = reader.readLine();
                }
            }

            boolean usesEnv = false;
            if (firstLine.length() > 2 && firstLine.charAt(0) == '#' && firstLine.charAt(1) == '!') {
                String[] options = firstLine.substring(2).split("\\s+");
                int i;
                for (i = 0; i < options.length; i++) {
                    // Skip /usr/bin/env if it's first
                    if (i == 0 && options[i].endsWith("/env")) {
                        usesEnv = true;
                        continue;
                    }
                    // Skip any assignments if /usr/bin/env is in play
                    if (usesEnv && options[i].indexOf('=') > 0) {
                        continue;
                    }
                    // Skip any commandline args if /usr/bin/env is in play
                    if (usesEnv && options[i].startsWith("-")) {
                        continue;
                    }
                    String basename = (new File(options[i])).getName();
                    if (basename.indexOf("ruby") > 0) {
                        break;
                    }
                }
                setHasShebangLine(true);
                System.arraycopy(options, i, result, 0, options.length - i);
            } else {
                // No shebang line found
                setHasShebangLine(false);
            }
        } catch (Exception ex) {
            // ignore error
        } finally {
            try {
                in.reset();
            } catch (IOException ex) {}
        }
        return result;
    }

    protected static boolean isShebangLine(String line) {
        return (line.length() > 2 && line.charAt(0) == '#' && line.charAt(1) == '!');
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

    public boolean isJitDumping() {
        return jitDumping;
    }

    public boolean isJitLoggingVerbose() {
        return jitLoggingVerbose;
    }

    public int getJitLogEvery() {
        return jitLogEvery;
    }

    public void setJitLogEvery(int jitLogEvery) {
        this.jitLogEvery = jitLogEvery;
    }

    public boolean isSamplingEnabled() {
        return samplingEnabled;
    }

    public int getJitThreshold() {
        return jitThreshold;
    }

    public void setJitThreshold(int jitThreshold) {
        this.jitThreshold = jitThreshold;
    }

    public int getJitMax() {
        return jitMax;
    }

    public void setJitMax(int jitMax) {
        this.jitMax = jitMax;
    }

    public int getJitMaxSize() {
        return jitMaxSize;
    }

    public void setJitMaxSize(int jitMaxSize) {
        this.jitMaxSize = jitMaxSize;
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

    public void setCompatVersion(CompatVersion compatVersion) {
        if (compatVersion == null) compatVersion = CompatVersion.RUBY1_8;

        this.compatVersion = compatVersion;
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
        if (newEnvironment == null) newEnvironment = new HashMap();
        environment = newEnvironment;
    }

    public Map getEnvironment() {
        return environment;
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public void setLoader(ClassLoader loader) {
        // Setting the loader needs to reset the class cache
        if(this.loader != loader) {
            this.classCache = new ClassCache<Script>(loader, this.classCache.getMax());
        }
        this.loader = loader;
    }

    public String[] getArgv() {
        return argv;
    }

    public void setArgv(String[] argv) {
        this.argv = argv;
    }

    public String getJRubyHome() {
        if (jrubyHome == null) {
            // try the normal property first
            if (!Ruby.isSecurityRestricted()) {
                jrubyHome = SafePropertyAccessor.getProperty("jruby.home");
            }

            if (jrubyHome != null) {
                // verify it if it's there
                jrubyHome = verifyHome(jrubyHome);
            } else {
                try {
                    jrubyHome = SystemPropertyCatcher.findFromJar(this);
                } catch (Exception e) {}

                if (jrubyHome != null) {
                    // verify it if it's there
                    jrubyHome = verifyHome(jrubyHome);
                } else {
                    // otherwise fall back on system temp location
                    jrubyHome = SafePropertyAccessor.getProperty("java.io.tmpdir");
                }
            }
        }
        return jrubyHome;
    }

    public void setJRubyHome(String home) {
        jrubyHome = verifyHome(home);
    }

    // We require the home directory to be absolute
    private String verifyHome(String home) {
        if (home.equals(".")) {
            home = SafePropertyAccessor.getProperty("user.dir");
        }
        if (home.startsWith("cp:")) {
            home = home.substring(3);
        } else if (!home.startsWith("file:") && !home.startsWith("classpath:")) {
            NormalizedFile f = new NormalizedFile(home);
            if (!f.isAbsolute()) {
                home = f.getAbsolutePath();
            }
            if (!f.exists()) {
                error.println("Warning: JRuby home \"" + f + "\" does not exist, using " + SafePropertyAccessor.getProperty("java.io.tmpdir"));
                return System.getProperty("java.io.tmpdir");
            }
        }
        return home;
    }

    private final class Argument {
        public final String originalValue;
        public final String dashedValue;
        public Argument(String value, boolean dashed) {
            this.originalValue = value;
            this.dashedValue = dashed && !value.startsWith("-") ? "-" + value : value;
        }
    }

    private class ArgumentProcessor {
        private List<Argument> arguments;
        private int argumentIndex = 0;
        private boolean processArgv;

        public ArgumentProcessor(String[] arguments) {
            this(arguments, true, false);
        }

        public ArgumentProcessor(String[] arguments, boolean processArgv, boolean dashed) {
            this.arguments = new ArrayList<Argument>();
            if (arguments != null && arguments.length > 0) {
                for (String argument : arguments) {
                    this.arguments.add(new Argument(argument, dashed));
                }
            }
            this.processArgv = processArgv;
        }

        public void processArguments() {
            processArguments(true);
        }

        public void processArguments(boolean inline) {
            while (argumentIndex < arguments.size() && isInterpreterArgument(arguments.get(argumentIndex).originalValue)) {
                processArgument();
                argumentIndex++;
            }

            if (inline && !hasInlineScript && scriptFileName == null) {
                if (argumentIndex < arguments.size()) {
                    setScriptFileName(arguments.get(argumentIndex).originalValue); //consume the file name
                    argumentIndex++;
                }
            }

            if (processArgv) processArgv();
        }

        private void processArgv() {
            List<String> arglist = new ArrayList<String>();
            for (; argumentIndex < arguments.size(); argumentIndex++) {
                String arg = arguments.get(argumentIndex).originalValue;
                if (argvGlobalsOn && arg.startsWith("-")) {
                    arg = arg.substring(1);
                    if (arg.indexOf('=') > 0) {
                        String[] keyvalue = arg.split("=", 2);
                        optionGlobals.put(keyvalue[0], keyvalue[1]);
                    } else {
                        optionGlobals.put(arg, null);
                    }
                } else {
                    argvGlobalsOn = false;
                    arglist.add(arg);
                }
            }

            // Remaining arguments are for the script itself
            arglist.addAll(Arrays.asList(argv));
            argv = arglist.toArray(new String[arglist.size()]);
        }

        private boolean isInterpreterArgument(String argument) {
            return argument.length() > 0 && (argument.charAt(0) == '-' || argument.charAt(0) == '+') && !endOfArguments;
        }

        private String getArgumentError(String additionalError) {
            return "jruby: invalid argument\n" + additionalError + "\n";
        }

        private void processArgument() {
            String argument = arguments.get(argumentIndex).dashedValue;
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
                            throw mee;
                        }
                    } catch (IOException e) {
                        MainExitException mee = new MainExitException(1, getArgumentError(" -C must be followed by a valid directory"));
                        throw mee;
                    }
                    break FOR;
                case 'd':
                    debug = true;
                    verbose = Boolean.TRUE;
                    break;
                case 'e':
                    inlineScript.append(grabValue(getArgumentError(" -e must be followed by an expression to evaluate")));
                    inlineScript.append('\n');
                    hasInlineScript = true;
                    break FOR;
                case 'E':
                    processEncodingOption(grabValue(getArgumentError("unknown encoding name")));
                    break FOR;
                case 'F':
                    inputFieldSeparator = grabValue(getArgumentError(" -F must be followed by a pattern for input field separation"));
                    break FOR;
                case 'h':
                    shouldPrintUsage = true;
                    shouldRunInterpreter = false;
                    break;
                case 'i' :
                    inPlaceBackupExtension = grabOptionalValue();
                    if(inPlaceBackupExtension == null) inPlaceBackupExtension = "";
                    break FOR;
                case 'I':
                    String s = grabValue(getArgumentError("-I must be followed by a directory name to add to lib path"));
                    String[] ls = s.split(java.io.File.pathSeparator);
                    loadPaths.addAll(Arrays.asList(ls));
                    break FOR;
                case 'J':
                    grabOptionalValue();
                    error.println("warning: "+argument+" argument ignored (launched in same VM?)");
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
                case 's' :
                    argvGlobalsOn = true;
                    break;
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
                case 'U':
                    internalEncoding = "UTF-8";
                    break;
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
               case 'x':
                   try {
                       String saved = grabOptionalValue();
                       if (saved != null) {
                           File base = new File(currentDirectory);
                           File newDir = new File(saved);
                           if (newDir.isAbsolute()) {
                               currentDirectory = newDir.getCanonicalPath();
                           } else {
                               currentDirectory = new File(base, newDir.getPath()).getCanonicalPath();
                           }
                           if (!(new File(currentDirectory).isDirectory())) {
                               MainExitException mee = new MainExitException(1, "jruby: Can't chdir to " + saved + " (fatal)");
                               throw mee;
                           }
                       }
                       xFlag = true;
                   } catch (IOException e) {
                       MainExitException mee = new MainExitException(1, getArgumentError(" -x must be followed by a valid directory"));
                       throw mee;
                   }
                   break FOR;
                case 'X':
                    String extendedOption = grabOptionalValue();

                    if (extendedOption == null) {
                        if (SafePropertyAccessor.getBoolean("jruby.launcher.nopreamble", false)) {
                            throw new MainExitException(0, getExtendedHelp());
                        } else {
                            throw new MainExitException(0, "jruby: missing argument\n" + getExtendedHelp());
                        }
                    } else if (extendedOption.equals("-O")) {
                        objectSpaceEnabled = false;
                    } else if (extendedOption.equals("+O")) {
                        objectSpaceEnabled = true;
                    } else if (extendedOption.equals("-C")) {
                        compileMode = CompileMode.OFF;
                    } else if (extendedOption.equals("-CIR")) {
                        compileMode = CompileMode.OFFIR;
                    } else if (extendedOption.equals("+C")) {
                        compileMode = CompileMode.FORCE;
                    } else {
                        MainExitException mee =
                                new MainExitException(1, "jruby: invalid extended option " + extendedOption + " (-X will list valid options)\n");
                        mee.setUsageError(true);

                        throw mee;
                    }
                    break FOR;
                case 'y':
                    parserDebug = true;
                    break FOR;
                case '-':
                    if (argument.equals("--command") || argument.equals("--bin")) {
                        characterIndex = argument.length();
                        runBinScript();
                        break;
                    } else if (argument.equals("--compat")) {
                        characterIndex = argument.length();
                        setCompatVersion(CompatVersion.getVersionFromString(grabValue(getArgumentError("--compat must be RUBY1_8 or RUBY1_9"))));
                        break FOR;
                    } else if (argument.equals("--copyright")) {
                        setShowCopyright(true);
                        shouldRunInterpreter = false;
                        break FOR;
                    } else if (argument.equals("--debug")) {
                        FULL_TRACE_ENABLED = true;
                        compileMode = CompileMode.OFF;
                        break FOR;
                    } else if (argument.equals("--jdb")) {
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
                        shouldRunInterpreter = false;
                        break FOR;
                    } else if (argument.equals("--bytecode")) {
                        setShowBytecode(true);
                        break FOR;
                    } else if (argument.equals("--fast")) {
                        compileMode = CompileMode.FORCE;
                        FASTOPS_COMPILE_ENABLED = true;
                        FASTSEND_COMPILE_ENABLED = true;
                        INLINE_DYNCALL_ENABLED = true;
                        break FOR;
                    } else if (argument.equals("--profile.api")) {
                        profilingMode = ProfilingMode.API;
                        break FOR;
                    } else if (argument.equals("--profile") ||
                            argument.equals("--profile.flat")) {
                        profilingMode = ProfilingMode.FLAT;
                        break FOR;
                    } else if (argument.equals("--profile.graph")) {
                        profilingMode = ProfilingMode.GRAPH;
                        break FOR;
                    } else if (argument.equals("--1.9")) {
                        setCompatVersion(CompatVersion.RUBY1_9);
                        break FOR;
                    } else if (argument.equals("--1.8")) {
                        setCompatVersion(CompatVersion.RUBY1_8);
                        break FOR;
                    } else if (argument.equals("--disable-gems")) {
                        disableGems = true;
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

        private void processEncodingOption(String value) {
            String[] encodings = value.split(":", 3);

            switch(encodings.length) {
                case 3:
                    throw new MainExitException(1, "extra argument for -E: " + encodings[2]);
                case 2:
                    internalEncoding = encodings[1];
                case 1:
                    externalEncoding = encodings[0];
                // Zero is impossible
            }
        }

        private void runBinScript() {
            String scriptName = grabValue("jruby: provide a bin script to execute");
            if (scriptName.equals("irb")) {
                scriptName = "jirb";
            }

            scriptFileName = resolveScript(scriptName);

            // run as a command if we couldn't find a script
            if (scriptFileName == null) {
                scriptFileName = scriptName;
                requiredLibraries.add("jruby/commands");
                inlineScript.append("JRuby::Commands.").append(scriptName);
                inlineScript.append("\n");
                hasInlineScript = true;
            }

            endOfArguments = true;
        }

        private String resolveScript(String scriptName) {
            // These try/catches are to allow failing over to the "commands" logic
            // when running from within a jruby-complete jar file, which has
            // jruby.home = a jar file URL that does not resolve correctly with
            // JRubyFile.create.
            File fullName = null;
            try {
                // try cwd first
                fullName = JRubyFile.create(currentDirectory, scriptName);
                if (fullName.exists() && fullName.isFile()) {
                    if (DEBUG_SCRIPT_RESOLUTION) {
                        error.println("Found: " + fullName.getAbsolutePath());
                    }
                    return scriptName;
                }
            } catch (Exception e) {
                // keep going, try bin/#{scriptName}
            }

            try {
                fullName = JRubyFile.create(getJRubyHome(), "bin/" + scriptName);
                if (fullName.exists() && fullName.isFile()) {
                    if (DEBUG_SCRIPT_RESOLUTION) {
                        error.println("Found: " + fullName.getAbsolutePath());
                    }
                    return fullName.getAbsolutePath();
                }
            } catch (Exception e) {
                // keep going, try PATH
            }

            try {
                String path = System.getenv("PATH");
                if (path != null) {
                    String[] paths = path.split(System.getProperty("path.separator"));
                    for (int i = 0; i < paths.length; i++) {
                        fullName = JRubyFile.create(paths[i], scriptName);
                        if (fullName.exists() && fullName.isFile()) {
                            if (DEBUG_SCRIPT_RESOLUTION) {
                                error.println("Found: " + fullName.getAbsolutePath());
                            }
                            return fullName.getAbsolutePath();
                        }
                    }
                }
            } catch (Exception e) {
                // will fall back to JRuby::Commands
            }

            if (debug) {
                error.println("warning: could not resolve -S script on filesystem: " + scriptName);
            }
            return null;
        }

        private String grabValue(String errorMessage) {
            String optValue = grabOptionalValue();
            if (optValue != null) {
                return optValue;
            }
            argumentIndex++;
            if (argumentIndex < arguments.size()) {
                return arguments.get(argumentIndex).originalValue;
            }

            MainExitException mee = new MainExitException(1, errorMessage);
            mee.setUsageError(true);

            throw mee;
        }

        private String grabOptionalValue() {
            characterIndex++;
            String argValue = arguments.get(argumentIndex).originalValue;
            if (characterIndex < argValue.length()) {
                return argValue.substring(characterIndex);
            }
            return null;
        }
    }

    public byte[] inlineScript() {
        return inlineScript.toString().getBytes();
    }

    public Collection<String> requiredLibraries() {
        return requiredLibraries;
    }

    public List<String> loadPaths() {
        return loadPaths;
    }

    public void setLoadPaths(List<String> loadPaths) {
        this.loadPaths = loadPaths;
    }

    public boolean shouldRunInterpreter() {
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
                String script = getScriptFileName();
                InputStream stream = null;
                if (script.startsWith("file:") && script.indexOf(".jar!/") != -1) {
                    stream = new URL("jar:" + script).openStream();
                } else if (script.startsWith("classpath:")) {
                    stream = Ruby.getClassLoader().getResourceAsStream(script.substring("classpath:".length()));
                } else {
                    File file = JRubyFile.create(getCurrentDirectory(), getScriptFileName());
                    if (isxFlag()) {
                        // search for a shebang line and
                        // return the script between shebang and __END__ or CTRL-Z (0x1A)
                        return findScript(file);
                    }
                    stream = new FileInputStream(file);
                }

                return new BufferedInputStream(stream, 8192);
            }
        } catch (IOException e) {
            // We haven't found any file directly on the file system,
            // now check for files inside the JARs.
            InputStream is = getJarScriptSource();
            if (is != null) {
                return new BufferedInputStream(is, 8129);
            }
            throw new MainExitException(1, "Error opening script file: " + e.getMessage());
        }
    }

    private InputStream findScript(File file) throws IOException {
        StringBuffer buf = new StringBuffer();
        BufferedReader br = new BufferedReader(new FileReader(file));
        String currentLine = br.readLine();
        while (currentLine != null && !(currentLine.length() > 2 && currentLine.charAt(0) == '#' && currentLine.charAt(1) == '!')) {
            currentLine = br.readLine();
        }

        buf.append(currentLine);
        buf.append("\n");

        do {
            currentLine = br.readLine();
            if (currentLine != null) {
            buf.append(currentLine);
            buf.append("\n");
            }
        } while (!(currentLine == null || currentLine.contains("__END__") || currentLine.contains("\026")));
        return new BufferedInputStream(new ByteArrayInputStream(buf.toString().getBytes()), 8192);
    }

    private InputStream getJarScriptSource() {
        String name = getScriptFileName();
        boolean looksLikeJarURL = name.startsWith("file:") && name.indexOf("!/") != -1;
        if (!looksLikeJarURL) {
            return null;
        }

        String before = name.substring("file:".length(), name.indexOf("!/"));
        String after =  name.substring(name.indexOf("!/") + 2);

        try {
            JarFile jFile = new JarFile(before);
            JarEntry entry = jFile.getJarEntry(after);

            if (entry != null && !entry.isDirectory()) {
                return jFile.getInputStream(entry);
            }
        } catch (IOException ignored) {
        }
        return null;
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

    public void setScriptFileName(String scriptFileName) {
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

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isParserDebug() {
        return parserDebug;
    }

    public boolean isShowVersion() {
        return showVersion;
    }
    
    public boolean isShowBytecode() {
        return showBytecode;
    }

    public boolean isShowCopyright() {
        return showCopyright;
    }

    protected void setShowVersion(boolean showVersion) {
        this.showVersion = showVersion;
    }
    
    protected void setShowBytecode(boolean showBytecode) {
        this.showBytecode = showBytecode;
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

    public String getInputFieldSeparator() {
        return inputFieldSeparator;
    }

    public KCode getKCode() {
        return kcode;
    }

    public void setKCode(KCode kcode) {
        this.kcode = kcode;
    }

    public String getInternalEncoding() {
        return internalEncoding;
    }

    public String getExternalEncoding() {
        return externalEncoding;
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

    public String getInPlaceBackupExtention() {
        return inPlaceBackupExtension;
    }

    public void setClassCache(ClassCache classCache) {
        this.classCache = classCache;
    }

    public Map getOptionGlobals() {
        return optionGlobals;
    }
    
    public boolean isManagementEnabled() {
        return managementEnabled;
    }
    
    public Set getExcludedMethods() {
        return excludedMethods;
    }

    public ASTCompiler newCompiler() {
        if (getCompatVersion() == CompatVersion.RUBY1_8) {
            return new ASTCompiler();
        } else {
            return new ASTCompiler19();
        }
    }

    public String getThreadDumpSignal() {
        return threadDumpSignal;
    }

    public boolean isHardExit() {
        return hardExit;
    }

    public void setHardExit(boolean hardExit) {
        this.hardExit = hardExit;
    }

    public boolean isProfiling() {
        return profilingMode != ProfilingMode.OFF;
    }
    
    public boolean isProfilingEntireRun() {
        return profilingMode != ProfilingMode.OFF && profilingMode != ProfilingMode.API;
    }

    public ProfilingMode getProfilingMode() {
        return profilingMode;
    }
    
    public AbstractProfilePrinter makeDefaultProfilePrinter(IProfileData profileData) {
        if (profilingMode == ProfilingMode.FLAT) {
            return new FlatProfilePrinter(profileData.getResults());
        }
        else if (profilingMode == ProfilingMode.GRAPH) {
            return new GraphProfilePrinter(profileData.getResults());
        }
        return null;
    }

    public boolean isDisableGems() {
        return disableGems;
    }

    public void setDisableGems(boolean dg) {
        this.disableGems = dg;
    }

    public TraceType getTraceType() {
        return traceType;
    }

    public void setTraceType(TraceType traceType) {
        this.traceType = traceType;
    }
    
    public static boolean hasLoadedNativeExtensions() {
        return loadedNativeExtensions;
    }
    
    public static void setLoadedNativeExtensions(boolean loadedNativeExtensions) {
        RubyInstanceConfig.loadedNativeExtensions = loadedNativeExtensions;
    }
}
