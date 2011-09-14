package org.jruby.util.cli;

import jnr.posix.util.Platform;
import org.jruby.CompatVersion;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.Constants;
import org.jruby.util.SafePropertyAccessor;

public class OutputStrings {
    public static String getBasicUsageHelp() {
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

    public static String getExtendedHelp() {
        StringBuilder sb = new StringBuilder();
        sb
                .append("Extended options:\n")
                .append("  -X-O        run with ObjectSpace disabled (default; improves performance)\n")
                .append("  -X+O        run with ObjectSpace enabled (reduces performance)\n")
                .append("  -X-C        disable all compilation\n")
                .append("  -X+C        force compilation of all scripts before they are run (except eval)\n");

        return sb.toString();
    }

    public static String getPropertyHelp() {
        StringBuilder sb = new StringBuilder();
        sb
                .append("These properties can be used to alter runtime behavior for perf or compatibility.\n")
                .append("Specify them by passing -X<property>=<value>\n")
                .append("  or if passing directly to Java, -Djruby.<property>=<value>\n")
                .append("\nCOMPILER SETTINGS:\n")
                .append("    compile.mode=JIT|FORCE|OFF\n")
                .append("       Set compilation mode. JIT is default; FORCE compiles all, OFF disables\n")
                .append("    compile.threadless=true|false\n")
                .append("       (EXPERIMENTAL) Turn on compilation without polling for \"unsafe\" thread events. Default is false\n")
                .append("    compile.dynopt=true|false\n")
                .append("       (EXPERIMENTAL) Use interpreter to help compiler make direct calls. Default is false\n")
                .append("    compile.fastops=true|false\n")
                .append("       Turn on fast operators for Fixnum and Float. Default is true\n")
                .append("    compile.chainsize=<line count>\n")
                .append("       Set the number of lines at which compiled bodies are \"chained\". Default is ").append(RubyInstanceConfig.CHAINED_COMPILE_LINE_COUNT_DEFAULT).append("\n")
                .append("    compile.lazyHandles=true|false\n")
                .append("       Generate method bindings (handles) for compiled methods lazily. Default is false.\n")
                .append("    compile.peephole=true|false\n")
                .append("       Enable or disable peephole optimizations. Default is true (on).\n")
                .append("\nJIT SETTINGS:\n")
                .append("    jit.threshold=<invocation count>\n")
                .append("       Set the JIT threshold to the specified method invocation count. Default is ").append(RubyInstanceConfig.JIT_THRESHOLD).append(".\n")
                .append("    jit.max=<method count>\n")
                .append("       Set the max count of active methods eligible for JIT-compilation.\n")
                .append("       Default is ").append(RubyInstanceConfig.JIT_MAX_METHODS_LIMIT).append(" per runtime. A value of 0 disables JIT, -1 disables max.\n")
                .append("    jit.maxsize=<jitted method size (full .class)>\n")
                .append("       Set the maximum full-class byte size allowed for jitted methods. Default is ").append(RubyInstanceConfig.JIT_MAX_SIZE_LIMIT).append(".\n")
                .append("    jit.logging=true|false\n")
                .append("       Enable JIT logging (reports successful compilation). Default is false\n")
                .append("    jit.logging.verbose=true|false\n")
                .append("       Enable verbose JIT logging (reports failed compilation). Default is false\n")
                .append("    jit.logEvery=<method count>\n")
                .append("       Log a message every n methods JIT compiled. Default is 0 (off).\n")
                .append("    jit.exclude=<ClsOrMod,ClsOrMod::method_name,-::method_name>\n")
                .append("       Exclude methods from JIT by class/module short name, c/m::method_name,\n")
                .append("       or -::method_name for anon/singleton classes/modules. Comma-delimited.\n")
                .append("    jit.cache=true|false\n")
                .append("       Cache jitted method in-memory bodies across runtimes and loads. Default is true.\n")
                .append("    jit.codeCache=<dir>\n")
                .append("       Save jitted methods to <dir> as they're compiled, for future runs.\n")
                .append("\nNATIVE SUPPORT:\n")
                .append("    native.enabled=true|false\n")
                .append("       Enable/disable native extensions (like JNA for non-Java APIs; Default is true\n")
                .append("       (This affects all JRuby instances in a given JVM)\n")
                .append("    native.verbose=true|false\n")
                .append("       Enable verbose logging of native extension loading. Default is false.\n")
                .append("\nTHREAD POOLING:\n")
                .append("    thread.pool.enabled=true|false\n")
                .append("       Enable reuse of native backing threads via a thread pool. Default is false.\n")
                .append("    thread.pool.min=<min thread count>\n")
                .append("       The minimum number of threads to keep alive in the pool. Default is 0.\n")
                .append("    thread.pool.max=<max thread count>\n")
                .append("       The maximum number of threads to allow in the pool. Default is unlimited.\n")
                .append("    thread.pool.ttl=<time to live, in seconds>\n")
                .append("       The maximum number of seconds to keep alive an idle thread. Default is 60.\n")
                .append("\nMISCELLANY:\n")
                .append("    compat.version=1.8|1.9\n")
                .append("       Specify the major Ruby version to be compatible with; Default is RUBY1_8\n")
                .append("    objectspace.enabled=true|false\n")
                .append("       Enable or disable ObjectSpace.each_object (default is disabled)\n")
                .append("    launch.inproc=true|false\n")
                .append("       Set in-process launching of e.g. system('ruby ...'). Default is true\n")
                .append("    bytecode.version=1.5|1.6\n")
                .append("       Set bytecode version for JRuby to generate. Default is current JVM version.\n")
                .append("    management.enabled=true|false\n")
                .append("       Set whether JMX management is enabled. Default is false.\n")
                .append("    jump.backtrace=true|false\n")
                .append("       Make non-local flow jumps generate backtraces. Default is false.\n")
                .append("    process.noUnwrap=true|false\n")
                .append("       Do not unwrap process streams (IBM Java 6 issue). Default is false.\n")
                .append("    reify.classes=true|false\n")
                .append("       Before instantiation, stand up a real Java class for ever Ruby class. Default is false. \n")
                .append("    reify.logErrors=true|false\n")
                .append("       Log errors during reification (reify.classes=true). Default is false. \n")
                .append("    reflected.handles=true|false\n")
                .append("       Use reflection for binding methods, not generated bytecode. Default is false.\n")
                .append("\nDEBUGGING/LOGGING:\n")
                .append("    debug.loadService=true|false\n")
                .append("       LoadService logging\n")
                .append("    debug.loadService.timing=true|false\n")
                .append("       Print load timings for each require'd library. Default is false.\n")
                .append("    debug.launch=true|false\n")
                .append("       ShellLauncher logging\n")
                .append("    debug.fullTrace=true|false\n")
                .append("       Set whether full traces are enabled (c-call/c-return). Default is false.\n")
                .append("    debug.scriptResolution=true|false\n")
                .append("       Print which script is executed by '-S' flag. Default is false.\n")
                .append("    errno.backtrace=true|false\n")
                .append("       Generate backtraces for heavily-used Errno exceptions (EAGAIN). Default is false.\n")
                .append("\nJAVA INTEGRATION:\n")
                .append("    ji.setAccessible=true|false\n")
                .append("       Try to set inaccessible Java methods to be accessible. Default is true.\n")
                .append("    interfaces.useProxy=true|false\n")
                .append("       Use java.lang.reflect.Proxy for interface impl. Default is false.\n");

        return sb.toString();
    }

    public static String getVersionString(CompatVersion compatVersion) {
        String ver = null;
        String patchDelimeter = "-p";
        int patchlevel = 0;
        switch (compatVersion) {
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

    public static String getCopyrightString() {
        return "JRuby - Copyright (C) 2001-2011 The JRuby Community (and contribs)";
    }
}
