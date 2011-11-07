package org.jruby.util.cli;

import org.jruby.runtime.Constants;
import org.jruby.util.SafePropertyAccessor;
import static org.jruby.util.cli.Options.Category.*;

public class Options {
    public static abstract class Option<T> {
        public Option(Category category, String name, Class<T> type, T[] options, T defval, String description) {
            this.category = category;
            this.name = name;
            this.type = type;
            this.options = options == null ? new String[]{type.getSimpleName()} : options;
            this.defval = defval;
            this.description = description;
        }
        
        @Override
        public String toString() {
            return "jruby." + name;
        }
        
        public final Category category;
        public final String name;
        public final Class type;
        public final Object[] options;
        public final T defval;
        public final String description;
    }
    
    public enum Category {
        COMPILER("compiler"),
        INVOKEDYNAMIC("invokedynamic"),
        JIT("jit"),
        IR("intermediate representation"),
        NATIVE("native"),
        THREADPOOL("thread pooling"),
        MISCELLANEOUS("miscellaneous"),
        DEBUG("debugging and logging"),
        JAVA_INTEGRATION("java integration");
        
        private final String desc;
        
        Category(String desc) {
            this.desc = desc;
        }
        
        public String desc() {
            return desc;
        }
        
        public String toString() {
            return desc;
        }
    }
    
    public static class StringOption extends Option<String> {
        public StringOption(Category category, String name, String[] options, String defval, String description) {
            super(category, name, String.class, options, defval, description);
        }
        
        public String load() {
            return SafePropertyAccessor.getProperty("jruby." + name, defval);
        }
    }
    
    public static class BooleanOption extends Option<Boolean> {
        public BooleanOption(Category category, String name, Boolean defval, String description) {
            super(category, name, Boolean.class, new Boolean[] {true, false}, defval, description);
        }
        
        public Boolean load() {
            return SafePropertyAccessor.getBoolean("jruby." + name, defval);
        }
    }
    
    public static class IntegerOption extends Option<Integer> {
        public IntegerOption(Category category, String name, Integer defval, String description) {
            super(category, name, Integer.class, null, defval, description);
        }
        
        public Integer load() {
            return SafePropertyAccessor.getInt("jruby." + name, defval);
        }
    }
    
    public static final StringOption COMPILE_MODE =
            new StringOption(COMPILER, "compile.mode", new String[]{"JIT", "FORCE", "OFF", "OFFIR"}, "JIT", "Set compilation mode. JIT = at runtime; FORCE = before execution.");
    public static final BooleanOption COMPILE_DUMP =
            new BooleanOption(COMPILER, "compile.dump", false, "Dump to console all bytecode generated at runtime.");
    public static final BooleanOption COMPILE_THREADLESS =
            new BooleanOption(COMPILER, "compile.threadless", false, "(EXPERIMENTAL) Turn on compilation without polling for \"unsafe\" thread events.");
    public static final BooleanOption COMPILE_DYNOPT =
            new BooleanOption(COMPILER, "compile.dynopt", false, "(EXPERIMENTAL) Use interpreter to help compiler make direct calls.");
    public static final BooleanOption COMPILE_FASTOPS =
            new BooleanOption(COMPILER, "compile.fastops", false, "Turn on fast operators for Fixnum and Float.");
    public static final IntegerOption COMPILE_CHAINSIZE =
            new IntegerOption(COMPILER, "compile.chainsize", Constants.CHAINED_COMPILE_LINE_COUNT_DEFAULT, "Set the number of lines at which compiled bodies are \"chained\".");
    public static final BooleanOption COMPILE_LAZYHANDLES =
            new BooleanOption(COMPILER, "compile.lazyHandles", false, "Generate method bindings (handles) for compiled methods lazily.");
    public static final BooleanOption COMPILE_PEEPHOLE =
            new BooleanOption(COMPILER, "compile.peephole", true, "Enable or disable peephole optimizations.");
    public static final BooleanOption COMPILE_NOGUARDS =
            new BooleanOption(COMPILER, "compile.noguards", false, "Compile calls without guards, for experimentation.");
    public static final BooleanOption COMPILE_FASTEST =
            new BooleanOption(COMPILER, "compile.fastest", false, "Compile with all \"mostly harmless\" compiler optimizations.");
    public static final BooleanOption COMPILE_FASTSEND =
            new BooleanOption(COMPILER, "compile.fastsend", false, "Compile obj.send(:sym, ...) as obj.sym(...).");
    public static final BooleanOption COMPILE_INLINEDYNCALLS =
            new BooleanOption(COMPILER, "compile.inlineDyncalls", false, "Emit method lookup + invoke inline in bytecode.");
    public static final BooleanOption COMPILE_FASTMASGN =
            new BooleanOption(COMPILER, "compile.fastMasgn", false, "Return true from multiple assignment instead of a new array.");
    public static final BooleanOption COMPILE_INVOKEDYNAMIC =
            new BooleanOption(COMPILER, "compile.invokedynamic", true, "Use invokedynamic on Java 7+.");
    
    public static final IntegerOption INVOKEDYNAMIC_MAXFAIL =
            new IntegerOption(INVOKEDYNAMIC, "invokedynamic.maxfail", 2, "Maximum size of invokedynamic PIC.");
    public static final BooleanOption INVOKEDYNAMIC_LOG_BINDING =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.log.binding", false, "Log binding of invokedynamic call sites.");
    public static final BooleanOption INVOKEDYNAMIC_LOG_CONSTANTS =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.log.constants", false, "Log invokedynamic-based constant lookups.");
    public static final BooleanOption INVOKEDYNAMIC_ALL =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.all", false, "Enable all possible uses of invokedynamic.");
    public static final BooleanOption INVOKEDYNAMIC_SAFE =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.safe", false, "Enable all safe (but maybe not fast) uses of invokedynamic.");
    public static final BooleanOption INVOKEDYNAMIC_INVOCATION =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.invocation", true, "Enable invokedynamic for method invocations.");
    public static final BooleanOption INVOKEDYNAMIC_INVOCATION_SWITCHPOINT =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.invocation.switchpoint", true, "Use SwitchPoint for class modification guards on invocations.");
    public static final BooleanOption INVOKEDYNAMIC_INVOCATION_INDIRECT =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.invocation.indirect", true, "Also bind indirect method invokers to invokedynamic.");
    public static final BooleanOption INVOKEDYNAMIC_INVOCATION_JAVA =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.invocation.java", false, "Bind Ruby to Java invocations with invokedynamic.");
    public static final BooleanOption INVOKEDYNAMIC_INVOCATION_ATTR =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.invocation.attr", true, "Bind Ruby attribue invocations directly to invokedynamic.");
    public static final BooleanOption INVOKEDYNAMIC_INVOCATION_FASTOPS =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.invocation.fastops", true, "Bind Fixnum and Float math using optimized logic.");
    public static final BooleanOption INVOKEDYNAMIC_CACHE =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.cache", true, "Use invokedynamic to load cached values like literals and constants.");
    public static final BooleanOption INVOKEDYNAMIC_CACHE_CONSTANTS =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.cache.constants", true, "Use invokedynamic to load constants.");
    public static final BooleanOption INVOKEDYNAMIC_CACHE_LITERALS =
            new BooleanOption(INVOKEDYNAMIC, "invokedynamic.cache.literals", true, "Use invokedynamic to load literals.");
    
    public static final IntegerOption JIT_THRESHOLD =
            new IntegerOption(JIT, "jit.threshold", Constants.JIT_THRESHOLD, "Set the JIT threshold to the specified method invocation count.");
    public static final IntegerOption JIT_MAX =
            new IntegerOption(JIT, "jit.max", Constants.JIT_MAX_METHODS_LIMIT, "Set the max count of active methods eligible for JIT-compilation.");
    public static final IntegerOption JIT_MAXSIZE =
            new IntegerOption(JIT, "jit.maxsize", Constants.JIT_MAX_SIZE_LIMIT, "Set the maximum full-class byte size allowed for jitted methods.");
    public static final BooleanOption JIT_LOGGING =
            new BooleanOption(JIT, "jit.logging", false, "Enable JIT logging (reports successful compilation).");
    public static final BooleanOption JIT_LOGGING_VERBOSE =
            new BooleanOption(JIT, "jit.logging.verbose", false, "Enable verbose JIT logging (reports failed compilation).");
    public static final BooleanOption JIT_DUMPING =
            new BooleanOption(JIT, "jit.dumping", false, "Enable stdout dumping of JITed bytecode.");
    public static final IntegerOption JIT_LOGEVERY =
            new IntegerOption(JIT, "jit.logEvery", 0, "Log a message every n methods JIT compiled.");
    public static final StringOption JIT_EXCLUDE =
            new StringOption(JIT, "jit.exclude", new String[]{"ClsOrMod","ClsOrMod::method_name","-::method_name"}, "none", "Exclude methods from JIT. Comma delimited.");
    public static final BooleanOption JIT_CACHE =
            new BooleanOption(JIT, "jit.cache", true, "Cache jitted method in-memory bodies across runtimes and loads.");
    public static final StringOption JIT_CODECACHE =
            new StringOption(JIT, "jit.codeCache", new String[]{"dir"}, null, "Save jitted methods to <dir> as they're compiled, for future runs.");
    public static final BooleanOption JIT_DEBUG =
            new BooleanOption(JIT, "jit.debug", false, "Log loading of JITed bytecode.");
    
    public static final BooleanOption IR_DEBUG =
            new BooleanOption(IR, "ir.debug", false, "Debug generation of JRuby IR.");
    public static final BooleanOption IR_COMPILER_DEBUG =
            new BooleanOption(IR, "ir.compiler.debug", false, "Debug compilation of JRuby IR.");
    public static final BooleanOption IR_PASS_LIVEVARIABLE =
            new BooleanOption(IR, "ir.pass.live_variable", false, "Enable live variable analysis of IR.");
    public static final BooleanOption IR_PASS_DEADCODE =
            new BooleanOption(IR, "ir.pass.dead_code", false, "Enable dead code elimination in IR.");
    public static final StringOption IR_PASS_TESTINLINER =
            new StringOption(IR, "ir.pass.test_inliner", null, "none", "Use specified class for inlining pass in IR.");
    
    public static final BooleanOption NATIVE_ENABLED =
            new BooleanOption(NATIVE, "native.enabled", true, "Enable/disable native code, including POSIX features and C exts.");
    public static final BooleanOption NATIVE_VERBOSE =
            new BooleanOption(NATIVE, "native.verbose", false, "Enable verbose logging of native extension loading.");
    public static final BooleanOption CEXT_ENABLED =
            new BooleanOption(NATIVE, "cext.enabled", true, "Enable or disable C extension support.");
    public static final BooleanOption FFI_COMPILE_DUMP =
            new BooleanOption(NATIVE, "ffi.compile.dump", false, "Dump bytecode-generated FFI stubs to console.");
    public static final IntegerOption FFI_COMPILE_THRESHOLD =
            new IntegerOption(NATIVE, "ffi.compile.threshold", 100, "Number of FFI invocations before generating a bytecode stub.");
    public static final BooleanOption FFI_COMPILE_INVOKEDYNAMIC =
            new BooleanOption(NATIVE, "ffi.compile.invokedynamic", false, "Use invokedynamic to bind FFI invocations.");
    
    public static final BooleanOption THREADPOOL_ENABLED =
            new BooleanOption(THREADPOOL, "thread.pool.enabled", false, "Enable reuse of native threads via a thread pool.");
    public static final IntegerOption THREADPOOL_MIN =
            new IntegerOption(THREADPOOL, "thread.pool.min", 0, "The minimum number of threads to keep alive in the pool.");
    public static final IntegerOption THREADPOOL_MAX =
            new IntegerOption(THREADPOOL, "thread.pool.max", Integer.MAX_VALUE, "The maximum number of threads to allow in the pool.");
    public static final IntegerOption THREADPOOL_TTL =
            new IntegerOption(THREADPOOL, "thread.pool.ttl", 60, "The maximum number of seconds to keep alive an idle thread.");
    
    public static final StringOption COMPAT_VERSION =
            new StringOption(MISCELLANEOUS, "compat.version", new String[]{"1.8","1.9"}, Constants.DEFAULT_RUBY_VERSION, "Specify the major Ruby version to be compatible with.");
    public static final BooleanOption OBJECTSPACE_ENABLED =
            new BooleanOption(MISCELLANEOUS, "objectspace.enabled", false, "Enable or disable ObjectSpace.each_object.");
    public static final BooleanOption LAUNCH_INPROC =
            new BooleanOption(MISCELLANEOUS, "launch.inproc", false, "Set in-process launching of e.g. system('ruby ...').");
    public static final StringOption BYTECODE_VERSION =
            new StringOption(MISCELLANEOUS, "bytecode.version", new String[]{"1.5","1.6","1.7"},
            SafePropertyAccessor.getProperty("java.specification.version", "1.5"), "Specify the major Ruby version to be compatible with.");
    public static final BooleanOption MANAGEMENT_ENABLED =
            new BooleanOption(MISCELLANEOUS, "management.enabled", false, "Set whether JMX management is enabled.");
    public static final BooleanOption JUMP_BACKTRACE =
            new BooleanOption(MISCELLANEOUS, "jump.backtrace", false, "Make non-local flow jumps generate backtraces.");
    public static final BooleanOption PROCESS_NOUNWRAP =
            new BooleanOption(MISCELLANEOUS, "process.noUnwrap", false, "Do not unwrap process streams (issue on some recent JVMs).");
    public static final BooleanOption REIFY_CLASSES =
            new BooleanOption(MISCELLANEOUS, "reify.classes", false, "Before instantiation, stand up a real Java class for ever Ruby class.");
    public static final BooleanOption REIFY_LOGERRORS =
            new BooleanOption(MISCELLANEOUS, "reify.logErrors", false, "Log errors during reification (reify.classes=true).");
    public static final BooleanOption REFLECTED_HANDLES =
            new BooleanOption(MISCELLANEOUS, "reflected.handles", false, "Use reflection for binding methods, not generated bytecode.");
    public static final BooleanOption BACKTRACE_COLOR =
            new BooleanOption(MISCELLANEOUS, "backtrace.color", false, "Enable colorized backtraces.");
    public static final StringOption BACKTRACE_STYLE =
            new StringOption(MISCELLANEOUS, "backtrace.style", new String[]{"normal","raw","full","mri","rubinius"}, "normal", "Set the style of exception backtraces.");
    public static final StringOption THREAD_DUMP_SIGNAL =
            new StringOption(MISCELLANEOUS, "thread.dump.signal", new String[]{"USR1", "USR2", "etc"}, "USR2", "Set the signal used for dumping thread stacks.");
    public static final BooleanOption NATIVE_NET_PROTOCOL =
            new BooleanOption(MISCELLANEOUS, "native.net.protocol", false, "Use native impls for parts of net/protocol.");
    public static final BooleanOption FIBER_COROUTINES =
            new BooleanOption(MISCELLANEOUS, "fiber.coroutines", false, "Use JVM coroutines for Fiber.");
    
    public static final BooleanOption DEBUG_LOADSERVICE =
            new BooleanOption(DEBUG, "debug.loadService", false, "Log require/load file searches.");
    public static final BooleanOption DEBUG_LOADSERVICE_TIMING =
            new BooleanOption(DEBUG, "debug.loadService.timing", false, "Log require/load parse+evaluate times.");
    public static final BooleanOption DEBUG_LAUNCH =
            new BooleanOption(DEBUG, "debug.launch", false, "Log externally-launched processes.");
    public static final BooleanOption DEBUG_FULLTRACE =
            new BooleanOption(DEBUG, "debug.fullTrace", false, "Set whether full traces are enabled (c-call/c-return).");
    public static final BooleanOption DEBUG_SCRIPTRESOLUTION =
            new BooleanOption(DEBUG, "debug.scriptResolution", false, "Print which script is executed by '-S' flag.");
    public static final BooleanOption ERRNO_BACKTRACE =
            new BooleanOption(DEBUG, "errno.backtrace", false, "Generate backtraces for heavily-used Errno exceptions (EAGAIN).");
    public static final BooleanOption LOG_EXCEPTIONS =
            new BooleanOption(DEBUG, "log.exceptions", false, "Log every time an exception is constructed.");
    public static final BooleanOption LOG_BACKTRACES =
            new BooleanOption(DEBUG, "log.backtraces", false, "Log every time an exception backtrace is generated.");
    public static final BooleanOption LOG_CALLERS =
            new BooleanOption(DEBUG, "log.callers", false, "Log every time a Kernel#caller backtrace is generated.");
    public static final StringOption LOGGER_CLASS =
            new StringOption(DEBUG, "logger.class", new String[] {"class name"}, "org.jruby.util.log.JavaUtilLoggingLogger", "Use specified class for logging.");
    
    public static final BooleanOption JI_SETACCESSIBLE =
            new BooleanOption(JAVA_INTEGRATION, "ji.setAccessible", true, "Try to set inaccessible Java methods to be accessible.");
    public static final BooleanOption JI_LOGCANSETACCESSIBLE =
            new BooleanOption(JAVA_INTEGRATION, "ji.logCanSetAccessible", false, "Log whether setAccessible is working.");
    public static final BooleanOption JI_UPPER_CASE_PACKAGE_NAME_ALLOWED =
            new BooleanOption(JAVA_INTEGRATION, "ji.upper.case.package.name.allowed", false, "Allow Capitalized Java pacakge names.");
    public static final BooleanOption INTERFACES_USEPROXY =
            new BooleanOption(JAVA_INTEGRATION, "interfaces.useProxy", false, "Use java.lang.reflect.Proxy for interface impl.");
    public static final BooleanOption JAVA_HANDLES =
            new BooleanOption(JAVA_INTEGRATION, "java.handles", false, "Use generated handles instead of reflection for calling Java.");
    public static final BooleanOption JI_NEWSTYLEEXTENSION =
            new BooleanOption(JAVA_INTEGRATION, "ji.newStyleExtension", false, "Extend Java classes without using a proxy object.");
    public static final BooleanOption JI_OBJECTPROXYCACHE =
            new BooleanOption(JAVA_INTEGRATION, "ji.objectProxyCache", true, "Cache Java object wrappers between calls.");

    public static final Option[] PROPERTIES = {
            COMPILE_MODE, COMPILE_DUMP, COMPILE_THREADLESS, COMPILE_DYNOPT, COMPILE_FASTOPS,
            COMPILE_CHAINSIZE, COMPILE_LAZYHANDLES, COMPILE_PEEPHOLE, COMPILE_NOGUARDS,
            COMPILE_FASTEST, COMPILE_FASTSEND, COMPILE_INLINEDYNCALLS, COMPILE_FASTMASGN,
            COMPILE_INVOKEDYNAMIC,
            
            INVOKEDYNAMIC_MAXFAIL, INVOKEDYNAMIC_LOG_BINDING, INVOKEDYNAMIC_LOG_CONSTANTS,
            INVOKEDYNAMIC_ALL, INVOKEDYNAMIC_SAFE, INVOKEDYNAMIC_INVOCATION, INVOKEDYNAMIC_INVOCATION_SWITCHPOINT,
            INVOKEDYNAMIC_INVOCATION_INDIRECT, INVOKEDYNAMIC_INVOCATION_JAVA, INVOKEDYNAMIC_INVOCATION_ATTR,
            INVOKEDYNAMIC_INVOCATION_FASTOPS, INVOKEDYNAMIC_CACHE, INVOKEDYNAMIC_CACHE_CONSTANTS,
            INVOKEDYNAMIC_CACHE_LITERALS,
            
            JIT_THRESHOLD, JIT_MAX, JIT_MAXSIZE, JIT_LOGGING, JIT_LOGGING_VERBOSE,
            JIT_DUMPING, JIT_LOGEVERY, JIT_EXCLUDE, JIT_CACHE, JIT_CODECACHE,
            JIT_DEBUG,
            
            IR_DEBUG, IR_COMPILER_DEBUG, IR_PASS_LIVEVARIABLE, IR_PASS_DEADCODE, IR_PASS_TESTINLINER,
            
            NATIVE_ENABLED, NATIVE_VERBOSE, CEXT_ENABLED, FFI_COMPILE_DUMP, FFI_COMPILE_THRESHOLD, FFI_COMPILE_INVOKEDYNAMIC,
            
            THREADPOOL_ENABLED, THREADPOOL_MIN, THREADPOOL_MAX, THREADPOOL_TTL,
            
            COMPAT_VERSION, OBJECTSPACE_ENABLED, LAUNCH_INPROC, BYTECODE_VERSION,
            MANAGEMENT_ENABLED, JUMP_BACKTRACE, PROCESS_NOUNWRAP, REIFY_CLASSES,
            REIFY_LOGERRORS, REFLECTED_HANDLES, BACKTRACE_COLOR, BACKTRACE_STYLE,
            THREAD_DUMP_SIGNAL, NATIVE_NET_PROTOCOL, FIBER_COROUTINES,
            
            DEBUG_LOADSERVICE, DEBUG_LOADSERVICE_TIMING, DEBUG_LAUNCH, DEBUG_FULLTRACE,
            DEBUG_SCRIPTRESOLUTION, ERRNO_BACKTRACE, LOG_EXCEPTIONS, LOG_BACKTRACES,
            LOG_CALLERS, LOGGER_CLASS,
            
            JI_SETACCESSIBLE, JI_UPPER_CASE_PACKAGE_NAME_ALLOWED, INTERFACES_USEPROXY,
            JAVA_HANDLES, JI_NEWSTYLEEXTENSION, JI_OBJECTPROXYCACHE
    };    
}
