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
package org.jruby.truffle.options;

import org.jruby.truffle.RubyLanguage;
import org.jruby.truffle.platform.Platform;

/**
 * Utility methods to generate the command-line output strings for help,
 * extended options, properties, version, and copyright strings.
 */
public class OutputStrings {
    public static String getBasicUsageHelp() {
        StringBuilder sb = new StringBuilder();
        sb
                .append("Usage: jruby [switches] [--] [programfile] [arguments]\n")
                .append("  -0[octal]         specify record separator (\\0, if no argument)\n")
                .append("  -a                autosplit mode with -n or -p (splits $_ into $F)\n")
                .append("  -c                check syntax only\n")
                .append("  -Cdirectory       cd to directory, before executing your script\n")
                .append("  -d                set debugging flags (set $DEBUG to true)\n")
                .append("  -e 'command'      one line of script. Several -e's allowed. Omit [programfile]\n")
                .append("  -Eex[:in]         specify the default external and internal character encodings\n")
                .append("  -Fpattern         split() pattern for autosplit (-a)\n")
                .append("  -G                load a Bundler Gemspec before executing any user code\n")
                .append("  -i[extension]     edit ARGV files in place (make backup if extension supplied)\n")
                .append("  -Idirectory       specify $LOAD_PATH directory (may be used more than once)\n")
                .append("  -J[java option]   pass an option on to the JVM (e.g. -J-Xmx512m)\n")
                .append("                      use --properties to list JRuby properties\n")
                .append("                      run 'java -help' for a list of other Java options\n")
                //.append("  -Kkcode           specifies code-set (e.g. -Ku for Unicode, -Ke for EUC and -Ks\n")
                //.append("                      for SJIS)\n")
                .append("  -l                enable line ending processing\n")
                .append("  -n                assume 'while gets(); ... end' loop around your script\n")
                .append("  -p                assume loop like -n but print line also like sed\n")
                .append("  -rlibrary         require the library, before executing your script\n")
                .append("  -s                enable some switch parsing for switches after script name\n")
                .append("  -S                look for the script in bin or using PATH environment variable\n")
                .append("  -T[level]         turn on tainting checks\n")
                .append("  -U                use UTF-8 as default internal encoding\n")
                .append("  -v                print version number, then turn on verbose mode\n")
                .append("  -w                turn warnings on for your script\n")
                .append("  -W[level]         set warning level; 0=silence, 1=medium, 2=verbose (default)\n")
                .append("  -x[directory]     strip off text before #!ruby line and perhaps cd to directory\n")
                .append("  -X[option]        enable extended option (omit option to list)\n")
                .append("  -y                enable parsing debug output\n")
                .append("  --copyright       print the copyright\n")
                .append("  --debug           sets the execution mode most suitable for debugger\n")
                .append("                      functionality\n")
                .append("  --jdb             runs JRuby process under JDB\n")
                .append("  --properties      List all configuration Java properties\n")
                .append("                      (prepend \"jruby.\" when passing directly to Java)\n")
                .append("  --sample          run with profiling using the JVM's sampling profiler\n")
                .append("  --profile         run with instrumented (timed) profiling, flat format\n")
                .append("  --profile.api     activate Ruby profiler API\n")
                .append("  --profile.flat    synonym for --profile\n")
                .append("  --profile.graph   run with instrumented (timed) profiling, graph format\n")
                .append("  --profile.html    run with instrumented (timed) profiling, graph format in HTML\n")
                .append("  --profile.json    run with instrumented (timed) profiling, graph format in JSON\n")
                .append("  --profile.out     [file]\n")
                .append("  --profile.service <ProfilingService implementation classname>\n")
                .append("                    output profile data to [file]\n")
                .append("  --client          use the non-optimizing \"client\" JVM\n")
                .append("                      (improves startup; default)\n")
                .append("  --server          use the optimizing \"server\" JVM (improves perf)\n")
                .append("  --headless        do not launch a GUI window, no matter what\n")
                .append("  --dev             prioritize startup time over long term performance\n")
                .append("  --manage          enable remote JMX management and monitoring of JVM and JRuby\n")
                .append("  --bytecode        show the JVM bytecode produced by compiling specified code\n")
                .append("  --version         print the version\n")
                .append("  --disable-gems    do not load RubyGems on startup\n")
                .append("  --enable=feature[,...], --disable=feature[,...]\n")
                .append("                    enable or disable features\n");

        return sb.toString();
    }

    public static String getVersionString() {
        return String.format(
            "jruby%s %s (%s) %s %s %s %s on %s%s%s [%s-%s]",
                "+truffle",
                RubyLanguage.VERSION,
                RubyLanguage.RUBY_VERSION,
                RubyLanguage.COMPILE_DATE,
                "unknown",
                System.getProperty("java.vm.name", "Unknown JVM"),
                System.getProperty("java.vm.version", "Unknown JVM version"),
                System.getProperty("java.runtime.version", System.getProperty("java.version", "Unknown version")),
                "",
                "",
                Platform.getOSName(),
                Platform.getArchitecture()
        );
    }

    public static String getCopyrightString() {
        return "JRuby - Copyright (C) 2001-2017 The JRuby Community (and contribs)";
    }
}
