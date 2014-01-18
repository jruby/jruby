/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2001-2011 The JRuby Community (and contribs)
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
package org.jruby.util.cli;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jruby.runtime.Constants;
import org.jruby.util.KCode;
import org.jruby.util.SafePropertyAccessor;
import static org.jruby.util.cli.Category.*;
import static org.jruby.RubyInstanceConfig.Verbosity;
import static org.jruby.RubyInstanceConfig.ProfilingMode;

/**
 * Options defines all configuration settings for JRuby in a consistent form.
 * Loading of individual settings, printing documentation for settings and their
 * options and defaults, and categorizing properties by function are all part
 * of the built-in structure.
 */
public class Options {
    private static final List<Option> _loadedOptions = new ArrayList<Option>();
    private static final boolean INVOKEDYNAMIC_DEFAULT = calculateInvokedynamicDefault();
    
    // This section holds all Options for JRuby. They will be listed in the
    // --properties output.
    public static final Option<String> COMPILE_MODE = string(COMPILER, "compile.mode", new String[]{"JIT", "FORCE", "OFF", "OFFIR"}, "JIT", "Set compilation mode. JIT = at runtime; FORCE = before execution.");
    public static final Option<Boolean> COMPILE_DUMP = bool(COMPILER, "compile.dump", false, "Dump to console all bytecode generated at runtime.");
    public static final Option<Boolean> COMPILE_THREADLESS = bool(COMPILER, "compile.threadless", false, "(EXPERIMENTAL) Turn on compilation without polling for \"unsafe\" thread events.");
    public static final Option<Boolean> COMPILE_FASTOPS = bool(COMPILER, "compile.fastops", true, "Turn on fast operators for Fixnum and Float.");
    public static final Option<Integer> COMPILE_CHAINSIZE = integer(COMPILER, "compile.chainsize", Constants.CHAINED_COMPILE_LINE_COUNT_DEFAULT, "Set the number of lines at which compiled bodies are \"chained\".");
    public static final Option<Boolean> COMPILE_LAZYHANDLES = bool(COMPILER, "compile.lazyHandles", false, "Generate method bindings (handles) for compiled methods lazily.");
    public static final Option<Boolean> COMPILE_PEEPHOLE = bool(COMPILER, "compile.peephole", true, "Enable or disable peephole optimizations.");
    public static final Option<Boolean> COMPILE_NOGUARDS = bool(COMPILER, "compile.noguards", false, "Compile calls without guards, for experimentation.");
    public static final Option<Boolean> COMPILE_FASTEST = bool(COMPILER, "compile.fastest", false, "Compile with all \"mostly harmless\" compiler optimizations.");
    public static final Option<Boolean> COMPILE_FASTSEND = bool(COMPILER, "compile.fastsend", false, "Compile obj.__send__(<literal>, ...) as obj.<literal>(...).");
    public static final Option<Boolean> COMPILE_FASTMASGN = bool(COMPILER, "compile.fastMasgn", false, "Return true from multiple assignment instead of a new array.");
    public static final Option<Boolean> COMPILE_INVOKEDYNAMIC = bool(COMPILER, "compile.invokedynamic", INVOKEDYNAMIC_DEFAULT, "Use invokedynamic for optimizing Ruby code.");
    public static final Option<Integer> COMPILE_OUTLINE_CASECOUNT = integer(COMPILER, "compile.outline.casecount", 50, "Outline when bodies when number of cases exceeds this value.");

    public static final Option<Integer> INVOKEDYNAMIC_MAXFAIL = integer(INVOKEDYNAMIC, "invokedynamic.maxfail", 1000, "Maximum call site failures after which to inline cache.");
    public static final Option<Integer> INVOKEDYNAMIC_MAXPOLY = integer(INVOKEDYNAMIC, "invokedynamic.maxpoly", 6, "Maximum polymorphism of PIC binding.");
    public static final Option<Boolean> INVOKEDYNAMIC_LOG_BINDING = bool(INVOKEDYNAMIC, "invokedynamic.log.binding", false, "Log binding of invokedynamic call sites.");
    public static final Option<Boolean> INVOKEDYNAMIC_LOG_CONSTANTS = bool(INVOKEDYNAMIC, "invokedynamic.log.constants", false, "Log invokedynamic-based constant lookups.");
    public static final Option<Boolean> INVOKEDYNAMIC_LOG_GLOBALS = bool(INVOKEDYNAMIC, "invokedynamic.log.globals", false, "Log invokedynamic-based global lookups.");
    public static final Option<Boolean> INVOKEDYNAMIC_ALL = bool(INVOKEDYNAMIC, "invokedynamic.all", false, "Enable all possible uses of invokedynamic.");
    public static final Option<Boolean> INVOKEDYNAMIC_SAFE = bool(INVOKEDYNAMIC, "invokedynamic.safe", false, "Enable all safe (but maybe not fast) uses of invokedynamic.");
    public static final Option<Boolean> INVOKEDYNAMIC_INVOCATION = bool(INVOKEDYNAMIC, "invokedynamic.invocation", true, "Enable invokedynamic for method invocations.");
    public static final Option<Boolean> INVOKEDYNAMIC_INVOCATION_SWITCHPOINT = bool(INVOKEDYNAMIC, "invokedynamic.invocation.switchpoint", true, "Use SwitchPoint for class modification guards on invocations.");
    public static final Option<Boolean> INVOKEDYNAMIC_INVOCATION_INDIRECT = bool(INVOKEDYNAMIC, "invokedynamic.invocation.indirect", true, "Also bind indirect method invokers to invokedynamic.");
    public static final Option<Boolean> INVOKEDYNAMIC_INVOCATION_JAVA = bool(INVOKEDYNAMIC, "invokedynamic.invocation.java", true, "Bind Ruby to Java invocations with invokedynamic.");
    public static final Option<Boolean> INVOKEDYNAMIC_INVOCATION_ATTR = bool(INVOKEDYNAMIC, "invokedynamic.invocation.attr", true, "Bind Ruby attribue invocations directly to invokedynamic.");
    public static final Option<Boolean> INVOKEDYNAMIC_INVOCATION_FASTOPS = bool(INVOKEDYNAMIC, "invokedynamic.invocation.fastops", true, "Bind Fixnum and Float math using optimized logic.");
    public static final Option<Boolean> INVOKEDYNAMIC_CACHE = bool(INVOKEDYNAMIC, "invokedynamic.cache", true, "Use invokedynamic to load cached values like literals and constants.");
    public static final Option<Boolean> INVOKEDYNAMIC_CACHE_CONSTANTS = bool(INVOKEDYNAMIC, "invokedynamic.cache.constants", true, "Use invokedynamic to load constants.");
    public static final Option<Boolean> INVOKEDYNAMIC_CACHE_LITERALS = bool(INVOKEDYNAMIC, "invokedynamic.cache.literals", true, "Use invokedynamic to load literals.");
    public static final Option<Boolean> INVOKEDYNAMIC_CACHE_IVARS = bool(INVOKEDYNAMIC, "invokedynamic.cache.ivars", true, "Use invokedynamic to get/set instance variables.");
    public static final Option<Boolean> INVOKEDYNAMIC_CLASS_VALUES = bool(INVOKEDYNAMIC, "invokedynamic.class.values", false, "Use ClassValue to store class-specific data.");
    public static final Option<Integer> INVOKEDYNAMIC_GLOBAL_MAXFAIL = integer(INVOKEDYNAMIC, "invokedynamic.global.maxfail", 100, "Maximum global cache failures after which to use slow path.");
    public static final Option<Boolean> INVOKEDYNAMIC_HANDLES = bool(INVOKEDYNAMIC, "invokedynamic.handles", false, "Use MethodHandles rather than generated code to bind Ruby methods.");
    
    public static final Option<Integer> JIT_THRESHOLD = integer(JIT, "jit.threshold", Constants.JIT_THRESHOLD, "Set the JIT threshold to the specified method invocation count.");
    public static final Option<Integer> JIT_MAX = integer(JIT, "jit.max", Constants.JIT_MAX_METHODS_LIMIT, "Set the max count of active methods eligible for JIT-compilation.");
    public static final Option<Integer> JIT_MAXSIZE = integer(JIT, "jit.maxsize", Constants.JIT_MAX_SIZE_LIMIT, "Set the maximum full-class byte size allowed for jitted methods.");
    public static final Option<Boolean> JIT_LOGGING = bool(JIT, "jit.logging", false, "Enable JIT logging (reports successful compilation).");
    public static final Option<Boolean> JIT_LOGGING_VERBOSE = bool(JIT, "jit.logging.verbose", false, "Enable verbose JIT logging (reports failed compilation).");
    public static final Option<Boolean> JIT_DUMPING = bool(JIT, "jit.dumping", false, "Enable stdout dumping of JITed bytecode.");
    public static final Option<Integer> JIT_LOGEVERY = integer(JIT, "jit.logEvery", 0, "Log a message every n methods JIT compiled.");
    public static final Option<String> JIT_EXCLUDE = string(JIT, "jit.exclude", new String[]{"ClsOrMod","ClsOrMod::method_name","-::method_name"}, "", "Exclude methods from JIT. Comma delimited.");
    public static final Option<Boolean> JIT_CACHE = bool(JIT, "jit.cache", !COMPILE_INVOKEDYNAMIC.load(), "Cache jitted method in-memory bodies across runtimes and loads.");
    public static final Option<String> JIT_CODECACHE = string(JIT, "jit.codeCache", new String[]{"dir"}, "Save jitted methods to <dir> as they're compiled, for future runs.");
    public static final Option<Boolean> JIT_DEBUG = bool(JIT, "jit.debug", false, "Log loading of JITed bytecode.");
    public static final Option<Boolean> JIT_BACKGROUND = bool(JIT, "jit.background", true, "Run the JIT compiler in a background thread.");
    
    public static final Option<Boolean> IR_DEBUG             = bool(IR, "ir.debug", false, "Debug generation of JRuby IR.");
    public static final Option<Boolean> IR_PROFILE           = bool(IR, "ir.profile", false, "[EXPT]: Profile IR code during interpretation.");
    public static final Option<Boolean> IR_COMPILER_DEBUG    = bool(IR, "ir.compiler.debug", false, "Debug compilation of JRuby IR.");
    public static final Option<Boolean> IR_VISUALIZER        = bool(IR, "ir.visualizer", false, "Visualization of JRuby IR.");
    public static final Option<String>  IR_COMPILER_PASSES = string(IR, "ir.passes", "Specify comma delimeted list of passes to run.");
    public static final Option<String>  IR_INLINE_COMPILER_PASSES = string(IR, "ir.inline_passes", "Specify comma delimeted list of passes to run after inlining a method.");
    
    public static final Option<Boolean> NATIVE_ENABLED = bool(NATIVE, "native.enabled", true, "Enable/disable native code, including POSIX features and C exts.");
    public static final Option<Boolean> NATIVE_VERBOSE = bool(NATIVE, "native.verbose", false, "Enable verbose logging of native extension loading.");
    public static final Option<Boolean> CEXT_ENABLED = bool(NATIVE, "cext.enabled", false, "Enable or disable C extension support.");
    public static final Option<Boolean> FFI_COMPILE_DUMP = bool(NATIVE, "ffi.compile.dump", false, "Dump bytecode-generated FFI stubs to console.");
    public static final Option<Integer> FFI_COMPILE_THRESHOLD = integer(NATIVE, "ffi.compile.threshold", 100, "Number of FFI invocations before generating a bytecode stub.");
    public static final Option<Boolean> FFI_COMPILE_INVOKEDYNAMIC = bool(NATIVE, "ffi.compile.invokedynamic", false, "Use invokedynamic to bind FFI invocations.");
    public static final Option<Boolean> FFI_COMPILE_REIFY = bool(NATIVE, "ffi.compile.reify", false, "Reify FFI compiled classes.");
    
    public static final Option<Integer> TIMEOUT_THREADPOOL_MAX = integer(THREADPOOL, "timeout.thread.pool.max", Runtime.getRuntime().availableProcessors(), "The maximum number of threads to allow in the timeout pool.");

    public static final Option<Boolean> THREADPOOL_ENABLED = bool(THREADPOOL, "thread.pool.enabled", false, "Enable reuse of native threads via a thread pool.");
    public static final Option<Integer> THREADPOOL_MIN = integer(THREADPOOL, "thread.pool.min", 0, "The minimum number of threads to keep alive in the pool.");
    public static final Option<Integer> THREADPOOL_MAX = integer(THREADPOOL, "thread.pool.max", Integer.MAX_VALUE, "The maximum number of threads to allow in the pool.");
    public static final Option<Integer> THREADPOOL_TTL = integer(THREADPOOL, "thread.pool.ttl", 60, "The maximum number of seconds to keep alive an idle thread.");
    
    public static final Option<String> COMPAT_VERSION = string(MISCELLANEOUS, "compat.version", new String[]{"1.8","1.9","2.0"}, Constants.DEFAULT_RUBY_VERSION, "Specify the major Ruby version to be compatible with.");
    public static final Option<Boolean> OBJECTSPACE_ENABLED = bool(MISCELLANEOUS, "objectspace.enabled", false, "Enable or disable ObjectSpace.each_object.");
    public static final Option<Boolean> SIPHASH_ENABLED = bool(MISCELLANEOUS, "siphash.enabled", false, "Enable or disable SipHash for String hash function.");
    public static final Option<Boolean> LAUNCH_INPROC = bool(MISCELLANEOUS, "launch.inproc", false, "Set in-process launching of e.g. system('ruby ...').");
    public static final Option<String> BYTECODE_VERSION = string(MISCELLANEOUS, "bytecode.version", new String[]{"1.5","1.6","1.7"}, SafePropertyAccessor.getProperty("java.specification.version", "1.5"), "Specify the major Java bytecode version.");
    public static final Option<Boolean> MANAGEMENT_ENABLED = bool(MISCELLANEOUS, "management.enabled", false, "Set whether JMX management is enabled.");
    public static final Option<Boolean> JUMP_BACKTRACE = bool(MISCELLANEOUS, "jump.backtrace", false, "Make non-local flow jumps generate backtraces.");
    public static final Option<Boolean> PROCESS_NOUNWRAP = bool(MISCELLANEOUS, "process.noUnwrap", false, "Do not unwrap process streams (issue on some recent JVMs).");
    public static final Option<Boolean> REIFY_CLASSES = bool(MISCELLANEOUS, "reify.classes", false, "Before instantiation, stand up a real Java class for every Ruby class.");
    public static final Option<Boolean> REIFY_LOGERRORS = bool(MISCELLANEOUS, "reify.logErrors", false, "Log errors during reification (reify.classes=true).");
    public static final Option<Boolean> REFLECTED_HANDLES = bool(MISCELLANEOUS, "reflected.handles", false, "Use reflection for binding methods, not generated bytecode.");
    public static final Option<Boolean> BACKTRACE_COLOR = bool(MISCELLANEOUS, "backtrace.color", false, "Enable colorized backtraces.");
    public static final Option<String> BACKTRACE_STYLE = string(MISCELLANEOUS, "backtrace.style", new String[]{"normal","raw","full","mri"}, "normal", "Set the style of exception backtraces.");
    public static final Option<Boolean> BACKTRACE_MASK = bool(MISCELLANEOUS, "backtrace.mask", false, "Mask .java lines in Ruby backtraces.");
    public static final Option<String> THREAD_DUMP_SIGNAL = string(MISCELLANEOUS, "thread.dump.signal", new String[]{"USR1", "USR2", "etc"}, "USR2", "Set the signal used for dumping thread stacks.");
    public static final Option<Boolean> NATIVE_NET_PROTOCOL = bool(MISCELLANEOUS, "native.net.protocol", false, "Use native impls for parts of net/protocol.");
    public static final Option<Boolean> FIBER_COROUTINES = bool(MISCELLANEOUS, "fiber.coroutines", false, "Use JVM coroutines for Fiber.");
    public static final Option<Boolean> GLOBAL_REQUIRE_LOCK = bool(MISCELLANEOUS, "global.require.lock", false, "Use a single global lock for requires.");
    public static final Option<Boolean> NATIVE_EXEC = bool(MISCELLANEOUS, "native.exec", true, "Do a true process-obliterating native exec for Kernel#exec.");
    public static final Option<Boolean> ENUMERATOR_LIGHTWEIGHT = bool(MISCELLANEOUS, "enumerator.lightweight", true, "Use lightweight Enumerator#next logic when possible.");
    public static final Option<Boolean> CONSISTENT_HASHING = bool(MISCELLANEOUS, "consistent.hashing", false, "Generate consistent object hashes across JVMs");
    public static final Option<Boolean> REIFY_VARIABLES = bool(MISCELLANEOUS, "reify.variables", false, "Attempt to expand instance vars into Java fields");
    public static final Option<Boolean> PREFER_IPV4 = bool(MISCELLANEOUS, "net.preferIPv4", true, "Prefer IPv4 network stack");

    public static final Option<Boolean> DEBUG_LOADSERVICE = bool(DEBUG, "debug.loadService", false, "Log require/load file searches.");
    public static final Option<Boolean> DEBUG_LOADSERVICE_TIMING = bool(DEBUG, "debug.loadService.timing", false, "Log require/load parse+evaluate times.");
    public static final Option<Boolean> DEBUG_LAUNCH = bool(DEBUG, "debug.launch", false, "Log externally-launched processes.");
    public static final Option<Boolean> DEBUG_FULLTRACE = bool(DEBUG, "debug.fullTrace", false, "Set whether full traces are enabled (c-call/c-return).");
    public static final Option<Boolean> DEBUG_SCRIPTRESOLUTION = bool(DEBUG, "debug.scriptResolution", false, "Print which script is executed by '-S' flag.");
    public static final Option<Boolean> DEBUG_PARSER = bool(DEBUG, "debug.parser", false, "disables JRuby impl script loads and prints parse exceptions");
    public static final Option<Boolean> ERRNO_BACKTRACE = bool(DEBUG, "errno.backtrace", false, "Generate backtraces for heavily-used Errno exceptions (EAGAIN).");
    public static final Option<Boolean> STOPITERATION_BACKTRACE = bool(DEBUG, "stop_iteration.backtrace", false, "Generate backtraces for heavily-used Errno exceptions (EAGAIN).");
    public static final Option<Boolean> LOG_EXCEPTIONS = bool(DEBUG, "log.exceptions", false, "Log every time an exception is constructed.");
    public static final Option<Boolean> LOG_BACKTRACES = bool(DEBUG, "log.backtraces", false, "Log every time an exception backtrace is generated.");
    public static final Option<Boolean> LOG_CALLERS = bool(DEBUG, "log.callers", false, "Log every time a Kernel#caller backtrace is generated.");
    public static final Option<Boolean> LOG_WARNINGS = bool(DEBUG, "log.warnings", false, "Log every time a built-in warning backtrace is generated.");
    public static final Option<String> LOGGER_CLASS = string(DEBUG, "logger.class", new String[] {"class name"}, "org.jruby.util.log.StandardErrorLogger", "Use specified class for logging.");
    public static final Option<Boolean> DUMP_INSTANCE_VARS = bool(DEBUG, "dump.variables", false, "Dump class + instance var names on first new of Object subclasses.");
    public static final Option<Boolean> REWRITE_JAVA_TRACE = bool(DEBUG, "rewrite.java.trace", false, "Rewrite stack traces from exceptions raised in Java calls.");

    public static final Option<Boolean> JI_SETACCESSIBLE = bool(JAVA_INTEGRATION, "ji.setAccessible", true, "Try to set inaccessible Java methods to be accessible.");
    public static final Option<Boolean> JI_LOGCANSETACCESSIBLE = bool(JAVA_INTEGRATION, "ji.logCanSetAccessible", false, "Log whether setAccessible is working.");
    public static final Option<Boolean> JI_UPPER_CASE_PACKAGE_NAME_ALLOWED = bool(JAVA_INTEGRATION, "ji.upper.case.package.name.allowed", false, "Allow Capitalized Java pacakge names.");
    public static final Option<Boolean> INTERFACES_USEPROXY = bool(JAVA_INTEGRATION, "interfaces.useProxy", false, "Use java.lang.reflect.Proxy for interface impl.");
    public static final Option<Boolean> JAVA_HANDLES = bool(JAVA_INTEGRATION, "java.handles", false, "Use generated handles instead of reflection for calling Java.");
    public static final Option<Boolean> JI_NEWSTYLEEXTENSION = bool(JAVA_INTEGRATION, "ji.newStyleExtension", false, "Extend Java classes without using a proxy object.");
    public static final Option<Boolean> JI_OBJECTPROXYCACHE = bool(JAVA_INTEGRATION, "ji.objectProxyCache", true, "Cache Java object wrappers between calls.");
    public static final Option<String> JI_PROXYCLASSFACTORY = string(JAVA_INTEGRATION, "ji.proxyClassFactory", "Allow external envs to replace JI proxy class factory");

    public static final Option<Integer> PROFILE_MAX_METHODS = integer(PROFILING, "profile.max.methods", 100000, "Maximum number of methods to consider for profiling.");
    
    public static final Option<Boolean> CLI_AUTOSPLIT = bool(CLI, "cli.autosplit", false, "Split $_ into $F for -p or -n. Same as -a.");
    public static final Option<Boolean> CLI_DEBUG = bool(CLI, "cli.debug", false, "Enable debug mode logging. Same as -d.");
    public static final Option<Boolean> CLI_PROCESS_LINE_ENDS = bool(CLI, "cli.process.line.ends", false, "Enable line ending processing. Same as -l.");
    public static final Option<Boolean> CLI_ASSUME_LOOP = bool(CLI, "cli.assume.loop", false, "Wrap execution with a gets() loop. Same as -n.");
    public static final Option<Boolean> CLI_ASSUME_PRINT = bool(CLI, "cli.assume.print", false, "Print $_ after each execution of script. Same as -p.");
    public static final Option<Boolean> CLI_VERBOSE = bool(CLI, "cli.verbose", false, "Verbose mode, as -w or -W2. Sets default for cli.warning.level.");
    public static final Option<Verbosity> CLI_WARNING_LEVEL = enumeration(CLI, "cli.warning.level", Verbosity.class, CLI_VERBOSE.load() ? Verbosity.TRUE : Verbosity.FALSE, "Warning level (off=0,normal=1,on=2). Same as -W.");
    public static final Option<Boolean> CLI_PARSER_DEBUG = bool(CLI, "cli.parser.debug", false, "Enable parser debug logging. Same as -y.");
    public static final Option<Boolean> CLI_VERSION = bool(CLI, "cli.version", false, "Print version to stderr. Same as --version.");
    public static final Option<Boolean> CLI_BYTECODE = bool(CLI, "cli.bytecode", false, "Print target script bytecode to stderr. Same as --bytecode.");
    public static final Option<Boolean> CLI_COPYRIGHT = bool(CLI, "cli.copyright", false, "Print copyright to stderr. Same as --copyright but runs script.");
    public static final Option<Boolean> CLI_CHECK_SYNTAX = bool(CLI, "cli.check.syntax", false, "Check syntax of target script. Same as -c but runs script.");
    public static final Option<String> CLI_AUTOSPLIT_SEPARATOR = string(CLI, "cli.autosplit.separator", "Set autosplit separator. Same as -F.");
    public static final Option<KCode> CLI_KCODE = enumeration(CLI, "cli.kcode", KCode.class, KCode.NONE, "Set kcode character set. Same as -K (1.8).");
    public static final Option<Boolean> CLI_HELP = bool(CLI, "cli.help", false, "Print command-line usage. Same as --help but runs script.");
    public static final Option<Boolean> CLI_PROPERTIES = bool(CLI, "cli.properties", false, "Print config properties. Same as --properties but runs script.");
    public static final Option<String> CLI_ENCODING_INTERNAL = string(CLI, "cli.encoding.internal", "Encoding name to use internally.");
    public static final Option<String> CLI_ENCODING_EXTERNAL = string(CLI, "cli.encoding.external", "Encoding name to treat external data.");
    public static final Option<String> CLI_ENCODING_SOURCE = string(CLI, "cli.encoding.source", "Encoding name to treat source code.");
    public static final Option<String> CLI_RECORD_SEPARATOR = string(CLI, "cli.record.separator", "\n", "Default record separator.");
    public static final Option<String> CLI_BACKUP_EXTENSION = string(CLI, "cli.backup.extension", "Backup extension for in-place ARGV files. Same as -i.");
    public static final Option<ProfilingMode> CLI_PROFILING_MODE = enumeration(CLI, "cli.profiling.mode", ProfilingMode.class, ProfilingMode.OFF, "Enable instrumented profiling modes.");
    public static final Option<Boolean> CLI_RUBYGEMS_ENABLE = bool(CLI, "cli.rubygems.enable", true, "Enable/disable RubyGems.");
    public static final Option<Boolean> CLI_STRIP_HEADER = bool(CLI, "cli.strip.header", false, "Strip text before shebang in script. Same as -x.");
    public static final Option<Boolean> CLI_LOAD_GEMFILE = bool(CLI, "cli.load.gemfile", false, "Load a bundler Gemfile in cwd before running. Same as -G.");
    
    public static String dump() {
        return "# JRuby configuration options with current values\n" +
                Option.formatValues(_loadedOptions);
    }

    public static final Collection<Option> PROPERTIES = Collections.unmodifiableCollection(_loadedOptions);
    
    private static Option<String> string(Category category, String name, String[] options, String defval, String description) {
        Option<String> option = Option.string("jruby", name, category, options, defval, description);
        _loadedOptions.add(option);
        return option;
    }
    
    private static Option<String> string(Category category, String name, String defval, String description) {
        Option<String> option = Option.string("jruby", name, category, defval, description);
        _loadedOptions.add(option);
        return option;
    }
    
    private static Option<String> string(Category category, String name, String[] options, String description) {
        Option<String> option = Option.string("jruby", name, category, options, description);
        _loadedOptions.add(option);
        return option;
    }
    
    private static Option<String> string(Category category, String name, String description) {
        Option<String> option = Option.string("jruby", name, category, description);
        _loadedOptions.add(option);
        return option;
    }
    
    private static Option<Boolean> bool(Category category, String name, Boolean defval, String description) {
        Option<Boolean> option = Option.bool("jruby", name, category, defval, description);
        _loadedOptions.add(option);
        return option;
    }
    
    private static Option<Integer> integer(Category category, String name, Integer defval, String description) {
        Option<Integer> option = Option.integer("jruby", name, category, defval, description);
        _loadedOptions.add(option);
        return option;
    }
    
    private static <T extends Enum<T>> Option<T> enumeration(Category category, String name, Class<T> enumClass, T defval, String description) {
        Option<T> option = Option.enumeration("jruby", name, category, enumClass, defval, description);
        _loadedOptions.add(option);
        return option;
    }

    private static boolean calculateInvokedynamicDefault() {
        String javaVersion = SafePropertyAccessor.getProperty("java.specification.version", "");
        if (!javaVersion.equals("") && new BigDecimal(javaVersion).compareTo(new BigDecimal("1.8")) >= 0) {
            return true;
        } else {
            // on only if forced
            return false;
        }
    }
}
