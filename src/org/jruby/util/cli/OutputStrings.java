package org.jruby.util.cli;

import java.util.Arrays;
import jnr.posix.util.Platform;
import org.jruby.CompatVersion;
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
                .append("                    (prepend \"jruby.\" when passing directly to Java)\n")
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
                .append("  or put <property>=<value> in .jrubyrc\n");
        
        Options.Category category = null;
        for (Options.Option property : Options.PROPERTIES) {
            if (category != property.category) {
                category = property.category;
                sb.append('\n').append(category).append(" settings:\n\n");
            }
            sb.append("   ").append(property.name).append('=').append(Arrays.toString(property.options)).append('\n');
            sb.append("      ").append(property.description).append(" Default is ").append(property.defval).append(".\n");
        }

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
